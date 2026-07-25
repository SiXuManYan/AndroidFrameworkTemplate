package com.template.framework

import android.app.Application
import android.content.Context
import com.template.framework.api.FrameworkConfig
import com.template.framework.datastore.PreferencesManager
import com.template.framework.repository.FrameworkRepository
import com.template.framework.util.TimberUtil

/**
 * 框架统一入口
 *
 * 使用步骤：
 * 1. 在 Application.onCreate 中调用 [init]
 * 2. 通过 [getRepository] / [getPreferences] / [getContext] 获取框架能力
 *
 * 框架依赖运行时配置（版本号、是否 Debug 等），由 App 通过 [init] 注入，
 * 这样框架模块就不需要 BuildConfig.
 *
 * @author Shiwei Wang
 * @date 2026-02
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
     * 初始化框架
     * 必须在 Application.onCreate 中调用，且只能调用一次
     *
     * @param app Application 实例
     * @param config 框架运行配置
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
     * 获取 Application 上下文
     */
    fun getContext(): Context {
        return application ?: throw IllegalStateException(
            "Framework 未初始化，请先在 Application.onCreate 中调用 Framework.init()"
        )
    }

    /**
     * 获取框架配置
     */
    fun getConfig(): FrameworkConfig {
        return config ?: throw IllegalStateException("Framework 未初始化")
    }

    /**
     * 获取偏好设置管理器
     */
    fun getPreferences(): PreferencesManager {
        return preferencesManager ?: throw IllegalStateException("Framework 未初始化")
    }

    /**
     * 获取仓库实例
     */
    fun getRepository(): FrameworkRepository {
        return repository ?: throw IllegalStateException("Framework 未初始化")
    }

    /**
     * Token 失效回调，由 App 层注入（通常用于跳转到登录页）
     */
    fun setOnTokenExpired(callback: () -> Unit) {
        FrameworkRepository.onTokenExpiredCallback = callback
    }
}