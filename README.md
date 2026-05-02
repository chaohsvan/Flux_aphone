# Flux

Flux 是一个本地优先的 Android 个人记录与日程管理应用。它把日记、待办、日历和备份放在同一个轻量工作流里，适合把日常记录、计划安排和个人数据长期保存在自己的设备上。

## 功能概览

- 日记：支持 Markdown 内容、心情、天气、地点、标签、附件和全文检索。
- 待办：支持项目、标签、子任务、优先级、截止日期、完成历史和软删除。
- 日历：提供月视图、周视图、日视图和季度视图，可叠加日记、待办、事件、节假日、订阅日历和回收站数据。
- 小部件：提供待办列表小部件和日历月视图小部件，主题跟随系统。应用退出后，小部件需要点击右上角刷新图标才会重新载入最新数据库内容。
- 备份：支持本地全量备份导入导出，并支持通过 WebDAV/坚果云进行云端全量备份与恢复。
- 设置：包含主题、每周开始日、提醒音、备份、附件和数据管理等入口。

## 技术栈

- Kotlin
- Jetpack Compose
- Room / SQLite / FTS5
- Hilt
- Coroutines / Flow
- Navigation Compose
- Glance App Widgets
- WorkManager / BroadcastReceiver
- OkHttp / WebDAV
- Gradle Kotlin DSL

## 项目结构

```text
app/src/main/java/com/example/flux
├── core                 # 数据库、备份、同步、通知、系统能力封装
├── data                 # 仓库实现和数据访问
├── feature              # 按业务功能拆分的页面、领域模型和网关
├── model                # 共享模型
├── ui                   # 通用 Compose UI 和主题
├── widget               # Android 小部件
└── worker               # 后台任务

docs
├── android              # 架构、数据库、UI 和重构文档
├── featuresREADME.md    # 功能说明
└── syncREADME.md        # 备份与云端恢复说明
```

## 构建与测试

本项目使用 Android Studio 自带的 JBR 作为 JDK。Windows PowerShell 下可使用：

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

当前备份与恢复采用全量方案：每次备份都会打包数据库和附件数据，每次恢复也会按完整备份包导入。这个方式简单可靠，适合小到中等规模的数据；但它不是增量同步，**对大附件和大量附件不友好**，备份包会变大，上传、下载和恢复耗时也会随之增加。

云端备份使用 WebDAV 目录保存备份文件。从云端导入备份后，应用会提示重启，以确保数据库、缓存和界面状态重新加载到最新数据。

更详细的备份说明见 [docs/syncREADME.md](docs/syncREADME.md)。

## 待改进

当前项目仍保留调试阶段的包名：

```text
com.example.flux
```

后续如果要正式发布，需要替换为最终应用包名，并同步检查 `namespace`、`applicationId`、备份路径、第三方服务配置和签名配置。

应用图标资源已经放入 Android 各密度目录。原始图标源文件位于：

```text
app/src/main/res/drawable-nodpi/launcher_icon_source.png
```

后续还可以继续改进：

- 应用名称、包名和版本号。
- Release 签名和混淆配置。
- 启动图标在主流桌面上的裁切效果。
- WebDAV 备份恢复流程。
- 全量备份对大附件和大量附件不友好，可考虑增量备份、附件分片或附件单独同步。
- 小部件在浅色、深色和系统动态主题下的显示效果。

## 参考文档

- [功能说明](docs/featuresREADME.md)
- [Android 架构设计](docs/android/01_Architecture_Design.md)
- [数据库设计](docs/android/02_Database_Design.md)
- [UI/UX 指南](docs/android/03_UI_UX_Guidelines.md)
- [重构 TODO](docs/android/04_Refactor_TODO.md)
- [备份与云端恢复](docs/syncREADME.md)
