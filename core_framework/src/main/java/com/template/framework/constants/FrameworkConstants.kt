package com.template.framework.constants

/**
 * Framework-owned network, storage, and header constants.
 *
 * Business endpoint prefixes belong in [com.template.framework.api.FrameworkConfig], not here.
 * - 中文：仅保存框架级常量，业务环境参数应通过 `FrameworkConfig` 注入。
 */
object FrameworkConstants {

    // region [网络请求配置]

    /**
     * TCP connection timeout in seconds.
     * - 中文：连接超时，单位秒。
     */
    const val CONNECT_TIMEOUT = 30L

    /**
     * Socket read timeout in seconds.
     * - 中文：读取超时，单位秒。
     */
    const val READ_TIMEOUT = 30L

    /**
     * Socket write timeout in seconds.
     * - 中文：写入超时，单位秒。
     */
    const val WRITE_TIMEOUT = 30L

    /**
     * Fixed WebSocket reconnect interval in milliseconds.
     * - 中文：重连间隔，单位毫秒。
     */
    const val WS_RECONNECT_INTERVAL = 5000L

    // endregion

    // region [HTTP Header]

    /** HTTP authorization header name. */
    const val HEADER_AUTHORIZATION = "Authorization"

    /** Bearer-token value prefix. */
    const val HEADER_AUTHORIZATION_PREFIX = "Bearer "

    /** Client identifier header name. */
    const val HEADER_CLIENT_ID = "clientid"

    /** App version-code header name. */
    const val HEADER_VERSION_CODE = "VersionCode"

    /** App version-name header name. */
    const val HEADER_VERSION_NAME = "VersionName"

    // endregion

    // region [数据库配置]

    /** Fallback Room database name when framework configuration is unavailable. */
    const val DATABASE_NAME = "framework_database"

    // endregion

    // region [DataStore Key]

    /** Preferences key for the server IP. */
    const val PREF_SERVER_IP = "server_ip"

    /** Preferences key for the server port. */
    const val PREF_SERVER_PORT = "server_port"

    /** Preferences key for the access token. */
    const val PREF_ACCESS_TOKEN = "access_token"

    /** Preferences key for the selected language. */
    const val PREF_LANGUAGE = "language"

    // endregion
}
