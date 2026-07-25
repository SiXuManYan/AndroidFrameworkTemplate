package com.template.framework.ui.base

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.ContextCompat
import com.template.framework.R
import com.template.framework.util.FullScreenUtils

/**
 * Dialog 基类
 *
 * 提供能力：
 * - 自动全屏（show 时调用 [FullScreenUtils.enableFullScreenForDialog]）
 * - 点击外部空白区域时，先隐藏软键盘，再关闭（可通过 [enableOutsideDismiss] 关闭）
 *
 * 布局要求：
 * - 根布局需要包含 id `R.id.rootLayout`（点击空白区域处理）
 * - 如果有卡片容器，需要 id `R.id.cardView`（阻止事件冒泡到 rootLayout）
 *
 * 使用示例：
 * ```kotlin
 * class DemoDialog(context: Context) : BaseDialog(context) {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         setContentView(R.layout.dialog_demo)
 *     }
 * }
 * ```
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
abstract class BaseDialog(context: Context) : Dialog(context) {

    /** 是否允许点击对话框外部时关闭 */
    protected open val enableOutsideDismiss: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun show() {
        super.show()
        FullScreenUtils.enableFullScreenForDialog(this)
        setupOutsideClickBehaviour()
    }

    private fun setupOutsideClickBehaviour() {
        setCanceledOnTouchOutside(false)
        findViewById<View>(R.id.rootLayout)?.setOnClickListener { handleOutsideClick() }
        findViewById<View>(R.id.cardView)?.setOnClickListener { /* 阻止冒泡 */ }
    }

    private fun handleOutsideClick() {
        if (isKeyboardVisible()) {
            hideKeyboard()
        } else if (enableOutsideDismiss) {
            dismiss()
        }
    }

    private fun isKeyboardVisible(): Boolean {
        val imm = ContextCompat.getSystemService(context, InputMethodManager::class.java)
            ?: return false
        return imm.isActive && window?.currentFocus is EditText
    }

    protected fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(context, InputMethodManager::class.java)
            ?: return
        val currentFocus = window?.currentFocus
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            currentFocus.clearFocus()
        }
    }
}