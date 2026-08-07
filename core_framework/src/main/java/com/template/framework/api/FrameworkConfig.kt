package com.template.framework.api

/**
 * 框架运行时配置
 *
 * 在 Application.onCreate 中创建并注入到 [com.template.framework.Framework.init]，
 * 框架模块通过此配置获取版本号、Debug 标志等信息，
 * 无需依赖 App 模块的 BuildConfig.
 *
 * @param debug 是否为 Debug 构建，影响日志输出和 HTTP/WS scheme
 * @param versionCode 应用 VersionCode，会自动加到 HTTP Header
 * @param versionName 应用 VersionName，会自动加到 HTTP Header
 * @param dataStoreName 用于派生默认 Room 数据库名；当前不会修改 PreferencesManager 的 DataStore 文件名
 * @param defaultServerIp 预留的默认服务器 IP；当前需要业务主动保存后才会生效
 * @param defaultServerPort 预留的默认服务器端口；当前需要业务主动保存后才会生效
 * @param debugApiPrefix Debug 模式 API 路径前缀
 * @param releaseApiPrefix Release 模式 API 路径前缀
 * @param sslCertRawResId 自定义 CA 或服务器证书的 raw 资源 ID；为 null 时使用系统信任链
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
data class FrameworkConfig(
    val debug: Boolean,
    val versionCode: Int,
    val versionName: String,
    val dataStoreName: String = "framework_preferences",
    val defaultServerIp: String = "",
    val defaultServerPort: String = "",
    val debugApiPrefix: String = "dev-api",
    val releaseApiPrefix: String = "prod-api",
    val sslCertRawResId: Int? = null
)
