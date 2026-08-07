# 接入新项目指南

本文说明如何从模板创建业务项目。开始前请准备支持 AGP 8.13.2 的 Android Studio、
JDK 17 和 Android SDK Platform 36。

## 步骤 1：获取模板

```bash
git clone https://github.com/SiXuManYan/AndroidFrameworkTemplate.git my-new-project
cd my-new-project
./gradlew :app:assembleDebug
```

先确认原始模板可以构建，再开始改名和删除 Demo。Windows 将最后一条命令改为
`gradlew.bat :app:assembleDebug`。

不需要示例页面时，可删除：

- `app/src/main/java/com/example/template/MainActivity.kt`
- `app/src/main/res/layout/activity_main.xml`

删除后需要同步从 Manifest 移除 `.MainActivity`，并配置自己的启动 Activity。

## 步骤 2：重命名包名

将 `com.example.template` 替换为业务包名，例如 `com.mycompany.myapp`。至少检查：

1. `app/build.gradle.kts` 中的 `namespace` 和 `applicationId`。
2. `App.kt`、`MainActivity.kt` 的 `package` 声明及对应源码目录。
3. Manifest 中相对类名 `.App`、`.MainActivity` 是否仍能由新 namespace 正确解析。
4. `settings.gradle.kts` 中的 `rootProject.name`。

`com.template.framework` 是框架库的独立命名空间，通常无需修改。确需修改时，要同步
更新 `core_framework` 的 namespace、全部 Kotlin package、源码目录和 App 中的 import。

## 步骤 3：配置签名与版本

修改 `app/build.gradle.kts`：

```kotlin
defaultConfig {
    applicationId = "com.mycompany.myapp"
    versionCode = 1
    versionName = "1.0.0"
}

signingConfigs {
    create("release") {
        storeFile = file("my-release.jks")
        storePassword = "..."
        keyAlias = "..."
        keyPassword = "..."
    }
}
```

示例中的密码只是配置位置说明。正式项目应从未提交的本地属性、环境变量或 CI Secret
读取签名信息，不要把 keystore 和明文密码提交到仓库。配置完成后同时验证
`:app:assembleDebug` 和 `:app:assembleRelease`。

## 步骤 4：在 FrameworkConfig 中配置运行时参数

修改 `app/src/main/java/com/mycompany/myapp/App.kt`：

```kotlin
Framework.init(
    app = this,
    config = FrameworkConfig(
        debug = BuildConfig.DEBUG,
        versionCode = BuildConfig.VERSION_CODE,
        versionName = BuildConfig.VERSION_NAME,
        sslCertRawResId = R.raw.my_cert,  // 可选；为空时使用系统信任链
        debugApiPrefix = "dev-api",
        releaseApiPrefix = "prod-api"
    )
)
```

配置含义：

| 参数 | 说明 |
| --- | --- |
| `debug` | 控制日志，并决定 HTTP/WS 使用明文还是 TLS |
| `versionCode` / `versionName` | 自动写入请求 Header |
| `debugApiPrefix` / `releaseApiPrefix` | 拼接到 HTTP 和 WebSocket 地址的路径前缀 |
| `sslCertRawResId` | 可选自定义 CA/服务器证书；`null` 使用系统信任链 |
| `dataStoreName` | 当前用于派生默认 Room 数据库名；框架 DataStore 名仍固定 |
| `defaultServerIp` / `defaultServerPort` | 当前为预留字段，业务仍需主动保存初始地址 |

## 步骤 5：实现业务

### 5.1 定义业务接口

与具体服务端绑定的接口建议放在 App 的 `data/remote/`，不要继续写入通用框架模块：

```kotlin
interface MyApi {
    @GET("user/profile")
    suspend fun getProfile(): ApiResponse<UserProfile>

    @POST("user/update")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): ApiResponse<Unit>
}
```

### 5.2 创建自定义 Repository

在 `app/src/main/java/com/mycompany/myapp/data/` 下添加：

