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
 * RecyclerView 通用适配器基类（单一 ViewBinding）
 *
 * 使用示例：
 * ```kotlin
 * class ItemAdapter : CommonAdapter<Item, ItemBinding>() {
 *     override fun onCreateBinding(inflater, parent, viewType) = ItemBinding.inflate(inflater, parent, false)
 *     override fun onBind(binding: ItemBinding, item: Item, position: Int, viewType: Int) {
 *         binding.tvName.text = item.name
 *     }
 * }
 *
 * recyclerView.adapter = ItemAdapter().apply {
 *     setNewDataList(list)
 *     setOnItemClickListener { item, _, _ -> /* click */ }
 * }
 * ```
 *
 * @author Shiwei Wang
 * @date 2026-02
 */
abstract class CommonAdapter<T, VB : ViewBinding> : RecyclerView.Adapter<CommonAdapter<T, VB>.BindingViewHolder>() {

    protected var context: Context? = null

    protected val dataList: MutableList<T> = mutableListOf()

    private var currentHolder: BindingViewHolder? = null

    private var onItemClickListener: OnItemClickListener<T>? = null
    private var onItemLongClickListener: OnItemLongClickListener<T>? = null
    private var onItemChildClickListener: OnItemChildClickListener<T>? = null

    protected abstract fun onCreateBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): VB

    protected abstract fun onBind(binding: VB, item: T, position: Int, viewType: Int)

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
        val binding: VB,
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