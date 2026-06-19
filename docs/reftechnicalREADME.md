# Flux Android 技术实现文档

本文档描述当前 Android 原生工程的实现现状。旧版 Web/Python 单体实现已经不再是主线；历史设计只作为产品语义参考，工程实现以本仓库的 Kotlin / Jetpack Compose / Room 版本为准。

## 1. 项目定位

Flux 是一个本地优先的 Android 个人记录与日程管理应用。核心目标是把日记、待办、日历、附件、提醒、回收站和备份放进同一个离线可用的数据空间里。

当前实现原则：

- 核心业务数据保存在 App 私有目录下的 Room / SQLite 数据库中。
- 附件文件保存在 App 私有 `files/data/attachments` 目录中，正文通过 Markdown 引用。
- 删除优先使用软删除，回收站负责恢复和合并。
- WebDAV 只承担手动云备份与恢复，不做多端自动合并。
- UI 使用 Compose 状态驱动，页面通过 `ViewModel` 暴露 `UiState`。

## 2. 技术栈

| 领域 | 当前技术 |
| :--- | :--- |
| 语言 | Kotlin |
| UI | Jetpack Compose、Material 3、Navigation Compose |
| 架构 | 单模块应用内的 feature 分层、MVVM、StateFlow |
| 数据库 | Room、SQLite、导出 schema |
| 依赖注入 | Hilt |
| 异步 | Coroutines、Flow |
| 图片与附件预览 | Coil、FileProvider |
| 小部件 | Glance App Widgets |
| 后台任务 | WorkManager、BroadcastReceiver |
| 提醒 | AlarmManager、通知、开机/时区/权限变更后重排 |
| 云备份 | OkHttp、WebDAV |
| 构建 | Gradle Kotlin DSL、AGP 9.2、Kotlin 2.2 |

当前工程不再包含 Python HTTP Server、浏览器前端、REST API 服务端、Redis、PostgreSQL 或 Elasticsearch。

## 3. 工程结构

```text
app/src/main/java/com/example/flux
├── FluxApplication.kt           # 应用启动、数据目录初始化、ICS Worker 与提醒重排
├── MainActivity.kt              # Compose 承载、通知权限请求、启动目的地处理
├── app/navigation               # 主导航、路由、底部/自适应导航入口
├── core                         # 数据库、Repository、UseCase、提醒、同步、工具
├── feature                      # diary / calendar / todo / settings / trash / search / widget
└── ui                           # 通用 Compose 组件和主题
```

`feature/*` 下按 `data / domain / presentation` 分层：

| 层级 | 职责 |
| :--- | :--- |
| `presentation` | Compose Screen、组件、ViewModel、页面状态 |
| `domain` | feature gateway、页面所需模型和业务边界 |
| `data` | gateway 默认实现、Hilt 绑定、对 core Repository / UseCase 的编排 |
| `core` | Room、Repository、跨 feature UseCase、提醒、备份、ICS、工具函数 |

## 4. 导航与页面

主入口由 `FluxAppNavHost` 承载，一级目的地定义在 `AppDestinations`：

| 目的地 | 页面 |
| :--- | :--- |
| `DIARY` | 日记列表与筛选 |
| `CALENDAR` | 月 / 周 / 日 / 季度日历 |
| `TODO` | 待办列表 |
| `SETTINGS` | 备份、订阅、附件、回收站和偏好设置 |

二级路由包括：

- 日记编辑器：`editor/{diaryId}`，也支持按日期新建。
- 待办详情：`todo/{todoId}`。
- 回收站：`trash`。
- 附件管理：`attachment_manager?query=...`。
- ICS 日历订阅设置：`calendar_subscriptions`。
- 天气 App 绑定：`weather_app_binding`。

全局搜索以底部弹层呈现，覆盖日记、待办、事件和附件；点击搜索结果会跳转到对应编辑器、待办详情、日历日期或附件管理查询。

## 5. 数据目录

启动时 `FluxApplication` 会调用 `DataDirectoryInitializer.ensure()`，确保 App 私有数据目录存在。核心路径由 `DataPaths` 统一维护：

```text
files/
└── data/
    ├── flux.db
    └── attachments/
```

仓库中 `app/src/main/assets/flux.db` 是预置数据库。启动时会对已有预置库做必要的主键规范化，避免旧库结构影响 Room 访问。

## 6. 数据库模型

当前 Room 数据库版本为 `11`，schema 导出到：

```text
app/schemas/com.example.flux.core.database.FluxDatabase/
```

主要实体：

| 表 | Entity | 说明 |
| :--- | :--- | :--- |
| `diaries` | `DiaryEntity` | 日记正文、日期、心情、天气、位置、收藏、提醒、软删除 |
| `diary_search_index` | `DiaryFtsEntity` | App 维护的日记搜索索引，不依赖可选 FTS 模块 |
| `diary_tags` / `diary_tag_links` | `DiaryTagEntity` / `DiaryTagLinkEntity` | 日记标签与关联 |
| `todos` | `TodoEntity` | 待办、开始/截止时间、提醒、重要标记、软删除，并保留 `is_my_day` 兼容字段 |
| `todo_subtasks` | `TodoSubtaskEntity` | 子任务 |
| `todo_projects` | `TodoProjectEntity` | 待办项目/分组 |
| `todo_history` | `TodoHistoryEntity` | 待办操作历史 |
| `calendar_events` | `CalendarEventEntity` | 手动事件与 ICS 订阅事件 |
| `calendar_holidays` | `CalendarHolidayOverrideEntity` | 用户节假日覆盖 |
| `calendar_static_holidays` | `CalendarStaticHolidayEntity` | 静态节假日 |
| `calendar_subscription` | `CalendarSubscriptionEntity` | ICS 订阅配置和同步元数据 |
| `attachment_metadata` | `AttachmentMetadataEntity` | 附件索引、大小、hash、引用计数 |

