package com.template.framework.util

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 通用 edge-to-edge 系统栏适配。
 *
 * 状态栏和导航栏保持可用，页面背景延伸到系统栏区域，内容则通过 padding
 * 避让刘海、状态栏和手势导航区域。需要 kiosk 模式时显式使用 [FullScreenUtils]。
 */
object SystemBarUtils {

    @Suppress("DEPRECATION")
    fun applyEdgeToEdge(activity: Activity, contentView: View) {
        val window = activity.window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }

        val isLightTheme = activity.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK != Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = isLightTheme
            isAppearanceLightNavigationBars = isLightTheme
        }

        val initialLeft = contentView.paddingLeft
        val initialTop = contentView.paddingTop
        val initialRight = contentView.paddingRight
        val initialBottom = contentView.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(contentView) { view, insets ->
            val systemBars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                    WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                initialLeft + systemBars.left,
                initialTop + systemBars.top,
                initialRight + systemBars.right,
                initialBottom + systemBars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(contentView)
    }
}
