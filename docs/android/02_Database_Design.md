# Flux Android - 本地数据库设计文档

## 1. 数据库定位

Flux Android 端使用 Room / SQLite 作为本地唯一真实数据源。当前数据库版本为 `11`，schema 导出目录为：

```text
app/schemas/com.example.flux.core.database.FluxDatabase/
```

数据库文件位于 App 私有目录：

```text
files/data/flux.db
```

设计准则：

- 核心业务事实写入 SQLite，附件二进制文件保存在 `files/data/attachments/`。
- 删除使用软删除字段 `deleted_at`，由回收站统一读取。
- 时间字段当前统一保存为字符串，日期使用 `YYYY-MM-DD`，时间点使用项目内 `TimeUtil` 生成的 ISO 字符串。
- 业务约束不能只依赖 UI，重要约束需要由索引、Repository 或 UseCase 兜底。
- 备份恢复以 `data/` 目录为边界，数据库和附件一起打包。

## 2. Entity 总览

`FluxDatabase` 当前包含以下实体：

| 表 | Entity | 说明 |
| :--- | :--- | :--- |
| `diaries` | `DiaryEntity` | 日记主体 |
| `diary_search_index` | `DiaryFtsEntity` | App 维护的日记搜索索引 |
| `diary_tags` | `DiaryTagEntity` | 日记标签 |
| `diary_tag_links` | `DiaryTagLinkEntity` | 日记与标签多对多关联 |
| `todo_projects` | `TodoProjectEntity` | 待办项目/分组 |
| `todos` | `TodoEntity` | 待办主体 |
| `todo_subtasks` | `TodoSubtaskEntity` | 子任务 |
| `todo_history` | `TodoHistoryEntity` | 待办操作历史 |
| `calendar_events` | `CalendarEventEntity` | 手动事件和 ICS 订阅事件 |
| `calendar_holidays` | `CalendarHolidayOverrideEntity` | 用户节假日覆盖 |
| `calendar_static_holidays` | `CalendarStaticHolidayEntity` | 静态节假日 |
| `calendar_subscription` | `CalendarSubscriptionEntity` | ICS 日历订阅 |
| `attachment_metadata` | `AttachmentMetadataEntity` | 附件索引和引用统计 |

## 3. 日记模型

### 3.1 `diaries`

核心字段：

| 字段 | 说明 |
| :--- | :--- |
| `id` | 主键 |
| `entry_date` | 日记日期，`YYYY-MM-DD` |
| `entry_time` | 可选时间 |
| `title` | 兼容旧数据和列表展示，产品上不强调标题 |
| `content_md` | Markdown 正文 |
| `mood` / `weather` / `location_name` | 元数据 |
| `is_favorite` | 收藏标记，`0/1` |
| `word_count` | 正文字数 |
| `reminder_minutes` | 提前提醒分钟数 |
| `created_at` / `updated_at` / `deleted_at` | 审计与软删除 |
| `restored_at` / `restored_into_id` | 合并恢复记录 |
| `version` | 本地版本号 |

索引：

- `idx_diaries_date(entry_date, deleted_at)`
- `idx_diaries_mood(mood, deleted_at)`
- `idx_diaries_one_active_per_day(entry_date)`，唯一索引

当前唯一索引会限制同一天出现多条日记记录。日记恢复合并逻辑用 `restored_at` 和 `restored_into_id` 保留来源记录状态，避免重复恢复。

### 3.2 `diary_search_index`

这是普通 Room 表，不是 FTS 虚拟表。它用于避免依赖 Android SQLite 可选 FTS 模块。

字段覆盖：

- `diary_id`
- `entry_date`
- `entry_time`
- `title`
- `content_md`
- `mood`
- `weather`
- `location_name`
- `tags`

日记创建、更新、删除和恢复时由 Repository 维护索引内容。

### 3.3 标签表

`diary_tags` 保存标签本体，`diary_tag_links` 保存多对多关系。关联表以 `diary_id + tag_id` 为联合主键，并对 `tag_id + deleted_at` 建索引，便于按标签筛选活跃日记。

## 4. 待办模型

### 4.1 `todos`

核心字段：

| 字段 | 说明 |
| :--- | :--- |
| `id` | 主键 |
| `project_id` | 所属项目/分组 |
| `title` / `description` | 标题与描述 |
| `status` | `pending` / `completed` |
| `priority` | 当前使用 `none` / `normal` 等字符串兼容旧数据 |
| `due_at` / `start_at` | 截止和开始时间 |
| `completed_at` | 完成时间 |
| `sort_order` | 手动排序 |
| `is_my_day` | 我的 一天兼容字段，当前不是主要 UI 入口 |
| `is_important` | 重要标记 |
| `reminder_minutes` | 提前提醒分钟数 |
| `deleted_at` | 软删除 |

索引：

- `idx_todos_due(due_at, status, deleted_at)`
- `idx_todos_project(project_id, deleted_at)`
- `idx_todos_status(status, deleted_at)`

