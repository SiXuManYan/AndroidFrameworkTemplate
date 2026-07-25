package com.example.template

import android.app.Application
import com.template.framework.Framework
import com.template.framework.api.FrameworkConfig

/**
 * 应用 Application 类
 *
 * 在 onCreate 中初始化 [Framework]，传入运行配置。
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Framework.init(
            app = this,
            config = FrameworkConfig(
                debug = BuildConfig.DEBUG,
                versionCode = BuildConfig.VERSION_CODE,
                versionName = BuildConfig.VERSION_NAME
            )
        )
    }
}