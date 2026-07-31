package com.template.framework.util

import android.content.Context
import android.os.Build
import android.util.TypedValue
import androidx.annotation.DimenRes

/**
 * 屏幕尺寸工具类
 *
 * 提供 dp/px 互转、屏幕宽高获取等功能。
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
object ScreenUtil {

    fun dp2px(context: Context, dpValue: Float): Int {
        val density = context.resources.displayMetrics.density
        return (dpValue * density + 0.5f).toInt()
    }

    fun dp2px(context: Context, dpValue: Int): Int = dp2px(context, dpValue.toFloat())

    fun px2dp(context: Context, pxValue: Float): Float {
        val density = context.resources.displayMetrics.density
        return pxValue / density
    }

    fun px2sp(context: Context, pxValue: Float): Float {
        val metrics = context.resources.displayMetrics
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            TypedValue.deriveDimension(TypedValue.COMPLEX_UNIT_SP, pxValue, metrics)
        } else {
            @Suppress("DEPRECATION")
            pxValue / metrics.scaledDensity
        }
    }

    fun sp2px(context: Context, spValue: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            spValue,
            context.resources.displayMetrics
        )
    }

    fun dimensionToPx(context: Context, @DimenRes resId: Int): Int {
        return context.resources.getDimensionPixelSize(resId)
    }

    fun screenWidth(context: Context): Int = context.resources.displayMetrics.widthPixels

    fun screenHeight(context: Context): Int = context.resources.displayMetrics.heightPixels
}
