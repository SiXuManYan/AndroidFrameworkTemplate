package com.template.framework.util

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner

fun EditText.setShowSoftInputOnFocus(show: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        showSoftInputOnFocus = show
    } else {
        try {
            val method = EditText::class.java.getMethod(
                "setShowSoftInputOnFocus",
                Boolean::class.javaPrimitiveType
            )
            method.isAccessible = true
            method.invoke(this, show)
        } catch (_: Exception) {
            // Unsupported on this device.
        }
    }
}

fun EditText.requestFocusSafely() {
    post { if (!isFocused) requestFocus() }
}

fun EditText.requestFocusSafely(delayMillis: Long) {
    if (delayMillis <= 0) {
        requestFocusSafely()
        return
    }
    postDelayed({ if (!isFocused) requestFocus() }, delayMillis)
}

fun EditText.keepFocus(lifecycleOwner: LifecycleOwner, delayAfterInput: Long = 300L) {
    val handler = Handler(Looper.getMainLooper())
    val focusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
        if (!hasFocus) {
            handler.postDelayed({ if (!isFocused) requestFocus() }, delayAfterInput)
        }
    }
    onFocusChangeListener = focusChangeListener
    lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            onFocusChangeListener = null
            handler.removeCallbacksAndMessages(null)
        }
    })
}

fun EditText.setTextAndMoveCursorToEnd(text: String?) {
    val safeText = text.orEmpty()
    setText(safeText)
    setSelection(safeText.length)
}

fun EditText.doAfterTrimmedTextChanged(action: (String) -> Unit): TextWatcher =
    doAfterTextChanged { action(it?.toString()?.trim().orEmpty()) }

fun areAllNotBlank(vararg editTexts: EditText): Boolean =
    editTexts.all { it.text?.toString()?.isNotBlank() == true }
