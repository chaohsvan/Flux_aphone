# Flux Android - 核心架构设计文档

## 1. 架构目标

Flux 是一个本地优先的个人记录与日程管理应用。Android 端架构围绕四个目标设计：

- **离线可用**：日记、待办、日历、附件和回收站在无网环境下可读写。
- **数据可控**：结构化数据和附件都保存在 App 私有目录，备份包结构清晰可检查。
- **状态响应式**：数据库和设置变化通过 Flow / StateFlow 进入 UI，减少手动刷新。
- **边界清晰**：虽然当前是单模块 Android App，仍按 feature、domain、data、core 划分职责。

## 2. 技术选型

| 领域 | 技术 | 用途 |
| :--- | :--- | :--- |
| 语言 | Kotlin | Android 主开发语言 |
| UI | Jetpack Compose、Material 3 | 声明式界面、响应式状态渲染 |
| 导航 | Navigation Compose、NavigationSuiteScaffold | 主路由和自适应导航 |
| 本地存储 | Room / SQLite | 结构化数据、schema 迁移、响应式查询 |
| 依赖注入 | Hilt | Repository、UseCase、Database、系统服务注入 |
| 异步 | Coroutines / Flow | 数据库、文件、网络和 UI 状态流 |
| 后台任务 | WorkManager | ICS 订阅周期同步 |
| 提醒 | AlarmManager、BroadcastReceiver、通知 | 日记、待办、事件提醒 |
| 小部件 | Glance App Widgets | 待办列表和月历小部件 |
| 网络 | OkHttp | WebDAV 备份和 ICS 下载 |

## 3. 分层模型

当前工程采用单模块内的分层结构：

```text
Presentation
  -> Feature Gateway / Domain
  -> Core UseCase / Repository
  -> Room / File / System Service
```

| 层级 | 典型位置 | 职责 |
| :--- | :--- | :--- |
| Presentation | `feature/*/presentation` | Compose 页面、组件、ViewModel、UiState |
| Feature Domain | `feature/*/domain` | 页面需要的网关接口、feature 级模型和边界 |
| Feature Data | `feature/*/data` | gateway 默认实现、Hilt 绑定、调用 core 能力 |
| Core Domain | `core/domain/*` | 跨 feature 的 use case，例如恢复、导出、备份、日历聚合 |
| Core Data | `core/database/*` | Room Database、Dao、Entity、Repository |
| System / Util | `core/reminder`、`core/sync`、`core/util`、`core/settings` | 提醒、WebDAV、ICS、路径、偏好设置 |

ViewModel 不直接拼复杂 SQL，也不直接操作系统服务；这类能力通过 gateway、repository 或 use case 间接完成。

## 4. 目录结构

```text
app/src/main/java/com/example/flux
├── FluxApplication.kt
├── MainActivity.kt
├── app/navigation
├── core
│   ├── database
│   │   ├── dao
│   │   ├── entity
│   │   ├── repository
│   │   └── di
│   ├── domain
│   │   ├── calendar
│   │   ├── diary
│   │   ├── settings
│   │   ├── todo
│   │   └── trash
│   ├── reminder
│   ├── settings
│   ├── sync
│   ├── ui
│   └── util
├── feature
│   ├── calendar
│   ├── diary
│   ├── search
│   ├── settings
│   ├── todo
│   ├── trash
│   └── widget
└── ui
    ├── component
    └── theme
```

`feature/trash` 同时承载回收站和附件管理。`feature/search` 是全局搜索入口。`feature/widget` 承载 Glance 小部件。

## 5. 应用启动

启动流程：

```text
FluxApplication.onCreate()
-> DataDirectoryInitializer.ensure()
-> normalizeExistingPrepackagedDatabase()
-> IcsSyncWorker.schedule()
-> ReminderRescheduler.rescheduleAll()

MainActivity.onCreate()
-> 请求 Android 13+ 通知权限
-> setContent { FluxAppNavHost(...) }
```

