package com.template.framework.repository

import android.content.Context
import com.template.framework.api.ApiService
import com.template.framework.api.FrameworkConfig
import com.template.framework.api.NetworkModule
import com.template.framework.api.model.ApiResponse
import com.template.framework.api.model.auth.DeviceLoginRequest
import com.template.framework.api.model.auth.LoginDataResponse
import com.template.framework.api.model.production.LineAndPostResponse
import com.template.framework.api.model.production.SaveProductsRequest
import com.template.framework.api.model.production.SaveProductsResponse
import com.template.framework.datastore.PreferencesManager
import com.template.framework.websocket.WebSocketManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * 框架仓库（单例）
 *
 * 负责：
 * 1. ApiService 缓存管理（按 (IP, Port) 缓存，配置变化时重建）
 * 2. WebSocket 连接/断开/消息发送
 * 3. 暴露 PreferencesManager 委托方法
 * 4. 暴露默认的 3 个示例 API
 *
 * ## ApiService 缓存策略
 * - 首次调用 [getApiService] 时从 [PreferencesManager] 读取当前 IP/Port，创建 ApiService
 * - 后续调用若 IP/Port 未变化，直接复用缓存实例
 * - IP/Port 变化或调用 [clearApiServiceCache] 时，下次调用会重新创建
 *
 * ## 自定义 ApiService
 * ```kotlin
 * interface MyApi {
 *     @GET("custom") suspend fun getCustom(): ApiResponse<MyData>
 * }
 *
 * // 在自己的 Repository 中：
 * class MyRepository(...) : FrameworkRepository(...) {
 *     private suspend fun myApi(): MyApi = NetworkModule.createApiService<MyApi>(currentBaseUrl(), getToken, clearToken)
 * }
 * ```
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
open class FrameworkRepository(
    context: Context,
    protected val preferencesManager: PreferencesManager
) {

    private val appContext = context.applicationContext

    private val webSocketManager = WebSocketManager.getInstance()

    // region [缓存配置]

    @Volatile
    protected var cachedApiService: ApiService? = null

    @Volatile
    protected var cachedServerIp: String? = null

    @Volatile
    protected var cachedServerPort: String? = null

    // endregion

    // region [Token 失效回调]

    companion object {
        /**
         * Token 失效全局回调，由 [com.template.framework.Framework.setOnTokenExpired] 注入
         */
        @Volatile
        var onTokenExpiredCallback: (() -> Unit)? = null
    }

    init {
        NetworkModule.onTokenExpired = { onTokenExpiredCallback?.invoke() }
    }

    // region [ApiService 缓存管理]

    /**
     * 获取 ApiService 实例（带缓存）
     */
    suspend fun getApiService(): ApiService {
        val currentIp = preferencesManager.serverIp.first()
        val currentPort = preferencesManager.serverPort.first()

        if (cachedApiService != null && cachedServerIp == currentIp && cachedServerPort == currentPort) {
            return cachedApiService!!
        }

        cachedServerIp = currentIp
        cachedServerPort = currentPort
        cachedApiService = NetworkModule.createApiService(
            baseUrl = buildBaseUrl(currentIp, currentPort),
            getToken = { preferencesManager.accessToken.first() },
            clearToken = { preferencesManager.clearAccessToken() }
        )
        return cachedApiService!!
    }

    /**
     * 清除 ApiService 缓存（IP/Port 改变后调用，下次 getApiService 会重建）
     */
    fun clearApiServiceCache() {
        cachedApiService = null
    }

    /**
     * 构建基础 URL
     * - Debug 模式：http://{ip}:{port}/{debugApiPrefix}/
     * - Release 模式：https://{ip}:{port}/{releaseApiPrefix}/
     */
    protected fun buildBaseUrl(ip: String, port: String): String {
        val config = com.template.framework.Framework.getConfig()
        val scheme = if (config.debug) "http://" else "https://"
        val apiPrefix = if (config.debug) config.debugApiPrefix else config.releaseApiPrefix
        return "$scheme$ip:$port/$apiPrefix/"
    }

    /**
     * 获取当前 BaseUrl（用于自定义 ApiService）
     */
    suspend fun getCurrentBaseUrl(): String {
        val ip = preferencesManager.serverIp.first()
        val port = preferencesManager.serverPort.first()
        return buildBaseUrl(ip, port)
    }

    // endregion

    // region [PreferencesManager 委托]

    val serverIp: Flow<String> = preferencesManager.serverIp
    val serverPort: Flow<String> = preferencesManager.serverPort
    val accessToken: Flow<String?> = preferencesManager.accessToken
    val language: Flow<String> = preferencesManager.language

    suspend fun saveServerIp(ip: String) = preferencesManager.saveServerIp(ip)
    suspend fun saveServerPort(port: String) = preferencesManager.saveServerPort(port)
    suspend fun saveAccessToken(token: String) = preferencesManager.saveAccessToken(token)
    suspend fun clearAccessToken() = preferencesManager.clearAccessToken()

    // endregion

    // region [示例 API]

    /**
     * 示例 1：登录（POST + @Body + ApiResponse<LoginDataResponse>）
     */
    suspend fun login(request: DeviceLoginRequest): ApiResponse<LoginDataResponse> {
        return getApiService().deviceLogin(request)
    }

    /**
     * 示例 2：获取列表（GET + ApiResponse<List<T>>）
     */
    suspend fun getLineAndPost(): ApiResponse<List<LineAndPostResponse>> {
        return getApiService().getLineAndPost()
    }

    /**
     * 示例 3：保存数据（POST + @Body + ApiResponse<T>）
     */
    suspend fun saveProducts(request: SaveProductsRequest): ApiResponse<SaveProductsResponse> {
        return getApiService().saveProducts(request)
    }

    // endregion

    // region [WebSocket 操作]

    /**
     * 连接 WebSocket
     */
    fun connectWebSocket(ip: String? = null, port: String? = null, snNumber: String? = null) {
        kotlinx.coroutines.runBlocking {
            val realIp = ip ?: preferencesManager.serverIp.first()
            val realPort = port ?: preferencesManager.serverPort.first()
            webSocketManager.connect(realIp, realPort, snNumber)
        }
    }

    /**
     * 断开 WebSocket
     */
    fun disconnectWebSocket() = webSocketManager.disconnect()

    /**
     * 发送 WebSocket 消息
     */
    fun sendWebSocketMessage(message: String) = webSocketManager.sendMessage(message)

    /** WebSocket 连接状态 */
    val webSocketConnectionState = webSocketManager.connectionState

    /** WebSocket 消息流 */
    val webSocketMessageFlow = webSocketManager.messageFlow

    // endregion
}