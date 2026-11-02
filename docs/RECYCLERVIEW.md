# RecyclerView 接入

## 如何选择

- 已使用 `CommonAdapter<T, VB>` / `MultiViewTypeAdapter<T>`：继续使用，旧导入与 BindingViewHolder 类型保留。
- 小列表、需要同步移动数据的拖拽列表：使用这两个可变适配器，可选传入 DiffUtil callback。
- 经常提交新列表、数据量较大：使用 `ui.recyclerview.BindingListAdapter`，由 ListAdapter 异步差分。
- 加载头尾或多段列表：可搭配 AndroidX ConcatAdapter；这里的点击/拖拽位置均为子适配器位置，不包含头部偏移。
- 固定行列分页使用 `GridPagerSnapHelper`，线性列表分割线使用 `DividerItemDecoration`，具体示例见 [可复用组件](REUSABLE_COMPONENTS.md)。

所有数据更新在主线程执行；条目尽量使用不可变 data class。列表快照只复制容器，不深拷贝条目。

## 1. 兼容适配器

```kotlin
import com.template.framework.ui.base.CommonAdapter

class ItemAdapter : CommonAdapter<ExampleItem, ItemExampleBinding>() {
    override fun onCreateBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int) =
        ItemExampleBinding.inflate(inflater, parent, false)

    override fun onBind(binding: ItemExampleBinding, item: ExampleItem, position: Int, viewType: Int) {
        binding.title.text = item.title
        bindChildClickListener(binding.title)
    }
}

val adapter = ItemAdapter()
recyclerView.adapter = adapter
adapter.setNewDataList(items)
adapter.setOnItemClickListener { item, position, view -> /* 点击时的当前条目和位置 */ }
adapter.setOnItemChildClickListener { item, position, view, viewId -> /* 子视图事件 */ }
adapter.addDataList(nextPage) // 只通知新增范围
adapter.deleteItem(0)
adapter.updateItem(0, replacement)
```

不需要子视图点击时不调用 `bindChildClickListener`。整项、长按及子视图事件都会检查 NO_POSITION；未注册长按监听器不会吞掉长按。完整绑定会清理上次注册的子视图，请每次在绑定钩子中重新注册。

`setNewDataList` 默认全量刷新；希望增量更新时在构造器传入 callback，例如 `CommonAdapter<ExampleItem, ItemExampleBinding>(ITEM_DIFF)`。同步 DiffUtil 适用于小列表，不在主线程计算大列表差分。

多布局使用 `MultiViewTypeAdapter<T>`，重写 `getItemViewType(position)`，在 `onCreateBinding` 返回对应 ViewBinding，再在 `onBind` 按 binding 类型分发。

## 2. 异步列表与 payload

```kotlin
import androidx.recyclerview.widget.DiffUtil
import com.template.framework.ui.recyclerview.BindingListAdapter

val ITEM_DIFF = object : DiffUtil.ItemCallback<ExampleItem>() {
    override fun areItemsTheSame(oldItem: ExampleItem, newItem: ExampleItem) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: ExampleItem, newItem: ExampleItem) = oldItem == newItem
    override fun getChangePayload(oldItem: ExampleItem, newItem: ExampleItem): Any = "title"
}

class ItemAdapter : BindingListAdapter<ExampleItem, ItemExampleBinding>(ITEM_DIFF) {
    override fun onCreateBinding(inflater: LayoutInflater, parent: ViewGroup, viewType: Int) =
        ItemExampleBinding.inflate(inflater, parent, false)

    override fun onBind(binding: ItemExampleBinding, item: ExampleItem, position: Int) {
        binding.title.text = item.title
    }

    override fun onBindPayloads(binding: ItemExampleBinding, item: ExampleItem, position: Int, payloads: List<Any>) {
        if (payloads.all { it == "title" }) binding.title.text = item.title
        else super.onBindPayloads(binding, item, position, payloads)
    }
}

val adapter = ItemAdapter()
adapter.submitList(items)
adapter.onItemClick = { item, position, view -> /* 当前条目 */ }
adapter.submitList(adapter.currentList.map { if (it.id == changedId) it.copy(title = newTitle) else it })
```

`ExampleItem` 与布局仅为示例，使用者在自己的 App 中创建；参考本仓库 `app/data/api/ExampleApi.kt`、`app/ui/ExampleItemAdapter.kt`。

不要修改 `currentList` 或其中的旧对象。两个 `submitList` 重载都会复制容器；快速连续提交时旧任务被新任务替代，commitCallback 不保证每次执行。不要用 callback 作为可靠业务完成事件。

两个兼容适配器也支持 `onBindPayloads`，手动更新用 `updateItem(position, item, payload)` 或 `notifyItemRangeChanged(..., payload)`。payload 不认识或缺失时必须回退完整绑定；如果模型新增字段，更新内容比较和 payload 逻辑。

## 3. 同步拖拽与滑动

```kotlin
import androidx.recyclerview.widget.ItemTouchHelper
import com.template.framework.ui.recyclerview.AdapterTouchCallback

// adapter 是 CommonAdapter 或 MultiViewTypeAdapter，不是异步 BindingListAdapter。
val touchHelper = ItemTouchHelper(AdapterTouchCallback(
    adapter = adapter,
    dragDirections = ItemTouchHelper.UP or ItemTouchHelper.DOWN,
    onMoved = { from, to -> /* 数据已移动，按业务需要保存新顺序 */ }
))
touchHelper.attachToRecyclerView(recyclerView)
```

网格增加 START/END 方向；手动拖动可设置 `longPressDragEnabled = false` 并调用 `touchHelper.startDrag(holder)`。默认不开滑动，启用后在 `onSwiped(position)` 决定删除或确认；框架默认恢复滑走的视图，不擅自删除业务数据。

`moveItem` 同步更新列表，再通知移动；不与异步 submitList 混用。同一 ConcatAdapter 的其他子列表不能拖入此列表。排序只改变数据顺序；若条目显示位置序号，业务另行刷新受影响范围。

## 4. 生命周期与边界

- 在 `onBindingRecycled(binding)` 中取消图片任务、清理重资源，避免复用时显示旧图。
- Fragment 的 `onDestroyView` 解除 `recyclerView.adapter`，释放页面回调；不要把 Adapter 或 ViewBinding 放进长期单例。
- 默认 PREVENT_WHEN_EMPTY，异步数据到达后再恢复滚动位置；特殊场景可自行覆盖策略。
- stable ID 默认关闭，只有数据提供唯一且稳定的 Long ID 才启用；位置或字符串 hash 不保证稳定身份。
- 未注册框架监听器时可直接在绑定钩子设置业务点击；注册框架监听器后由框架统一分派。自行写的点击监听器也要读取当前位置，不能捕获过时条目。