```kotlin
class AppRepository private constructor(
    context: Context,
    private val preferences: PreferencesManager = Framework.getPreferences(),
) : FrameworkRepository(context, preferences) {

    private suspend fun createMyApi(): MyApi = NetworkModule.createApiService<MyApi>(
        baseUrl = getCurrentBaseUrl(),
        getToken = { preferences.accessToken.first() },
        clearToken = { preferences.clearAccessToken() },
    )

    suspend fun getProfile(): ApiResponse<UserProfile> =
        createMyApi().getProfile()

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(context: Context): AppRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
```

上述写法保留了 Token 注入和 401 清理行为。高频请求场景应像 `FrameworkRepository`
一样按 Base URL 缓存自定义 ApiService，并在服务器地址变化时清除缓存。

### 5.3 实现 Activity

```kotlin
class ProfileActivity : BaseActivity<ActivityProfileBinding>() {
    override fun initViewBinding() = ActivityProfileBinding.inflate(layoutInflater)
    override fun initView() { /* 初始化视图 */ }
    override fun initListener() { /* 设置监听 */ }
    override fun initData() {
        lifecycleScope.launch {
            val response = AppRepository.getInstance(this@ProfileActivity).getProfile()
            if (response.isSuccess) {
                binding.tvName.text = response.data?.name
            }
        }
    }
}
```

### 5.4 配置 Android 12 SplashScreen

模板的 `MainActivity` 已包含 `installSplashScreen()`，Manifest 使用
`Theme.Template.Starting` 作为启动主题。替换启动图标或背景色时，修改
`app/src/main/res/values/themes.xml` 中的 `windowSplashScreenAnimatedIcon`
和 `windowSplashScreenBackground` 即可，不需要额外创建 SplashActivity。

## 常见问题

### Q1: 如何修改 DataStore 文件名？

修改 `core_framework/src/main/java/com/template/framework/datastore/PreferencesManager.kt`：

```kotlin
private val Context.frameworkDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "my_app_preferences"  // 修改此处
)
```

`FrameworkConfig.dataStoreName` 当前不会修改这个文件名。若只是业务数据需要独立存储，
优先在 App 模块创建单独的 DataStore，不要修改框架默认存储。

### Q2: 如何添加自定义 DataStore Key？

框架 DataStore 和 Key 被封装在 `PreferencesManager` 内部，不能只靠继承增加 Key。
在 App 模块创建独立 Manager：

```kotlin
private val Context.appDataStore by preferencesDataStore(name = "app_preferences")

class AppPreferences(context: Context) {
    private val dataStore = context.applicationContext.appDataStore
    private val userIdKey = stringPreferencesKey("user_id")

    val userId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[userIdKey]
    }

    suspend fun saveUserId(userId: String) {
        dataStore.edit { preferences -> preferences[userIdKey] = userId }
    }
}
```

### Q3: 如何禁用 BaseActivity 的语言自动应用？

当前 `applyLanguageSettingsSync()` 是 `BaseActivity` 的私有实现，子类不能覆盖。无需框架
语言能力的页面可以直接继承 `AppCompatActivity`；若整个项目都不需要，应在自己的模板
分支中为 BaseActivity 增加开关，再由子类覆盖该开关。

### Q4: 如何让 WebSocket 自动连接？

框架不会在启动时擅自建立连接。登录成功且服务器地址已确定后主动调用：

```kotlin
Framework.getRepository().connectWebSocket(ip = "192.168.1.1", port = "8080")
```

首次连接由业务触发；连接异常断开后的重连由 `WebSocketManager` 处理。页面或账号退出时
调用 `disconnectWebSocket()`，避免继续重连。

### Q5: Token 失效如何跳转到登录页？

在 Application.onCreate 中注入：

```kotlin
Framework.setOnTokenExpired {
    Handler(Looper.getMainLooper()).post {
        val intent = Intent(this, LoginActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(intent)
    }
}
```

回调可能从 OkHttp 工作线程触发，因此导航前要切到主线程。当前实现会清除 Token 并通知
业务，不包含自动刷新 Token。

### Q6: 如何配置自定义 SSL 证书？

