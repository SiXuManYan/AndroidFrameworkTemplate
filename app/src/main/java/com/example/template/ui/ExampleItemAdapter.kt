package com.example.template.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import com.example.template.data.api.ExampleItem
import com.example.template.databinding.ItemExampleBinding
import com.template.framework.ui.recyclerview.BindingListAdapter

/** 用列表身份、内容差分和 payload 演示局部文本刷新。 */
class ExampleItemAdapter : BindingListAdapter<ExampleItem, ItemExampleBinding>(DIFF) {
    override fun onCreateBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int) =
        ItemExampleBinding.inflate(inflater, parent, false)

    override fun onBind(binding: ItemExampleBinding, item: ExampleItem, position: Int) {
        binding.title.text = item.title
    }

    override fun onBindPayloads(binding: ItemExampleBinding, item: ExampleItem, position: Int, payloads: List<Any>) {
        if (payloads.all { it == TITLE_CHANGED }) binding.title.text = item.title
        else super.onBindPayloads(binding, item, position, payloads)
    }

    companion object {
        private const val TITLE_CHANGED = "title_changed"
        private val DIFF = object : DiffUtil.ItemCallback<ExampleItem>() {
            override fun areItemsTheSame(oldItem: ExampleItem, newItem: ExampleItem) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: ExampleItem, newItem: ExampleItem) = oldItem == newItem
            override fun getChangePayload(oldItem: ExampleItem, newItem: ExampleItem): Any = TITLE_CHANGED
        }
    }
}
