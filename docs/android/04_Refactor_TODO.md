# Flux Android Refactor TODO

## 目标

这轮重构不追求重写产品功能，而是在保持现有能力可用的前提下，逐步整理结构、明确边界、降低后续开发成本。

重构期间遵循这些约束：

- 每个阶段结束后都要通过 `assembleDebug`
- 先做等价重构，再做体验优化
- 一次只处理一个模块或一类横切能力
- 不在同一阶段同时改导航、数据结构和核心业务规则

## 当前主要问题

1. `MainActivity.kt` 曾承担过多入口和导航逻辑
2. `CalendarScreen.kt`、`AttachmentManagerScreen.kt`、`MarkdownText.kt`、`TodoScreen.kt` 曾经体量过大
3. 多个页面直接订阅零散 `StateFlow`
4. `ViewModel / Repository / UseCase` 之间的边界不够清晰
5. 历史乱码、旧命名、兼容逻辑分散在多个模块里
6. 核心流程可用，但结构保护和测试覆盖不足

## 目标目录结构

```text
app/
  navigation/
  theme/

core/
  database/
  common/
  util/

feature/
  diary/
    data/
    domain/
    presentation/
  todo/
    data/
    domain/
    presentation/
  calendar/
    data/
    domain/
    presentation/
  trash/
    data/
    domain/
    presentation/
  settings/
    data/
    domain/
    presentation/
  search/
    data/
    domain/
    presentation/
```

说明：

- `entity / dao / room` 继续保留在 `core/database`
- 各模块自己的 `Gateway / UseCase / UiState` 逐步收口到模块内部
- 通用工具和共享能力继续保留在 `core`

## 分阶段状态

### Phase 0 - 基线冻结

- [x] 冻结新增功能，优先做结构整理
- [x] 确认当前功能基线
- [x] 记录当前构建基线和关键手动验收路径

### Phase 1 - 应用骨架与导航收口

目标：把入口层和导航层从 `MainActivity.kt` 中拆出来。

- [x] 新建 `app/navigation/`
- [x] 拆出 `AppNavHost.kt`
- [x] 拆出 `AppDestinations.kt`
- [x] 拆出统一入口协调逻辑，减小 `MainActivity.kt` 体积
- [x] 明确页面级导航参数传递方式
- [x] 整理附件管理和日历聚焦等跳转参数

验收：

- [x] `MainActivity.kt` 只保留应用启动和导航承载
- [x] 导航路径集中定义
- [x] 原有页面跳转行为保持不变

### Phase 2 - 状态模型统一

目标：统一 ViewModel 输出结构。

- [x] 为以下页面引入 `UiState`
  - [x] `DiaryScreen`
  - [x] `DiaryEditorScreen`
  - [x] `TodoScreen`
  - [x] `TodoDetailScreen`
  - [x] `CalendarScreen`
  - [x] `AttachmentManagerScreen`
  - [x] `SettingsScreen`
- [x] 收敛零散 `StateFlow`
- [x] 统一页面主要状态出口

验收：

- [x] 主要页面通过单一主状态对象渲染
- [x] UI 不再直接拼接复杂业务数据

### Phase 3 - Diary 模块重构

目标：先整理相对稳定、风险较低的日记模块。

- [x] 拆分 `feature/diary` 为 `data / domain / presentation`
- [x] 拆分 `DiaryScreen.kt`
- [x] 拆分 `DiaryEditorScreen.kt`
- [x] 拆分 `MarkdownText.kt`
- [x] 收口标签、导出、搜索、筛选逻辑
- [x] 清理 Markdown 组件中的历史乱码和弃用 API

验收：

- [ ] 日记列表和编辑功能完成一轮人工回归
- [x] Markdown 渲染入口更清晰
- [x] 日记页面文件体积明显下降

### Phase 4 - Todo 模块重构

目标：整理待办的状态、详情、历史和重复规则边界。

- [x] 拆分 `feature/todo` 为 `data / domain / presentation`
- [x] 拆分 `TodoScreen.kt`
- [x] 拆分 `TodoDetailScreen.kt`
- [x] 将重复待办生成逻辑从 Repository 下沉到 UseCase
- [x] 统一优先级、状态、筛选模型
- [x] 清理 `project / tag` 历史命名混用

验收：

- [ ] 待办排序、批量操作、详情编辑完成一轮人工回归
- [x] 子任务与历史逻辑边界更清晰

### Phase 5 - Calendar 模块重构

目标：处理当前最重的页面和跨模块聚合逻辑。

- [x] 拆分 `feature/calendar` 为 `data / domain / presentation`
- [x] 新增 `CalendarFeatureGateway`
- [x] 拆分 `CalendarScreen.kt`
  - [x] 顶栏
  - [x] 模式切换
  - [x] 图层控制
  - [x] 月视图
  - [x] 周视图
  - [x] 日视图
  - [x] 季度视图
  - [x] 日期详情面板
- [x] 统一事件输入、节假日覆盖、回收站图层相关状态
- [x] 将导航切换到新的 `presentation` 包
- [x] 删除旧 `feature/calendar/ui` 实现

验收：

- [ ] 月 / 日 / 周 / 季视图完成一轮人工回归
- [ ] 事件新增、编辑、删除、聚焦跳转完成一轮人工回归
- [x] `assembleDebug` 通过
- [x] 日历模块目录边界清晰

### Phase 6 - Trash / Attachment / Settings / Search 收口

目标：整理横切能力。

- [x] 拆分 `AttachmentManagerScreen.kt`
- [x] 整理附件扫描、引用分析、删除逻辑边界
- [x] 将 Trash / Attachment 迁入 `feature/trash/data / domain / presentation`
- [x] 将 Settings 迁入 `feature/settings/data / domain / presentation`
- [x] 将统一搜索迁入 `feature/search/data / domain / presentation`
- [x] 统一回收站恢复逻辑
- [x] 整理设置模块备份导入导出流程边界
- [x] 收口统一搜索结构

验收：

- [x] Attachment / Trash / Settings / Search 不再直接依赖旧包结构
- [x] 主要横切能力具备 feature 内部 gateway 边界
- [x] `assembleDebug` 通过

### Phase 7 - 字符串、命名、测试与清理

目标：完成最终收尾。

- [x] 将高频用户可见文案补充迁入 `strings.xml`
- [x] 清理剩余乱码和旧命名
- [x] 补充重构后的纯函数测试
  - [x] `RecurrenceUtil`
  - [x] Markdown 解析
  - [x] Markdown 富文本标注
  - [x] 日记编辑文本动作
- [x] 删除已知弃用实现和临时兼容逻辑

验收：

- [x] `testDebugUnitTest` 通过
- [x] `assembleDebug` 通过
- [x] 重构文档恢复为正常 UTF-8 内容

## 实际执行顺序

1. Phase 1：导航与应用骨架
2. Phase 2：状态模型统一
3. Phase 3：Diary
4. Phase 4：Todo
5. Phase 5：Calendar
6. Phase 6：Trash / Attachment / Settings / Search
7. Phase 7：测试与清理

## 阶段完成定义

每个阶段结束时，至少满足：

- [ ] 代码已提交
- [x] `assembleDebug` 通过
- [ ] 主要功能完成对应人工回归
- [x] TODO 状态已更新

## 后续建议

这轮重构已经完成，后续更适合进入“稳定化”而不是继续大拆：

1. 补完 Diary / Todo / Calendar 的人工回归记录
2. 按真实高频页面继续迁移更多文案到 `strings.xml`
3. 逐步补 `CalendarAggregatorUseCase`、`Restore*UseCase`、`AttachmentManagerUseCase` 与 Room migration 测试
