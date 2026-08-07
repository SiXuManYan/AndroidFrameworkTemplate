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
 * Controls whether focusing this `EditText` opens the software keyboard.
 *
 * Useful for barcode scanners and other hardware-input workflows.
 * - 中文：适用于扫码枪等硬件输入场景，可禁止焦点触发软键盘。
 *
 * ## Usage
 * ```kotlin
 * editText.setShowSoftInputOnFocus(false)
 * ```
 *
 * @param show whether focus should request the software keyboard
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

/** Posts a focus request so it runs after the current view traversal. */
fun EditText.requestFocusSafely() {
    this.post {
        if (!this.isFocused) this.requestFocus()
    }
}

/**
 * Requests focus after [delayMillis]. Non-positive delays fall back to [requestFocusSafely].
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
 * Reclaims focus after it is lost, until [lifecycleOwner] is destroyed.
 *
 * The lifecycle observer removes callbacks and the focus listener to avoid retaining a destroyed
 * screen. This is intended for continuous scanner/card-reader input.
 * - 中文：焦点丢失后自动恢复，并在生命周期结束时清理回调。
 *
 * @param lifecycleOwner owner that controls cleanup
 * @param delayAfterInput delay in milliseconds before reclaiming focus
 */
fun EditText.keepFocus(lifecycleOwner: LifecycleOwner, delayAfterInput: Long = 300L) {
    val handler = Handler(Looper.getMainLooper())
    val focusDelay = delayAfterInput.coerceAtLeast(0L)
    val focusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) {
            handler.postDelayed({
                if (!this.isFocused) this.requestFocus()
            }, focusDelay)
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

/** Replaces the text and moves the cursor to the end; `null` becomes an empty string. */
fun EditText.setTextAndMoveCursorToEnd(text: String?) {
    val safeText = text ?: ""
    setText(safeText)
    setSelection(safeText.length)
}
