package com.template.framework.util

import timber.log.Timber

/**
 * Timber 日志工具类
 *
 * - Debug：使用 [Timber.DebugTree] 输出到 Logcat
 * - Release：使用空 Tree，完全不输出，避免性能开销
 *
 * 由 [com.template.framework.Framework.init] 自动初始化，业务无需手动调用。
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
object TimberUtil {

    /**
     * Release 模式下的空 Tree
     */
    private class ReleaseTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Release 模式不输出
        }
    }

    /**
     * 初始化 Timber
     */
    fun init(debug: Boolean) {
        if (debug) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }
    }
}