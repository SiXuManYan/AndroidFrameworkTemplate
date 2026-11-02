# HTTP 使用示例

先完成 [快速接入](QUICKSTART.md) 中的依赖、权限和 `Framework.init`。以下代码放在自己的 App 中，不放入 Library；`BuildConfig` 使用自己 App 的包名。所有 URL 与接口都是示例，没有配套真实后端。

## 1. 正式／测试环境与第三方支付怎么选

- 正式、测试通常是同一套接口的两种环境：启动时显式选择地址，创建一个自家 API 即可，不必同时初始化两套。可由 App 构建变体提供地址，不让框架根据 `DEBUG` 自动改 URL。
- 自家后端 + 微信／支付宝官方支付 SDK：方式 A 通常足够。App 向自家后端请求支付参数，再调 SDK；商户接口由后端调用。
- App 确实直连多个服务：方式 B 更清晰。每个服务保存自己的 API 与响应模型；不同认证规则使用独立 OkHttp，不把自家 Token、版本头或清理登录态的拦截器带给第三方。
- 多个 API 只有在认证、拦截器等规则兼容时才共享客户端。也可用 A 管理自家后端、用独立工厂管理第三方，A / B 并非不能组合。

常见支付流程：`App → 自家后端创建支付订单 → 后端调用支付平台 → App 获取支付参数并调 SDK → 后端通过支付通知／查询确认结果 → App 查询自家订单状态`。商户私钥、签名密钥不能放进 APK；不能只凭客户端支付回调发放权益。仅在平台明确允许客户端直连时，才为对应接口创建 App 侧 API。

运行时切换正式／测试环境时，先取消或等待旧请求结束，再重建对应 API；正式、测试 Token 应分别管理。下面的 `lazy` 示例适用于启动时选定环境，不会自动跟随环境切换。

## 2. 两种方式共用的地址与接口

放在自己的 `app/data/network` 和 `app/data/api`。正式、测试的路径仅用于区分占位配置，接入时替换为各环境的实际完整地址：

```kotlin
object AppEndpoints {
    const val FORMAL_BASE_URL = "https://com.exmpale.xxxx/formal/"
    const val TEST_BASE_URL = "https://com.exmpale.xxxx/test/"
    const val WECHAT_BASE_URL = "https://com.exmpale.xxxx/wechat/"
    const val ALIPAY_BASE_URL = "https://com.exmpale.xxxx/alipay/"

    // 启动时显式选环境；可替换为 App 构建变体提供的地址。
    val backendBaseUrl = TEST_BASE_URL
}
```

接口与本仓库的 `app/data/api/ExampleApi.kt` 一致；放入自己的包，不要依赖示例 App 模块：

```kotlin
import com.template.framework.api.model.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ExampleApi {
    @GET("example/items")
    suspend fun getItems(@Query("page") page: Int = 1): ApiResponse<List<ExampleItem>>

    @POST("example/items")
    suspend fun saveItem(@Body item: ExampleItem): ApiResponse<ExampleItem>
}

data class ExampleItem(val id: Long, val title: String)
```

`ApiResponse` 只演示 `{code, msg, data}` 且业务成功码为 `200` 的后端；其他后端定义自己的模型。第三方不必遵循这个结构，也不要照搬这些虚构路径。

## 3. 初始化后怎么使用

选择 A 或 B 的一个 `AppNetwork` 实现，不要同时复制两个同名对象。接口和客户端保存后重复使用，不要每次点击都重新初始化。

<details>
<summary>方式 A：框架保存 Retrofit → App 保存 API → 页面发起请求</summary>

**第一步：** 在自己的 `Application.onCreate()` 中，基础 `Framework.init(...)` 之后显式初始化 HTTP：

```kotlin
import com.template.framework.Framework
import com.template.framework.network.HttpConfig

Framework.initHttp(HttpConfig(
    baseUrl = AppEndpoints.backendBaseUrl,
    debug = BuildConfig.DEBUG
))
```