1. 将证书文件放到 `app/src/main/res/raw/my_cert.crt`
2. `FrameworkConfig.sslCertRawResId = R.raw.my_cert`

默认使用系统信任链和主机名校验。Debug 连接自签名 HTTPS/WSS 服务时，同样通过
`sslCertRawResId` 提供可信证书，不建议关闭证书或主机名校验。

### Q7: 如何添加自定义数据库实体？

业务数据库建议在 App 模块独立定义，避免业务迁移和框架示例表绑定：

```kotlin
@Database(
    entities = [MyEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun myDao(): MyDao
}
```

使用 `Room.databaseBuilder(context, AppDatabase::class.java, "app.db")` 创建并缓存实例。
如果业务确实需要复用 `FrameworkDatabase` 的三张示例表，则继承它，并在 App 的
`@Database.entities` 中显式列出框架实体和业务实体；同时必须使用 `AppDatabase::class.java`
创建独立实例，不能继续调用 `FrameworkDatabase.getDatabase()`。

### Q8: 如何启用平板 kiosk 全屏？

`BaseActivity` 默认采用适合手机和平板的 edge-to-edge 布局，并保留系统栏。
只在确实需要隐藏状态栏和导航栏的页面显式调用：

```kotlin
FullScreenUtils.enableFullScreen(this)
```

### Q9: Android Studio 提示 Gradle JVM 25 不兼容怎么办？

Gradle 8.13 支持的运行 JVM 上限是 23，而本项目固定使用 JDK 17。打开 Android Studio
的 **Settings/Preferences > Build, Execution, Deployment > Build Tools > Gradle**，将
**Gradle JDK** 改成 `jbr-17` 或已安装的 JDK 17，然后重新 Sync。

可用以下命令核对：

```bash
./gradlew --version
```

`settings.gradle.kts` 中的 Foojay resolver 用于解析或下载缺失的 daemon toolchain，
它要等 Gradle 启动后才会执行，因此不能修复“Android Studio 先用 JDK 25 启动 Gradle”
这一阶段的错误。

---

## 其他：工具链版本兼容与升级

Android 工具链不是所有版本都可以独立升级。建议先确定 Android Studio/AGP，再确定
Gradle 和 JDK，最后配套 Kotlin/KSP；`compileSdk` 和 `targetSdk` 则根据目标 Android
平台单独评估。

官方兼容资料：

