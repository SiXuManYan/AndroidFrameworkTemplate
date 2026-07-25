package com.template.framework.constants

/**
 * 框架通用常量
 *
 * 仅保留与业务无关的基础网络/数据/Header 常量。
 * 业务相关 Key、scheme、API 前缀请通过 [com.template.framework.api.FrameworkConfig] 注入。
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
object FrameworkConstants {

    // region [网络请求配置]

    /** 连接超时（秒） */
    const val CONNECT_TIMEOUT = 30L

    /** 读取超时（秒） */
    const val READ_TIMEOUT = 30L

    /** 写入超时（秒） */
    const val WRITE_TIMEOUT = 30L

    /** WebSocket 重连间隔（毫秒） */
    const val WS_RECONNECT_INTERVAL = 5000L

    // endregion

    // region [HTTP Header]

    /** Authorization 请求头 */
    const val HEADER_AUTHORIZATION = "Authorization"

    /** Authorization 前缀 */
    const val HEADER_AUTHORIZATION_PREFIX = "Bearer "

    /** 客户端 ID 请求头 */
    const val HEADER_CLIENT_ID = "clientid"

    /** VersionCode 请求头 */
    const val HEADER_VERSION_CODE = "VersionCode"

    /** VersionName 请求头 */
    const val HEADER_VERSION_NAME = "VersionName"

    // endregion

    // region [数据库配置]

    /** 框架数据库名（可通过 FrameworkConfig.databaseName 自定义，本处提供默认值） */
    const val DATABASE_NAME = "framework_database"

    // endregion

    // region [DataStore Key]

    /** DataStore Key：服务器 IP */
    const val PREF_SERVER_IP = "server_ip"

    /** DataStore Key：服务器端口 */
    const val PREF_SERVER_PORT = "server_port"

    /** DataStore Key：访问令牌 */
    const val PREF_ACCESS_TOKEN = "access_token"

    /** DataStore Key：语言 */
    const val PREF_LANGUAGE = "language"

    // endregion
}