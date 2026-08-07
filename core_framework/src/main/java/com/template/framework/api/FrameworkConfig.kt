package com.template.framework.api

/**
 * Immutable runtime configuration injected through [com.template.framework.Framework.init].
 *
 * - 中文：由 App 模块提供的运行时配置，使框架模块无需依赖 App 的 `BuildConfig`。
 *
 * @property debug enables debug logging and selects `http`/`ws`; release uses `https`/`wss`
 * @property versionCode app version code added to HTTP and WebSocket headers
 * @property versionName app version name added to HTTP and WebSocket headers
 * @property dataStoreName value currently used to derive the default Room database name; it does
 * not rename the DataStore owned by `PreferencesManager`
 * @property defaultServerIp reserved default IP; callers must currently persist it explicitly
 * @property defaultServerPort reserved default port; callers must currently persist it explicitly
 * @property debugApiPrefix URL path prefix used by debug HTTP and WebSocket connections
 * @property releaseApiPrefix URL path prefix used by release HTTP and WebSocket connections
 * @property sslCertRawResId optional raw resource containing a trusted CA or server certificate;
 * `null` keeps the platform trust store
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
