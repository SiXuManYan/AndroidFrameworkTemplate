# 可复用组件说明

本文记录从 WhereRebirth 提炼并纳入 AndroidFrameworkTemplate `:core_framework`
的通用组件，包括组件职责、使用方式、资源约定及未纳入能力的取舍依据。

## 组件清单

| 类/资源 | 包名或目录 | 用途 |
| --- | --- | --- |
| `DecimalUtils` | `com.template.framework.util` | 基于 `BigDecimal` 的统一精度与舍入运算 |
| `StringNumberExtensions` | `com.template.framework.util` | 可空字符串安全转换为 Int、Long、BigDecimal |
| `AppInfoUtils` | `com.template.framework.util` | 获取版本信息、创建拨号 Intent、打开系统拨号器 |
| `EmptyBodyConverterFactory` | `com.template.framework.api` | Retrofit 空响应体转 `null`，非空响应交给后续 Converter |
| `FlowLayout` | `com.template.framework.ui.widget` | 支持间距、最大行数和 RTL 的流式 ViewGroup |
| `RatingStarView` | `com.template.framework.ui.widget` | Canvas 星级评分，支持半星、只读、RTL 和状态恢复 |
| `NumberStepperView` | `com.template.framework.ui.widget` | 有上下限、步长和变更回调的整数步进器 |
| `GridPagerSnapHelper` | `com.template.framework.ui.recyclerview` | RecyclerView 固定行列分页吸附 |
| `DividerItemDecoration` | `com.template.framework.ui.recyclerview` | 无 BRVAH 依赖的横向/纵向分割线 |
| `LoadingDialog` | `com.template.framework.ui.dialog` | 基于现有 `BaseDialog` 的通用加载弹窗 |
| View 属性 | `core_framework/src/main/res/values/` | `fw_*` 自定义属性 |
| Loading/Stepper 资源 | `core_framework/src/main/res/` | 加载布局、加减图标和中英文无障碍文本 |

## 工具与网络

### DecimalUtils

金额或其他要求十进制定点精度的数据，应优先从字符串构造 `BigDecimal`。从
`Double` 转换时使用 `fromDouble`，不要直接调用 `BigDecimal(double)`。

```kotlin
import com.template.framework.util.DecimalUtils
import java.math.BigDecimal
import java.math.RoundingMode

val unitPrice = BigDecimal("19.90")
val quantity = BigDecimal("3")
val subtotal = DecimalUtils.multiply(unitPrice, quantity) // 59.70

val average = DecimalUtils.divide(
    dividend = BigDecimal("10"),
    divisor = BigDecimal("3"),
    scale = 3,
    roundingMode = RoundingMode.HALF_UP,
) // 3.333

val total = DecimalUtils.sum(
    listOf(BigDecimal("0.10"), BigDecimal("0.20"), BigDecimal("0.30")),
)
```

`divide` 遇到除数为零时会保留 `BigDecimal` 的标准异常行为，调用方应按业务语义预先
处理。

### StringNumberExtensions

```kotlin
import com.template.framework.util.toBigDecimalOrDefault
import com.template.framework.util.toIntOrDefault
import com.template.framework.util.toLongOrDefault
import java.math.BigDecimal

val page = inputPage.toIntOrDefault(defaultValue = 1)
val recordId = rawId.toLongOrDefault()
val price = priceText.toBigDecimalOrDefault(BigDecimal.ZERO)
```

扩展函数会先 `trim`；空白、`null` 或格式错误时返回指定默认值。

### AppInfoUtils

```kotlin
import com.template.framework.util.AppInfoUtils

val versionName = AppInfoUtils.getVersionName(context).orEmpty()
val versionCode = AppInfoUtils.getVersionCode(context) ?: 0L

val opened = AppInfoUtils.openDialer(context, "400-123-4567")
if (!opened) {
    // 当前设备没有可处理 ACTION_DIAL 的 Activity
}
```

`openDialer` 使用 `ACTION_DIAL`，不会直接拨出电话，因此不需要
`CALL_PHONE` 权限。

### EmptyBodyConverterFactory

工厂必须放在 JSON Converter 之前。空响应体返回 `null`，非空响应体继续交给后续
Converter。

