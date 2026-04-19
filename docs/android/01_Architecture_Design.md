# Flux Android - 核心架构设计文档

## 1. 架构愿景与目标

Flux 是一个“本地优先”的个人效率与生活记录系统。在向 Android 平台迁移和原生化开发的过程中，架构设计必须坚守以下核心目标：
- **本地优先与数据绝对掌控**：所有核心业务必须在无网环境下完全可用，所有数据（结构化数据与附件二进制）优先落盘本地存储。
- **高响应性与离线能力**：UI 必须是响应式的，任何数据变更需即时反映在界面上，无网络加载的割裂感。
- **高内聚低耦合的单体化演进**：虽是单机应用，但需严格按照功能域（日记、待办、日历、附件）进行模块化隔离，为后续可能的多端同步打好地基。

## 2. 核心技术栈选型

针对 Android 原生开发，为兼顾现代开发范式与长期可维护性，确立以下技术选型：

| 领域 | 核心技术 | 选型原因 |
| :--- | :--- | :--- |
| **开发语言** | Kotlin | 现代语言特性，协程支持，Android 官方推荐 |
| **UI 框架** | Jetpack Compose | 声明式 UI，便于构建复杂状态的交互（如日历多图层显示），更易实现设计规范中的“组件化”思想 |
| **架构模式** | MVVM / MVI | 配合 Compose 强绑定的状态驱动 UI (StateFlow)，使得状态流转可控 |
| **本地存储** | Room Database | 官方 SQLite ORM 库，完美支持 FTS5 全文检索及 Flow 响应式查询 |
| **异步/响应式** | Coroutines & Flow | 用于数据库操作、文件系统读写和 UI 状态流转，替代传统的回调和 RxJava |
| **依赖注入** | Hilt | 降低模块间耦合，统一管理单例（如 Database 实例、文件管理器、各域的 Repository） |
| **依赖管理** | Gradle (Kotlin DSL) | `build.gradle.kts` 已配置，更好的类型推断和配置代码化 |

## 3. 核心分层架构

架构将遵循现代 Android 应用架构指南（Modern Android Architecture），分为 **UI 层**、**领域层（Domain Layer）** 和 **数据层（Data Layer）**。

### 3.1 表现层 / UI 层 (Presentation Layer)
- **职责**：只负责将状态（State）渲染到屏幕，并将用户意图（Intent/Event）传递给 ViewModel。
- **组件**：
  - **Screen / Composables**：纯 UI 呈现，不包含业务逻辑。
  - **ViewModel**：管理 UI State，处理用户输入，调用 Domain 层/Data 层进行业务操作。通过 `StateFlow` 将单一且不可变的状态暴露给 UI。

### 3.2 领域层 (Domain Layer) - 建议保留
- **职责**：封装复杂或跨模块的业务规则。例如：“恢复一条删除的日记时，如果同日已有记录，需进行增量合并” —— 这种跨表、高一致性要求的逻辑不应放在 ViewModel，而应放在 UseCase。
- **组件**：
  - **UseCases (Interactors)**：例如 `RestoreDiaryUseCase`、`ToggleHolidayUseCase`。

### 3.3 数据层 (Data Layer)
- **职责**：提供对应用数据的完全访问权限。负责读取、写入以及保证本地数据的一致性。
- **组件**：
  - **Repositories**：向外暴露数据流（如 `Flow<List<Diary>>`），屏蔽数据来源细节。
  - **Data Sources**：
    - `LocalRoomDataSource` (结构化数据)
    - `FileAttachmentDataSource` (附件二进制文件)

## 4. 模块划分 (Modularization)

为防止单体应用演变为“大泥球”，工程结构应按“特性（Feature）”而非“层（Layer）”进行拆分：

```text
app/src/main/java/com/flux/
  ├── core/                 # 核心基础库
  │   ├── database/         # Room 数据库配置与通用 Dao
  │   ├── designsystem/     # 全局 Compose Theme、Color、Typography 和通用组件
  │   ├── model/            # 跨模块通用实体 (如通用异常)
  │   └── util/             # 文件管理、时间格式化、扩展函数
  ├── feature/
  │   ├── diary/            # 日记业务域：包括写日记、标签筛选、回收站
  │   ├── todo/             # 待办业务域：待办列表、子任务、历史
  │   ├── calendar/         # 日历业务域：聚合视图、节假日管理
  │   ├── attachment/       # 附件管理域：文件扫描、引用清理
  │   └── settings/         # 系统设置、数据导出
```

## 5. 关键技术方案

### 5.1 数据响应流 (Reactive Data Flow)
由于采用 Room + Flow，所有的列表展示（日记列表、日历事件聚合）不再需要手动刷新。数据库表发生变更时，Room 会自动向监听该查询的 Flow 发射新数据。ViewModel 通过 `stateIn` 操作符将其转化为 `StateFlow`，Compose UI 自动重组。这完美契合了“日历图层切换不改变数据，只改变 UI 展现”的原则。

### 5.2 全文检索实现方案
继续沿用原有 SQLite 的 FTS5 特性。在 Room 中，通过 `@Fts5` 注解创建虚拟表，将 `DiaryEntity` 关联。搜索时，通过 ViewModel 向 Dao 传递关键词， Dao 执行 `MATCH` 语句并返回 `Flow<List<Diary>>`，搜索结果将做到字符输入级别的毫秒级响应。

### 5.3 附件存储与弱一致性模型
- **存储机制**：使用 Android 的 `Context.getExternalFilesDir()` 或内部存储存放。将附件按 `YYYY/MM/UUID.ext` 规则归档。
- **引用机制**：数据库内不建附件强关系表。依赖领域层的 `AttachmentScannerUseCase`，通过正则表达式读取 `content_md` 字段中的 Markdown 引用，动态计算附件的“被引用状态”供“附件管理”模块使用。

### 5.4 软删除机制的全局统一
在业务表（如 `diaries`, `todos`, `calendar_events`）引入 `deleted_at: Long?` 字段。
Room Dao 在常规查询（获取活跃列表）时统一追加 `WHERE deleted_at IS NULL` 条件；在回收站功能中则查询 `deleted_at IS NOT NULL` 的数据。所有“删除”动作本质上是执行一次 `UPDATE` 操作。