需要认证时，在上述 `HttpConfig` 中配置 `getToken` / `clearToken`，见 [快速接入](QUICKSTART.md#可选认证与版本-header)。

**第二步：** 在自己的 `app/data/network/AppNetwork.kt` 中保存 API：

```kotlin
import com.template.framework.api.NetworkModule

object AppNetwork {
    // 首次取得时创建；必须在 initHttp 之后访问。
    val backendApi: ExampleApi by lazy {
        NetworkModule.getRetrofit().create(ExampleApi::class.java)
    }
}
```

**第三步：** 在 Fragment 的 `onViewCreated()` 或按钮点击回调中调用：

```kotlin
requestItems(AppNetwork.backendApi)
```

`requestItems` 的完整实现见下一节。调用 `api.getItems(...)` 才会发起请求，不是 `getRetrofit()` 或 `create()` 时请求。

</details>

<details>
<summary>方式 B：App 保存独立客户端与 API → 页面发起请求</summary>

**第一步：** 保留基础 `Framework.init(...)`，不调用 `Framework.initHttp`。在自己的 `app/data/network/AppNetwork.kt` 中保存对象：

```kotlin
import com.template.framework.Framework
import com.template.framework.api.NetworkModule
import kotlinx.coroutines.flow.first

object AppNetwork {
    private val backendClient by lazy {
        val preferences = Framework.getPreferences()
        NetworkModule.createOkHttpClient(
            getToken = { preferences.accessToken.first() },
            clearToken = { preferences.clearAccessToken() },
            debug = BuildConfig.DEBUG
        )
    }

    val backendApi: ExampleApi by lazy {
        NetworkModule.createApiService<ExampleApi>(
            AppEndpoints.backendBaseUrl,
            backendClient
        )
    }

    // 仅实际直连第三方时访问；不复用自家认证或清理登录态的逻辑。
    val wechatRetrofit by lazy {
        NetworkModule.createRetrofit(
            AppEndpoints.WECHAT_BASE_URL,
            NetworkModule.createOkHttpClient(debug = BuildConfig.DEBUG)
        )
    }

    val alipayRetrofit by lazy {
        NetworkModule.createRetrofit(
            AppEndpoints.ALIPAY_BASE_URL,
            NetworkModule.createOkHttpClient(debug = BuildConfig.DEBUG)
        )
    }
}
```

自家后端不需要认证时，删掉 `getToken` / `clearToken`。登录成功后另行保存 Token，这些回调本身不保存 Token。

第三方客户端目前没有认证拦截器；只有明确允许移动端调用时，才按平台要求在构建处配置客户端认证，再用对应 Retrofit 的 `create(自己的接口类::class.java)` 创建并保存服务。接口和响应模型在 App 中按官方协议定义，此处不虚构支付商户接口。如果使用官方支付 SDK，无需访问以上两个第三方 Retrofit。

**第二步：** 在 Fragment 的 `onViewCreated()` 或按钮点击回调中调用：

```kotlin
requestItems(AppNetwork.backendApi)
```

请求写法与 A 相同。B 不使用 `NetworkModule.getRetrofit()`，因为它没有初始化框架的全局 Retrofit。

</details>

## 4. 页面发起请求与处理结果（A / B 共用）

把下面方法放在自己的 Fragment 类中；页面销毁视图时，请求协程会取消。为了突出调用流程，这里用 Toast 展示结果，实际项目可换成列表渲染，或通过 Repository / ViewModel 管理数据。

```kotlin
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

private fun requestItems(api: ExampleApi) {
    viewLifecycleOwner.lifecycleScope.launch {
        val message = try {
            // Retrofit 的 suspend 请求异步执行；无需用 runBlocking 或手动切 IO。
            val response = api.getItems(page = 1)
            if (response.isSuccess) {
                "获取到 ${response.data.orEmpty().size} 条数据"
            } else {
                "业务失败：${response.code}，${response.message}"
            }
        } catch (e: CancellationException) {
            // 页面销毁属于正常取消，不能吞掉或当作请求失败提示。
            throw e
        } catch (e: HttpException) {
            "HTTP 失败：${e.code()}"
        } catch (e: IOException) {
            "网络不可用或连接失败"
        } catch (e: Exception) {
            "请求或响应解析失败"
        }
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
```

`getItems(page = 1)` 会发起 `GET <baseUrl>example/items?page=1`；网络或 HTTP 错误会抛异常，HTTP 成功仍须检查后端的业务成功码。

发送 POST 时，在同样的协程与异常处理结构中调用 `api.saveItem(ExampleItem(id = 1, title = "示例"))`；处理返回的 `ApiResponse<ExampleItem>`。按钮触发的 POST 不要放进 `repeatOnLifecycle`，避免页面重新进入前台时重复提交。
