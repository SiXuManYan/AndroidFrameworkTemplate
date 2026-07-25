package com.template.framework.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

/**
 * Fragment 基类
 *
 * 提供能力：
 * - ViewBinding 生命周期管理
 * - 统一的生命周期钩子：initView / initListener / observeViewModel / initData
 * - 关闭软键盘辅助方法
 *
 * 使用示例：
 * ```kotlin
 * class DemoFragment : BaseFragment<FragmentDemoBinding>() {
 *     override fun initViewBinding(inflater, container) = FragmentDemoBinding.inflate(inflater, container, false)
 *     override fun initView() { /* 初始化视图 */ }
 *     override fun observeViewModel() { /* 观察 ViewModel */ }
 * }
 * ```
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
abstract class BaseFragment<VB : ViewBinding> : Fragment() {

    private var _binding: VB? = null

    protected val binding: VB
        get() = _binding ?: throw IllegalStateException(
            "Binding should not be accessed before onCreateView or after onDestroyView"
        )

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
        initView()
        initListener()
        observeViewModel()
        initData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    protected abstract fun initViewBinding(inflater: LayoutInflater, container: ViewGroup?): VB

    protected open fun initView() {}
    protected open fun initListener() {}
    protected open fun observeViewModel() {}
    protected open fun initData() {}

    /**
     * 关闭软键盘
     */
    protected fun hideKeyboard() {
        val view = view ?: return
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * 通过指定 View 关闭软键盘
     */
    protected fun hideKeyboard(view: View) {
        val imm = ContextCompat.getSystemService(requireContext(), InputMethodManager::class.java)
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
    }
}