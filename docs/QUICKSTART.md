# 快速接入

## 0. 在自己的 App 中依赖 Library

只复制 `core_framework`，不复制本仓库的示例 `app`。在使用者工程的 `settings.gradle.kts` 添加：

```kotlin
include(":core_framework")
```

在使用者的 App `build.gradle.kts` 中添加：

```kotlin
dependencies { implementation(project(":core_framework")) }
android { buildFeatures { viewBinding = true } }
```

库构建文件使用 `libs` 版本目录：将其用到的别名合并到使用者的 `gradle/libs.versions.toml`，或改为使用者已有依赖坐标和插件版本。不要覆盖整个工程的版本目录；统一 AGP/Kotlin 与 SDK 要求，最低支持 API 29。

公开 API 的 AppCompat、Material、Lifecycle、RecyclerView、Retrofit、OkHttp、Gson、DataStore 和协程类型通过 `api` 暴露。使用者自己添加 Room 时才需要 Room / KSP；库不需要依赖示例 App。当前未发布 Maven 坐标；手动复制 AAR 不会携带依赖元数据，优先使用源码模块接入。

## 1. 基础初始化

在自己的 Application 中配置，并在 Manifest 设置 `android:name`：

```kotlin
import com.template.framework.Framework
import com.template.framework.FrameworkConfig

Framework.init(
    this,
    FrameworkConfig(BuildConfig.DEBUG, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME)
)
```

仅首次初始化生效。不启动 HTTP、WebSocket 或 Room；偏好设置在 `getPreferences()` 时才创建。业务不必使用 `getRepository()`，优先组合自己的 API 与存储。

## 2. HTTP：选择由谁管理实例

Retrofit 把接口方法转换为 HTTP 请求并解析响应；OkHttp 负责实际发送请求、连接池、超时和 TLS。下面两种方式都使用 Retrofit + OkHttp，区别是实例由框架保存，还是由自己的 App 保存。初始化只创建对象，不会自动请求服务器，也不初始化 WebSocket。

### A. 框架保存共享实例（普通单后端 App 优先选择）

```kotlin
import com.template.framework.api.NetworkModule
import com.template.framework.network.HttpConfig

Framework.initHttp(HttpConfig(
    baseUrl = "https://com.exmpale.xxxx/", // 替换为自己的后端
    debug = BuildConfig.DEBUG
))
val api = NetworkModule.getRetrofit().create(ExampleApi::class.java)
// ExampleApi 参考本仓库 app/data/api，然后在自己的 App 中定义接口。
```

可通过 `NetworkModule.getClient()` 取得这个 Retrofit 使用的 OkHttp，供其他 API 或图片加载显式复用。

### B. App 自己保存实例（已有客户端或多个后端时选择）

不必调用 `initHttp`；保存返回的 `client` / `api`，多个服务传入同一个 `client` 即可共享连接池：

```kotlin
val client = NetworkModule.createOkHttpClient(
    connectTimeoutSeconds = 10,
    readTimeoutSeconds = 20,
    writeTimeoutSeconds = null, // 保留 OkHttp 默认写超时
    debug = BuildConfig.DEBUG
)
val api = NetworkModule.createApiService<ExampleApi>("https://com.exmpale.xxxx/", client)
```

`createApiService` 内部仍会创建 Retrofit，不是“只用 OkHttp”。真正只用 OkHttp 时，仅创建 `client`，随后自己用 OkHttp 的 `Request` / `newCall` 发请求，不调用 `createApiService`。

### 可选认证与版本 Header

默认不会额外添加 `Authorization`、`clientid`、`VersionCode` 或 `VersionName` 这些业务 Header，不代表没有任何 HTTP Header。后端需要哪种就显式配置哪种；示例 `ApiResponse` 仅适用于 `{code, msg, data}`，不同后端直接定义自己的响应模型。

```kotlin
val preferences = Framework.getPreferences()
Framework.setOnTokenExpired { /* 回调在请求线程触发，UI 操作切到主线程 */ }
Framework.initHttp(HttpConfig(
    baseUrl = "https://com.exmpale.xxxx/",
    getToken = { preferences.accessToken.first() }, // 请求时读 Token；import kotlinx.coroutines.flow.first
    clearToken = { preferences.clearAccessToken() }, // 认证失效时删除 Token
    includeVersionHeaders = true, // 仅后端需要应用版本 Header 时开启
    debug = BuildConfig.DEBUG
))
```

- `getToken` 是读取回调，不保存 Token；`first()` 取 Flow 首次发出的值后结束收集。请求未自带认证头且读到非空 Token 时，自动添加 `Authorization: Bearer <token>`。
- `clearToken` 是删除回调；收到 HTTP 401，或符合内置约定的 HTTP 200 JSON 响应中 `code == 401` 时执行，再通知 `setOnTokenExpired`。不负责自动刷新 Token。
- 登录成功后，在协程的 IO 上下文中显式调用 `preferences.saveAccessToken(tokenFromServer)` 保存；当前存储是 DataStore Preferences 主存储 + SharedPreferences 备份，保存和删除会操作两者，不是加密存储。两个回调也可换成自己的存储实现。
- `includeVersionHeaders` 是本库参数，不是 OkHttp 自带参数；开启后从 `Framework.init` 的配置取应用版本，添加 `VersionCode` / `VersionName`。`clientId` 同样是本库的可选参数，不会自动生成设备 ID。