- [Android Studio 与 AGP 兼容表](https://developer.android.com/studio/releases#android_gradle_plugin_and_android_studio_compatibility)
- [AGP 与 Gradle/JDK 兼容要求](https://developer.android.com/build/releases/about-agp?hl=zh-cn)
- [AGP 发布说明与 API 支持](https://developer.android.com/build/releases/gradle-plugin)
- [Android Kotlin 支持表（Kotlin 与最低 AGP）](https://developer.android.com/build/kotlin-support?hl=zh-cn)
- [Android SDK 平台与 API Level](https://developer.android.com/tools/releases/platforms)
- [KSP 官方版本发布页](https://github.com/google/ksp/releases)

### AGP 与 Gradle 版本对应关系

下表中的 Gradle 是该 AGP 系列要求的**最低 Gradle 版本**，不是“任意更高版本都一定
兼容”。实际升级时应优先采用对应 AGP 发布说明中验证过的 Gradle 版本。AGP 8.x
运行 Gradle 时至少需要 JDK 17。

| Android Gradle Plugin | 最低 Gradle 版本 | 最低 Gradle JDK |
| --- | --- | --- |
| 8.13.x | 8.13 | 17 |
| 8.12.x | 8.13 | 17 |
| 8.11.x | 8.13 | 17 |
| 8.10.x | 8.11.1 | 17 |
| 8.9.x | 8.11.1 | 17 |
| 8.8.x | 8.10.2 | 17 |
| 8.7.x | 8.9 | 17 |
| 8.6.x / 8.5.x | 8.7 | 17 |
| 8.4.x | 8.6 | 17 |
| 8.3.x | 8.4 | 17 |
| 8.2.x | 8.2 | 17 |
| 8.1.x / 8.0.x | 8.0 | 17 |

当前模板使用 `AGP 8.13.2 + Gradle 8.13`，满足官方兼容要求。AGP 9.x 与 API 37
工具链仍在快速迭代，准备升级时应重新查看官方实时表，不要直接沿用上表推算。

### Kotlin 与最低 AGP 版本对应关系

Android 官方表给出的是“使用某个 Kotlin 编译器版本时所需的最低 AGP”，AGP 高于
最低版本通常可以使用，但还需要确认 KSP、Compose Compiler 和其他编译插件。

| Kotlin 版本 | 最低 Android Gradle Plugin |
| --- | --- |
| 2.3 | 8.13.2 |
| 2.2 | 8.10 |
| 2.1 | 8.6 |
| 2.0 | 8.5 |
| 1.9 | 8.0 |
| 1.8 | 7.4 |
| 1.7 | 7.2 |
| 1.6 | 7.1 |

当前模板的 `Kotlin 2.0.21 + AGP 8.13.2` 高于 Kotlin 2.0 所需的最低 AGP 8.5。
KSP 还必须单独匹配 Kotlin 编译器；当前为：

```text
Kotlin  2.0.21
KSP     2.0.21-1.0.27
        └──────┘ Kotlin 版本前缀一致
```

> 这些表用于判断“能否构建”，不代表必须升级到表中最新版本。模板应优先保留经过
> 编译、Lint、测试和真机验证的稳定组合。

### 目前模板已验证的组合

| 组件 | 当前版本 | 作用与约束 |
| --- | --- | --- |
| Android Studio | 以兼容表为准 | IDE 必须支持项目使用的 AGP |
| Android Gradle Plugin | 8.13.2 | 决定 Android 构建行为、Gradle/JDK 要求和可用的 compileSdk 范围 |
| Gradle Wrapper | 8.13 | 必须满足当前 AGP 的 Gradle 版本要求 |
| Gradle 运行 JDK | 至少 JDK 17 | 同时要处于 Gradle 支持的 JDK 范围；不等同于 App 的字节码版本 |
| Kotlin | 2.0.21 | 由 Kotlin Gradle Plugin 管理 |
| KSP | 2.0.21-1.0.27 | 前缀需与 Kotlin 编译器版本匹配，后缀是 KSP 发布版本 |
| compileSdk | 36 | 编译时可用的 Android API；应不低于 targetSdk |
| targetSdk | 36 | 应用声明已适配的 Android 行为版本，会影响系统兼容策略 |
| minSdk | 29 | 应用允许运行的最低 Android 版本 |

> `compileSdk`、`targetSdk` 和 Android Studio/AGP 不是同一个版本号。看到新的 API
> 平台并不代表只修改 `targetSdk` 就完成了升级；还要确认 AGP、Gradle、JDK 和依赖
> 是否支持该平台。

### 推荐升级顺序

1. 在 Android Studio/AGP 兼容表中选择目标 AGP。
2. 根据 AGP 发布说明升级 Gradle Wrapper 和 Gradle 运行 JDK。
3. 按 Kotlin 支持表选择 Kotlin，并选择相同 Kotlin 前缀的 KSP 版本。
4. 安装目标 Android SDK Platform，先升级 `compileSdk`，再评估 `targetSdk`。
5. 最后升级 AndroidX、Room、Navigation、Coroutines 等库，并阅读各自的迁移说明。
6. 执行 Debug/Release 编译、Lint、单元测试和真机冒烟测试。

### API 37 / Android 17 升级说明

如果 Android Studio 提示 `targetSdk = 36` 不是最新版本，通常表示 SDK Manager
中已经提供 API 37。模板当前保持 36 是有意的稳定版本锁定。升级到 API 37 时应
同时确认支持 API 37 的 AGP、Gradle、JDK 和 Kotlin/KSP 组合，然后一起修改：

```kotlin
android {
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        targetSdk = 37
    }
}
```

不要只把 `targetSdk` 从 36 改成 37。主版本工具链升级建议在独立分支中完成，验证
通过后再合并到模板主分支。
