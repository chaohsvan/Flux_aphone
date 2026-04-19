# Flux 技术实现文档

## 1. 项目概述

Flux 是一个本地优先的个人效率与生活记录系统，核心能力覆盖日记、日历、待办、节假日、附件管理和数据导出。

当前系统采用轻量级单体架构：前端为原生 Web，后端为 Python 标准库 HTTP 服务，结构化数据存储在 SQLite 中，日记全文检索使用 SQLite FTS5，附件文件存储在本地文件系统。

该架构的目标不是追求分布式复杂度，而是在本地个人数据场景下实现：

- 低依赖
- 易运行
- 易备份
- 数据可控
- 功能闭环清晰

### 1.1 文档阅读路径

这份文档按“先全局、再模块、再运行、最后演进”的顺序组织。不同角色可以按下表快速定位重点：

| 阅读者 | 建议重点 |
| :--- | :--- |
| 技术架构师 | `1-9`、`13-21`、`24-29`，重点看架构边界、数据模型、状态机和技术债 |
| 后端开发 | `5-8`、`13-25`，重点看路由、表结构、数据流、错误处理和备份恢复 |
| 前端开发 | `4-7`、`14-18`、`26-28`，重点看 API 约定、业务状态、测试矩阵和模块拆分 |
| 数据库维护者 | `6`、`13`、`20`、`24`、`25`，重点看 SQLite 表结构、迁移和备份 |
| 测试人员 | `14-18`、`21-23`，重点看业务流、状态机、错误码和附件边界 |
| 产品/设计协作者 | `1`、`4`、`7`、`21`，重点看业务模型和关键约束 |

### 1.2 系统能力地图

Flux 当前可以按五类能力理解：

| 能力域 | 子能力 | 主要落点 |
| :--- | :--- | :--- |
| 记录系统 | 日记、Markdown、附件、标签、收藏、全文搜索 | `diaries`、`diaries_fts`、`data/attachments` |
| 时间系统 | 日历视图、日/周/月/季度、历史回看、节假日 | `calendar_events`、`calendar_holidays`、`calendar_static_holidays` |
| 行动系统 | 待办、子任务、优先级、拖拽排序、完成状态、历史 | `todos`、`todo_subtasks`、`todo_history` |
| 恢复系统 | 日记/待办/事件回收站、恢复、日记合并恢复 | `deleted_at`、`restored_at`、`restored_into_id` |
| 数据治理 | 导出、附件清理、备份恢复、迁移、同步预留 | `sync_outbox`、导出接口、附件扫描 |

### 1.3 分层模型

```text
Presentation Layer
  - index.html
  - styles.css
  - app.js rendering functions

Client State and Interaction Layer
  - view state
  - filters
  - selection state
  - dialog state
  - calendar layer state

API Layer
  - fetch wrapper
  - JSON endpoints
  - multipart attachment upload

Domain / Repository Layer
  - diary restore and merge
  - todo status and history
  - calendar aggregation
  - holiday resolution
  - attachment reference scan

Persistence Layer
  - SQLite tables
  - SQLite FTS5
  - data/attachments filesystem
```

该分层不是通过目录强制隔离，而是当前单体文件内部已经形成的逻辑分层。后续模块化时，应优先保持这些边界。

层级职责可以进一步拆解为：

| 层级 | 核心职责 | 当前承载位置 | 不应承担的职责 |
| :--- | :--- | :--- | :--- |
| 表现层 | 页面结构、视觉呈现、按钮、表单、弹窗、列表和日历格子 | `index.html`, `styles.css` | 不直接决定业务规则，不直接读写数据库 |
| 交互状态层 | 当前视图、筛选条件、选中项、弹窗状态、日历图层开关 | `app.js` 中的状态对象和事件处理函数 | 不持久化业务事实，不绕过 API 修改数据 |
| 前端服务层 | 封装 `fetch`、拼接查询参数、处理 JSON 和 multipart 上传 | `app.js` 中的 API helper | 不实现数据库筛选规则，不做最终一致性判断 |
| 路由层 | 解析 HTTP 方法和 URL，把请求分发给对应业务方法 | `FluxHandler._handle_api` | 不写复杂业务逻辑，不直接拼装跨模块聚合结果 |
| 领域服务层 | 执行日记、待办、日历、附件的业务规则和流程编排 | `FluxRepository` 的业务方法 | 不关心按钮样式、DOM 结构和页面布局 |
| 数据访问层 | SQL 查询、事务、索引维护、行数据转换 | `FluxRepository` 内部 SQL 与 helper | 不保存前端临时状态，不引入 UI 语义 |
| 持久化层 | 存储事实数据、全文索引、附件二进制文件 | `data/flux.db`, `data/attachments` | 不承担业务流程判断，只提供可靠读写基础 |

从职责归属看，当前项目虽然是单体结构，但已经具备可拆分边界：前端负责表达用户意图，后端负责解释意图并落库，SQLite 和文件系统负责保存事实。

目录到层级的对应关系如下：

| 目录 / 文件 | 所属层级 | 说明 |
| :--- | :--- | :--- |
| `apps/client/web/index.html` | 表现层 | 提供静态页面骨架、主要面板、弹窗容器和输入控件。 |
| `apps/client/web/styles.css` | 表现层 | 定义布局、色彩、按钮、卡片、日历、日记编辑器和响应式规则。 |
| `apps/client/web/app.js` | 交互状态层 + 前端服务层 | 维护页面状态、绑定事件、调用 API、渲染日记/日历/待办/设置页面。 |
| `apps/server/flux_server.py` | 路由层 + 领域服务层 + 数据访问层 | 承担 HTTP 路由、业务规则、SQLite 初始化、SQL 查询、附件服务和导出生成。 |
| `data/flux.db` | 持久化层 | 保存结构化业务数据、软删除状态、历史记录、FTS5 全文索引和静态节假日。 |
| `data/attachments/` | 持久化层 | 保存日记图片、音频和普通文件附件。 |
| `docs/technical/` | 工程知识层 | 记录架构、数据模型、API、状态机、部署和演进策略。 |
| `docs/design/` | 产品体验层 | 记录设计理念、用户场景、视觉语言和交互原则。 |

层级调用方向应保持单向：

```text
用户操作
  -> 表现层
  -> 交互状态层
  -> 前端服务层
  -> 后端路由层
  -> 领域服务层
  -> 数据访问层
  -> 持久化层
```

反向只允许通过响应数据和重新渲染体现。例如数据库不会主动驱动 DOM，领域服务也不直接操作页面元素；后端返回 JSON 后，由前端状态层决定如何刷新页面。

### 1.4 模块责任矩阵

| 模块 | 前端责任 | 后端责任 | 存储责任 | 关键约束 |
| :--- | :--- | :--- | :--- | :--- |
| 日记 | 编辑器、筛选、搜索、标签视觉、导出选择 | 创建/更新、FTS、恢复合并、标签关系 | `diaries`、`diary_tags`、`diaries_fts` | 一天只能有一篇未删除日记 |
| 日历 | 视图切换、图层显示、日期选择 | 日/月/历史聚合、节假日判断 | `calendar_events`、holiday tables | 图层影响展示，不改变数据 |
| 待办 | 列表、拖拽、完成按钮、多选、详情面板 | 状态更新、排序、历史、回收站 | `todos`、`todo_subtasks`、`todo_history` | 完成任务留在原列表并置底 |
| 附件 | 上传入口、Markdown 插入、管理面板 | 上传校验、静态服务、引用扫描、删除清理 | `data/attachments` | 小于 100MB，路径不得逃逸附件根目录 |
| 节假日 | 日历按钮、手动标记模式、假日视觉 | 默认周末、静态数据、用户覆盖 | `calendar_static_holidays`、`calendar_holidays` | 用户覆盖优先级最高 |
| 导出 | 多选、格式选择、下载触发 | 生成 JSON/CSV/Markdown 内容 | 即时生成，不持久化 | 附件二进制不随 Markdown 自动打包 |

### 1.5 横切能力地图

| 横切能力 | 当前实现 | 风险点 | 建议演进 |
| :--- | :--- | :--- | :--- |
| 时间处理 | 后端 UTC，前端本地展示，日记日期保持语义日期 | 技术接口时间与用户展示时间混淆 | 建立统一时间工具和测试用例 |
| 软删除 | `deleted_at` 跨日记、待办、事件复用 | 各模块恢复规则不同 | 抽象统一 recycle bin service |
| 搜索 | 日记使用 FTS5，待办使用普通筛选 | 搜索能力割裂 | 引入跨模块搜索入口 |
| 附件引用 | 扫描 Markdown 得出引用关系 | 数据量变大后性能下降 | 建立附件索引表 |
| 导出 | 前后端共同完成导出体验 | 附件未被打包 | 设计完整归档格式 |
| 迁移 | 启动时补列和建表 | 复杂迁移不可审计 | 引入 `schema_migrations` |

