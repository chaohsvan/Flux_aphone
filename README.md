# Flux

（基于 vibe coding）

Flux 是一个本地优先的 Android 个人记录与日程管理应用。它把日记、待办、日历、提醒、附件、回收站和备份放在同一个轻量工作流里，适合把日常记录、计划安排和个人数据长期保存在自己的设备上。

## 功能概览

- 日记：支持 Markdown 内容、心情、天气、地点、标签、收藏、附件、提醒和本地搜索。
- 待办：支持项目、子任务、优先级、开始时间、截止时间、提醒、重要标记、完成历史和软删除。
- 日历：提供月视图、周视图、日视图和季度视图，可叠加日记、待办、事件、节假日、ICS 订阅日历和回收站数据。
- 全局搜索：覆盖日记、待办、日历事件和附件，搜索结果可直接跳转到对应上下文。
- 提醒：日记、待办和日历事件可设置提前提醒；应用启动、开机、应用更新、时间或时区变化后会重排提醒。
- 小部件：提供待办列表小部件和日历月视图小部件，主题跟随系统，并支持手动刷新。
- 备份：支持本地备份导入导出，并支持通过 WebDAV/坚果云进行云端全量备份与恢复；恢复可选择全量替换或增量合并。
- 设置：包含 WebDAV 云备份配置、天气 App 绑定、ICS 日历订阅、附件管理、回收站、每周开始日、提醒音和提醒权限等入口。

## 技术栈

- Kotlin
- Jetpack Compose / Material 3
- Room / SQLite
- Hilt
- Coroutines / Flow
- Navigation Compose
- Glance App Widgets
- WorkManager / BroadcastReceiver / AlarmManager
- OkHttp / WebDAV
- Gradle Kotlin DSL

## 项目结构

```text
app/src/main/java/com/example/flux
├── FluxApplication.kt       # 应用启动、数据目录初始化、ICS Worker 和提醒重排
├── MainActivity.kt          # Compose 承载、通知权限请求、启动目的地处理
├── app/navigation           # 主导航、路由和自适应导航容器
├── core                     # 数据库、Repository、UseCase、提醒、同步、设置和工具
├── feature                  # diary / calendar / todo / settings / trash / search / widget
└── ui                       # 通用 Compose 组件和主题

docs
├── android                  # 架构、数据库、UI 和重构文档
├── featuresREADME.md        # 功能说明
├── refdesignREADME.md       # 产品与体验参考
├── reftechnicalREADME.md    # Android 技术实现说明
└── syncREADME.md            # WebDAV 备份与云端恢复说明
```

## 构建与测试

本项目使用 Android Studio 自带的 JBR 作为 JDK。

macOS / Linux:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Windows PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

生成的调试 APK 位于：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 数据与备份

Flux 的数据以 Room 数据库为核心，数据库 schema 通过迁移维护。删除类数据默认采用软删除策略，因此日历和回收站仍然可以追踪这些记录的状态。

App 私有数据目录结构：

```text
files/
└── data/
    ├── flux.db
    └── attachments/
```

本地备份和 WebDAV 云备份使用同一套 zip 格式，内容为完整 `data/` 目录。全量恢复会用备份包替换本机 `data/`，增量恢复会合并备份数据库和附件文件。恢复前会先保留本机当前 `data/` 副本。

当前 WebDAV 只作为手动云备份空间使用，不做多端自动同步、冲突合并或附件级双向增量同步。更详细的备份说明见 [docs/syncREADME.md](docs/syncREADME.md)。

## 待改进

当前项目仍保留调试阶段的包名：

```text
com.example.flux
```

后续如果要正式发布，需要替换为最终应用包名，并同步检查 `namespace`、`applicationId`、备份路径、第三方服务配置和签名配置。

后续还可以继续改进：

- 应用名称、包名、版本号、签名和混淆配置。
- 通知、精确闹钟、网络、文件分享和备份相关权限说明。
- WebDAV 历史备份列表、指定版本恢复和旧备份清理。
- 大附件和大量附件场景下的备份体验，例如附件分片或附件单独同步。
- 小部件在浅色、深色和系统动态主题下的显示效果。

## 参考文档

- [功能说明](docs/featuresREADME.md)
- [Android 技术实现](docs/reftechnicalREADME.md)
- [Android 架构设计](docs/android/01_Architecture_Design.md)
- [数据库设计](docs/android/02_Database_Design.md)
- [UI/UX 指南](docs/android/03_UI_UX_Guidelines.md)
- [重构 TODO](docs/android/04_Refactor_TODO.md)
- [备份与云端恢复](docs/syncREADME.md)

## 许可证

本项目采用 GNU General Public License v3.0 许可证开源，详见 [LICENSE](LICENSE)。

Copyright (C) 2026 chaohsvan
