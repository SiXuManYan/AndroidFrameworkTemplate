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
 * RecyclerView adapter base for item lists that use multiple ViewBinding layouts.
 *
 * Subclasses select a view type, create its binding in [onCreateBinding], then handle the concrete
 * binding type in [onBind].
 * - 中文：适用于多布局列表，子类负责 viewType 与具体 ViewBinding 的映射。
 *
 * ## Usage
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
 * @param T item model type
 */
abstract class MultiViewTypeAdapter<T> : RecyclerView.Adapter<MultiViewTypeAdapter<T>.BindingViewHolder>() {

    /** Host context captured after the first ViewHolder is created. */
    protected var context: Context? = null

    /** Mutable backing list available to subclasses. */
    protected val dataList: MutableList<T> = mutableListOf()

    private var currentHolder: BindingViewHolder? = null

    private var onItemClickListener: OnItemClickListener<T>? = null
    private var onItemLongClickListener: OnItemLongClickListener<T>? = null
    private var onItemChildClickListener: OnItemChildClickListener<T>? = null

    /** Creates the ViewBinding associated with [viewType]. */
    protected abstract fun onCreateBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int): ViewBinding

    /** Binds [item] to its concrete [binding] implementation. */
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

    /** Replaces all items with [list]; `null` produces an empty adapter. */
    @SuppressLint("NotifyDataSetChanged")
    fun setNewDataList(list: List<T>?) {
        dataList.clear()
        list?.let { dataList.addAll(it) }
        notifyDataSetChanged()
    }

    /** Appends [list] and refreshes the adapter; `null` is ignored. */
    @SuppressLint("NotifyDataSetChanged")
    fun addDataList(list: List<T>?) {
        list?.let { dataList.addAll(it); notifyDataSetChanged() }
    }

    /** Appends [item] and dispatches an insertion notification. */
    fun addItem(item: T) {
        dataList.add(item)
        notifyItemInserted(dataList.size - 1)
    }

    /** Inserts [item] at [position]; an invalid position is ignored. */
    fun insertItem(position: Int, item: T) {
        if (position < 0 || position > dataList.size) return
        dataList.add(position, item)
        notifyItemInserted(position)
    }

    /** Removes the item at [position]; an invalid position is ignored. */
    fun deleteItem(position: Int) {
        if (position < 0 || position >= dataList.size) return
        dataList.removeAt(position)
        notifyItemRemoved(position)
    }

    /** Replaces the item at [position]; an invalid position is ignored. */
    fun updateItem(position: Int, item: T) {
        if (position < 0 || position >= dataList.size) return
        dataList[position] = item
        notifyItemChanged(position)
    }

    /** Returns whether the adapter has no items. */
    fun isEmpty(): Boolean = dataList.isEmpty()

    /** Returns whether the adapter contains at least one item. */
    fun isNotEmpty(): Boolean = dataList.isNotEmpty()

    /** Resolves [resId], or returns an empty string before a host context is available. */
    fun getString(@StringRes resId: Int): String = context?.getString(resId) ?: ""

    /** Returns an immutable snapshot of the current items. */
    fun getAllData(): List<T> = dataList.toList()

    /** Removes every item and refreshes the adapter. */
    @SuppressLint("NotifyDataSetChanged")
    fun clearAllData() {
        dataList.clear()
        notifyDataSetChanged()
    }

    /**
     * Marks child [views] for callbacks on the ViewHolder currently being bound.
     *
     * Call this only from [onBind].
     */
    protected fun bindChildClickListener(vararg views: View) {
        currentHolder?.bindChildClickListener(*views)
    }

    /** Replaces the item click listener; pass `null` to clear it. */
    fun setOnItemClickListener(listener: OnItemClickListener<T>?) {
        this.onItemClickListener = listener
    }

    /** Replaces the item long-click listener; pass `null` to clear it. */
    fun setOnItemLongListener(listener: OnItemLongClickListener<T>?) {
        this.onItemLongClickListener = listener
    }

    /** Replaces the registered-child click listener; pass `null` to clear it. */
    fun setOnItemChildClickListener(listener: OnItemChildClickListener<T>?) {
        this.onItemChildClickListener = listener
    }

    /** ViewHolder containing the concrete [binding] and its [viewType]. */
    inner class BindingViewHolder(
        val binding: ViewBinding,
        val viewType: Int
    ) : RecyclerView.ViewHolder(binding.root) {
        /** Child views that dispatch [OnItemChildClickListener] callbacks. */
        val childClickViews: MutableList<View> = mutableListOf()

        /** Adds [views] to this holder's child click targets. */
        fun bindChildClickListener(vararg views: View) {
            childClickViews.addAll(views)
        }
    }

    /** Receives clicks on a complete item view. */
    fun interface OnItemClickListener<T> {
        /** Called with the bound [item], binding-time [position], and clicked [v]. */
        fun onItemClick(item: T, position: Int, v: View)
    }

    /** Receives clicks from child views registered during [onBind]. */
    fun interface OnItemChildClickListener<T> {
        /** Called with the item, position, clicked [view], and its [viewId]. */
        fun onItemChildClick(item: T, position: Int, view: View, viewId: Int)
    }

    /** Receives long clicks on a complete item view. */
    fun interface OnItemLongClickListener<T> {
        /** Called with the bound [item], binding-time [position], and pressed [v]. */
        fun onItemLongClick(item: T, position: Int, v: View)
    }
}
