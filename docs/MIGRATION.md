# 旧版迁移

这次调整清理了混入框架的业务契约。现有业务项目升级框架前，先保留自己仍在使用的业务代码；不要直接删除业务数据库文件。

## 初始化与网络

- `Framework.init` 不再创建仓库、偏好或 WebSocket。需要 HTTP 时调用 `initHttp(HttpConfig)`，需要 WS 时调用 `initWebSocket(WebSocketConfig)`；不需要的能力不调用。
- `FrameworkConfig` 移到根包，旧 import 保留弃用别名；移除 `dataStoreName`、IP/Port、API 前缀和证书字段。DataStore 使用固定文件名，自定义证书改用 `TlsConfig`，服务器地址分别放入 HTTP / WS 配置。
- 不再按 Debug 自动选择协议及 `dev-api` / `prod-api`。将原地址整理成完整 URL，再显式提供。
- 移除 `ApiService`、设备登录/生产 DTO、固定 client ID 和 `WebSocketParams`；把实际业务接口留在业务 App 中。新的 `ExampleApi` 只是通用演示，不能直接替代旧后端契约。
- 原 `getRepository().login/getLineAndPost/saveProducts/getApiService()` 改为自己的 API 和业务仓库。兼容仓库保留偏好委托，`getApiService<T>()` 访问已单独初始化的 HTTP。
- 不再按 IP/Port 缓存业务 API；换服务器调用 `initHttp` 并重新创建 Service。直接提供 `client` 的工厂支持显式共享连接池。
- `configureSslSocketFactory` 改用 `TlsConfig.trustCertificate`。认证、client ID 和版本头默认关闭，按需配置；Token 回调直接由 `Framework.setOnTokenExpired` 或 `NetworkModule.onTokenExpired` 设置。

## WebSocket

- 仓库上的 WS 委托移除，直接操作 `WebSocketManager`。
- `connect(ip, port, snNumber)` 改为配置完整 `url` 和动态 `headers` 后 `connect()`；需要 SN 等 Header 时由业务自行提供。
- `disconnect(stopReconnect)` 改为 `disconnect()`，始终停止重连。更换全局配置调用 `initialize`，多连接用 `create`。
- `messageFlow` 从可空 StateFlow 改为 `SharedFlow<String>`；移除旧 `.value` 读取与空值判断，按生命周期收集事件。没有订阅者时不缓存消息。
- `release()` 后不可复用，重新初始化或创建管理器；不会关闭外部传入的客户端。

## Room 与列表

- 移除生产实体、DAO 和 `FrameworkDatabase`。业务基于自己的 `RoomDatabase` 保留原表及 Migration；App 的 `ExampleDatabase` 是新示例数据库，不自动迁移或读取旧库。
- KSP / Room 代码生成归属 App，框架不再依赖 Room。新业务实体加在 App，迁移必须显式维护。
- 旧适配器类名、绑定签名和数据方法保留；点击现在返回当前的位置/条目。子视图应在 `onBind` 中重新注册，长按没有监听器时不消费事件。
- 列表可选传入 DiffUtil callback 使用同步差分；拖拽用 `moveItem`。需要异步差分使用新的 `BindingListAdapter`，不要直接修改其 `currentList`。
- 数据插入、删除和移动不会自动重绑其他条目；若 UI 显示序号，业务需对受影响范围发 payload 更新。

## 示例页面与安全

`MainActivity` 移到 `app/ui`，Manifest 已同步。示例地址不可访问，需替换 URL 和 API；默认禁止明文。日志只保留方法、主机、状态码和耗时，不再打印 Header/正文。

清理范围是当前源码与文档，不改写 Git 历史。如果真实标识曾提交且具有凭证作用，另行轮换并按发布要求清理历史。
