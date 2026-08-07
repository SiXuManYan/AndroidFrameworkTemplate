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
 * Process-wide WebSocket connection manager with fixed-interval reconnection after a connection
 * has opened successfully.
 *
 * It exposes connection state and the latest text message as [StateFlow], shares the HTTP TLS
 * configuration, and ignores duplicate connection requests for the same server.
 * - 中文：管理单例 WebSocket 连接、状态流、最新消息与异常断线重连。
 *
 * ## Usage
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
 */
class WebSocketManager private constructor() {

    companion object {
        private const val TAG = "WebSocket"

        @Volatile
        private var INSTANCE: WebSocketManager? = null

        /** Returns the process-wide manager instance. */
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

    /** Observable state of the current connection attempt. */
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _messageFlow = MutableStateFlow<String?>(null)

    /**
     * Most recently received message represented as text, or `null` before the first message.
     *
     * Binary messages are decoded as UTF-8. This is state, not an event queue: identical
     * consecutive messages may not emit again.
     */
    val messageFlow: StateFlow<String?> = _messageFlow

    /** Host retained for duplicate detection and automatic reconnect. */
    private var currentConnectedIp: String? = null

    /** Port retained for duplicate detection and automatic reconnect. */
    private var currentConnectedPort: String? = null

    /** Optional device serial number reused during reconnect. */
    private var currentConnectedSnNumber: String? = null

    /** Represents the observable lifecycle of a WebSocket connection. */
    sealed class ConnectionState {
        /** No active connection or connection attempt. */
        object Disconnected : ConnectionState()

        /** A handshake is currently in progress. */
        object Connecting : ConnectionState()

        /** The WebSocket handshake completed successfully. */
        object Connected : ConnectionState()

        /**
         * The latest connection attempt failed.
         *
         * @property message human-readable failure detail
         */
        data class Error(val message: String) : ConnectionState()
    }

    /**
     * Connects to the configured WebSocket endpoint.
     *
     * Debug builds use `ws`; release builds use `wss`. Calling this method again with the same host
     * and port is ignored while connecting or connected unless [forceReconnect] is `true`.
     *
     * @param ip host or IP without a scheme
     * @param port server port
     * @param snNumber optional device serial number sent in the handshake header
     * @param forceReconnect closes and recreates an existing connection to the same server
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
     * Closes the current socket and resets state to [ConnectionState.Disconnected].
     *
     * @param stopReconnect when `true`, forgets the endpoint and terminates automatic reconnect;
     * internal reconnect flows may pass `false` to retain endpoint state
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
     * Sends a UTF-8 text [message] through the current socket.
     *
     * @return `true` when OkHttp accepted the message for transmission
     */
    fun sendMessage(message: String): Boolean {
        return webSocket?.send(message) ?: false
    }

    /** Starts the single reconnect monitor for the retained endpoint. */
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
     * Permanently releases the socket and reconnect coroutine for this process.
     *
     * Because the manager is a singleton, call this only during terminal process-level cleanup;
     * normal screens should use [disconnect].
     */
    fun release() {
        disconnect()
        scope.cancel()
    }
}