### 1.6 架构决策索引

| 决策 | 当前选择 | 原因 | 代价 |
| :--- | :--- | :--- | :--- |
| 应用形态 | 本地优先单体 | 降低部署成本，保护个人数据 | 不适合直接承载多用户高并发 |
| 数据库 | SQLite | 单文件、事务可靠、部署简单 | 复杂并发和横向扩展能力有限 |
| 搜索 | SQLite FTS5 | 和主库同源，避免外部搜索服务 | 主要覆盖日记，跨模块搜索尚未统一 |
| 附件 | 文件系统存储 | 大文件不污染数据库 | 引用一致性需要额外维护 |
| 前端 | 原生 Web | 无构建链路，便于快速迭代 | `app.js` 会随功能增长变大 |
| 删除 | 软删除 | 降低误删风险 | 需要定期治理历史数据 |
| 日记模型 | 一天一篇主日记 | 产品认知清晰，回看简单 | 导入和恢复必须处理同日冲突 |

## 2. 技术栈

| 层级 | 当前技术 | 作用 | 选型原因 |
| :--- | :--- | :--- | :--- |
| 前端 | HTML / CSS / Vanilla JavaScript | 页面结构、状态管理、业务渲染、接口调用 | 无构建依赖，降低本地运行门槛，适合当前 MVP 和本地工具形态 |
| 后端 | Python 标准库 HTTP Server | REST API、静态资源托管、附件服务、业务逻辑 | 零框架依赖，便于审查和本地部署 |
| 主数据库 | SQLite | 日记、待办、事件、节假日、回收站状态、历史记录 | 单文件、事务可靠、适合本地优先和个人数据 |
| 全文检索 | SQLite FTS5 | 日记关键词搜索和全文定位 | 无需外部搜索引擎，和 SQLite 数据保持同库一致 |
| 附件存储 | 本地文件系统 | 图片、音频、普通文件附件 | 大文件不进入数据库，便于备份、清理和直接服务 |
| 节假日数据 | SQLite 静态节假日表 + 用户覆盖表 | 周末、静态节假日、手动标记节假日 | 查询稳定，不依赖运行时读取外部文件 |
| 通信协议 | REST / JSON / multipart | 前后端通信与附件上传 | 简单、可调试、符合当前单体服务规模 |

当前项目没有使用 React、Next.js、Redis、Elasticsearch、PostgreSQL 或消息队列。原因是现阶段业务定位为本地单用户应用，这些基础设施会增加部署复杂度，而不是解决当前主要问题。

## 3. 目录结构

```text
apps/
  client/
    web/
      index.html
      app.js
      styles.css
  server/
    flux_server.py
data/
  flux.db
  attachments/
tools/
vendor/
docs/
  technical/
    README.md
  design/
    README.md
```

关键文件职责：

| 文件 | 职责 |
| :--- | :--- |
| `apps/client/web/index.html` | 页面骨架、主要视图、弹窗和输入控件 |
| `apps/client/web/app.js` | 前端状态、数据请求、渲染、交互逻辑 |
| `apps/client/web/styles.css` | 布局、视觉系统、响应式适配 |
| `apps/server/flux_server.py` | API 路由、SQLite 初始化、业务仓储、附件服务 |
| `data/flux.db` | 本地 SQLite 主数据库 |
| `data/attachments/` | 日记附件文件存储目录 |

## 4. 系统架构

```text
浏览器
  |
  | REST / JSON / multipart
  v
Python HTTP Server
  |
  | Repository / Domain Logic
  v
SQLite
  |
  +-- FTS5 全文检索
  |
  +-- 本地附件目录
```

系统目前是单体应用，前后端由同一个 Python 服务托管。前端通过 `/api/v1/*` 调用后端接口，后端读写 SQLite 和附件目录。

这种架构适合当前阶段，因为：

- 用户数据主要在本地
- 并发压力低
- 运维要求低
- 数据迁移和备份路径清晰
- 代码路径便于快速审查

### 4.1 系统上下文

```text
User
  |
  v
Browser UI
  |
  | HTTP
  v
Flux Local Server
  |
  +-- SQLite database
  |
  +-- Attachment filesystem
  |
  +-- Static holiday dataset already imported into SQLite
```

外部依赖边界：

| 类型 | 当前状态 |
| :--- | :--- |
| 网络服务 | 无强依赖 |
| 第三方账号 | 无 |
| 云端数据库 | 无 |
| 对象存储 | 无 |
| 本地文件系统 | 必需，用于 SQLite 和附件 |

### 4.2 请求生命周期

以日记保存为例：

```text
用户提交表单
  |
  v
app.js 读取表单状态并组装 payload
  |
  v
fetch('/api/v1/diaries')
  |
  v
FluxHandler._handle_api 分发路由
  |
  v
FluxRepository.create_diary / update_diary
  |
  v
SQLite transaction
  |
  +-- 写入 diaries
  +-- 写入 diary_tag_links
  +-- 刷新 diaries_fts
  +-- 写入 sync_outbox
  |
  v
返回 JSON
  |
  v
前端重新加载标签、日期筛选和日记列表
```

### 4.3 一致性边界

Flux 当前主要有三类一致性边界：

| 边界 | 一致性策略 |
| :--- | :--- |
| SQLite 内部数据 | 通过单连接事务保证同一业务操作内一致 |
| FTS 索引 | 日记创建、更新、删除、恢复时同步刷新 |
| 附件与日记 Markdown | 通过引用扫描建立弱一致关系 |

附件是当前最明显的弱一致点：文件存在不代表一定被引用，日记引用也可能指向已被手动删除的文件。因此附件管理面板必须展示“已引用/未引用”和引用来源，帮助用户做人工确认。

### 4.4 模块依赖关系

```text
Calendar
  ├─ reads Diary summaries
  ├─ reads Todo due items
  ├─ reads Calendar Events
  └─ reads Holiday state

Diary
  ├─ writes Diaries
  ├─ writes Diary Tags
  ├─ updates FTS index
  └─ references Attachments by Markdown

Todo
  ├─ writes Todos
  ├─ writes Subtasks
  ├─ writes Todo History
  └─ reads Todo Labels

Attachment Manager
  ├─ scans data/attachments
  └─ scans active diary Markdown references
```

依赖方向上，日历是读取型聚合模块；日记、待办、事件是写入型业务模块；附件管理是文件系统与日记正文之间的治理模块。

### 4.5 分层协作规则

为了让后续开发不把职责重新揉在一起，当前项目建议遵循以下协作规则：

| 规则 | 说明 | 典型例子 |
| :--- | :--- | :--- |
| UI 只表达意图 | 前端按钮、筛选和表单只描述用户想做什么，不决定最终数据是否合法 | 日记保存按钮提交 `entry_date` 和正文，但“一天一篇”的判断由后端和数据库完成 |
| API 是边界 | 所有业务数据变更都通过 `/api/v1/*`，不让前端直接读写数据库或附件目录 | 附件删除通过 API 校验路径，而不是前端拼文件路径删除 |
| 领域服务持有规则 | 跨表、跨模块、需要一致性的规则放在 `FluxRepository` | 恢复同日日记时合并正文、标签和缺失元数据 |
| 数据库兜底约束 | 对重要不变量使用索引或字段状态兜底 | `idx_diaries_one_active_per_day` 保证同一天最多一篇未删除日记 |
| 聚合只读优先 | 日历聚合读取日记、待办、事件和节假日，但不替这些模块修改业务事实 | 日历图层展示/隐藏不会改变日记和待办数据 |
| 附件弱一致显式化 | 附件引用以 Markdown 为准，系统必须展示引用来源和未引用状态 | 附件管理面板显示某文件被哪篇日记哪一行引用 |
| 删除统一软删除 | 用户删除业务对象时保留原始状态，恢复时按模块规则处理 | 日记、待办、日历事件都使用 `deleted_at` 进入回收站 |

### 4.6 模块分工边界

从业务分工看，Flux 可以划分为五个核心域，每个核心域都有独立主数据，同时允许被日历和设置页读取。

