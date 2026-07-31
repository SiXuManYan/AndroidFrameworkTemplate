package com.template.framework.ui.base

import android.animation.ObjectAnimator
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import androidx.viewbinding.ViewBinding
import com.template.framework.R
import com.template.framework.util.FullScreenUtils

/**
 * DialogFragment 基类
 *
 * 提供能力：
 * - ViewBinding 生命周期管理
 * - 全窗口展示，默认保留系统栏
 * - 点击外部空白区域时，先隐藏软键盘，再关闭（可通过 [enableOutsideDismiss] 关闭）
 * - 软键盘弹出时，自动上移 CardView，保证 EditText 可见
 *
 * 布局要求：
 * - 根布局需要包含 id `R.id.rootLayout`（点击空白区域处理）
 * - 卡片容器需要 id `R.id.cardView`（阻止事件冒泡 + 跟随软键盘上移）
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
abstract class BaseDialogFragment<VB : ViewBinding> : DialogFragment() {

    private var _binding: VB? = null

    protected val binding: VB
        get() = _binding ?: throw IllegalStateException(
            "Binding is only valid between onCreateView and onDestroyView"
        )

    /** 是否允许点击对话框外部时关闭 */
    protected open val enableOutsideDismiss: Boolean = true

    /** kiosk 场景可覆盖为 true；通用手机界面默认保留系统栏。 */
    protected open val enableImmersiveFullScreen: Boolean = false

    private var currentCardViewTranslationY = 0f

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = initViewBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupOutsideClickBehaviour(view)
        setupKeyboardAdjustment(view)
        initView()
        initData()
        initListener()
        observeViewModel()
    }

    abstract fun initData()

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        if (enableImmersiveFullScreen) {
            dialog?.let(FullScreenUtils::enableFullScreenForDialog)
        }
    }

    private fun setupOutsideClickBehaviour(root: View) {
        root.findViewById<View>(R.id.rootLayout)?.setOnClickListener { handleOutsideClick() }
        root.findViewById<View>(R.id.cardView)?.setOnClickListener { /* 阻止冒泡 */ }
    }

    /**
     * 监听软键盘弹出，自动调整 CardView 位置
     */
    private fun setupKeyboardAdjustment(root: View) {
        val cardView = root.findViewById<View>(R.id.cardView) ?: return
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navigationBarsInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val keyboardHeight = imeInsets.bottom - navigationBarsInsets.bottom

            if (keyboardHeight > 0) {
                val editText = findEditTextInView(cardView)
                val targetTranslationY = if (editText != null) {
                    val location = IntArray(2)
                    editText.getLocationOnScreen(location)
                    val editTextBottom = location[1] + editText.height
                    val screenHeight = resources.displayMetrics.heightPixels
                    val availableHeight = screenHeight - keyboardHeight
                    if (editTextBottom > availableHeight) {
                        val offset = (editTextBottom - availableHeight + 100).toFloat()
                        -offset.coerceAtMost(keyboardHeight.toFloat())
                    } else {
                        -(keyboardHeight / 3).toFloat()
                    }
                } else {
                    -(keyboardHeight / 3).toFloat()
                }
                if (currentCardViewTranslationY != targetTranslationY) {
                    ObjectAnimator.ofFloat(cardView, "translationY", currentCardViewTranslationY, targetTranslationY)
                        .apply { duration = 200; start() }
                    currentCardViewTranslationY = targetTranslationY
                }
            } else {
                if (currentCardViewTranslationY != 0f) {
                    ObjectAnimator.ofFloat(cardView, "translationY", currentCardViewTranslationY, 0f)
                        .apply { duration = 200; start() }
                    currentCardViewTranslationY = 0f
                }
            }
            insets
        }
    }

    private fun findEditTextInView(view: View): EditText? {
        if (view is EditText) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val result = findEditTextInView(view.getChildAt(i))
                if (result != null) return result
            }
        }
        return null
    }

    private fun handleOutsideClick() {
        if (isKeyboardVisible()) {
            hideKeyboard()
        } else if (enableOutsideDismiss && isCancelable) {
            dismiss()
        }
    }

    private fun isKeyboardVisible(): Boolean {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
            ?: return false
        return imm.isActive && dialog?.window?.currentFocus is EditText
    }

    protected fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
            ?: return
        val currentFocus = dialog?.window?.currentFocus
        if (currentFocus != null) {
            imm.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            currentFocus.clearFocus()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        currentCardViewTranslationY = 0f
    }

    protected abstract fun initViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    protected open fun initView() {}
    protected open fun initListener() {}
    protected open fun observeViewModel() {}
}
