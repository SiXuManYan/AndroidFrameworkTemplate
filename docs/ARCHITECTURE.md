# 架构说明

## 整体分层

```mermaid
flowchart TB
    subgraph App["App (业务壳)"]
        A1["Application.onCreate()<br/>init Framework"]
        A2["Activity / Fragment / ViewModel<br/>业务 UI + 业务逻辑"]
    end

    subgraph CF["core_framework (Library module)"]
        direction TB
        FWK["Framework.kt — 统一入口<br/>init / getRepository / getPreferences / setOnTokenExpired"]

        subgraph ui_base["ui/base/"]
            UI["BaseActivity / BaseFragment<br/>BaseDialog / BaseDialogFragment<br/>CommonAdapter / MultiViewTypeAdapter"]
        end
        subgraph api["api/"]
            AS["ApiService + ApiResponse"]
            NM["NetworkModule"]
            IC["Interceptors<br/>Token / Auth / Version / Logging"]
        end
        subgraph ws["websocket/"]
            WSM["WebSocketManager<br/>单例 + 自动重连 + Flow"]
        end
        subgraph db["database/"]
            DB["Room Database + DAOs<br/>(Flow 自动刷新)"]
        end
        subgraph ds["datastore/"]
            PM["PreferencesManager<br/>+ DataStoreBackupHelper (双写双读)"]
        end
        subgraph repo["repository/"]
            FR["FrameworkRepository<br/>(ApiService 按 IP:Port 缓存)"]
        end
        subgraph util["util/"]
            UT["TimberUtil / DeviceUtils<br/>ScreenUtil / LanguageUtils<br/>FullScreenUtils / TimeUtils ..."]
        end
        subgraph const["constants/"]
            FC["FrameworkConstants"]
        end
    end

    App -->|依赖| CF
    FWK --> PM
    FWK --> FR
    FR --> AS
    FR --> WSM
    AS --> IC
    IC --> NM
    WSM --> NM
```

## 各模块职责

### 1. Framework（统一入口）
- 提供 `init(app, config)` 初始化方法
- 提供 `getContext / getConfig / getPreferences / getRepository` 单例访问
- 提供 `setOnTokenExpired(callback)` 注入 token 失效跳转回调

### 2. api（网络层）
- **`ApiService`** - 默认提供 4 个示例接口（POST @Body / GET 列表 / POST 复杂对象 / POST ApiResponse<Unit>）
- **`NetworkModule`** - Retrofit/OkHttp/SSL 工厂
- **`TokenInterceptor`** - 自动注入 `Authorization: Bearer {token}` 和 `clientid`
- **`AuthErrorInterceptor`** - 同时处理 HTTP 401 与业务 code 401，自动清理 token
- **`VersionInterceptor`** - 自动注入 `VersionCode` / `VersionName` Header
- **`HttpLoggingInterceptor`** - 详细打印请求/响应 Header 与 Body
- **`ApiResponse<T>`** - 统一响应包装（`code` / `msg` / `data`）
- **`FrameworkConfig`** - 运行时配置（debug、versionCode、SSL 策略等）

### 3. websocket（WebSocket 管理）
- **`WebSocketManager`** - 单例、自动重连、连接状态/消息以 `StateFlow` 暴露
- 复用 `NetworkModule.configureSslSocketFactory` 共享 SSL 策略
- 同一服务器多次 `connect` 不会重复连接

### 4. database（Room 模板）
- **`FrameworkDatabase`** - 默认包含 ProductHistory + Line + LinePosition（一对多示例）
- **`ProductHistoryDao` / `LineDao`** - CRUD + Flow 自动刷新
- 业务可继承 `FrameworkDatabase` 扩展自己的实体与 DAO

### 5. datastore（偏好设置）
- **`PreferencesManager`** - 通用 Key（IP / Port / Token / Language）
- **`DataStoreBackupHelper`** - DataStore + SharedPreferences 双写双读模式
- 业务可继承 `PreferencesManager` 添加自定义 Key

### 6. repository（仓库层）
- **`FrameworkRepository`** - ApiService 缓存（按 IP:Port 缓存）+ WebSocket 委托 + 3 个示例 API
- 业务可继承 `FrameworkRepository` 添加业务方法

### 7. ui/base（UI 基类）
- **`BaseActivity<VB>`** - ViewBinding + 全屏 + 点击外部隐藏键盘 + 禁用系统返回 + 语言切换
- **`BaseFragment<VB>`** - ViewBinding + 生命周期钩子
- **`BaseDialog(context)`** - 全屏 + 点击外部隐藏键盘
- **`BaseDialogFragment<VB>`** - 全屏 + 软键盘上移 + 点击外部隐藏键盘
- **`CommonAdapter<T, VB>`** - 单一 ViewBinding 适配器
- **`MultiViewTypeAdapter<T>`** - 多 ViewBinding 适配器