| 业务域 | 主数据 | 被谁读取 | 对外提供的能力 |
| :--- | :--- | :--- | :--- |
| 日记域 | `diaries`, `diary_tags`, `diary_tag_links`, `diaries_fts` | 日记页、日历页、附件管理、导出 | 写作、筛选、全文检索、恢复合并、附件引用、导出 |
| 待办域 | `todos`, `todo_subtasks`, `todo_history`, `todo_projects` | 待办页、日历页、导出 | 任务管理、状态切换、子任务、历史、拖拽排序、批量操作 |
| 日历域 | `calendar_events`, `calendar_holidays`, `calendar_static_holidays` | 日历页 | 事件管理、视图聚合、节假日判断、历史同日/同月查询 |
| 附件域 | `data/attachments/` + Markdown 引用 | 日记页、设置页附件管理 | 上传、静态访问、引用反查、单个删除、未引用清理 |
| 导出域 | 即时查询结果 | 日记页、待办页 | JSON、CSV、Markdown 生成，不单独持久化 |

其中日历域的定位最特殊：它不是所有模块的上级，而是跨模块读取视图。日历可以展示日记、待办和事件，但日记编辑仍回到日记域，待办完成仍回到待办域，事件修改才属于日历域。

### 4.7 典型功能的层级落点

| 用户动作 | 前端落点 | 后端落点 | 数据落点 | 说明 |
| :--- | :--- | :--- | :--- | :--- |
| 写一篇日记 | 日记编辑器、日期/心情/天气/位置输入 | `create_diary` / `update_diary` | `diaries`, `diary_tag_links`, `diaries_fts` | 后端负责同日唯一、标签更新和 FTS 刷新。 |
| 搜索日记关键词 | 搜索框、筛选按钮、结果高亮 | `list_diaries(filters)` | `diaries_fts`, `diaries` | 数据库返回候选结果，前端负责高亮与定位。 |
| 恢复删除日记 | 回收站恢复按钮 | `restore_diary` | `diaries`, `diary_tag_links`, `diaries_fts` | 同日已有日记时执行增量合并。 |
| 完成待办 | 任务完成按钮 | `set_todo_status` | `todos`, `todo_history` | 状态写入数据库，列表排序让完成项置底。 |
| 切换日历图层 | 日历页图层按钮 | 无数据变更，必要时重新请求聚合接口 | 不改变主数据 | 图层是展示状态，不是业务事实。 |
| 标记节假日 | 日历节假日模式 | `toggle_holiday` | `calendar_holidays` | 用户覆盖优先于静态节假日和周末默认值。 |
| 上传附件 | 日记编辑器附件按钮 | `_upload_attachment` | `data/attachments/diaries/YYYY/MM` | 文件 URL 写入 Markdown，引用关系由扫描得出。 |
| 导出数据 | 多选框和格式按钮 | `export_diaries` / `export_todos` | 即时查询结果 | 导出结果不额外持久化。 |

## 5. API 通信机制

所有业务接口统一挂载在：

```text
/api/v1
```

典型接口：

| 接口 | 方法 | 功能 |
| :--- | :--- | :--- |
| `/api/v1/health` | GET | 服务健康检查 |
| `/api/v1/diaries` | GET / POST | 查询和创建日记 |
| `/api/v1/diaries/{id}` | GET / PATCH / DELETE | 获取、更新、删除日记 |
| `/api/v1/diaries/{id}/restore` | POST | 恢复日记，必要时合并到当天原日记 |
| `/api/v1/events` | GET / POST | 查询和创建日历事件 |
| `/api/v1/events/{id}/restore` | POST | 恢复日历事件 |
| `/api/v1/todos` | GET / POST | 查询和创建待办 |
| `/api/v1/calendar/day/{date}` | GET | 获取某一天的聚合数据 |
| `/api/v1/calendar/month/{year}/{month}` | GET | 获取月视图摘要数据 |
| `/api/v1/attachments` | GET / POST | 查询附件、上传附件 |
| `/api/v1/attachments/cleanup` | POST | 清理未引用附件 |
| `/api/v1/attachments/delete` | DELETE | 删除单个附件 |

普通业务请求使用 JSON。附件上传使用 `multipart/form-data`。

## 6. 数据库设计

当前数据库为 SQLite，核心表如下：

| 表 | 功能 |
| :--- | :--- |
| `diaries` | 日记主体、日期、时间、心情、天气、位置、正文、收藏、删除状态、恢复合并状态 |
| `diary_tags` | 日记标签 |
| `diary_tag_links` | 日记与标签多对多关系 |
| `diaries_fts` | 日记全文检索虚拟表 |
| `todo_projects` | 待办标签，历史命名仍保留 project |
| `todos` | 待办任务、优先级、截止时间、提醒、状态、删除状态 |
| `todo_subtasks` | 子任务 |
| `todo_history` | 待办详情历史 |
| `calendar_events` | 日历事件 |
| `calendar_holidays` | 用户手动标记节假日 |
| `calendar_static_holidays` | 静态节假日数据库 |
| `sync_outbox` | 操作记录和后续同步扩展预留 |

### 6.1 日记唯一性约束

Flux 的日记模型是“一天一篇主日记”。数据库通过部分唯一索引保证同一天只能存在一篇未删除日记：

```sql
CREATE UNIQUE INDEX idx_diaries_one_active_per_day
ON diaries(entry_date)
WHERE deleted_at IS NULL;
```

这个设计直接服务于产品模型：用户回看某一天时，看到的是该日的主记录，而不是多个相互竞争的条目。

### 6.2 软删除与恢复

系统使用 `deleted_at` 实现软删除，已覆盖：

- 待办
- 日记
- 日历事件

日记额外包含恢复合并语义：

```text
restored_at
restored_into_id
```

当恢复的删除日记与当天已有日记冲突时，系统会把删除日记正文合并进当天原日记，并将被合并记录标记为已恢复来源，避免重复恢复和错误统计。

### 6.3 索引与查询意图

当前索引设计围绕页面查询路径，而不是围绕抽象数据库范式。每个索引都服务于一个明确功能入口。

| 索引 | 服务功能 | 查询意图 |
| :--- | :--- | :--- |
| `idx_diaries_date(entry_date, deleted_at)` | 日记列表、日历聚合、日记回收站 | 快速按日期范围读取日记，并区分正常/删除状态 |
| `idx_diaries_mood(mood, deleted_at)` | 心情筛选 | 支持日记按心情分类查看 |
| `idx_diary_tag_links_tag(tag_id, deleted_at)` | 日记标签筛选 | 支持通过标签找到相关日记 |
| `idx_diaries_one_active_per_day(entry_date) WHERE deleted_at IS NULL` | 一天一篇主日记 | 数据库层保证同一天最多一篇未删除日记 |
| `idx_todos_due(due_at, status, deleted_at)` | 近期、逾期、定时任务 | 支持待办按时间分组和状态过滤 |
| `idx_todos_status(status, deleted_at)` | 待办状态筛选 | 支持待完成、进行中、已完成等状态读取 |
| `idx_todos_project(project_id, deleted_at)` | 待办标签筛选 | 支持按标签查看任务 |
| `idx_todo_history_todo(todo_id, created_at)` | 任务详情历史 | 支持按任务读取历史记录 |
| `idx_events_start(start_at, deleted_at)` | 日历事件查询 | 支持事件按开始日期读取 |
| `idx_events_range(start_at, end_at, deleted_at)` | 时间范围查询 | 为跨日事件或范围查询预留 |

### 6.4 数据一致性规则

| 规则 | 实现方式 | 影响范围 |
| :--- | :--- | :--- |
| 一天只有一篇未删除日记 | 唯一索引 + `create_diary` / `update_diary` 中检查 | 日记创建、编辑、恢复、导入 |
| 删除日记不进入搜索 | `delete_diary` 调用 `_delete_diary_fts` | 搜索结果、全文定位 |
| 恢复日记刷新搜索 | `restore_diary` 调用 `_refresh_diary_fts` | 搜索结果、日历日记显示 |
| 合并来源不再回收站展示 | `list_diaries(deleted=True)` 排除 `restored_at IS NOT NULL` | 日记回收站、日历删除标记 |
| 已完成待办置底 | SQL `ORDER BY CASE status WHEN 'completed' THEN 1` | 待办列表、标签列表、智能排序 |
| 节假日用户覆盖优先 | `is_holiday` 先查 `calendar_holidays` | 日历所有视图 |
| 附件路径不逃逸根目录 | `_attachment_url_path` 使用 `resolve()` 校验 | 附件访问、删除 |
| 附件引用弱一致 | 扫描活跃日记 Markdown | 附件管理、清理未使用资源 |