`project_id` 外键指向 `todo_projects.id`。

### 4.2 子任务与历史

`todo_subtasks` 通过 `todo_id` 关联待办，支持软删除和排序。

`todo_history` 记录待办操作历史，字段包括：

- `action`
- `summary`
- `payload_json`
- `created_at`

完成、重新打开、编辑等操作可通过历史表追踪。

## 5. 日历模型

### 5.1 `calendar_events`

事件字段：

| 字段 | 说明 |
| :--- | :--- |
| `title` / `description` | 标题与描述 |
| `start_at` / `end_at` | 起止时间 |
| `all_day` | 全天事件 |
| `color` | 展示颜色 |
| `location_name` | 地点 |
| `reminder_minutes` | 提前提醒分钟数 |
| `recurrence_rule` | 手动重复规则 |
| `subscription_id` / `external_uid` / `external_hash` | ICS 订阅事件元数据 |
| `deleted_at` | 软删除 |

索引：

- `idx_events_range(start_at, end_at, deleted_at)`
- `idx_events_start(start_at, deleted_at)`
- `idx_events_subscription(subscription_id, external_uid)`，唯一索引

`subscription_id + external_uid` 用于保证 ICS 同一事件重复同步时可幂等更新。

### 5.2 节假日

`calendar_static_holidays` 保存静态节假日数据，字段包括日期、是否假日、名称、来源和更新时间。

`calendar_holidays` 保存用户覆盖。日期是主键，用户覆盖优先级高于静态节假日和默认周末规则。

### 5.3 ICS 订阅

`calendar_subscription` 保存：

- 订阅名称
- ICS URL
- 是否启用
- 最近同步时间
- ETag
- Last-Modified
- 最近错误

后台同步会根据订阅元数据进行条件下载。

## 6. 附件模型

`attachment_metadata` 以 `relative_path` 为主键，不保存绝对路径。

字段：

| 字段 | 说明 |
| :--- | :--- |
| `relative_path` | 相对 `data/attachments` 的路径 |
| `file_name` | 文件名 |
| `kind` | 图片、音频、普通文件等类型 |
| `size_bytes` | 文件大小 |
| `modified_at` | 文件修改时间 |
| `sha256` | 文件 hash |
| `reference_count` | Markdown 引用次数 |
| `last_scanned_at` | 最近扫描时间 |

索引覆盖类型、大小、修改时间和 hash，支持附件管理筛选和排序。

## 7. 关键一致性规则

### 7.1 一天一篇日记

产品模型是一日一篇主日记。保存日记时，Repository 会根据日期和 id 判断是创建、更新还是拒绝冲突。数据库唯一索引作为最后兜底。

### 7.2 软删除

普通列表查询默认过滤 `deleted_at IS NULL`。回收站查询 `deleted_at IS NOT NULL`，并排除已经合并恢复的来源日记。

删除操作还需要同步处理：

- 日记：更新搜索索引，取消提醒。
- 待办：写入历史，取消提醒。
- 事件：取消提醒。

### 7.3 恢复

恢复逻辑位于 use case：

- `RestoreDiaryUseCase`
- `RestoreTodoUseCase`
- `RestoreEventUseCase`

恢复后需要刷新搜索索引和重新安排有效提醒。

### 7.4 ICS 同步

ICS 同步按 `subscription_id + external_uid` 做幂等更新。外部事件从订阅中消失时，当前实现会删除对应订阅事件；如果订阅返回空事件集合，则会删除该订阅下已有外部事件。

## 8. 迁移

Room 数据库通过 `DatabaseModule` 注册迁移：

- `1 -> 8`：旧库兼容、补齐源表、规范化预置库。
- `2 -> 3`：重建待办历史表结构。
- `3 -> 6`：重建日记搜索索引。
- `6 -> 7`：新增静态节假日表。
- `7 -> 8`：新增附件 metadata 表和索引。
- `8 -> 9`：新增 ICS 订阅表和订阅事件字段。
- `9 -> 10`：重建待办表，补齐开始时间、提醒、重要等字段约束。
- `10 -> 11`：日记新增 `reminder_minutes` 并重建搜索索引。

所有新增字段都应同步更新：

- Entity
- Dao / Repository
- Room schema 导出
- 迁移
- 备份增量合并表或列兼容逻辑
- 文档

## 9. 备份与增量恢复

导出备份前会执行 WAL checkpoint，然后把 `files/data/` 打包成 zip：

```text
data/flux.db
data/attachments/...
```

全量恢复：

- 关闭数据库。
- 备份当前 `data/` 为 `data_before_restore_*`。
- 用备份包中的 `data/` 替换当前目录。

增量恢复：

- 备份当前 `data/`。
- ATTACH 备份数据库。
- 按主键删除本机同名记录后插入备份记录。
- 合并附件文件，跳过数据库文件本身。

增量恢复覆盖的表由 `ImportBackupUseCase.MERGE_TABLES` 维护。