```kotlin
import com.template.framework.api.EmptyBodyConverterFactory
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val retrofit = Retrofit.Builder()
    .baseUrl("https://api.example.com/")
    .addConverterFactory(EmptyBodyConverterFactory.create())
    .addConverterFactory(GsonConverterFactory.create())
    .build()
```

如果接口允许服务器返回完全空的 body，对应返回类型应允许 `null`，并由 repository
统一转换成业务状态，避免在 UI 层散落空值判断。

## 自定义 View

XML 示例需要在根布局声明：

```xml
xmlns:app="http://schemas.android.com/apk/res-auto"
```

### FlowLayout

```xml
<com.template.framework.ui.widget.FlowLayout
    android:id="@+id/tagFlow"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:paddingVertical="8dp"
    app:fw_horizontalSpacing="8dp"
    app:fw_verticalSpacing="8dp"
    app:fw_maxRows="2">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Android" />

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="Kotlin" />
</com.template.framework.ui.widget.FlowLayout>
```

也可在 Kotlin 中调整，属性值单位为 px：

```kotlin
binding.tagFlow.apply {
    horizontalSpacing = ScreenUtil.dp2px(context, 8)
    verticalSpacing = ScreenUtil.dp2px(context, 8)
    maxRows = 3
}
```

`maxRows = 0` 会隐藏所有行；子 View 支持 `MarginLayoutParams`，RTL 下会从右侧开始
排列。

### RatingStarView

```xml
<com.template.framework.ui.widget.RatingStarView
    android:id="@+id/rating"
    android:layout_width="200dp"
    android:layout_height="40dp"
    android:padding="4dp"
    app:fw_starCount="5"
    app:fw_rating="3.5"
    app:fw_stepSize="0.5"
    app:fw_activeColor="#FFB700"
    app:fw_inactiveColor="#E8E8E8"
    app:fw_strokeColor="#A0A0A0"
    app:fw_strokeWidth="1dp"
    app:fw_starSpacing="6dp"
    app:fw_starCornerRadius="1dp"
    app:fw_ratingEnabled="true" />
```

```kotlin
binding.rating.apply {
    setOnRatingChangeListener { _, newRating, fromUser ->
        if (fromUser) viewModel.updateRating(newRating)
    }

    // 展示服务端结果时同样会触发 listener，回调中的 fromUser 为 false。
    rating = 4.5f
    ratingEnabled = false
}
```

`stepSize` 仅支持 0.5 或 1.0；其他数值会归一化到最近的受支持步长。

### NumberStepperView

```xml
<com.template.framework.ui.widget.NumberStepperView
    android:id="@+id/quantityStepper"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:fw_stepper_value="1"
    app:fw_stepper_min_value="1"
    app:fw_stepper_max_value="99"
    app:fw_stepper_step="1"
    app:fw_stepper_hide_value_at_min="false"
    app:fw_stepper_show_decrement_at_min="false" />
```

```kotlin
binding.quantityStepper.apply {
    onValueChangeListener =
        NumberStepperView.OnValueChangeListener { _, oldValue, newValue, change ->
            viewModel.updateQuantity(newValue)
        }

    // 默认不通知 listener。
    value = 2

    // 需要把程序化变更作为事件发出时显式指定 notify。
    setValue(3, notify = true)
}
```

`minValue <= maxValue`、`step > 0` 是强约束；越界 value 会自动压到有效范围。

## RecyclerView 与 Dialog

### GridPagerSnapHelper

下面示例每页 2 行、3 列。Adapter 必须保证每页的 6 个元素在 adapter position 中连续。

```kotlin
val rows = 2
val columns = 3

binding.recyclerView.apply {
    layoutManager = GridLayoutManager(
        context,
        rows,
        RecyclerView.HORIZONTAL,
        false,
    )
    adapter = pageAdapter
}

val pagerSnapHelper = GridPagerSnapHelper(rows, columns)
pagerSnapHelper.attachToRecyclerView(binding.recyclerView)

// 在 Fragment.onDestroyView() 中调用：
// pagerSnapHelper.attachToRecyclerView(null)
```

`rows` 和 `columns` 必须大于零。首尾页会按页索引限制目标位置，最后一页即使不满，
也会吸附到最后一页的第一个 item。

### DividerItemDecoration