这些规则共同构成当前系统的数据保护层。它们比 UI 校验更重要，因为它们直接决定本地数据是否长期可信。

## 7. 核心功能实现

### 7.1 日记模块

日记支持：

- 每天一篇主日记
- Markdown 正文
- 图片、音频、普通文件附件
- 时间、心情、天气、位置
- 标签和收藏
- 关键词搜索
- 全文定位
- 导出
- 回收站
- 同日删除日记恢复合并

恢复合并流程：

```text
用户点击恢复删除日记
  |
  v
检查该日期是否已有未删除日记
  |
  +-- 没有：直接恢复
  |
  +-- 有：追加正文、合并标签、补齐缺失元数据
        |
        v
      被恢复日记标记 restored_at / restored_into_id
```

### 7.2 全文检索

日记全文检索基于 SQLite FTS5。索引字段包括：

- 日期
- 时间
- 正文
- 心情
- 天气
- 位置
- 标签

日记创建、更新、删除和恢复时，后端会维护 FTS 索引一致性。前端搜索结果支持关键词高亮和编辑器定位。

### 7.3 日历模块

日历是跨模块聚合视图，不只是事件列表。

当前支持：

- 月视图
- 日视图时间轴
- 周视图
- 季度视图
- 那年今日
- 那年这月
- 日历事件图层
- 节假日图层
- 待办图层
- 日记图层
- 回收站图层

日历聚合接口会把日记、待办、事件、节假日和删除项统一返回给前端，由前端按用户选择的图层决定是否渲染。

### 7.4 待办模块

待办支持：

- 正常 / 高优先级
- 完成与重新打开
- 子任务
- 截止时间
- 提醒字段
- 标签
- 拖拽排序
- 回收站
- 多选导出
- 批量设置优先级
- 任务详情历史

完成后的任务保留在原列表中，以完成状态置底展示，而不是进入单独的完成列表。

### 7.5 附件模块

附件支持：

- 图片
- 音频
- 普通文件

普通文件支持类型包括：

```text
pdf, txt, md, csv, json,
doc, docx, xls, xlsx, ppt, pptx,
zip, rar, 7z
```

大小限制：

```text
附件必须小于 100MB
```

附件以 Markdown 形式引用：

```markdown
![图片](/attachments/diaries/2026/04/example.png)
[audio:录音](/attachments/diaries/2026/04/example.mp3)
[file:文档](/attachments/diaries/2026/04/example.pdf)
```

附件管理通过扫描日记 Markdown 建立引用关系，展示每个附件被哪篇日记、哪一行引用。

### 7.6 功能与数据库实现方法

本项目的业务实现以 SQLite 为事实源，后端服务层负责把前端操作翻译为明确的数据读写动作。前端不直接拼接数据库查询，而是通过 REST API 传递筛选条件、编辑内容和批量操作意图；后端再根据业务规则完成校验、事务写入、索引维护和聚合返回。

#### 7.6.1 功能到存储的实现映射

| 功能 | 后端入口 | 主要表 / 存储 | 实现方法 |
| :--- | :--- | :--- | :--- |
| 日记列表与筛选 | `list_diaries(filters)` | `diaries`, `diary_tags`, `diary_tag_links`, `diaries_fts` | 默认排除软删除数据；按日期、心情、收藏、标签和关键词动态拼接查询；结果按 `entry_date DESC, updated_at DESC` 排序。 |
| 新建日记 | `create_diary(payload)` | `diaries`, `diary_tag_links`, `diaries_fts` | 写入前检查同日是否已有未删除日记；如果已存在则转为更新，保证“一天一篇主日记”；写入后刷新标签和 FTS。 |
| 编辑日记 | `update_diary(item_id, payload)` | `diaries`, `diary_tag_links`, `diaries_fts` | 合并前端提交字段，重新计算字数；若修改日期会检查目标日期唯一性；提交后重建标签关联和全文索引。 |
| 删除 / 恢复日记 | `delete_diary`, `restore_diary` | `diaries`, `diaries_fts` | 删除采用 `deleted_at` 软删除并移除 FTS；恢复时若同日已有日记，则把删除日记的正文、标签和缺失元数据增量合并到现有日记。 |
| 日记全文检索 | `list_diaries(filters.keyword)` | `diaries_fts` + `LIKE` 回退 | FTS5 承担正文、日期、时间、心情、天气、位置、标签检索；同时保留 `LIKE` 条件覆盖非分词场景。 |
| 待办列表与智能排序 | `list_todos(filters)` | `todos`, `todo_projects`, `todo_subtasks` | 按状态、优先级、标签、时间范围和关键词筛选；排序规则为未完成优先、高优先级优先、有截止时间优先、截止时间更早优先。 |
| 待办状态切换 | `set_todo_status` | `todos`, `todo_history` | 完成按钮在未完成和已完成之间切换；完成项仍保留在原列表中，通过排序置底；状态变化写入历史。 |
| 子任务 | todo 相关接口 | `todo_subtasks` | 子任务独立记录完成状态；完成后保持原位置，不触发父任务置底规则。 |
| 待办回收站 | `delete_todo`, `restore_todo` | `todos`, `todo_history` | 删除保留任务原状态、优先级、标签、子任务等数据；恢复时清空删除标记并记录恢复历史。 |
| 日历事件 | event 相关接口 | `calendar_events` | 事件按起止时间查询；删除同样采用软删除，支持日历回收站展示和恢复。 |
| 日历聚合 | `month_summary`, `day_aggregate` | `diaries`, `todos`, `calendar_events`, `calendar_holidays` | 后端按日期聚合多类数据，返回给前端图层系统；前端根据“事件、节假日、待办、日记”等开关决定渲染或隐藏。 |
| 那年今日 / 那年这月 | `calendar_history(day, mode)` | `diaries`, `todos`, `calendar_events` | 使用 `substr(entry_date, 6, 5)` 或 `substr(entry_date, 6, 2)` 匹配历史同日 / 同月数据。 |
| 节假日标记 | `toggle_holiday`, `is_holiday` | `calendar_holidays`, 静态节假日库 | 判断优先级为用户覆盖、静态节假日、周末默认值；当用户选择值等于默认值时删除覆盖记录，保持数据干净。 |
| 附件上传 | `_upload_attachment` | `data/attachments/diaries/YYYY/MM` | multipart 上传后校验扩展名和大小，按年月归档，生成 UUID 文件名，返回可写入 Markdown 的本地 URL。 |
| 附件管理 | `_list_attachments`, `_delete_attachment` | 文件系统 + 日记 Markdown 引用扫描 | 扫描附件目录并按 MIME 区分图片、音频和普通文件；通过 Markdown 链接反查引用位置，支持单个附件删除和未引用附件清理。 |
| 导出 | `export_diaries`, `export_todos` | SQLite 查询结果 | 后端按当前筛选或选择结果生成 JSON、CSV、Markdown；附件在 Markdown 中保留本地 `/attachments/...` 引用。 |

#### 7.6.2 日记模块的数据库实现

日记表是日记功能的主表，记录日期、时间、正文、心情、天气、位置、收藏状态、字数、删除状态和恢复状态。标签不直接存为逗号字符串，而是拆成 `diary_tags` 和 `diary_tag_links`，这样可以支持稳定的标签筛选、标签复用和软删除关联。

创建日记时，服务层先按 `entry_date` 查询未删除日记。如果该日期已有日记，则不再插入第二条主日记，而是把请求转为更新，避免前端重复提交导致同日多篇主日记。数据库层还有 `idx_diaries_one_active_per_day(entry_date) WHERE deleted_at IS NULL` 作为最终约束，保证业务规则不会只依赖前端。

编辑日记时，后端以白名单字段合并更新内容，并重新计算 `word_count`。如果用户修改日记日期，后端会检查目标日期是否已有其他未删除日记；有冲突时拒绝更新，避免破坏“一天一篇主日记”的约束。

删除日记时，系统写入 `deleted_at`，并从 `diaries_fts` 删除对应索引行。这样回收站仍能看到原始日记数据，但普通列表和全文检索不会再命中已删除日记。

恢复日记时分两种路径：

```text
删除日记
  |
  v
检查同日期是否已有未删除日记
  |
  |-- 无：清空 deleted_at，恢复原记录，刷新 FTS
  |
  |-- 有：把删除日记的正文追加到现有日记
          合并标签、收藏和缺失元数据
          标记源记录 restored_at / restored_into_id
          刷新目标日记 FTS
```

