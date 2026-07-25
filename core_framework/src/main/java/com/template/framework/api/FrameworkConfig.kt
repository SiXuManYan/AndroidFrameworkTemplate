package com.template.framework.api

/**
 * 框架运行时配置
 *
 * 在 Application.onCreate 中创建并注入到 [com.template.framework.Framework.init]，
 * 框架模块通过此配置获取版本号、Debug 标志等信息，
 * 无需依赖 App 模块的 BuildConfig.
 *
 * @param debug 是否为 Debug 构建，影响日志输出、HTTP/WS scheme、SSL 策略
 * @param versionCode 应用 VersionCode，会自动加到 HTTP Header
 * @param versionName 应用 VersionName，会自动加到 HTTP Header
 * @param dataStoreName DataStore Preferences 文件名，默认 framework_preferences
 * @param defaultServerIp Debug 模式默认服务器 IP（可选）
 * @param defaultServerPort Debug 模式默认服务器端口（可选）
 * @param debugApiPrefix Debug 模式 API 路径前缀
 * @param releaseApiPrefix Release 模式 API 路径前缀
 * @param sslCertRawResId Release 模式下 SSL 证书的 raw 资源 ID（可选；为 null 时 Release 模式也信任所有证书）
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