package com.template.framework.util

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

/**
 * EditText 通用扩展函数
 *
 * 业务级扩展（刷卡、扫码枪监听）请在 App 模块中实现，框架层只提供通用能力。
 *
 * @author Shiwei Wang
 * @date 2026-02
 */

/**
 * 设置 EditText 在获取焦点时是否显示软键盘
 *
 * 用法：
 * ```kotlin
 * editText.setShowSoftInputOnFocus(false)  // 禁止弹出软键盘（用于硬件输入设备）
 * ```
 */
fun EditText.setShowSoftInputOnFocus(show: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.showSoftInputOnFocus = show
    } else {
        try {
            val method = EditText::class.java.getMethod(
                "setShowSoftInputOnFocus",
                Boolean::class.javaPrimitiveType
            )
            method.isAccessible = true
            method.invoke(this, show)
        } catch (_: Exception) {
            // 低版本兼容失败，忽略
        }
    }
}

/**
 * 安全地请求 EditText 焦点
 * 使用 post 确保视图渲染完成后请求焦点
 */
fun EditText.requestFocusSafely() {
    this.post {
        if (!this.isFocused) this.requestFocus()
    }
}

/**
 * 延迟请求 EditText 焦点
 */
fun EditText.requestFocusSafely(delayMillis: Long) {
    if (delayMillis <= 0) {
        requestFocusSafely()
        return
    }
    Handler(Looper.getMainLooper()).postDelayed({
        this.post {
            if (!this.isFocused) this.requestFocus()
        }
    }, delayMillis)
}

/**
 * 保持 EditText 焦点持续
 *
 * 输入完成后自动重新获取焦点，适合连续扫码/刷卡场景。
 *
 * @param lifecycleOwner 生命周期拥有者
 * @param delayAfterInput 输入完成后延迟重新获取焦点的时间（毫秒），默认 300ms
 */
fun EditText.keepFocus(lifecycleOwner: LifecycleOwner, delayAfterInput: Long = 300L) {
    val handler = Handler(Looper.getMainLooper())
    val focusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) {
            handler.postDelayed({
                if (!this.isFocused) this.requestFocus()
            }, 200L)
        }
    }
    this.onFocusChangeListener = focusChangeListener
    lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            this.onFocusChangeListener = null
            handler.removeCallbacksAndMessages(null)
        }
    })
}

/**
 * 设置文本并将光标移动到末尾
 */
fun EditText.setTextAndMoveCursorToEnd(text: String?) {
    val safeText = text ?: ""
    setText(safeText)
    setSelection(safeText.length)
}