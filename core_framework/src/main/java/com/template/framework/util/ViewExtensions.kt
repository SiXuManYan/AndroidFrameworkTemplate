package com.template.framework.util

import android.view.View

/**
 * View 扩展函数工具类
 *
 * @author Shiwei Wang
 * @date 2026-02
 */

/**
 * 为 View 设置连续点击监听器
 *
 * 在指定时间窗口内连续点击指定次数后触发回调，常用于「隐藏的管理员入口」。
 *
 * @param clickCount 需要连续点击的次数，默认 3
 * @param timeWindow 时间窗口（毫秒），默认 2000ms
 * @param onTrigger 达到点击次数后的回调
 *
 * 用法：
 * ```kotlin
 * titleView.setOnMultiClickListener(clickCount = 5) {
 *     // 进入管理员设置
 * }
 * ```
 */
fun View.setOnMultiClickListener(
    clickCount: Int = 3,
    timeWindow: Long = 2000L,
    onTrigger: () -> Unit
) {
    var clickCounter = 0
    var lastClickTime = 0L
    setOnClickListener {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > timeWindow) {
            clickCounter = 0
        }
        lastClickTime = currentTime
        clickCounter++
        if (clickCounter >= clickCount) {
            clickCounter = 0
            onTrigger()
        }
    }
}