这种设计解决了“同一天曾经删除多篇日记，恢复时不应再制造多篇主日记”的问题。被合并的源记录保留恢复去向，避免重复恢复时再次追加内容。

#### 7.6.3 全文检索与关键词定位

日记全文检索使用 SQLite FTS5 虚拟表 `diaries_fts`。FTS 索引不只包含正文，还包含日期、时间、心情、天气、位置和标签，目的是让用户输入一个关键词时可以跨元数据和正文统一搜索。

索引维护采用“先删除旧索引，再插入新索引”的方式。日记创建、更新、恢复后调用刷新逻辑；删除后移除索引。这个策略比增量修改字段更简单，适合当前本地应用的数据规模，也能减少索引与主表内容不一致的概率。

搜索接口同时组合 FTS 和 `LIKE` 条件。FTS 负责正文和标签等主要检索，`LIKE` 作为补充用于覆盖日期、短文本和中文分词边界不理想的情况。前端拿到结果后再做关键词高亮和编辑器定位，数据库只负责返回候选结果。

#### 7.6.4 待办模块的数据库实现

待办主表记录标题、详情、状态、优先级、截止时间、提醒时间、标签、排序值和删除状态。优先级被收敛为 `normal` 和 `high` 两档，“标记为重要”和“高优先级”在数据层合并为同一类语义，避免同一个任务出现两个相近但不一致的优先级字段。

列表查询的核心是筛选条件和排序条件分离。筛选条件负责决定“哪些任务进入结果集”，例如标签、状态、优先级、近期、逾期、定时任务和关键词；排序条件负责决定“这些任务如何展示”，当前规则为：

```text
未完成任务
  -> 高优先级
  -> 有截止时间
  -> 截止时间更早
  -> 手动 sort_order
  -> 创建时间更早
已完成任务置底
```

完成按钮不会把任务移动到独立的完成列表，而是修改 `status`。前端仍在原标签或原筛选结果中展示该任务，后端通过排序表达式把已完成项置底。子任务的完成状态独立存储，完成后保持原位置，不影响父任务排序。

任务详情历史由 `todo_history` 记录。创建、编辑、完成、恢复、批量设置优先级等动作都会写入动作摘要和 payload，便于在详情页展示任务状态变化轨迹。

#### 7.6.5 日历模块的数据库实现

日历页面不是单一表的展示，而是一个聚合查询界面。它同时读取日记、待办、事件、节假日和回收站数据，再由前端图层开关决定是否渲染。

`month_summary(year, month)` 面向月视图和季度视图，按日期聚合：

- 当天是否有日记
- 当天是否有待办截止
- 当天是否有日历事件
- 当天是否是假日
- 当天是否有删除项

`day_aggregate(day)` 面向日期详情面板和日视图，返回某一天的完整数据集合，包括日记、待办、事件、已删除日记和已删除事件。前端在这个结果上做卡片分组和颜色区分。

`calendar_history(day, mode)` 支撑“那年今日”和“那年这月”。该接口不是新增存储，而是复用现有日期字段做历史匹配：同日模式匹配 `MM-DD`，同月模式匹配 `MM`。这样不会引入额外冗余表，也能覆盖日记、待办和事件三类历史记录。

节假日判断采用三层优先级：

```text
用户手动标记 / 取消
  > 静态节假日数据库
  > 周六周日默认假期
```

手动标记不会修改静态节假日库，而是写入 `calendar_holidays` 作为用户覆盖层。当用户设置的结果与默认判断一致时，后端会删除这条覆盖记录，让数据库只保存真正有差异的用户决策。

#### 7.6.6 附件模块的数据库与文件实现

附件没有单独维护强关系表，而是以“文件系统存储 + Markdown 引用扫描”的方式实现。这样做的原因是附件天然依附于日记正文，Markdown 本身就是引用关系的来源；在本地单用户场景下，扫描引用比维护一套复杂的附件关系表更直接。

上传入口读取 multipart 文件，执行三层校验：

- 请求必须是 `multipart/form-data`
- 文件大小必须小于 100MB
- 扩展名必须在图片、音频或普通文件白名单中

通过校验后，附件被保存到 `data/attachments/diaries/YYYY/MM`，文件名使用 UUID，避免用户上传同名文件时互相覆盖。后端返回 `/attachments/...` URL，前端把它插入日记 Markdown：

```markdown
![图片](/attachments/diaries/2026/04/a.png)
[audio:录音](/attachments/diaries/2026/04/a.mp3)
[file:文档](/attachments/diaries/2026/04/a.pdf)
```

附件管理面板扫描附件目录，并解析所有未删除日记正文里的 Markdown 链接，建立“附件 -> 引用日记 -> 行号 -> 摘要”的弱一致性映射。弱一致性意味着：引用关系以正文内容为准，不额外强制写入数据库；只要用户删除 Markdown 链接，附件管理就会在下一次扫描时把该文件识别为未引用。

#### 7.6.7 导出功能的实现

导出功能复用列表查询能力，而不是重新实现一套筛选逻辑。日记导出会先根据前端传入的选择结果或筛选条件读取 `diaries`，再展开标签并生成目标格式。

当前导出格式包括：

- JSON：保留结构化字段，适合备份和二次处理。
- CSV：保留日期、心情、天气、位置、收藏、字数、标签和正文等扁平字段，适合表格分析。
- Markdown：按日期生成文章式内容，保留附件 Markdown 链接，适合阅读和迁移。

待办导出同样支持 JSON、CSV 和 Markdown。Markdown 会使用复选框语法表达任务完成状态，并保留子任务层级，便于在其他 Markdown 工具中继续使用。

## 8. 安全与校验

当前已实现的安全措施：

- 附件扩展名白名单
- 附件大小限制小于 100MB
- 附件服务路径限制在 `data/attachments/`
- 删除采用软删除，降低误操作损失
- 日记同日唯一性由数据库约束保证
- 日记恢复合并避免重复恢复导致内容重复追加
- 用户展示时间按本地时间格式化，内部仍可保持 UTC 记录

当前未实现：

- 用户认证
- 多用户权限控制
- 外部对象存储
- 附件病毒扫描
- 文档沙箱预览

这些能力对于本地单用户 MVP 不是当前必要项，但如果未来进入多用户或分发场景，需要重新评估。

## 9. 缓存与并发

当前没有引入 Redis 或其他缓存服务。

原因：

- 当前定位是本地单用户应用
- SQLite 已提供可靠事务能力
- 数据规模和访问模式暂不需要外部缓存
- 外部缓存会增加部署和一致性复杂度

并发方面，当前系统适合本地或低并发访问，不适合直接作为高 QPS 多租户服务部署。

## 10. 部署与运行

本地运行：

```powershell
python apps/server/flux_server.py --port 8822
```

浏览器访问：

```text
http://127.0.0.1:8822
```

当前项目尚未内置：

- Dockerfile
- docker-compose
- CI/CD
- 生产级反向代理配置
- 自动化发布脚本

## 11. 建议 CI/CD 流程

建议最小 CI 流程：

```text
1. Python 语法检查
2. JavaScript 语法检查
3. 后端 API 烟测
4. 日记恢复合并测试
5. 附件上传限制测试
6. SQLite 初始化测试
7. 打包发布产物
```

当前可执行的基础检查：

```powershell
python -m py_compile apps/server/flux_server.py
node --check apps/client/web/app.js
```

## 12. 扩展建议

### 12.1 前端模块化

`app.js` 已经承担较多业务职责，后续建议拆分为：

```text
api/
calendar/
diary/
todo/
attachments/
settings/
shared/
```

### 12.2 附件索引表

当前附件引用通过扫描 Markdown 得到。后续可增加：

```text
attachments
attachment_references
```

优势：

- 查询更快
- 删除前引用检测更明确
- 支持排序、筛选和批量管理
- 附件状态可审计

### 12.3 备份与恢复

建议增加一键备份：

- `data/flux.db`
- `data/attachments/`
- 节假日静态数据
- 导出的日记和待办

### 12.4 同步能力

`sync_outbox` 可作为未来同步能力的基础，用于：

- 多设备同步
- 冲突检测
- 操作回放
- 变更审计

该能力应作为可选扩展，避免破坏本地优先的核心体验。

## 13. 关键数据字段说明

### 13.1 `diaries`

