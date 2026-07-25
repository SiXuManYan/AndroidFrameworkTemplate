package com.template.framework.ui.base

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding

/**
 * 多布局 RecyclerView 通用适配器基类（多 ViewBinding）
 *
 * 当 RecyclerView 的 item 有多种布局时使用。
 *
 * 使用示例：
 * ```kotlin
 * class MultiAdapter : MultiViewTypeAdapter<ListItem>() {
 *     override fun getItemViewType(position: Int): Int = dataList[position].type.ordinal
 *
 *     override fun onCreateBinding(inflater, parent, viewType): ViewBinding = when (viewType) {
 *         TYPE_A -> ItemABinding.inflate(inflater, parent, false)
 *         TYPE_B -> ItemBBinding.inflate(inflater, parent, false)
 *         else -> throw IllegalArgumentException("Unknown viewType")
 *     }
 *
 *     override fun onBind(binding: ViewBinding, item: ListItem, position: Int, viewType: Int) {
 *         when (binding) {
 *             is ItemABinding -> binding.tvA.text = item.name
 *             is ItemBBinding -> binding.tvB.text = item.name
 *         }
 *     }
 * }
 * ```
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
abstract class MultiViewTypeAdapter<T> : RecyclerView.Adapter<MultiViewTypeAdapter<T>.BindingViewHolder>() {

    protected var context: Context? = null

    protected val dataList: MutableList<T> = mutableListOf()

    private var currentHolder: BindingViewHolder? = null

    private var onItemClickListener: OnItemClickListener<T>? = null
    private var onItemLongClickListener: OnItemLongClickListener<T>? = null
    private var onItemChildClickListener: OnItemChildClickListener<T>? = null

    protected abstract fun onCreateBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): ViewBinding

    protected abstract fun onBind(binding: ViewBinding, item: T, position: Int, viewType: Int)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder {
        val binding = onCreateBinding(LayoutInflater.from(parent.context), parent, viewType)
        context = parent.context
        return BindingViewHolder(binding, viewType)
    }

    override fun onBindViewHolder(holder: BindingViewHolder, position: Int) {
        if (position < 0 || position >= dataList.size) return
        val item = dataList[position]
        currentHolder = holder

        onBind(holder.binding, item, position, holder.viewType)

        holder.itemView.setOnClickListener { v -> onItemClickListener?.onItemClick(item, position, v) }
        holder.itemView.setOnLongClickListener { v ->
            onItemLongClickListener?.onItemLongClick(item, position, v)
            true
        }
        holder.childClickViews.forEach { child ->
            child.setOnClickListener { v -> onItemChildClickListener?.onItemChildClick(item, position, v, v.id) }
        }
    }

    override fun getItemCount(): Int = dataList.size

    @SuppressLint("NotifyDataSetChanged")
    fun setNewDataList(list: List<T>?) {
        dataList.clear()
        list?.let { dataList.addAll(it) }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addDataList(list: List<T>?) {
        list?.let { dataList.addAll(it); notifyDataSetChanged() }
    }

    fun addItem(item: T) {
        dataList.add(item)
        notifyItemInserted(dataList.size - 1)
    }

    fun insertItem(position: Int, item: T) {
        if (position < 0 || position > dataList.size) return
        dataList.add(position, item)
        notifyItemInserted(position)
    }

    fun deleteItem(position: Int) {
        if (position < 0 || position >= dataList.size) return
        dataList.removeAt(position)
        notifyItemRemoved(position)
    }

    fun updateItem(position: Int, item: T) {
        if (position < 0 || position >= dataList.size) return
        dataList[position] = item
        notifyItemChanged(position)
    }

    fun isEmpty(): Boolean = dataList.isEmpty()
    fun isNotEmpty(): Boolean = dataList.isNotEmpty()

    fun getString(@StringRes resId: Int): String = context?.getString(resId) ?: ""

    fun getAllData(): List<T> = dataList.toList()

    @SuppressLint("NotifyDataSetChanged")
    fun clearAllData() {
        dataList.clear()
        notifyDataSetChanged()
    }

    protected fun bindChildClickListener(vararg views: View) {
        currentHolder?.bindChildClickListener(*views)
    }

    fun setOnItemClickListener(listener: OnItemClickListener<T>?) {
        this.onItemClickListener = listener
    }

    fun setOnItemLongListener(listener: OnItemLongClickListener<T>?) {
        this.onItemLongClickListener = listener
    }

    fun setOnItemChildClickListener(listener: OnItemChildClickListener<T>?) {
        this.onItemChildClickListener = listener
    }

    inner class BindingViewHolder(
        val binding: ViewBinding,
        val viewType: Int
    ) : RecyclerView.ViewHolder(binding.root) {
        val childClickViews: MutableList<View> = mutableListOf()

        fun bindChildClickListener(vararg views: View) {
            childClickViews.addAll(views)
        }
    }

    fun interface OnItemClickListener<T> {
        fun onItemClick(item: T, position: Int, v: View)
    }

    fun interface OnItemChildClickListener<T> {
        fun onItemChildClick(item: T, position: Int, view: View, viewId: Int)
    }

    fun interface OnItemLongClickListener<T> {
        fun onItemLongClick(item: T, position: Int, v: View)
    }
}