```kotlin
binding.recyclerView.addItemDecoration(
    DividerItemDecoration(
        color = ContextCompat.getColor(requireContext(), R.color.divider),
        thicknessPx = ScreenUtil.dp2px(requireContext(), 1),
        orientation = RecyclerView.VERTICAL,
        drawLastItem = false,
        startPaddingPx = ScreenUtil.dp2px(requireContext(), 16),
        endPaddingPx = ScreenUtil.dp2px(requireContext(), 16),
    )
)
```

横向列表把 `orientation` 改为 `RecyclerView.HORIZONTAL`。此时 start/end padding
对应上/下留白；纵向列表中对应左/右留白。该实现只读取标准 `RecyclerView.Adapter`
的 itemCount，不识别也不依赖 BRVAH 的 Header/Footer API。

### LoadingDialog

```kotlin
val loadingDialog = LoadingDialog(
    context = requireContext(),
    message = getString(R.string.loading_data),
)

loadingDialog.show()
loadingDialog.message = getString(R.string.submitting)

// 请求完成或页面销毁时关闭。
loadingDialog.dismiss()
```

弹窗默认不可取消，也不响应外部点击。如需允许系统返回键取消，可在构造时传入
`cancelable = true`；外部点击仍不会取消。布局使用目标框架现有
`BaseDialog` 约定的 `rootLayout` 和 `cardView` ID。

## 资源与依赖策略

- 新增 drawable、layout、string 和 id 使用 `framework_` 前缀；LoadingDialog 复用
  `BaseDialog` 已有契约 ID `rootLayout`、`cardView`，不另建同义 ID。
- 自定义属性使用 `fw_` 前缀，减少宿主 App 与其他 library 的资源名冲突。
- Kotlin 包统一位于 `com.template.framework` 下，按 `util`、`api`、
  `ui.widget`、`ui.recyclerview`、`ui.dialog` 分责。
- 本批组件只使用 Android SDK 以及目标 `:core_framework` 已有的 AndroidX Core、
  AppCompat、Material、RecyclerView、Retrofit、OkHttp 等依赖，不要求新增三方依赖。
- 本批体量和依赖边界适合放在现有 `:core_framework`，无需新建 module。以后若引入
  图片、媒体等可选重依赖，再评估拆成独立 module。

## 暂缓或不纳入

| 类别/代表类 | 处理 | 原因 |
| --- | --- | --- |
| Glide 工具（如 `GlideUtil`） | 暂缓 | 目标框架目前没有 Glide；图片加载涉及缓存、占位图、生命周期和加载策略，适合以后作为可选图片模块设计 |
| Json/Gson 工具 | 暂缓 | 全局 Gson 实例会固化日期、空值和字段策略；应先确定 Gson、Moshi 或 kotlinx.serialization 的统一边界，避免与 Retrofit Converter 重复 |
| Time/Date 工具 | 不整类复制 | 目标已有 `TimeUtils`，来源实现还可能隐含时区、Locale 和格式约定；只应按缺失能力逐方法合并并补测试 |
| Permission 工具 | 暂缓 | 通常绑定第三方权限库、Manifest 权限和 Activity 交互；应先定义宿主回调及拒绝/永久拒绝语义 |
| 业务 Adapter、ItemDecoration | 淘汰直接迁移 | 依赖 BRVAH、业务 model、业务资源或 Header/Footer 约定；仅保留本批独立的 `DividerItemDecoration` |
| `VerificationCode` 相关 View/倒计时 | 保留在业务层 | 绑定登录/注册流程、短信接口、倒计时文案和生命周期，不是通用框架控件 |
| XBanner 及 Banner Adapter | 暂缓 | 引入 XBanner 和图片加载链路，且点击、埋点、轮播策略通常属于业务 |
| `BusinessUtils` 等大而全工具类 | 淘汰直接迁移 | 职责混杂、依赖业务单例和页面跳转；只提炼无状态、边界清晰且可测试的方法 |
| 分享、地图、支付、推送工具 | 保留在 feature/integration 层 | 依赖供应商 SDK、Manifest 配置、密钥与产品流程，不应进入基础 core |

后续评审这些暂缓项时，应以“是否无业务 model、无页面跳转、依赖可选、API 可测试”为
准入条件，而不是按来源目录整体复制。