| 字段 | 说明 |
| :--- | :--- |
| `id` | 日记唯一标识 |
| `entry_date` | 日记归属日期，是“一天一篇主日记”的核心字段 |
| `entry_time` | 用户手动记录的时间，可为空 |
| `title` | 兼容字段，当前产品不要求用户填写标题 |
| `content_md` | Markdown 正文，图片、音频和普通附件也通过正文引用 |
| `mood` | 心情枚举值 |
| `weather` | 手动记录天气 |
| `location_name` | 手动记录位置 |
| `is_favorite` | 是否收藏 |
| `word_count` | 正文字数统计 |
| `deleted_at` | 软删除时间 |
| `restored_at` | 删除日记被恢复或合并的时间 |
| `restored_into_id` | 删除日记合并进入的目标日记 ID |
| `created_at` / `updated_at` | 创建和更新时间 |
| `version` | 版本号，供后续同步或冲突处理扩展 |

关键约束：

- `entry_date + deleted_at IS NULL` 保证每天最多一篇未删除日记。
- 已合并恢复的删除日记不再出现在回收站，也不再进入日历删除标记统计。

### 13.2 `todos`

| 字段 | 说明 |
| :--- | :--- |
| `title` | 任务标题 |
| `description` | 任务描述 |
| `status` | 当前状态，包括待完成、进行中、已完成等 |
| `priority` | 当前只保留正常和高优先级 |
| `is_important` | 历史兼容字段，语义已并入高优先级 |
| `due_at` | 截止或计划时间 |
| `reminder_minutes` | 提醒提前量，目前只记录字段，不实现真实系统通知 |
| `project_id` | UI 中表现为标签 |
| `sort_order` | 拖拽排序依据 |
| `deleted_at` | 回收站状态 |

### 13.3 `calendar_events`

| 字段 | 说明 |
| :--- | :--- |
| `title` | 事件标题 |
| `description` | 事件描述 |
| `start_at` / `end_at` | 事件开始与结束时间 |
| `all_day` | 是否全天 |
| `color` | 事件颜色 |
| `location_name` | 事件地点 |
| `reminder_minutes` | 提醒提前量 |
| `recurrence_rule` | 预留重复规则字段，目前重复任务/重复事件不是当前主路径 |
| `deleted_at` | 事件回收站状态 |

### 13.4 `calendar_holidays` 与 `calendar_static_holidays`

`calendar_static_holidays` 表示从静态节假日数据导入的默认节假日信息。  
`calendar_holidays` 表示用户在日历页面手动标记或取消的覆盖信息。

节假日判断优先级：

```text
用户手动覆盖
  >
静态节假日数据库
  >
默认周六周日
```

## 14. 业务数据流示例

### 14.1 写入日记

```text
用户在日记编辑器输入正文、时间、心情、天气、位置、标签和附件
  |
  v
前端组装 JSON payload
  |
  v
POST /api/v1/diaries
  |
  v
后端检查 entry_date 是否已有未删除日记
  |
  +-- 已存在：更新该日记
  |
  +-- 不存在：创建新日记
  |
  v
写入 diaries / diary_tag_links
  |
  v
刷新 diaries_fts
  |
  v
返回最新日记对象
```

### 14.2 恢复同日日记

```text
用户从回收站点击恢复
  |
  v
POST /api/v1/diaries/{id}/restore
  |
  v
查询该删除日记的 entry_date
  |
  v
检查当天是否已有未删除日记
  |
  +-- 没有：deleted_at 置空，正常恢复
  |
  +-- 有：正文追加到原日记，标签去重合并，空缺元数据补齐
        |
        v
      源删除日记写入 restored_at / restored_into_id
```

### 14.3 日历月视图渲染

```text
用户切换月份
  |
  v
GET /api/v1/calendar/month?year=YYYY&month=MM
  |
  v
后端统计每天的日记、待办、事件、删除项、节假日状态
  |
  v
前端根据图层状态渲染 marker
```

### 14.4 附件上传与引用

```text
用户在日记编辑器选择图片、音频或附件
  |
  v
前端检查文件大小 < 100MB
  |
  v
POST /api/v1/attachments
  |
  v
后端检查 Content-Length、扩展名、实际内容长度
  |
  v
写入 data/attachments/diaries/YYYY/MM/
  |
  v
返回 /attachments/... URL
  |
  v
前端插入 Markdown 引用
```

## 15. API 参数细节

### 15.1 日记查询

`GET /api/v1/diaries` 支持参数：

| 参数 | 说明 |
| :--- | :--- |
| `date_from` / `date_to` | 日期范围 |
| `mood` | 心情筛选 |
| `tag_id` | 标签筛选 |
| `keyword` | FTS 或 LIKE 关键词检索 |
| `is_favorite` | 收藏筛选 |
| `deleted` | 查询回收站日记 |

### 15.2 待办查询

`GET /api/v1/todos` 支持参数：

| 参数 | 说明 |
| :--- | :--- |
| `status` | 状态筛选 |
| `priority` | 优先级筛选 |
| `project_id` | 标签筛选 |
| `due` | 近期、逾期、定时任务等时间分组 |
| `deleted` | 查询回收站 |
| `sort` | 排序策略 |

### 15.3 日历查询

| 接口 | 说明 |
| :--- | :--- |
| `/calendar/day/{date}` | 返回某一天的完整聚合数据 |
| `/calendar/month` | 返回月视图每天摘要 |
| `/calendar/history?mode=day` | 那年今日 |
| `/calendar/history?mode=month` | 那年这月 |
| `/calendar/holidays/{date}/toggle` | 手动标记或取消节假日 |

## 16. 错误处理约定

