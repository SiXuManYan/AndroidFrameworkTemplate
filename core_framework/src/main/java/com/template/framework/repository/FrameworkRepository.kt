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
 * Coordinates server preferences, the cached sample API, and the shared WebSocket manager.
 *
 * [com.template.framework.Framework] owns one default instance, while App repositories may extend
 * this class. [getApiService] caches by server IP and port and rebuilds when either value changes.
 *
 * - 中文：统一协调服务器配置、HTTP Service 缓存与 WebSocket；默认 Service 按 IP/端口缓存。
 *
 * ## Custom service
 * ```kotlin
 * interface MyApi {
 *     @GET("custom") suspend fun getCustom(): ApiResponse<MyData>
 * }
 *
 * // 在自己的 Repository 中：
 * class MyRepository(...) : FrameworkRepository(...) {
 *     private suspend fun myApi(): MyApi = NetworkModule.createApiService(
 *         baseUrl = getCurrentBaseUrl(),
 *         getToken = { accessToken.first() },
 *         clearToken = { clearAccessToken() },
 *     )
 * }
 * ```
 *
 * @param context any context; only its application context is retained
 * @property preferencesManager preference source used by HTTP and WebSocket operations
 */
open class FrameworkRepository(
    context: Context,
    protected val preferencesManager: PreferencesManager
) {

    private val appContext = context.applicationContext

    private val webSocketManager = WebSocketManager.getInstance()

    // region [缓存配置]

    /** Cached default service for subclasses that need to inspect or invalidate it. */
    @Volatile
    protected var cachedApiService: ApiService? = null

    /** Server IP associated with [cachedApiService]. */
    @Volatile
    protected var cachedServerIp: String? = null

    /** Server port associated with [cachedApiService]. */
    @Volatile
    protected var cachedServerPort: String? = null

    // endregion

    // region [Token 失效回调]

    companion object {
        /**
         * Process-wide expired-token callback installed by
         * [com.template.framework.Framework.setOnTokenExpired].
         */
        @Volatile
        var onTokenExpiredCallback: (() -> Unit)? = null
    }

    init {
        NetworkModule.onTokenExpired = { onTokenExpiredCallback?.invoke() }
    }

    // region [ApiService 缓存管理]

    /**
     * Returns the default [ApiService] for the currently persisted server.
     *
     * The service is reused while IP and port remain unchanged. Token lookup is dynamic and occurs
     * per request, so saving a new token does not require cache invalidation.
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

    /** Invalidates the default API cache; the next [getApiService] call rebuilds it. */
    fun clearApiServiceCache() {
        cachedApiService = null
    }

    /**
     * Builds the configured HTTP base URL.
     *
     * Debug uses `http://{ip}:{port}/{debugApiPrefix}/`; release uses
     * `https://{ip}:{port}/{releaseApiPrefix}/`.
     *
     * @param ip host or IP without a scheme
     * @param port server port
     */
    protected fun buildBaseUrl(ip: String, port: String): String {
        val config = com.template.framework.Framework.getConfig()
        val scheme = if (config.debug) "http://" else "https://"
        val apiPrefix = if (config.debug) config.debugApiPrefix else config.releaseApiPrefix
        return "$scheme$ip:$port/$apiPrefix/"
    }

    /** Returns a base URL built from the latest persisted server values. */
    suspend fun getCurrentBaseUrl(): String {
        val ip = preferencesManager.serverIp.first()
        val port = preferencesManager.serverPort.first()
        return buildBaseUrl(ip, port)
    }

    // endregion

    // region [PreferencesManager 委托]

    /** Delegated stream of the current server IP. */
    val serverIp: Flow<String> = preferencesManager.serverIp

    /** Delegated stream of the current server port. */
    val serverPort: Flow<String> = preferencesManager.serverPort

    /** Delegated stream of the current access token. */
    val accessToken: Flow<String?> = preferencesManager.accessToken

    /** Delegated stream of the selected language code. */
    val language: Flow<String> = preferencesManager.language

    /** Persists [ip]; [getApiService] rebuilds its cache when the value changes. */
    suspend fun saveServerIp(ip: String) = preferencesManager.saveServerIp(ip)

    /** Persists [port]; [getApiService] rebuilds its cache when the value changes. */
    suspend fun saveServerPort(port: String) = preferencesManager.saveServerPort(port)

    /** Persists [token] for subsequent authenticated requests. */
    suspend fun saveAccessToken(token: String) = preferencesManager.saveAccessToken(token)

    /** Clears the persisted access token. */
    suspend fun clearAccessToken() = preferencesManager.clearAccessToken()

    // endregion

    // region [示例 API]

    /** Sends the sample device-login request. */
    suspend fun login(request: DeviceLoginRequest): ApiResponse<LoginDataResponse> {
        return getApiService().deviceLogin(request)
    }

    /** Returns the sample production-line list. */
    suspend fun getLineAndPost(): ApiResponse<List<LineAndPostResponse>> {
        return getApiService().getLineAndPost()
    }

    /** Saves the sample product payload. */
    suspend fun saveProducts(request: SaveProductsRequest): ApiResponse<SaveProductsResponse> {
        return getApiService().saveProducts(request)
    }

    // endregion

    // region [WebSocket 操作]

    /**
     * Starts a WebSocket connection.
     *
     * Missing [ip] or [port] values are read with `runBlocking`, so prefer passing explicit values
     * or call this away from latency-sensitive UI work.
     *
     * @param ip optional host override; defaults to persisted server IP
     * @param port optional port override; defaults to persisted server port
     * @param snNumber optional device serial number sent in the handshake header
     */
    fun connectWebSocket(ip: String? = null, port: String? = null, snNumber: String? = null) {
        kotlinx.coroutines.runBlocking {
            val realIp = ip ?: preferencesManager.serverIp.first()
            val realPort = port ?: preferencesManager.serverPort.first()
            webSocketManager.connect(realIp, realPort, snNumber)
        }
    }

    /** Closes the active WebSocket and disables automatic reconnect. */
    fun disconnectWebSocket() = webSocketManager.disconnect()

    /**
     * Sends [message] through the active socket.
     *
     * @return `false` when no active socket can accept the message
     */
    fun sendWebSocketMessage(message: String) = webSocketManager.sendMessage(message)

    /**
     * Current WebSocket connection state.
     * - 中文：WebSocket 当前连接状态。
     */
    val webSocketConnectionState = webSocketManager.connectionState

    /**
     * Most recent WebSocket message represented as text.
     * - 中文：最近收到的文本化消息。
     */
    val webSocketMessageFlow = webSocketManager.messageFlow

    // endregion
}
