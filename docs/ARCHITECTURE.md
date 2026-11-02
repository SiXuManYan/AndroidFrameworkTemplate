# 分包与职责

交付主体是 `core_framework` Android Library，`app` 是仓库原有的使用示例；使用者仅接入 Library，不需要创建或复制额外 App 模块。

## 项目结构

```text
app/
  App.kt                     基础初始化，不自动连接网络
  data/api/                  可替换的 ExampleApi 与 DTO
  data/database/             通用 Room CRUD 示例
  ui/                        示例页面与适配器；业务项目按功能继续分包
core_framework/
  Framework.kt               基础入口、可选 HTTP / WS 初始化入口
  FrameworkConfig.kt         Debug 与应用版本，不含网络地址
  api/                       HTTP 工厂、可选拦截器、响应包装
  network/                   HttpConfig 与独立 TLS 策略
  websocket/                 WebSocketConfig、连接管理、状态与事件
  datastore/                 按需 Preferences 与备份工具
  repository/                兼容偏好入口，不承载业务 API 或 WebSocket
  ui/base/                   UI 基类与兼容适配器
  ui/recyclerview/           可变数据基类、异步列表与拖拽桥接
  util/                      系统栏、语言、屏幕、时间与扩展
  constants/                 通用网络和偏好常量
```

## 初始化边界

- `Framework.init` → 上下文、运行配置、日志。
- 首次 `Framework.getPreferences` → 偏好管理器；不启动网络。
- `Framework.initHttp` / `NetworkModule.init` → HTTP 客户端与 Retrofit；不连接 WebSocket。
- `Framework.initWebSocket` / `WebSocketManager.create` → WebSocket 管理器；`connect()` 才连接，不创建 Retrofit。
- HTTP 与 WebSocket 仅可共享独立 `TlsConfig` 或调用方显式传入的客户端。
- Room 由 App 自己创建，不在框架或 Application 初始化中默认打开。

按需初始化是**运行时解耦**，不是独立的 Gradle 可选模块：当前仍保留一个 `core_framework` Library，纯本地 App 若希望缩减依赖体积，可另行按功能拆模块。

库不再依赖 Room，也移除了没有源码调用的 Navigation、Material、ConstraintLayout、SwipeRefreshLayout 与 OkHttp logging-interceptor；使用者按自己的页面需求声明这些依赖。

## 推荐业务用法

业务项目按功能放置 `ui/<feature>`，接口和 DTO 放 `data/api`，数据库放 `data/database`，业务仓库放 `data/repository`。ViewModel 依赖业务仓库，不通过框架调用固定业务方法。复用共享 OkHttp 客户端，不在每次请求中创建连接池。

提炼的通用场景包括：共享 HTTP 客户端、纯本地启动、兼容适配器、异步差分列表、payload 与同步拖拽。不包含参考项目的接口或业务实现。

## 保持兼容的入口

`CommonAdapter` / `MultiViewTypeAdapter` 仍位于 `ui.base`，保留其 BindingViewHolder 类型；共同的数据操作下沉至 `ui.recyclerview.MutableDataAdapter`。`api.FrameworkConfig` 是根包配置的弃用别名。具体迁移见 [旧版迁移](MIGRATION.md)。
