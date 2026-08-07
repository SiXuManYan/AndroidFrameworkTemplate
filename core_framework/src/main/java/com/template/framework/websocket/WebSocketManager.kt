package com.template.framework.websocket

import com.template.framework.Framework
import com.template.framework.api.NetworkModule
import com.template.framework.api.model.websocket.WebSocketParams
import com.template.framework.constants.FrameworkConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * WebSocket 管理器（单例）
 *
 * 特性：
 * - 异常断线后按固定间隔自动重连
 * - 状态 / 消息以 [StateFlow] 形式暴露，方便 ViewModel 收集
 * - 复用 NetworkModule 的 SSL 配置（HTTPS / WSS 共享）
 * - 同一服务器多次调用 connect 不会重复连接
 *
 * 使用示例：
 * ```kotlin
 * val manager = WebSocketManager.getInstance()
 *
 * // 1) 连接
 * manager.connect(ip = "192.168.1.1", port = "8080", snNumber = "SN001")
 *
 * // 2) 监听状态
 * lifecycleScope.launch {
 *     manager.connectionState.collect { state -> ... }
 * }
 *
 * // 3) 监听消息
 * lifecycleScope.launch {
 *     manager.messageFlow.collect { message -> ... }
 * }
 *
 * // 4) 发送消息
 * manager.sendMessage("""{"type":"ping"}""")
 *
 * // 5) 断开
 * manager.disconnect()
 * ```
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
class WebSocketManager private constructor() {

    companion object {
        private const val TAG = "WebSocket"

        @Volatile
        private var INSTANCE: WebSocketManager? = null

        fun getInstance(): WebSocketManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebSocketManager().also { INSTANCE = it }
            }
        }
    }

    private var webSocket: WebSocket? = null
    private var client: OkHttpClient? = null
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _messageFlow = MutableStateFlow<String?>(null)
    val messageFlow: StateFlow<String?> = _messageFlow

    /** 当前连接的 IP */
    private var currentConnectedIp: String? = null

    /** 当前连接的端口 */
    private var currentConnectedPort: String? = null

    /** 当前连接的 SN 号 */
    private var currentConnectedSnNumber: String? = null

    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object Connecting : ConnectionState()
        object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    /**
     * 连接 WebSocket
     *
     * @param ip 服务器 IP
     * @param port 服务器端口
     * @param snNumber 设备 SN 号（可选，会作为 Header 传递）
     * @param forceReconnect 是否强制重连（即使已连接到相同服务器），默认 false
     */
    fun connect(ip: String, port: String, snNumber: String? = null, forceReconnect: Boolean = false) {
        val currentState = _connectionState.value

        // 已连接相同服务器，跳过
        if (!forceReconnect && currentState is ConnectionState.Connected &&
            currentConnectedIp == ip && currentConnectedPort == port
        ) {
            Timber.tag(TAG).d("WebSocket 已连接 $ip:$port，跳过")
            return
        }
        // 正在连接相同服务器，跳过
        if (!forceReconnect && currentState is ConnectionState.Connecting &&
            currentConnectedIp == ip && currentConnectedPort == port
        ) {
            Timber.tag(TAG).d("WebSocket 正在连接 $ip:$port，跳过")
            return
        }

        val serverChanged = currentConnectedIp != ip || currentConnectedPort != port
        if (serverChanged || forceReconnect) {
            if (serverChanged) {
                disconnect()
            } else {
                reconnectJob?.cancel()
                webSocket?.close(1000, "Force reconnect")
                webSocket = null
                client = null
            }
        }

        currentConnectedIp = ip
        currentConnectedPort = port
        currentConnectedSnNumber = snNumber

        val scheme = if (Framework.getConfig().debug) "ws" else "wss"
        val apiPrefix = if (Framework.getConfig().debug) {
            Framework.getConfig().debugApiPrefix
        } else {
            Framework.getConfig().releaseApiPrefix
        }
        val url = "$scheme://$ip:$port/$apiPrefix/${WebSocketParams.Path.value}"

        Timber.tag(TAG).d("连接 WebSocket: $url")
        _connectionState.value = ConnectionState.Connecting

        val builder = OkHttpClient.Builder()
            .connectTimeout(FrameworkConstants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(FrameworkConstants.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(FrameworkConstants.WRITE_TIMEOUT, TimeUnit.SECONDS)
        NetworkModule.configureSslSocketFactory(builder)
        client = builder.build()

        val requestBuilder = Request.Builder()
            .url(url)
            .header(WebSocketParams.HeaderClientId.value, com.template.framework.api.TokenInterceptor.DEFAULT_CLIENT_ID)
        val cfg = Framework.getConfig()
        requestBuilder.header(FrameworkConstants.HEADER_VERSION_CODE, cfg.versionCode.toString())
        requestBuilder.header(FrameworkConstants.HEADER_VERSION_NAME, cfg.versionName)
        if (!snNumber.isNullOrEmpty()) {
            requestBuilder.header(WebSocketParams.HeaderSnNumber.value, snNumber)
        }

        webSocket = client?.newWebSocket(requestBuilder.build(), createWebSocketListener())
    }

    /**
     * 断开连接
     *
     * @param stopReconnect 是否停止自动重连，默认 true
     */
    fun disconnect(stopReconnect: Boolean = true) {
        if (stopReconnect) {
            reconnectJob?.cancel()
            reconnectJob = null
        }
        webSocket?.close(1000, "Normal closure")
        webSocket = null
        client = null
        if (stopReconnect) {
            currentConnectedIp = null
            currentConnectedPort = null
            currentConnectedSnNumber = null
        }
        _connectionState.value = ConnectionState.Disconnected
    }

    /**
     * 发送消息
     *
     * @return 是否发送成功（false 表示未连接或连接已关闭）
     */
    fun sendMessage(message: String): Boolean {
        return webSocket?.send(message) ?: false
    }

    /**
     * 启动自动重连协程
     */
    private fun startReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            while (currentConnectedIp != null && currentConnectedPort != null) {
                val state = _connectionState.value
                if (state is ConnectionState.Disconnected || state is ConnectionState.Error) {
                    val ip = currentConnectedIp ?: break
                    val port = currentConnectedPort ?: break
                    val snNumber = currentConnectedSnNumber
                    Timber.tag(TAG).d("WebSocket 断开，尝试自动重连 $ip:$port")
                    delay(FrameworkConstants.WS_RECONNECT_INTERVAL)
                    connect(ip, port, snNumber)
                } else {
                    delay(FrameworkConstants.WS_RECONNECT_INTERVAL)
                }
            }
        }
    }

    private fun createWebSocketListener(): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Timber.tag(TAG).d("WebSocket connected")
                _connectionState.value = ConnectionState.Connected
                startReconnect()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Timber.tag(TAG).d("Received: $text")
                _messageFlow.value = text
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                _messageFlow.value = bytes.utf8()
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Timber.tag(TAG).d("WebSocket closing: $code $reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Timber.tag(TAG).d("WebSocket closed: $code $reason")
                if (currentConnectedIp != null && currentConnectedPort != null) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Timber.tag(TAG).e(t, "WebSocket failure: ${t.message}")
                if (currentConnectedIp != null && currentConnectedPort != null) {
                    _connectionState.value = ConnectionState.Error(t.message ?: "Unknown error")
                }
            }
        }
    }

    /**
     * 释放资源
     */
    fun release() {
        disconnect()
        scope.cancel()
    }
}