后端错误统一返回 JSON，核心字段包括：

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "错误说明",
    "details": {}
  }
}
```

常见错误：

| Code | 场景 |
| :--- | :--- |
| `VALIDATION_ERROR` | 参数缺失、格式错误、附件类型不支持 |
| `NOT_FOUND` | 目标日记、任务、事件或附件不存在 |
| `PAYLOAD_TOO_LARGE` | 附件超过大小限制 |
| `DIARY_DATE_EXISTS` | 更新日记日期时违反一天一篇约束 |

前端目前多数场景通过 `showToast` 或 `alert` 反馈错误。后续建议统一错误提示组件，避免同类错误在不同模块中表现不一致。

## 17. 时间处理策略

当前策略：

- 后端内部时间使用 UTC ISO 字符串。
- 面向用户展示的恢复合并时间转换成本地时间。
- 日记的 `entry_date` 是用户语义日期，不应被时区转换影响。
- 日历事件的 `start_at` / `end_at` 是具体时间点，需要在 UI 中以本地时间展示。

注意事项：

- 不要把 `entry_date` 当作 UTC 时间戳处理。
- 健康检查接口中的服务时间可以保留 UTC，因为它是技术接口。
- 任何写入日记正文的人类可读时间都应使用本地展示格式。

## 18. 测试矩阵建议

| 模块 | 必测场景 |
| :--- | :--- |
| 日记 | 新建、更新、删除、恢复、同日恢复合并、标签合并、FTS 搜索 |
| 日历 | 月视图统计、日视图聚合、周视图溢出、季度视图、那年今日、节假日覆盖 |
| 待办 | 完成/重新打开、子任务完成、拖拽排序、回收站恢复、批量优先级 |
| 附件 | 图片上传、音频上传、普通附件上传、100MB 限制、引用扫描、单个删除、清理未引用 |
| 导出 | Todo 导出、日记多选导出、Markdown/JSON/CSV 格式 |
| 时间 | UTC 存储、本地展示、恢复合并标记展示 |

推荐优先补充的自动化烟测：

```text
1. 初始化空 SQLite 数据库
2. 创建同一天日记
3. 插入删除日记并恢复
4. 验证正文合并
5. 验证回收站不再显示合并来源
6. 验证月历删除标记为 0
```

## 19. 完整路由清单

本节按当前后端路由实现整理，便于后续联调、重构和测试覆盖。

### 19.1 基础与附件

| 方法 | 路由 | 说明 |
| :--- | :--- | :--- |
| GET | `/api/v1/health` | 服务健康检查 |
| GET | `/api/v1/attachments` | 列出附件、统计大小、引用来源 |
| POST | `/api/v1/attachments` | 上传附件，使用 `multipart/form-data` |
| POST | `/api/v1/attachments/cleanup` | 删除未被日记引用的附件 |
| DELETE | `/api/v1/attachments/delete` | 删除指定附件，返回引用数量和释放空间 |
| GET | `/attachments/...` | 静态服务附件文件 |

### 19.2 日记

| 方法 | 路由 | 说明 |
| :--- | :--- | :--- |
| GET | `/api/v1/diaries` | 查询日记，支持日期、心情、标签、关键词、收藏、回收站 |
| POST | `/api/v1/diaries` | 创建日记；如果当天已有未删除日记则更新该日记 |
| GET | `/api/v1/diaries/export` | 导出日记，支持 `json`、`csv`、`markdown` |
| GET | `/api/v1/diaries/by-date/{date}` | 查询某一天的日记 |
| GET | `/api/v1/diaries/{id}` | 获取日记详情 |
| PATCH | `/api/v1/diaries/{id}` | 更新日记 |
| DELETE | `/api/v1/diaries/{id}` | 软删除日记 |
| POST | `/api/v1/diaries/{id}/restore` | 恢复日记；必要时合并到当天原日记 |
| GET | `/api/v1/diary-tags` | 获取日记标签 |
| POST | `/api/v1/diary-tags` | 创建日记标签 |

### 19.3 待办

| 方法 | 路由 | 说明 |
| :--- | :--- | :--- |
| GET | `/api/v1/todos` | 查询待办，支持筛选、排序、回收站 |
| POST | `/api/v1/todos` | 创建待办 |
| GET | `/api/v1/todos/summary` | 获取侧栏摘要计数 |
| GET | `/api/v1/todos/stats` | 获取待办统计 |
| GET | `/api/v1/todos/export` | 导出待办，支持 `json`、`csv`、`markdown` |
| PATCH | `/api/v1/todos/bulk` | 批量更新待办 |
| POST | `/api/v1/todos/reorder` | 保存拖拽排序 |
| GET | `/api/v1/todos/{id}` | 获取待办详情 |
| PATCH | `/api/v1/todos/{id}` | 更新待办 |
| DELETE | `/api/v1/todos/{id}` | 软删除待办 |
| POST | `/api/v1/todos/{id}/complete` | 标记完成 |
| POST | `/api/v1/todos/{id}/reopen` | 重新打开 |
| GET | `/api/v1/todos/{id}/history` | 获取任务详情历史 |
| POST | `/api/v1/todos/{id}/restore` | 从回收站恢复 |
| POST | `/api/v1/todos/{id}/subtasks` | 创建子任务 |
| GET | `/api/v1/subtasks/{id}` | 获取子任务 |
| PATCH | `/api/v1/subtasks/{id}` | 更新子任务，包括完成状态 |
| DELETE | `/api/v1/subtasks/{id}` | 删除子任务 |

### 19.4 待办标签

| 方法 | 路由 | 说明 |
| :--- | :--- | :--- |
| GET | `/api/v1/todo-projects` | 获取待办标签 |
| POST | `/api/v1/todo-projects` | 创建待办标签 |
| GET | `/api/v1/todo-projects/{id}` | 获取标签详情 |
| PATCH | `/api/v1/todo-projects/{id}` | 更新标签 |
| DELETE | `/api/v1/todo-projects/{id}` | 删除标签，关联任务会解除标签 |

说明：后端表名和接口仍使用 `project` 历史命名，UI 已统一称为“标签”。

### 19.5 日历与事件

| 方法 | 路由 | 说明 |
| :--- | :--- | :--- |
| GET | `/api/v1/events` | 查询事件，支持日期范围和回收站 |
| POST | `/api/v1/events` | 创建事件 |
| GET | `/api/v1/events/{id}` | 获取事件 |
| PATCH | `/api/v1/events/{id}` | 更新事件 |
| DELETE | `/api/v1/events/{id}` | 软删除事件 |
| POST | `/api/v1/events/{id}/restore` | 从回收站恢复事件 |
| GET | `/api/v1/calendar/month` | 月视图摘要 |
| GET | `/api/v1/calendar/day/{date}` | 日视图聚合 |
| GET | `/api/v1/calendar/history` | 那年今日 / 那年这月 |
| POST | `/api/v1/calendar/holidays/{date}/toggle` | 手动标记或取消节假日 |

### 19.6 遗留接口

| 方法 | 路由 | 当前状态 |
| :--- | :--- | :--- |
| GET | `/api/v1/analytics/overview` | 后端仍存在，前端统计页面已移除。后续可删除或转为内部诊断接口 |

## 20. 表结构 DDL 摘要

以下不是完整迁移脚本，而是当前核心表结构摘要，便于评审快速理解数据形态。

### 20.1 日记相关

```sql
CREATE TABLE diaries (
  id TEXT PRIMARY KEY,
  entry_date TEXT NOT NULL,
  entry_time TEXT,
  title TEXT NOT NULL,
  content_md TEXT NOT NULL DEFAULT '',
  mood TEXT,
  weather TEXT,
  location_name TEXT,
  is_favorite INTEGER NOT NULL DEFAULT 0,
  word_count INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  deleted_at TEXT,
  version INTEGER NOT NULL DEFAULT 1,
  restored_at TEXT,
  restored_into_id TEXT
);

