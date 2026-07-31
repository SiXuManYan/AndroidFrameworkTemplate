# 接入新项目指南

> 在 5 分钟内基于此模板创建新项目。

## 步骤 1：复制模板

```bash
cp -r AristonVsop my-new-project
cd my-new-project
```

清理模板的演示资源：
- 删除 `app/src/main/java/com/example/template/MainActivity.kt`（保留 `App.kt`）
- 删除 `app/src/main/res/layout/activity_main.xml`

## 步骤 2：重命名包名

全局替换以下字符串：
- `com.example.template` → 你的应用包名，如 `com.mycompany.myapp`
- `com.template.framework` → 通常保持不变（如需修改，需同步修改 `core_framework/` 所有文件的 package）

修改位置：
1. `app/build.gradle.kts` - `namespace` 和 `applicationId`
2. `app/src/main/AndroidManifest.xml` - `.App` 和 `.MainActivity`
3. `core_framework/build.gradle.kts` - `namespace`（如要改 framework 包名）

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

## 步骤 5：实现业务

### 5.1 定义业务接口
在 `core_framework/src/main/java/com/template/framework/api/` 下添加：
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
class AppRepository private constructor(context: Context) : FrameworkRepository(context, Framework.getPreferences()) {

    suspend fun getProfile(): ApiResponse<UserProfile> =
        NetworkModule.createApiService<MyApi>(getCurrentBaseUrl()).getProfile()

    companion object {
        @Volatile private var INSTANCE: AppRepository? = null
        fun getInstance(context: Context): AppRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}
```

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

### Q2: 如何添加自定义 DataStore Key？
在 `App` 模块中继承 `PreferencesManager`：
```kotlin
class AppPreferences(context: Context) : PreferencesManager(context) {
    val userId: Flow<String?> = ... // 自定义 Key
}
```

### Q3: 如何禁用 BaseActivity 的语言自动应用？
子类覆盖：
```kotlin
class MyActivity : BaseActivity<ActivityMyBinding>() {
    // 默认行为即可，或重写 applyLanguageSettingsSync
}
```

### Q4: 如何让 WebSocket 自动连接？
在 Application.onCreate 或登录成功后：
```kotlin
Framework.getRepository().connectWebSocket(ip = "192.168.1.1", port = "8080")
```

### Q5: Token 失效如何跳转到登录页？
在 Application.onCreate 中注入：
```kotlin
Framework.setOnTokenExpired {
    // 跳转到登录页
    startActivity(Intent(this, LoginActivity::class.java))
}
```

### Q6: Release 模式如何配置 SSL 证书？
1. 将证书文件放到 `app/src/main/res/raw/my_cert.crt`
2. `FrameworkConfig.sslCertRawResId = R.raw.my_cert`

默认使用系统信任链和主机名校验。Debug 连接自签名 HTTPS/WSS 服务时，同样通过
`sslCertRawResId` 提供可信证书，不建议关闭证书或主机名校验。

### Q7: 如何添加自定义数据库实体？
在 `app/src/main/java/com/mycompany/myapp/data/db/` 下创建：
```kotlin
@Database(
    entities = [...],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : FrameworkDatabase() {
    abstract fun myDao(): MyDao
}
```

### Q8: 如何启用平板 kiosk 全屏？

`BaseActivity` 默认采用适合手机和平板的 edge-to-edge 布局，并保留系统栏。
只在确实需要隐藏状态栏和导航栏的页面显式调用：

```kotlin
FullScreenUtils.enableFullScreen(this)
```
