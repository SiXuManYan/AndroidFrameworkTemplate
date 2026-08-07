package com.example.template

import android.app.Application
import com.template.framework.Framework
import com.template.framework.api.FrameworkConfig

/**
 * Demo application entry point.
 *
 * Initializes [Framework] before any Activity accesses framework services.
 * - 中文：示例应用入口，负责在页面创建前完成框架初始化。
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
