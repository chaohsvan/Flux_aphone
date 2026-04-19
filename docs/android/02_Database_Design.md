# Flux Android - 本地数据库设计文档

## 1. 数据库选型与准则

Android 端的本地数据持久化选用 **Room Database**。
作为 Flux 系统的唯一真实数据源（Single Source of Truth），本地数据库必须承担起保护数据一致性的最终责任。
- 绝不依赖 UI 层保证约束（如“一天一篇主日记”必须在数据库事务和实体索引级别得到保障）。
- 一切业务对象删除均采用**软删除（Soft Delete）**，便于历史找回。
- 时间戳字段（如 `created_at`, `updated_at`, `deleted_at`）建议统一使用 `Long` (Unix Epoch 毫秒) 存储，由 TypeConverter 统一转换。

## 2. 核心实体模型设计 (Entity Mapping)

基于原系统 SQLite 表结构，映射为 Room Entity。

### 2.1 日记模块 (Diary)

**主表 `diaries`**
```kotlin
@Entity(
    tableName = "diaries",
    indices = [
        Index(value = ["entry_date"], unique = true) // 此处需配合业务逻辑保证活跃状态下的唯一性
    ]
)
data class DiaryEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "entry_date") val entryDate: String, // YYYY-MM-DD
    @ColumnInfo(name = "entry_time") val entryTime: String?, // HH:mm
    val title: String,
    @ColumnInfo(name = "content_md") val contentMd: String = "",
    val mood: String?,
    val weather: String?,
    @ColumnInfo(name = "location_name") val locationName: String?,
    @ColumnInfo(name = "is_favorite") val isFavorite: Boolean = false,
    @ColumnInfo(name = "word_count") val wordCount: Int = 0,
    
    // 软删除与恢复逻辑字段
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    @ColumnInfo(name = "restored_at") val restoredAt: Long? = null,
    @ColumnInfo(name = "restored_into_id") val restoredIntoId: String? = null,
    
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    val version: Int = 1
)
```

**标签及其多对多关联**
```kotlin
@Entity(tableName = "diary_tags")
data class DiaryTagEntity(...)

@Entity(
    tableName = "diary_tag_links",
    primaryKeys = ["diary_id", "tag_id"]
)
data class DiaryTagLinkEntity(
    @ColumnInfo(name = "diary_id") val diaryId: String,
    @ColumnInfo(name = "tag_id") val tagId: String,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null
)
```

**全文检索虚拟表 (FTS5)**
```kotlin
@Entity(tableName = "diaries_fts")
@Fts5(contentEntity = DiaryEntity::class)
data class DiaryFtsEntity(
    @ColumnInfo(name = "diary_id") val diaryId: String,
    @ColumnInfo(name = "entry_date") val entryDate: String,
    @ColumnInfo(name = "content_md") val contentMd: String,
    val mood: String?,
    val tags: String? // 组装好的标签字符串
)
```

### 2.2 待办模块 (Todo)

**主表 `todos`**
```kotlin
@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "project_id") val projectId: String?, // UI 显示为“标签”
    val title: String,
    val description: String = "",
    val status: String = "pending", // pending, completed
    val priority: String = "normal", // normal, high
    @ColumnInfo(name = "due_at") val dueAt: Long?,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
    // ... 其他审计字段
)
```

**子任务 `todo_subtasks`**
子任务与父任务是 `1对多` 关系，由 Room 的 `@Relation` 处理聚合查询。

### 2.3 日历模块 (Calendar)

包含 `calendar_events`（事件）、`calendar_holidays`（用户标记覆盖）与 `calendar_static_holidays`（静态库节假日）。

## 3. 约束与一致性保障设计

### 3.1 “一天一篇主日记” 的落地
在 Android 原生端：
1. **数据库兜底**：使用 Partial Index（如果 SQLite/Room 版本支持 `CREATE UNIQUE INDEX ... WHERE deleted_at IS NULL`）。如果 Room 在注解支持上有局限，可以在 `DiaryDao` 中提供事务级别的 `upsertDiary` 方法。
2. **Dao 事务保障**：
```kotlin
@Transaction
suspend fun saveDiarySafe(diary: DiaryEntity) {
    val existing = getActiveDiaryByDate(diary.entryDate)
    if (existing != null && existing.id != diary.id) {
        // 更新现有日记或处理冲突
        updateDiary(...)
    } else {
        insertDiary(diary)
    }
}
```

### 3.2 回收站日记的合并恢复逻辑
必须置于 `Domain Layer` (例如 `RestoreDiaryUseCase`) 内作为单事务执行：
1. 判断当天是否已有未删除的活跃日记。
2. 若无：清空该删除记录的 `deleted_at` 字段。
3. 若有：将已删除日记的文本追加到当前活跃日记的 `content_md` 中，处理标签的去重并入；为原删除记录写入 `restored_at` 和 `restored_into_id`，使其彻底归档。

### 3.3 已完成待办置底排序
查询待办列表时，在 Room DAO 中强制要求排序规则：
```sql
SELECT * FROM todos 
WHERE deleted_at IS NULL 
ORDER BY 
    CASE WHEN status = 'completed' THEN 1 ELSE 0 END ASC,
    CASE WHEN priority = 'high' THEN 0 ELSE 1 END ASC,
    due_at ASC,
    sort_order ASC
```

## 4. 迁移与数据同步策略

- **数据库迁移 (Migrations)**：务必预留良好的 Schema 版本管理策略。Room 提供了 `Migration` 类用于处理表结构的增删改。
- **跨平台与迁移过渡**：若后续支持从原 Web 版本导入数据，需支持读取原有 JSON 或 CSV 并进行完整的数据批量映射，保证 `id` 一致性和 `version` 的一致性。