`MainActivity` 只保留启动和 Compose 承载职责；页面路由集中在 `app/navigation`。

## 6. 导航架构

一级目的地定义在 `AppDestinations`：

- `DIARY`
- `CALENDAR`
- `TODO`
- `SETTINGS`

二级路由定义在 `AppRoutes`：

- 日记编辑器
- 待办详情
- 回收站
- 附件管理
- ICS 日历订阅设置
- 天气 App 绑定

全局搜索由主 Scaffold 持有，作为底部弹层覆盖当前目的地。搜索结果点击后由结果类型决定跳转路径。

## 7. 数据响应流

Room Dao 暴露 Flow，Repository 和 Gateway 继续组合数据，ViewModel 通过 `stateIn` 或 `combine` 输出 `StateFlow<UiState>`。

典型链路：

```text
Room Dao Flow
-> Repository / UseCase
-> Feature Gateway
-> ViewModel UiState
-> Compose collectAsState()
```

日历图层开关、搜索 scope、筛选项等属于 UI 状态；日记、待办、事件、附件 metadata 属于业务事实。

## 8. 数据与文件

App 私有数据目录：

```text
files/
└── data/
    ├── flux.db
    └── attachments/
```

`DataPaths` 统一维护数据库、数据目录和附件目录路径。备份导出会先执行 WAL checkpoint，然后将 `data/` 打包为 zip。

附件不嵌入数据库正文。日记正文保存 Markdown 引用，附件管理通过扫描附件目录和 Markdown 引用维护 `attachment_metadata`。

## 9. 搜索

当前不依赖 Android SQLite 的可选 FTS 模块。日记搜索使用 App 维护的 `diary_search_index` 普通表，覆盖日期、时间、标题、正文、心情、天气、位置和标签。

全局搜索则汇总日记、待办、事件和附件，在内存中过滤并按日期排序。这个方案足够覆盖当前个人数据规模，同时避免引入外部搜索服务。

## 10. 提醒

提醒能力位于 `core/reminder`：

| 类 | 职责 |
| :--- | :--- |
| `ReminderPlanner` | 根据日记、待办、事件计算下一次触发时间 |
| `ReminderScheduler` | 用 AlarmManager 安排或取消提醒 |
| `ReminderReceiver` | 接收 alarm 并展示通知 |
| `ReminderRescheduler` | 启动、开机、更新、时间/时区变化后重排提醒 |

Repository 在新增、更新、恢复、删除和完成业务对象时同步安排或取消提醒。

## 11. ICS 与 WebDAV

ICS 日历订阅：

- `IcsSyncWorker` 每小时同步一次启用订阅。
- `IcsCalendarDownloader` 使用条件请求下载。
- `IcsCalendarParser` 解析 VEVENT。
- `IcsCalendarSyncUseCase` 将订阅事件写入 `calendar_events`。

WebDAV 云备份：

- 只用于手动备份和恢复。
- 固定使用坚果云 WebDAV 地址。
- 远端目录为 `FluxBackups`。
- 本地备份和云备份使用同一 zip 格式。

## 12. 软删除与恢复

日记、待办和事件都使用 `deleted_at` 表示软删除。常规列表只显示活跃数据，回收站读取删除数据。

恢复规则：

- 日记恢复可能与当天活跃日记合并。
- 待办恢复后重新进入待办列表。
- 事件恢复后重新进入日历聚合。
- 恢复后如有有效提醒配置，会重新安排提醒。

## 13. 演进原则

- 共享规则进入 `core/domain`，页面独有逻辑留在 feature 内。
- 新增数据库字段必须补充 Room schema、迁移和文档。
- WebDAV 继续保持备份职责，不重新混入多端实时同步语义。
- 大型页面继续优先拆组件和 UiState，避免 ViewModel 与 Screen 重新变胖。
