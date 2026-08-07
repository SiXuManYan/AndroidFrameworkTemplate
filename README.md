# AndroidFrameworkTemplate

> 基于 Kotlin、ViewBinding 和 Android View 体系的双模块 Android 项目模板。
> 通用基础能力集中在 `:core_framework`，`:app` 保留初始化代码和可运行 Demo，适合复制后继续开发传统 View 项目。

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![AGP](https://img.shields.io/badge/AGP-8.13.2-brightgreen.svg)](https://developer.android.com/build/releases/gradle-plugin)
[![Gradle](https://img.shields.io/badge/Gradle-8.13-02303A.svg)](https://gradle.org/releases/)
[![API](https://img.shields.io/badge/API-29--36-orange.svg)](https://developer.android.com/tools/releases/platforms)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](./LICENSE)

## 项目能力

| 领域 | 已提供能力 |
| --- | --- |
| 网络 | Retrofit + OkHttp；Token、401、版本信息和日志 4 类拦截器；系统信任链或指定证书 |
| WebSocket | 单例连接、连接状态与消息 `StateFlow`、异常断线自动重连、与 HTTP 共用证书策略 |
| 数据 | Room 示例实体/DAO；DataStore Preferences 与 SharedPreferences 双写备份 |
| UI 基础 | ViewBinding 泛型 Activity、Fragment、Dialog、DialogFragment 和 RecyclerView Adapter |
| 通用组件 | FlowLayout、星级评分、数量步进器、网格分页吸附、分割线和 LoadingDialog |
| 工具 | 数值精度、字符串转换、设备/应用信息、屏幕、语言、时间、系统栏和输入框扩展 |
| 启动与适配 | AndroidX SplashScreen、edge-to-edge 安全区域、可选 kiosk 沉浸式全屏 |

模板提供的是基础设施和示例调用链，不强制业务层采用特定的依赖注入、导航或状态管理方案。业务项目可以在 `:app` 中继续使用 MVVM，也可以按实际规模拆分 feature module。

## 环境要求

| 项目 | 当前配置 |
| --- | --- |
| Android Gradle Plugin | 8.13.2 |
| Gradle Wrapper | 8.13 |
| Gradle 运行 JDK | 17（项目 daemon toolchain 指定 JetBrains JDK 17） |
| Kotlin / KSP | 2.0.21 / 2.0.21-1.0.27 |
| Android SDK | compileSdk 36、targetSdk 36、minSdk 29 |
| App 字节码目标 | Java 11 / JVM 11 |

请使用支持 AGP 8.13.2 的 Android Studio，并安装 Android SDK Platform 36。Gradle 运行 JDK 和 App 字节码目标是两件事：Gradle 使用 JDK 17 启动，App 仍编译为 JVM 11 字节码。

## 快速运行

```bash
git clone https://github.com/SiXuManYan/AndroidFrameworkTemplate.git
cd AndroidFrameworkTemplate
./gradlew :app:assembleDebug
```

Windows 使用：

```powershell
gradlew.bat :app:assembleDebug
```

在 Android Studio 中打开项目后：

1. 将 **Gradle JDK** 设为 JDK 17，例如 `jbr-17`。
2. 执行 **Sync Project with Gradle Files**。
3. 选择 `app` 配置并运行到 API 29 及以上设备。

Demo 页面可以直接验证服务器地址的本地保存。Login API 和 WebSocket 使用示例服务端协议，只有连接到实现了相应路径和响应结构的服务端时才会成功。

## 项目结构

```text
AndroidFrameworkTemplate/
├── app/
│   ├── App.kt                         # Framework.init() 初始化入口
│   ├── MainActivity.kt                # DataStore、HTTP、Token、WebSocket Demo
│   └── activity_main.xml
├── core_framework/
│   └── src/main/java/com/template/framework/
│       ├── Framework.kt               # 框架统一入口
│       ├── api/                       # Retrofit、OkHttp、拦截器与响应模型
│       ├── websocket/                 # WebSocket 连接与状态流
│       ├── repository/                # 示例 Repository 与 ApiService 缓存
│       ├── datastore/                 # DataStore 与 SharedPreferences 备份
│       ├── database/                  # Room Database、DAO 和实体示例
│       ├── ui/base/                   # Activity、Fragment、Dialog、Adapter 基类
│       ├── ui/widget/                 # FlowLayout、RatingStarView、NumberStepperView
│       ├── ui/recyclerview/           # 分页吸附与分割线
│       ├── ui/dialog/                 # LoadingDialog
│       └── util/                      # 无业务依赖的通用工具
├── docs/
│   ├── ARCHITECTURE.md                # 模块职责与核心流程
│   ├── QUICKSTART.md                  # 从模板创建业务项目
│   └── REUSABLE_COMPONENTS.md         # 可复用组件 API 与示例
├── gradle/libs.versions.toml          # 依赖版本目录
└── gradle/gradle-daemon-jvm.properties # Gradle daemon JDK 约束
```

依赖方向保持单向：

```text
:app  --->  :core_framework
```

`core_framework` 不依赖 `app` 的 `BuildConfig`、资源或业务类型，运行参数由 App 初始化时注入。

## 核心用法

### 初始化

在自定义 `Application` 中初始化一次：

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        Framework.init(
            app = this,
            config = FrameworkConfig(
                debug = BuildConfig.DEBUG,
                versionCode = BuildConfig.VERSION_CODE,
                versionName = BuildConfig.VERSION_NAME,
                debugApiPrefix = "dev-api",
                releaseApiPrefix = "prod-api",
                sslCertRawResId = null,
            ),
        )

        Framework.setOnTokenExpired {
            // 该回调可能来自网络线程；页面跳转前切换到主线程。
        }
    }
}
```

`Framework.init()` 必须早于任何 `Framework.getPreferences()`、`getRepository()` 或 `getConfig()` 调用。

### 保存连接配置并调用 API

```kotlin
lifecycleScope.launch {
    Framework.getPreferences().saveServerIp("192.168.1.10")
    Framework.getPreferences().saveServerPort("8080")
    Framework.getRepository().clearApiServiceCache()

    val response = Framework.getRepository().login(
        DeviceLoginRequest(
            grantType = "device",
            userId = "demo_user",
            snNumber = "SN_DEMO_001",
        ),
    )
    response.data?.accessToken?.let { token ->
        Framework.getPreferences().saveAccessToken(token)
    }
}
```

Debug 构建默认生成 `http://{ip}:{port}/{debugApiPrefix}/`，Release 构建生成 `https://{ip}:{port}/{releaseApiPrefix}/`。更复杂的域名、端口和环境切换策略应在业务 Repository 中明确实现。

### 监听 WebSocket

```kotlin
lifecycleScope.launch {
    Framework.getRepository().webSocketConnectionState.collect { state ->
        renderConnectionState(state)
    }
}

lifecycleScope.launch {
    Framework.getRepository().webSocketMessageFlow.collect { message ->
        message?.let(::handleMessage)
    }
}
```

### 继承 UI 基类

```kotlin
class ProfileActivity : BaseActivity<ActivityProfileBinding>() {
    override fun initViewBinding() = ActivityProfileBinding.inflate(layoutInflater)

    override fun initView() {
        // 初始化 View
    }

    override fun initListener() {
        // 注册监听
    }

    override fun initData() {
        // 加载数据
    }
}
```

`BaseActivity` 默认保留系统栏并处理 edge-to-edge 安全区域。仅 kiosk 页面需要显式调用 `FullScreenUtils.enableFullScreen(this)`。

## 可复用组件

最近加入的通用组件包括：

- `DecimalUtils`、`StringNumberExtensions`、`AppInfoUtils`
- `EmptyBodyConverterFactory`
- `FlowLayout`、`RatingStarView`、`NumberStepperView`
- `GridPagerSnapHelper`、`DividerItemDecoration`
- `LoadingDialog`

完整 XML/Kotlin 示例、属性说明、边界条件和资源命名约定见 [可复用组件说明](./docs/REUSABLE_COMPONENTS.md)。

## 构建与检查

```bash
# Debug APK
./gradlew :app:assembleDebug

# JVM 单元测试
./gradlew test

# Android Lint
./gradlew lint
```

发布前还应配置正式签名，检查 Release 混淆策略，并在目标设备上完成 HTTP、WebSocket、数据库迁移和系统栏适配的冒烟测试。

## 常见问题

### Incompatible Gradle JVM version

Gradle 8.13 不能由 JDK 25 启动。若 Android Studio 提示当前 Gradle JVM 为 25：

1. 打开 **Settings/Preferences > Build, Execution, Deployment > Build Tools > Gradle**。
2. 将 **Gradle JDK** 改为 `jbr-17` 或其他已安装的 JDK 17。
3. 重新 Sync，并用 `./gradlew --version` 确认 Launcher JVM 与 Daemon JVM。

项目在 `gradle/gradle-daemon-jvm.properties` 中指定了 JetBrains JDK 17，并通过 Foojay resolver 在缺少匹配 toolchain 时提供下载地址。但 Foojay 插件在 Gradle 启动后才生效，不能修复 Android Studio 先用 JDK 25 启动 Gradle 的兼容错误。

### API 请求失败或 WebSocket 无法连接

- 先保存非空 IP 和端口；修改后调用 `clearApiServiceCache()`。
- 确认服务端实现了 `debugApiPrefix` / `releaseApiPrefix` 对应路径。
- 自签名 HTTPS/WSS 需要通过 `sslCertRawResId` 提供受信证书。
- 真机访问开发机时不要使用 `127.0.0.1`；应使用开发机局域网地址并检查防火墙。

### DataStore 文件名为什么没有变化

当前 `PreferencesManager` 的 DataStore 文件名固定为 `framework_preferences`。`FrameworkConfig.dataStoreName` 目前只参与默认 Room 数据库名称的派生；业务需要独立 DataStore 时，应在 App 模块创建自己的 Manager。详见 [快速接入指南](./docs/QUICKSTART.md)。

## 安全与发布注意事项

- Demo 为方便连接局域网 HTTP 服务，Manifest 当前设置了 `usesCleartextTraffic="true"`。生产版本应关闭明文流量，或配置仅允许指定开发域名的 Network Security Config。
- `sslCertRawResId = null` 使用系统信任链；配置自定义证书后只信任指定证书存储中的证书。
- 签名密码、API Key、Token 和服务地址不要提交到版本库。
- `release` 当前未启用代码压缩；正式发布前应评估 R8 和 `consumer-rules.pro`。
- 401 处理会清除 Token 并触发回调，不包含 Token 自动刷新流程。

## 文档导航

| 文档 | 内容 |
| --- | --- |
| [架构说明](./docs/ARCHITECTURE.md) | 模块职责、启动流程、HTTP 和 WebSocket 流程 |
| [快速接入指南](./docs/QUICKSTART.md) | 环境准备、改名、初始化、业务扩展和工具链升级 |
| [可复用组件说明](./docs/REUSABLE_COMPONENTS.md) | 工具、View、RecyclerView 与 Dialog 的完整用法 |

## 路线图

- [ ] 为网络请求增加可配置的重试与退避策略
- [ ] 为 WebSocket 增加可配置的心跳保活
- [ ] 增加图片加载可选模块
- [ ] 增加 Media3 视频播放可选模块
- [ ] 增加 Compose 适配层
- [ ] 补齐框架单元测试与仪器测试

## 贡献

建议使用 Conventional Commits 风格，例如：

```text
feat: add image loading module
fix: handle websocket reconnect race
docs: improve project setup guide
build: update Android Gradle Plugin
```

## License

[MIT](./LICENSE) © 2026 Shiwei Wang