提供 `HttpConfig.client` 表示“直接使用已有客户端”，框架不再构建或修改它。因此同一个 `HttpConfig` 中的 `debug`、超时、`getToken`、`clearToken`、`clientId`、`includeVersionHeaders` 和 `configureClient` 不会应用到该客户端；这些选项应在创建 `client` 时配置。重复初始化可更换 URL，但已创建的 API 不会自动切换，必须重新创建。

## 3. WebSocket：无需初始化 HTTP

```kotlin
import com.template.framework.websocket.WebSocketConfig

val socket = Framework.initWebSocket(WebSocketConfig(
    url = "wss://com.exmpale.xxxx/example/socket",
    headers = { emptyMap() }, // 重连时重新读取，勿在这里阻塞读取 DataStore
    autoReconnect = true
))
socket.connect()
socket.sendMessage("ping") // true 表示入队，不代表服务端确认
socket.disconnect()        // 停止重连，可再次 connect
socket.release()           // 释放后需重新初始化或创建实例
```

配置完整 URL，不按 Debug 改写 ws/wss 或路径。重复 `connect()` 会跳过；刷新认证用 `connect(forceReconnect = true)`。首次握手失败与意外断线均会重试，默认退避 5s → 10s → 20s → 30s。

在 Fragment 中按视图生命周期收集：

```kotlin
viewLifecycleOwner.lifecycleScope.launch {
    viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
        launch { socket.connectionState.collect { state -> /* 渲染状态 */ } }
        launch { socket.messageFlow.collect { message -> /* 处理消息 */ } }
    }
}
```

状态是 StateFlow；消息是无重放的 SharedFlow，同样内容连续到达仍逐条发送。没有订阅者时不保存消息，慢订阅者最多缓冲 64 条，超量丢弃最旧事件；可靠业务需要在 App 层实现确认、持久化与补偿。

多连接用 `WebSocketManager.create(config)`，由持有者释放。全局 `initialize` 会释放旧实例。传入共享客户端时 `release` 不关闭该客户端。

## 4. TLS 与权限

默认使用系统信任链和主机名校验。自定义 CA 通过独立工具配置，两种网络互不依赖：

```kotlin
configureClient = {
    TlsConfig.trustCertificate(this, appContext, R.raw.server_ca)
}
```

将该 lambda 放入 `HttpConfig` 或 `WebSocketConfig`；import `com.template.framework.network.TlsConfig`。提供自定义证书会替代系统信任库，证书必须与目标主机匹配。

使用网络需在 App Manifest 声明 `INTERNET`。模板默认禁止明文；本地 HTTP/WS 联调时仅在 debug Manifest 或按域名的 Network Security Config 中开放，不要全局关闭 TLS 校验。

## 5. 存储与 UI

- `Framework.getPreferences()` 提供 Token、语言、完整 HTTP / WebSocket URL 的 Flow 与 suspend 保存方法。旧 IP/Port Key 仅为兼容保留，不再自动生成 URL。
- DataStore 文件名固定为 `framework_preferences`，不是运行时配置。自定义文件及 Key 建议在 App 层建立自己的存储管理器。
- `DataStoreBackupHelper` 提供双写与空值回退，不是加密存储，也不保证所有故障下无损恢复；敏感数据另行选择安全存储与备份策略。
- `BaseActivity` 首帧前仅读取语言备份，不阻塞等待 DataStore；若外部迁移只恢复了 DataStore，应由 App 异步应用语言。
- Room 示例在 `app/data/database/ExampleDatabase.kt`，首次 `getInstance(context)` 时创建；业务继承 `RoomDatabase`，升级提供 Migration，勿默认破坏性清库。
- Activity / Fragment 继承对应的 ViewBinding 基类，重写 `initViewBinding`、`initView`、`initListener`、`initData`；Fragment Binding 只在视图生命周期内可用。
- 系统栏默认 edge-to-edge，确有 kiosk 需求时显式调用 `FullScreenUtils.enableFullScreen(this)`。
- 列表接入见 [RecyclerView](RECYCLERVIEW.md)。
- 流式布局、评分、步进器、分页吸附、分割线、加载弹窗及空响应转换器见 [可复用组件](REUSABLE_COMPONENTS.md)。加载弹窗需要宿主使用 MaterialComponents 或兼容的 Material 主题。

## 6. Android Studio 检查

同步并构建 App，确认 Room / ViewBinding 生成成功；运行后点击本地条目观察差分更新，再替换 URL 验证 HTTP-only、WS-only、首次连接失败重连、手动断开和页面销毁释放。当前未执行这些运行时检查。
