package com.template.framework.ui.recyclerview

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

/** 检查同步数据与精确通知的对应关系，不创建 View 或运行页面。 */
class MutableDataAdapterTest {
    private class Adapter : MutableDataAdapter<Int, RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = error("Not used")
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
        fun submitInternalList() = setNewDataList(dataList)
    }

    @Test
    fun appendAndClearDispatchExactRanges() {
        val adapter = Adapter()
        val events = mutableListOf<String>()
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) { events.add("insert:$positionStart:$itemCount") }
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) { events.add("remove:$positionStart:$itemCount") }
        })
        adapter.addDataList(listOf(1, 2, 3))
        adapter.clearAllData()
        assertEquals(listOf("insert:0:3", "remove:0:3"), events)
    }

    @Test
    fun moveKeepsDataOrderAndRejectsInvalidPositions() {
        val adapter = Adapter()
        adapter.addDataList(listOf(1, 2, 3))
        adapter.moveItem(0, 2)
        assertEquals(listOf(2, 3, 1), adapter.getAllData())
        assertFalse(adapter.moveItem(RecyclerView.NO_POSITION, 1))
    }

    @Test
    fun replacingWithSameMutableListDoesNotEraseIt() {
        val adapter = Adapter()
        adapter.addDataList(listOf(1, 2))
        adapter.submitInternalList()
        assertEquals(listOf(1, 2), adapter.getAllData())
    }
}
