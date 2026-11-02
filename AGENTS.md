# 开发约束

## 范围与分层
- `core_framework` 只放跨项目通用能力；业务 API、DTO、Room 实体和仓库放在 `app/data`，页面按功能放在 `app/ui`。
- 参考其他项目时只读取，不修改参考项目，不复制真实服务地址、客户名称、客户端 ID、设备标识、Token 或签名配置。
- 示例地址统一用 `https://com.exmpale.xxxx/`（不可访问的占位地址）；WebSocket 用对应的 `wss://com.exmpale.xxxx/`。
- 改动保持最小范围；兼容入口标明替代方式，破坏性变更同步更新迁移文档。

## 初始化与安全
- `Framework.init` 不得创建 HTTP/WebSocket 客户端、连接服务器或打开数据库；能力分别按需初始化。
- HTTP 与 WebSocket 不得相互初始化；客户端由调用方显式复用，配置不随 Debug 自动更改协议或 URL。
- 默认使用系统 TLS 信任链及主机名校验，禁止信任所有证书或关闭主机名校验。
- HTTP 日志仅在 Debug 按需安装，不打印正文、认证头和查询参数；认证及版本 Header 由业务按需启用。
- 不在主线程使用 `runBlocking` 读取存储或连接网络；Flow 收集使用页面生命周期，事件不要当作 StateFlow 状态保存。

## RecyclerView
- 点击时读取 `bindingAdapterPosition` 并检查 `NO_POSITION`，不得捕获绑定时的位置。
- 更新优先精确通知或 DiffUtil；payload 必须有完整绑定回退，回收时释放图片任务等资源。
- 异步差分列表提交不可变快照；拖拽列表同步移动数据并通知，不能混用两套数据源。
- stable ID 必须唯一且稳定，不能使用位置或可能冲突的字符串 hash 充当 ID。

## 注释、文档与验证
- 新增代码在关键逻辑处添加简短准确的中文注释；函数和成员可使用文档注释。
- 文档以可复制的接入示例为主，不堆叠版本兼容表，不宣称未经执行的验证已通过。
- Kotlin/Android 可复用知识按 `E:\android-knowledge-base\AGENTS.md` 写入知识库，不记录业务敏感信息。
- 未经用户明确要求，不执行 Gradle 编译、测试、Lint、安装或其他 Gradle 验证；普通修改交回 Android Studio 验证。
- 构建配置、依赖或跨模块 API 的高风险修改，应说明验证价值，由用户决定是否运行。
- 不覆盖用户现有改动，不执行破坏性 Git 命令，不自动提交。