CREATE TABLE diary_tags (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL UNIQUE,
  color TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  deleted_at TEXT,
  version INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE diary_tag_links (
  diary_id TEXT NOT NULL,
  tag_id TEXT NOT NULL,
  created_at TEXT NOT NULL,
  deleted_at TEXT,
  PRIMARY KEY(diary_id, tag_id)
);
```

### 20.2 待办相关

```sql
CREATE TABLE todos (
  id TEXT PRIMARY KEY,
  project_id TEXT,
  title TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  status TEXT NOT NULL DEFAULT 'pending',
  priority TEXT NOT NULL DEFAULT 'normal',
  due_at TEXT,
  start_at TEXT,
  completed_at TEXT,
  sort_order INTEGER NOT NULL DEFAULT 0,
  is_important INTEGER NOT NULL DEFAULT 0,
  reminder_minutes INTEGER,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  deleted_at TEXT,
  version INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE todo_subtasks (
  id TEXT PRIMARY KEY,
  todo_id TEXT NOT NULL,
  title TEXT NOT NULL,
  is_completed INTEGER NOT NULL DEFAULT 0,
  sort_order INTEGER NOT NULL DEFAULT 0,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  deleted_at TEXT,
  version INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE todo_history (
  id TEXT PRIMARY KEY,
  todo_id TEXT NOT NULL,
  action TEXT NOT NULL,
  summary TEXT NOT NULL,
  payload_json TEXT NOT NULL DEFAULT '{}',
  created_at TEXT NOT NULL
);
```

### 20.3 日历相关

```sql
CREATE TABLE calendar_events (
  id TEXT PRIMARY KEY,
  title TEXT NOT NULL,
  description TEXT NOT NULL DEFAULT '',
  start_at TEXT NOT NULL,
  end_at TEXT NOT NULL,
  all_day INTEGER NOT NULL DEFAULT 0,
  color TEXT,
  location_name TEXT,
  reminder_minutes INTEGER,
  recurrence_rule TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  deleted_at TEXT,
  version INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE calendar_holidays (
  day TEXT PRIMARY KEY,
  is_holiday INTEGER NOT NULL DEFAULT 1,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE calendar_static_holidays (
  day TEXT PRIMARY KEY,
  is_holiday INTEGER NOT NULL DEFAULT 1,
  name TEXT,
  source TEXT NOT NULL DEFAULT 'chinese-days',
  updated_at TEXT NOT NULL
);
```

### 20.4 FTS 与同步预留

```sql
CREATE VIRTUAL TABLE diaries_fts USING fts5(
  diary_id UNINDEXED,
  entry_date,
  entry_time,
  content_md,
  mood,
  weather,
  location_name,
  tags,
  tokenize='unicode61'
);

CREATE TABLE sync_outbox (
  id TEXT PRIMARY KEY,
  entity_type TEXT NOT NULL,
  entity_id TEXT NOT NULL,
  operation TEXT NOT NULL,
  payload_json TEXT NOT NULL,
  base_version INTEGER,
  created_at TEXT NOT NULL,
  retry_count INTEGER NOT NULL DEFAULT 0,
  status TEXT NOT NULL DEFAULT 'pending'
);
```

## 21. 状态机与业务状态

### 21.1 Todo 状态

```text
pending
  |
  | 用户标记完成
  v
completed
  |
  | 用户点击完成按钮恢复
  v
pending
```

当前也保留 `in_progress` 等历史状态筛选能力，但主要交互路径是“未完成 / 已完成”。

删除状态独立于业务状态：

```text
正常任务 --DELETE--> deleted_at != NULL
回收站任务 --restore--> deleted_at = NULL
```

恢复时应保留删除前状态，而不是强制改为待完成。

### 21.2 日记状态

```text
active diary
  |
  | delete
  v
deleted diary
  |
  | restore, no active diary on same date
  v
active diary
```

冲突恢复：

```text
deleted diary
  |
  | restore, active diary exists on same date
  v
merged source
  |
  +-- deleted_at 保留
  +-- restored_at 写入
  +-- restored_into_id 指向目标日记
  +-- 不再显示在回收站
```

### 21.3 日历事件状态

```text
active event
  |
  | delete
  v
deleted event
  |
  | restore
  v
active event
```

事件目前没有冲突合并逻辑，因为同一天多个事件是合理业务形态。

### 21.4 节假日状态

```text
周末默认假期
静态数据库节假日
用户手动覆盖
```

用户手动标记优先级最高。再次点击已标记日期会取消手动标记或恢复默认样式，具体表现取决于当前覆盖记录。

## 22. 导出格式细节

### 22.1 Todo 导出

支持格式：

- JSON
- CSV
- Markdown

Markdown 格式示例：

```markdown
# Flux Todo Export

- [ ] 写技术文档 @ 2026-04-18T09:00:00 #文档
  - [x] 梳理接口
  - [ ] 补充测试矩阵
- [x] 整理附件
```

CSV 字段：

```text
id,title,status,priority,due_at,tag_name
```

### 22.2 日记导出

支持格式：

- JSON
- CSV
- Markdown

CSV 字段：

```text
id,entry_date,entry_time,mood,weather,location_name,is_favorite,word_count,tags,content_md,created_at,updated_at
```

Markdown 格式示例：

```text
# Flux Diary Export

\## 2026-04-18 21:30

> 心情：calm ｜ 天气：晴 ｜ 位置：家 ｜ 标签：项目, 记录

今天继续整理 Flux 文档。
```

导出注意事项：

- 日记导出默认遵循当前筛选条件。
- 多选导出由前端控制选择集合。
- 附件在 Markdown 中保留本地 `/attachments/...` 引用，不会自动打包二进制文件。

## 23. 附件安全模型

### 23.1 当前校验

上传时校验：

- 请求必须是 `multipart/form-data`
- `Content-Length` 必须大于 0
- multipart 总体大小不能明显超过 100MB
- 实际文件内容必须小于 100MB
- 文件扩展名必须在白名单中
- 服务端使用 UUID 重命名文件，避免直接使用用户文件名作为路径

服务时校验：

- URL 必须以 `/attachments/` 开头
- 解析后的路径必须位于 `data/attachments/` 内部
- 不存在的文件返回 404

删除时校验：

- 删除目标必须解析到附件根目录内部
- 删除后会尝试清理空目录
- 返回删除字节数和引用数量

### 23.2 当前限制

当前附件安全仍有边界：

- 只按扩展名和 MIME 猜测分类，不解析文件内容签名。
- 不做病毒扫描。
- 普通文档以浏览器打开或下载为主，不做沙箱预览。
- 单个附件引用关系通过 Markdown 扫描得出，未建立数据库级外键。

### 23.3 后续增强建议

可考虑：

- 增加附件元数据表。
- 记录上传时间、原始文件名、hash、大小、kind。
- 用 SHA-256 去重。
- 增加附件永久删除前的二次确认。
- 对未知类型统一强制下载，减少浏览器直接执行风险。

## 24. 数据迁移策略

当前迁移方式集中在 `_ensure_columns` 和初始化 SQL 中。

现有策略：

- 新表使用 `CREATE TABLE IF NOT EXISTS`。
- 新列通过 `PRAGMA table_info` 检查后 `ALTER TABLE`。
- FTS 表在初始化阶段确保存在，并可重建。
- 日记重复数据通过 `_dedupe_diaries` 在建立唯一索引前清理。

优点：

- 零外部迁移工具依赖。
- 启动时自动补齐轻量 schema 变更。
- 适合本地单文件数据库。

风险：

- 迁移历史不可审计。
- 复杂数据迁移会使启动逻辑膨胀。
- 回滚能力有限。
- 多版本客户端同时使用同一数据库时容易出现兼容问题。

建议下一阶段引入：

```text
schema_migrations
  version INTEGER PRIMARY KEY
  name TEXT
  applied_at TEXT
```

并把迁移拆成显式版本脚本。

## 25. 备份与恢复 Runbook

### 25.1 备份范围

必须备份：

```text
data/flux.db
data/attachments/
```

建议同时备份：

```text
docs/
tools/
vendor/chinese-days 或已导入节假日来源说明
```

### 25.2 手动备份流程

```text
1. 停止正在运行的 Flux 服务。
2. 复制 data/flux.db。
3. 复制 data/attachments/。
4. 将两者放入同一个带日期的备份目录。
5. 重新启动服务。
```

备份目录示例：

```text
backups/
  2026-04-18-1630/
    flux.db
    attachments/
```

### 25.3 恢复流程

```text
1. 停止 Flux 服务。
2. 将当前 data/flux.db 改名为 flux.db.bak。
3. 将备份的 flux.db 放回 data/。
4. 将备份的 attachments/ 放回 data/。
5. 启动服务。
6. 打开日记、日历、附件管理进行抽样检查。
```

### 25.4 一致性注意事项

SQLite 数据库和附件目录必须来自同一时间点。否则可能出现：

- 日记引用了不存在的附件。
- 附件存在但管理面板显示未引用。
- 附件删除记录和文件实际状态不一致。

## 26. 代码组织与拆分路线

当前后端集中在一个文件中，优点是易读和易运行，缺点是长期维护压力会增加。

建议拆分路径：

```text
apps/server/
  main.py
  api/
    diaries.py
    todos.py
    calendar.py
    attachments.py
  repository/
    sqlite.py
    diaries.py
    todos.py
    calendar.py
  services/
    diary_restore.py
    attachment_index.py
    calendar_aggregate.py
  migrations/
  tests/
```

前端建议拆分：

```text
apps/client/web/
  app.js
  modules/
    api.js
    state.js
    diary.js
    calendar.js
    todo.js
    attachments.js
    dialogs.js
  styles/
    base.css
    diary.css
    calendar.css
    todo.css
    attachments.css
```

拆分原则：

- 先拆纯函数和渲染函数。
- 再拆 API 和状态。
- 最后拆复杂业务流程。
- 每次拆分都保持现有功能可运行。

## 27. 性能关注点

### 27.1 当前复杂度

| 功能 | 当前实现 | 潜在瓶颈 |
| :--- | :--- | :--- |
| 日记搜索 | SQLite FTS5 | 数据量极大时需要优化 FTS 维护 |
| 附件管理 | 扫描文件系统 + 扫描日记 Markdown | 附件和日记很多时会变慢 |
| 月历摘要 | 多个 SQL 聚合 | 目前可接受，未来可缓存月摘要 |
| 前端渲染 | 字符串模板整页局部重绘 | 列表很长时可能需要分页或虚拟列表 |
| Markdown 预览 | 轻量正则解析 | 复杂 Markdown 支持有限 |

### 27.2 优化优先级

优先级建议：

```text
1. 附件索引表
2. 日记列表分页或按月份分段加载
3. 月历摘要缓存
4. 前端模块化后局部渲染
5. 更完整的 Markdown parser
```

## 28. 兼容性与技术债

### 28.1 命名技术债

`todo_projects` 和相关 API 仍保留 project 命名，但 UI 已改为“标签”。

短期可以接受，因为它避免大规模迁移。长期建议：

- 数据库保留旧字段，通过文档说明。
- 新 API 可以逐步引入 `todo-tags`。
- 等数据迁移机制成熟后再考虑表重命名。

### 28.2 遗留统计接口

后端仍存在 `analytics_overview` 和 `/api/v1/analytics/overview`，但前端统计页面已移除。

处理建议：

- 如果后续不再需要，删除接口和相关方法。
- 如果保留，应标注为内部诊断接口。

### 28.3 轻量 Markdown parser

当前 Markdown 支持能覆盖标题、粗体、斜体、列表、链接、图片、音频和文件附件，但不是完整 CommonMark 实现。

如果日记写作能力继续增强，建议引入成熟解析器或在本地实现更系统的语法支持。

## 29. 架构结论

Flux 当前是一个边界清晰、低依赖、符合本地优先目标的单体应用。

它没有过度引入分布式组件，也没有为不存在的高并发问题提前复杂化。SQLite、FTS5、本地文件系统和原生 Web 的组合，与当前产品阶段高度匹配。

后续技术重点应放在：

- 测试补齐
- 前端模块化
- 备份恢复
- 附件索引化
- 时间体系统一
- 打包与分发
