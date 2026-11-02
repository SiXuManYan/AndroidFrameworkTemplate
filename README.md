# Android 基础框架 Library

Kotlin + ViewBinding 的 Android Library（`:core_framework`），接入使用者自己的 App。仓库原有 `app` 仅作为使用示例，不属于 Library，也不是接入所需模块。

## 开始使用

1. 将 `core_framework` 接入自己的工程，在自己的 App 中依赖 `project(":core_framework")`；具体步骤见快速接入。
2. 自己的 `Application.onCreate()` 中调用 `Framework.init(this, FrameworkConfig(...))`，只配置上下文和日志。
3. 根据需要单独初始化 HTTP 或 WebSocket；纯本地页面不需要初始化网络。
4. 自定义 API、Room 实体和仓库放在使用者的 App；无需复制本仓库的示例 App。

示例地址 `https://com.exmpale.xxxx/` **不可访问**，网络演示前必须替换 URL 和 `ExampleApi` 契约。启动页面的本地 RecyclerView 示例无需网络。

如需查看示例，在 Android Studio 打开本仓库并运行现有 `app` 即可；不需要示例时，使用者工程仅引入 `core_framework`。

## 提供的能力

- UI：Activity / Fragment / Dialog 的 ViewBinding 基类、edge-to-edge、可选 kiosk 全屏。
- 列表：兼容的 `CommonAdapter` / `MultiViewTypeAdapter`、payload、同步差分与拖拽、异步 `BindingListAdapter`。
- 通用组件：流式布局、星级评分、数量步进器、网格分页吸附、分割线、加载弹窗及空响应转换器。
- HTTP：OkHttp / Retrofit 工厂、客户端显式复用、可选认证及版本头、安全概要日志。
- WebSocket：独立初始化、连接状态、消息事件、指数退避重连与资源释放。
- 存储：按需创建的 PreferencesManager、DataStore 备份；App 层的通用 Room CRUD 示例。
- 工具：屏幕单位、语言、系统栏、时间、文件、JSON、数值与点击扩展。

## 文档

- [快速接入](docs/QUICKSTART.md)：初始化、HTTP、WebSocket、存储和 UI。
- [HTTP 使用示例](docs/HTTP_USAGE.md)：正式／测试环境、第三方支付，以及方式 A / B 初始化后的请求代码。
- [RecyclerView](docs/RECYCLERVIEW.md)：适配器选择、差分、payload、拖拽。
- [可复用组件](docs/REUSABLE_COMPONENTS.md)：控件、分页吸附、分割线、弹窗与工具示例。
- [分包与职责](docs/ARCHITECTURE.md)：框架与业务边界。
- [旧版迁移](docs/MIGRATION.md)：删除的业务模板与接口变化。
- [开发约束](AGENTS.md)：脱敏、安全、注释与验证约定。

## 工具链与验证

当前配置：AGP 8.13.2、Gradle 8.13、Kotlin 2.0.21、KSP 2.0.21-1.0.27、compileSdk / targetSdk 36、minSdk 29。版本以 `gradle/libs.versions.toml`、Wrapper 和模块构建文件为准；升级前查阅 [Android 官方兼容说明](https://developer.android.com/build/releases/gradle-plugin)。

仓库通过 `gradle/gradle-daemon-jvm.properties` 与 Foojay 配置 JetBrains JDK 17；这是 Gradle 运行 JDK，不等同于各模块的 JVM 字节码目标。接入源码 Library 时使用宿主工程的工具链，不需要复制示例 App 或 daemon 配置。

此次改动仅做源码、资源、脱敏和文档一致性检查，**未执行 Gradle 构建、测试或安装**。请在 Android Studio 验证示例列表、独立 HTTP/WS 启动及 Room 代码生成；HTTP/WS 联调需要自己的后端。

许可证：[MIT](LICENSE)。
