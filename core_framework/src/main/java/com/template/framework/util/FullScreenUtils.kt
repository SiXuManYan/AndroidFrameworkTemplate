package com.template.framework.util

import android.app.Activity
import android.app.Dialog
import android.view.Window
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Dialog 扩展函数：自动应用全屏设置
 *
 * 用法：
 * ```kotlin
 * dialog.showFullScreen()
 * ```
 */
fun Dialog.showFullScreen() {
    show()
    FullScreenUtils.enableFullScreenForDialog(this)
}

/**
 * 全屏工具类
 *
 * - 隐藏状态栏和导航栏
 * - 允许通过滑动临时显示系统栏
 * - 使用 AndroidX WindowInsets API 兼容各系统版本
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
object FullScreenUtils {

    /**
     * 为 Activity 设置全屏
     */
    fun enableFullScreen(activity: Activity) {
        configureImmersiveWindow(activity.window)
    }

    /**
     * 为 Dialog 设置全屏
     */
    fun enableFullScreenForDialog(dialog: Dialog) {
        val window: Window = dialog.window ?: return
        configureImmersiveWindow(window)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun configureImmersiveWindow(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