重要约束：

- `diaries.entry_date` 有唯一索引，当前产品模型是一日一篇未删除主日记。
- 业务删除通过 `deleted_at` 标记；普通列表默认只读取活跃数据。
- `calendar_events` 对 `subscription_id + external_uid` 建唯一索引，用于 ICS 订阅幂等同步。
- 附件以 `relative_path` 作为主键，避免把 App 私有绝对路径写入数据库。

## 7. 核心业务

### 7.1 日记

日记支持 Markdown 正文、日期、时间、心情、天气、地点、标签、收藏、附件和提醒提前量。保存时由 `DiaryRepository` 维护日记表、标签关联、搜索索引和提醒。

附件插入流程：

```text
系统文件选择器
-> DiaryEditorViewModel 复制到 data/attachments
-> 正文插入 Markdown 引用
-> 附件管理扫描引用关系
```

单个附件大小限制为小于 100MB。

### 7.2 待办

待办支持项目、描述、开始时间、截止时间、优先级、重要标记、子任务、完成状态、提醒和操作历史。完成待办会写入历史，并取消后续提醒。数据库仍保留 `is_my_day` 兼容字段，但当前不是主要 UI 入口。

### 7.3 日历

日历通过 `CalendarAggregatorUseCase` 聚合日记、待办、事件、节假日和回收站数据，支持月、周、日、季度视图。图层开关只影响展示，不修改源数据。

手动事件支持提醒和重复规则；订阅事件来自 ICS，同步后写入 `calendar_events` 并保留外部 UID 与 hash。

### 7.4 回收站

回收站覆盖日记、待办和事件。恢复逻辑放在 use case 中：

- 日记恢复时，如果当天没有活跃日记，则直接恢复。
- 日记恢复时，如果当天已有活跃日记，则将删除日记合并到当天日记，并记录 `restored_at` / `restored_into_id`。
- 待办和事件恢复会清空 `deleted_at`，并重新安排可用提醒。

### 7.5 附件管理

`AttachmentManagerUseCase` 扫描附件目录和日记 Markdown 引用，维护附件 metadata、引用计数和引用来源。附件管理页可以按文件名、类型、引用状态查询，并清理未引用文件。

### 7.6 全局搜索

全局搜索由 `GlobalSearchViewModel` 汇总日记、待办、事件和附件数据。搜索是本地内存过滤和排序，适合当前个人数据规模；日记页面仍维护独立的搜索索引用于列表查询。

## 8. 提醒系统

提醒由 `ReminderScheduler`、`ReminderPlanner`、`ReminderReceiver` 和 `ReminderRescheduler` 组成。

支持对象：

- 日记：基于 `entry_date + entry_time - reminder_minutes`。
- 待办：优先基于 `due_at`，否则基于 `start_at`。
- 日历事件：基于 `start_at - reminder_minutes`，重复事件会扫描下一次有效发生时间。

系统行为：

- 新增、更新、恢复记录时重新安排提醒。
- 删除、完成记录时取消提醒。
- App 启动、开机、应用更新、精确闹钟权限变化、系统时间或时区变化后重新安排提醒。
- Android 13+ 请求通知权限；Android 12+ 会检查精确闹钟权限，不可用时退回非精确提醒。

## 9. ICS 日历订阅

设置页提供 ICS 订阅管理，可以添加、编辑、启停、删除和手动同步订阅。

后台同步由 `IcsSyncWorker` 每小时触发一次，要求网络可用。同步逻辑：

```text
读取启用的订阅
-> 使用 ETag / Last-Modified 条件下载
-> 解析 VEVENT
-> 按 subscription_id + external_uid 插入、更新或删除事件
-> 保存同步元数据和错误信息
```

当前 ICS 导入会保存标题、描述、起止时间、全天标记、地点和外部 hash；订阅事件的提醒和重复规则不从 ICS 自动映射。

## 10. 备份与恢复

本地备份和 WebDAV 云备份使用同一套 zip 格式：

```text
data/flux.db
data/attachments/...
```

| 模式 | 行为 |
| :--- | :--- |
| 全量恢复 | 用备份包中的 `data/` 替换本机 `data/` |
| 增量恢复 | 合并备份数据库中可匹配的表和主键，同时合并附件文件 |

WebDAV 备份固定面向坚果云 WebDAV，远端目录为 `FluxBackups`。更多细节见 [syncREADME.md](syncREADME.md)。

## 11. 小部件

当前提供两个 Glance 小部件：

- 待办列表小部件。
- 月视图日历小部件。

小部件通过 Hilt entry point 读取数据库聚合数据，并提供刷新与跳转到 App 指定目的地的 action。由于 Android 小部件刷新受系统调度限制，用户可通过小部件右上角刷新按钮手动拉取最新数据。

## 12. 构建与测试

推荐使用 Android Studio 自带 JBR。常用命令：

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Windows PowerShell 示例：

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

当前重点测试覆盖：

- `RecurrenceUtil`
- ICS 解析
- 提醒时间解析与提醒计划
- WebDAV 路径、配置完整性和 hash 工具
- Markdown 解析与渲染标注
- 日记编辑文本动作

## 13. 发布前注意事项

当前工程仍使用调试阶段包名：

```text
com.example.flux
```

正式发布前需要确认：

- `namespace` 和 `applicationId` 是否替换为正式包名。
- Release 签名、混淆、备份规则和隐私声明。
- 通知、精确闹钟、网络、文件分享等权限说明。
- WebDAV 凭据保存和错误提示文案。
- 预置数据库和 Room schema 迁移链。
