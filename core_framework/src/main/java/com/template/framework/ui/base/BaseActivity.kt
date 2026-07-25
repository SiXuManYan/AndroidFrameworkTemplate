package com.template.framework.ui.base

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.os.Bundle
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.template.framework.Framework
import com.template.framework.util.FullScreenUtils
import com.template.framework.util.LanguageUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber

/**
 * Activity 基类
 *
 * 提供能力：
 * - ViewBinding 生命周期管理（onCreate 初始化、onDestroy 置空）
 * - 自动全屏
 * - 全局禁用系统返回键（子类可覆盖 [onBackPressedCallback] 实现自定义行为）
 * - 点击空白处关闭软键盘
 * - 语言设置同步应用（基于 [LanguageUtils] + [Framework.getPreferences]）
 *
 * 使用示例：
 * ```kotlin
 * class DemoActivity : BaseActivity<ActivityDemoBinding>() {
 *     override fun initViewBinding() = ActivityDemoBinding.inflate(layoutInflater)
 *     override fun initView() { /* 初始化视图 */ }
 * }
 * ```
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null

    /**
     * ViewBinding，仅在 onCreate ~ onDestroy 之间可用
     */
    protected val binding: VB
        get() = _binding ?: throw IllegalStateException(
            "Binding should not be accessed before onCreate or after onDestroy"
        )

    /**
     * 是否启用全局返回键拦截
     * 子类可重写：返回 false 即可恢复系统默认返回行为
     */
    protected open val enableBackKeyInterceptor: Boolean = true

    private val onBackPressedCallback = object : OnBackPressedCallback(enableBackKeyInterceptor) {
        override fun handleOnBackPressed() {
            Timber.tag(TAG).d("禁用返回键")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 在 super.onCreate 之前应用语言设置（同步方式）
        applyLanguageSettingsSync()

        super.onCreate(savedInstanceState)
        _binding = initViewBinding()
        setContentView(binding.root)

        FullScreenUtils.enableFullScreen(this)

        if (enableBackKeyInterceptor) {
            onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        }

        initView()
        initListener()
        initData()

        if (LanguageUtils.isLanguageSwitching()) {
            applyFadeInAnimation()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) FullScreenUtils.enableFullScreen(this)
    }

    override fun onResume() {
        super.onResume()
        FullScreenUtils.enableFullScreen(this)
    }

    /**
     * 同步应用语言设置（在 onCreate 中调用）
     */
    private fun applyLanguageSettingsSync() {
        runCatching {
            runBlocking {
                val lang = Framework.getPreferences().language.first()
                if (lang.isNotEmpty()) {
                    LanguageUtils.setLanguage(this@BaseActivity, lang)
                }
            }
        }
    }

    /**
     * 语言切换后的淡入动画
     */
    private fun applyFadeInAnimation() {
        val rootView = window.decorView.rootView
        rootView.alpha = 0f
        rootView.post {
            val fadeIn = ValueAnimator.ofFloat(0.0f, 1.0f).apply {
                duration = 200
                addUpdateListener { rootView.alpha = it.animatedValue as Float }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        LanguageUtils.clearLanguageSwitchingFlag()
                    }
                })
            }
            fadeIn.start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    /**
     * 点击空白处关闭软键盘
     */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val currentFocus = currentFocus
            if (currentFocus is EditText) {
                val touchX = ev.rawX.toInt()
                val touchY = ev.rawY.toInt()
                val location = IntArray(2)
                currentFocus.getLocationOnScreen(location)
                val editTextX = location[0]
                val editTextY = location[1]
                val editTextWidth = currentFocus.width
                val editTextHeight = currentFocus.height
                val isOutsideEditText = touchX < editTextX ||
                        touchX > editTextX + editTextWidth ||
                        touchY < editTextY ||
                        touchY > editTextY + editTextHeight
                if (isOutsideEditText) {
                    hideKeyboard()
                    currentFocus.clearFocus()
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * 隐藏软键盘
     */
    protected fun hideKeyboard() {
        val imm = ContextCompat.getSystemService(this, InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(window.decorView.windowToken, 0)
    }

    /**
     * 初始化 ViewBinding
     */
    protected abstract fun initViewBinding(): VB

    /**
     * 初始化视图（子类实现）
     */
    protected open fun initView() {}

    /**
     * 初始化监听器（子类实现）
     */
    protected open fun initListener() {}

    /**
     * 初始化数据（子类实现）
     */
    protected open fun initData() {}

    private companion object {
        const val TAG = "BaseActivity"
    }
}