### 8. util（工具类）
- `TimberUtil` - Debug/Release 日志分流
- `DeviceUtils` - IP / SN / 大小写转换
- `ScreenUtil` - dp/px/sp 互转
- `LanguageUtils` - 中英文切换 + 淡入动画
- `FullScreenUtils` - 全屏 + Dialog 全屏扩展
- `ViewExtensions` - `setOnMultiClickListener`
- `EditTextExtensions` - `requestFocusSafely` / `keepFocus` / `setShowSoftInputOnFocus`
- `TimeUtils` - 时间格式化

### 9. constants（常量）
- `FrameworkConstants` - 超时 / Header 名称 / DataStore Key / 数据库名

## 核心流程

### 启动流程

```mermaid
sequenceDiagram
    autonumber
    participant App
    participant FW as Framework
    participant TB as TimberUtil
    participant PM as PreferencesManager
    participant FR as FrameworkRepository
    participant AC as MainActivity (BaseActivity)

    App->>FW: init(app, config)
    activate FW
    FW->>TB: init(config.debug)
    FW->>PM: 实例化 (DataStore + BackupHelper)
    FW->>FR: 实例化 (ApiService 缓存 + WS 单例)
    deactivate FW

    AC->>AC: onCreate()
    AC->>PM: 同步读取 language
    PM-->>AC: Flow.first()
    AC->>AC: LanguageUtils.setLanguage()
    AC->>AC: initViewBinding()
    AC->>AC: FullScreenUtils.enableFullScreen()
    AC->>AC: 注册 onBackPressedCallback (禁用返回)
    AC->>AC: initView / initListener / initData
    Note over AC: 若检测到语言切换标志<br/>播放 200ms 淡入动画
```

### 网络请求流程

```mermaid
sequenceDiagram
    autonumber
    participant Caller as ViewModel / Activity
    participant Repo as FrameworkRepository
    participant Cache as ApiService 缓存
    participant NM as NetworkModule
    participant OK as OkHttpClient
    participant INT as Interceptors
    participant API as ApiService
    participant SV as Server

    Caller->>Repo: login(request)
    Repo->>Cache: getApiService()
    alt 缓存命中 (IP:Port 未变)
        Cache-->>Repo: 返回缓存 ApiService
    else 缓存未命中
        Cache->>NM: createApiService(baseUrl)
        activate NM
        NM->>OK: Builder + 超时配置
        NM->>INT: addInterceptor(HttpLogging)
        NM->>INT: addInterceptor(Version)
        NM->>INT: addInterceptor(Token)
        NM->>INT: addInterceptor(Auth)
        NM->>OK: configureSslSocketFactory
        Note right of OK: Debug → 信任所有证书<br/>Release → 仅信任 raw 证书
        NM->>API: Retrofit.create(ApiService)
        deactivate NM
        API-->>Repo: ApiService 实例 (缓存)
    end

    Repo->>API: deviceLogin(request)
    activate API
    API->>INT: TokenInterceptor 加 Authorization / clientid
    INT->>OK: 加入 VersionCode / VersionName
    OK->>SV: HTTP Request
    SV-->>OK: HTTP Response
    OK-->>INT: 拦截器链逆序处理
    INT->>INT: AuthErrorInterceptor 检查 401
    alt HTTP 401 或 业务 code == 401
        INT->>Repo: clearToken() + onTokenExpired()
    end
    INT-->>API: Response
    API-->>Repo: ApiResponse<T>
    deactivate API
    Repo-->>Caller: ApiResponse<T>
```

### WebSocket 连接流程

```mermaid
sequenceDiagram
    autonumber
    participant Caller as 业务代码
    participant Repo as FrameworkRepository
    participant WSM as WebSocketManager
    participant OK as OkHttp
    participant SV as Server

    Caller->>Repo: connectWebSocket()
    Repo->>WSM: connect(ip, port, sn)

    activate WSM
    WSM->>WSM: 检查 (ip, port) 是否已连接
    alt 已连接 / 正在连接 相同服务器
        WSM-->>Repo: 跳过
    else 需要连接
        WSM->>WSM: 构建 URL<br/>ws://ip:port/apiPrefix/resource/websocket
        WSM->>OK: Builder + 共享 SSL 配置
        WSM->>OK: newWebSocket(request, listener)

        OK->>SV: WebSocket Upgrade
        SV-->>OK: 101 Switching Protocols
        OK-->>WSM: onOpen
        WSM->>WSM: _connectionState = Connected
        WSM->>WSM: 启动自动重连协程

        loop 消息循环
            SV-->>WSM: onMessage(text / bytes)
            WSM->>WSM: _messageFlow.value = text
        end

        alt 异常断开 (onFailure / onClosed 非主动)
            SV-->>WSM: onFailure(throwable)
            WSM->>WSM: _connectionState = Error
            Note over WSM: 自动重连协程检测状态<br/>delay(5s) 后重新 connect
        end
    end
    deactivate WSM
    Repo-->>Caller: 连接已建立
```