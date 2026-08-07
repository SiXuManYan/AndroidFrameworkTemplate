package com.template.framework

import android.app.Application
import android.content.Context
import com.template.framework.api.FrameworkConfig
import com.template.framework.datastore.PreferencesManager
import com.template.framework.repository.FrameworkRepository
import com.template.framework.util.TimberUtil

/**
 * Process-wide entry point for the framework module.
 *
 * Call [init] once from `Application.onCreate`, then obtain shared services through
 * [getRepository], [getPreferences], and [getContext]. Runtime values are supplied by the App
 * module, so `:core_framework` does not depend on the App's `BuildConfig`.
 *
 * - 中文：框架的进程级统一入口。请先在 `Application.onCreate` 中初始化，再访问其他能力。
 *
 * ## Usage
 * ```kotlin
 * Framework.init(
 *     app = this,
 *     config = FrameworkConfig(
 *         debug = BuildConfig.DEBUG,
 *         versionCode = BuildConfig.VERSION_CODE,
 *         versionName = BuildConfig.VERSION_NAME,
 *     ),
 * )
 * ```
 */
object Framework {

    @Volatile
    private var application: Application? = null

    @Volatile
    private var config: FrameworkConfig? = null

    @Volatile
    private var repository: FrameworkRepository? = null

    @Volatile
    private var preferencesManager: PreferencesManager? = null

    /**
     * Initializes framework singletons for the current process.
     *
     * The first call wins; later calls return without replacing the existing configuration.
     * - 中文：仅第一次调用生效，重复调用不会覆盖已有配置。
     *
     * @param app application instance used to create process-scoped services
     * @param config immutable runtime configuration supplied by the App module
     */
    @Synchronized
    fun init(app: Application, config: FrameworkConfig) {
        if (this.application != null) {
            return
        }
        this.application = app
        this.config = config

        // 初始化日志
        TimberUtil.init(config.debug)

        // 初始化偏好设置
        this.preferencesManager = PreferencesManager(app)

        // 初始化仓库
        this.repository = FrameworkRepository(app, this.preferencesManager!!)
    }

    /**
     * Returns the initialized application context.
     *
     * @throws IllegalStateException if [init] has not completed
     */
    fun getContext(): Context {
        return application ?: throw IllegalStateException(
            "Framework 未初始化，请先在 Application.onCreate 中调用 Framework.init()"
        )
    }

    /**
     * Returns the runtime configuration passed to [init].
     *
     * @throws IllegalStateException if [init] has not completed
     */
    fun getConfig(): FrameworkConfig {
        return config ?: throw IllegalStateException("Framework 未初始化")
    }

    /**
     * Returns the process-scoped preferences manager.
     *
     * @throws IllegalStateException if [init] has not completed
     */
    fun getPreferences(): PreferencesManager {
        return preferencesManager ?: throw IllegalStateException("Framework 未初始化")
    }

    /**
     * Returns the process-scoped framework repository.
     *
     * @throws IllegalStateException if [init] has not completed
     */
    fun getRepository(): FrameworkRepository {
        return repository ?: throw IllegalStateException("Framework 未初始化")
    }

    /**
     * Registers the App-level action to run after an HTTP or business-level `401` response.
     *
     * The callback may run on an OkHttp worker thread. Dispatch UI work to the main thread.
     * - 中文：回调可能位于网络线程，页面跳转前请切换到主线程。
     *
     * @param callback action that clears the authenticated UI state or opens the login screen
     */
    fun setOnTokenExpired(callback: () -> Unit) {
        FrameworkRepository.onTokenExpiredCallback = callback
    }
}
