package com.template.framework.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/**
 * 语言工具类
 *
 * - 提供中/英文切换能力
 * - 通过 [setLanguageSwitchingFlag] / [isLanguageSwitching] 控制「语言切换后淡入动画」标记
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
object LanguageUtils {

    private const val ZH = "zh"
    private const val EN = "en"

    @Volatile
    private var isLanguageSwitching = false

    /**
     * 设置应用语言
     *
     * @param context 上下文
     * @param language 语言代码，常用值："zh" / "en"
     */
    fun setLanguage(context: Context, language: String) {
        val locale = when (language) {
            EN -> Locale.ENGLISH
            ZH -> Locale.SIMPLIFIED_CHINESE
            else -> Locale.getDefault()
        }

        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocale(locale)
            val newContext = context.createConfigurationContext(config)
            if (context is AppCompatActivity) {
                @Suppress("DEPRECATION")
                context.resources.updateConfiguration(config, context.resources.displayMetrics)
            }
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        }
    }

    /**
     * 标记正在进行语言切换（用于淡入动画）
     */
    fun setLanguageSwitchingFlag() {
        isLanguageSwitching = true
    }

    /**
     * 清除语言切换标记
     */
    fun clearLanguageSwitchingFlag() {
        isLanguageSwitching = false
    }

    /**
     * 是否正在进行语言切换
     */
    fun isLanguageSwitching(): Boolean = isLanguageSwitching

    /**
     * 提供一个标准的淡入动画
     * - duration = 200ms
     * - alpha: 0 -> 1
     * - 动画结束后自动清除 [isLanguageSwitching] 标记
     */
    fun applyFadeInAnimation(rootView: android.view.View, onEnd: (() -> Unit)? = null) {
        rootView.alpha = 0f
        rootView.post {
            ValueAnimator.ofFloat(0.0f, 1.0f).apply {
                duration = 200
                addUpdateListener { rootView.alpha = it.animatedValue as Float }
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        clearLanguageSwitchingFlag()
                        onEnd?.invoke()
                    }
                })
                start()
            }
        }
    }
}