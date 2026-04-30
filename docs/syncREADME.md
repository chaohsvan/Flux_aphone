# Flux 同步与 WebDAV 云备份详细说明

本文说明 Flux 当前与 WebDAV 相关的两套能力：

- **多端同步**：面向多设备协同，目录为 `FluxSync`，使用数据库快照、附件增量、manifest 和本地同步状态判断变化。
- **云备份与云恢复**：面向手动备份/恢复，目录为 `FluxBackups`，只上传和恢复完整备份 zip，不参与多端同步冲突判断。

两套能力共用坚果云 WebDAV 账号和应用密码，但远端目录、文件资源、业务目标和调用链是分开的。

## 1. 总体原则

### 1.1 为什么分成两套功能

多端同步和云备份解决的是不同问题：

| 功能 | 目标 | 典型场景 | 云端目录 |
| :--- | :--- | :--- | :--- |
| 多端同步 | 多设备之间尽量保持当前数据一致 | 手机 A 新建日记，手机 B 同步后看到 | `FluxSync` |
| 云备份/恢复 | 给当前设备做手动备份点 | 换机、重装、回滚到最近备份 | `FluxBackups` |

多端同步会根据本地和云端变化做上传、下载或合并；云备份只是把当前本地完整数据打成 zip 放到云端，恢复时再取回最新 zip。

### 1.2 本地数据模型

Flux 的核心数据在 App 私有目录中：

```text
files/
└── data/
    ├── flux.db
    ├── attachments/...
    └── 其他纳入本地备份的数据文件
```

本地备份 zip 的结构与此一致：

```text
data/flux.db
data/attachments/...
```

因此：

- 多端同步的数据库快照来自本地备份能力。
- 云备份上传的 zip 与“导出本地备份”生成的 zip 格式一致。
- 恢复时复用本地备份导入逻辑，支持全量替换和增量合并。

## 2. WebDAV 与坚果云资源

### 2.1 固定服务地址

当前 App 侧 WebDAV 服务商固定为坚果云：

```text
https://dav.jianguoyun.com/dav/
```

设置页不再让用户手动填写 WebDAV 地址，用户只需要填写：

| 配置项 | 用途 |
| :--- | :--- |
| 账号 | 坚果云账号邮箱 |
| 应用密码 | 坚果云第三方应用密码，不是登录密码 |
| 同步目录 | 多端同步目录，默认 `FluxSync` |

云备份复用账号和应用密码，但不使用同步目录配置，云备份目录固定为 `FluxBackups`。

### 2.2 坚果云根目录映射

坚果云 WebDAV 下用户文件的根目录是：

```text
我的坚果云
```

App 内部路径映射规则：

```text
FluxSync    -> 我的坚果云/FluxSync
FluxBackups -> 我的坚果云/FluxBackups
```

注意：

- `我的坚果云` 是坚果云已有根目录，代码不会尝试创建。
- 代码会跳过该根目录，只创建它下面的业务目录。
- 目录创建使用 WebDAV `MKCOL`。
- 目录检测使用 WebDAV `PROPFIND`。

### 2.3 底层 WebDAV 客户端

底层客户端类：

```text
app/src/main/java/com/example/flux/core/sync/webdav/WebDavClient.kt
```

底层使用 OkHttp，而不是 Android `HttpURLConnection`。原因是 `HttpURLConnection` 对 `PROPFIND`、`MKCOL` 等 WebDAV 扩展方法支持不稳定。

当前封装的方法：

| 方法 | WebDAV 操作 | 用途 |
| :--- | :--- | :--- |
| `testConnection()` | `PROPFIND` | 测试根地址是否可访问 |
| `ensureDirectory(path)` | `PROPFIND` + `MKCOL` | 逐级确保目录存在 |
| `exists(path)` | `PROPFIND` | 判断远端路径是否存在 |
| `readText(path)` | `GET` | 读取 manifest 或 json |
| `writeText(path, value)` | `PUT` | 写入 manifest 或 json |
| `uploadFile(path, file)` | `PUT` | 上传 zip 或附件 |
| `downloadFile(path, target)` | `GET` | 下载 zip 或附件 |
| `delete(path)` | `DELETE` | 删除远端文件 |
| `listFiles(path)` | `PROPFIND Depth: 1` | 列出目录下文件 |

## 3. 多端同步

### 3.1 入口与调用方法

主要入口类：

```text
app/src/main/java/com/example/flux/core/sync/FluxSyncManager.kt
```

对外方法：

```kotlin
suspend fun testConnection(config: WebDavSyncConfig): Boolean
suspend fun syncNow(): SyncRunResult
```

设置页调用链：

```text
SettingsScreen
-> SettingsViewModel.testSyncConnection()
-> SettingsFeatureGateway.testSyncConnection()
-> FluxSyncManager.testConnection()

SettingsScreen
-> SettingsViewModel.syncNow()
-> SettingsFeatureGateway.syncNow()
-> FluxSyncManager.syncNow()
```

App 进入/退出前台同步由应用层调用 `FluxSyncManager.syncNow()`，设置页手动点击“立即同步”也调用同一个方法。

### 3.2 触发时机

当前只做前台同步：

- 进入 App 时同步一次。
- 退出 App 时同步一次。
- 设置页点击“立即同步”时同步一次。
- 设置页点击“测试”时只测试连接、目录、临时文件写入和删除。

不做：

- 后台定时同步。
- 后台常驻监听。
- 复杂实时协同。

### 3.3 云端资源布局

默认同步目录：

```text
我的坚果云/FluxSync
```

如果同步目录为 `FluxSync`，远端资源如下：

| 资源 | 路径 | 说明 |
| :--- | :--- | :--- |
| 主 manifest | `我的坚果云/FluxSync/FluxSync_manifest.json` | 记录最新数据库快照 ID、hash、更新时间、设备 ID |
| 最新数据库快照 | `我的坚果云/FluxSync/FluxSync_db_latest.zip` | 当前云端最新数据库备份 zip |
| 最新快照元数据 | `我的坚果云/FluxSync/FluxSync_db_latest.meta.json` | 记录 snapshotId、sha256、大小、schema 版本 |
| 历史数据库快照 | `我的坚果云/FluxSync/FluxSync_db_history_<snapshotId>.zip` | 历史版本，当前保留最近 10 个 |
| 附件 manifest | `我的坚果云/FluxSync/FluxSync_attachments_manifest.json` | 记录附件列表、hash、删除 tombstone |
| 附件文件 | `我的坚果云/FluxSync/FluxSync_att_<base64url-relativePath>` | 附件文件，文件名由本地相对路径 Base64 URL-safe 编码生成 |

### 3.4 主 manifest 内容

`SyncManifest` 字段：

```kotlin
data class SyncManifest(
    val protocolVersion: Int = 1,
    val latestDbSnapshotId: String = "",
    val latestDbPath: String = "",
    val latestDbHash: String = "",
    val updatedAt: String = "",
    val updatedByDeviceId: String = "",
    val attachmentManifestVersion: Long = 0
)
```

含义：

| 字段 | 说明 |
| :--- | :--- |
| `protocolVersion` | 同步协议版本 |
| `latestDbSnapshotId` | 最新数据库快照 ID |
| `latestDbPath` | 最新数据库快照远端路径 |
| `latestDbHash` | 最新数据库快照 sha256 |
| `updatedAt` | 云端 manifest 更新时间 |
| `updatedByDeviceId` | 上传该版本的设备 ID |
| `attachmentManifestVersion` | 附件 manifest 版本号 |

### 3.5 附件 manifest 内容

附件 manifest 由 `AttachmentSyncManifest` 和 `AttachmentSyncEntry` 表示：

```kotlin
data class AttachmentSyncManifest(
    val version: Long = 0,
    val updatedAt: String = "",
    val files: List<AttachmentSyncEntry> = emptyList()
)

data class AttachmentSyncEntry(
    val path: String,
    val sha256: String,
    val sizeBytes: Long,
    val modifiedAt: Long,
    val updatedAt: String,
    val updatedByDeviceId: String,
    val deletedAt: String? = null
)
```

`deletedAt != null` 表示附件 tombstone：该附件已经被删除，其他设备同步时不应该再把它传回来。

### 3.6 数据库同步原理

数据库不做逐表远端增量传输，而是做“快照 + 本地增量恢复”：

1. 本地生成数据库备份 zip。
2. 上传最新 zip 和历史 zip。
3. 云端 manifest 记录最新快照 ID 和 hash。
4. 其他设备下载 zip 后，调用本地备份恢复逻辑。
5. 如果是云端单边变化，则全量替换本地数据。
6. 如果本地和云端都变化，则先把云端快照增量合并到本地，再上传合并后的本地快照。

数据库变化判断依赖：

| 状态 | 来源 |
| :--- | :--- |
| 当前本地数据库 hash | `DatabaseSnapshotManager.currentDatabaseHash()` |
| 上次本地数据库 hash | `SyncStateStore.getLastLocalDbHash()` |
| 云端最新快照 ID | `SyncManifest.latestDbSnapshotId` |
| 上次云端快照 ID | `SyncStateStore.getLastRemoteSnapshotId()` |

判断逻辑：

| 情况 | 处理 |
| :--- | :--- |
| 本地没变，云端没变 | 数据库不处理 |
| 本地变了，云端没变 | 上传本地快照 |
| 本地没变，云端变了 | 下载云端快照，全量替换 |
| 本地变了，云端也变了 | 下载云端快照，增量合并，然后上传合并后的新快照 |

### 3.7 数据库冲突策略

当前策略：

```text
本地和云端都变了
-> 下载云端数据库快照
-> 对本地执行增量恢复合并
-> 同 ID 冲突时云端记录优先
-> 上传合并后的新本地快照
```

这意味着云端快照在同 ID 冲突中优先，但本地独有的新数据会被保留并随合并后的快照重新上传。

### 3.8 附件同步原理

附件按文件路径和 sha256 判断：

1. 扫描本地附件目录。
2. 计算或复用附件 sha256。
3. 读取云端附件 manifest。
4. 本地新增或 hash 不同：上传附件。
5. 云端有、本地没有且不是 tombstone：下载附件。
6. 本地删除了此前同步过的附件：写入 tombstone，并尝试删除云端附件文件。
7. 云端已有 tombstone：本地删除对应附件。
8. 上传新的附件 manifest。

附件文件远端命名：

```text
FluxSync_att_<base64url-relativePath>
```

例如本地相对路径：

```text
attachments/diaries/a.png
```

会被编码成 URL-safe Base64，避免斜杠、中文和特殊字符影响 WebDAV 文件名。

### 3.9 删除行为说明

附件已有 tombstone，所以能解决：

```text
本机删除附件后，其他设备不再把该附件重新传回云端
```

日记、Todo、事件目前主要依赖数据库快照表达删除状态：

- 如果本机删除后立刻同步，会上传删除后的数据库快照，云端也变成删除后的状态。
- 如果其他旧设备仍保留该记录，并且随后上传旧状态，实体记录仍可能被带回。

后续更稳妥的改进是增加实体级 tombstone：

```text
entity_type = diary/todo/event
entity_id = xxx
deleted_at = xxx
deleted_by_device_id = xxx
```

这样云端旧快照合并时，就能明确知道某条记录已经被删除，不再恢复它。

## 4. 云备份与云恢复

### 4.1 入口与调用方法

主要入口类：

```text
app/src/main/java/com/example/flux/core/sync/CloudBackupManager.kt
```

对外方法：

```kotlin
suspend fun backupNow(): CloudBackupResult
suspend fun restoreLatest(mode: ImportBackupMode): CloudBackupResult
```

设置页调用链：

```text
SettingsScreen
-> SettingsViewModel.backupToCloud()
-> SettingsFeatureGateway.backupToCloud()
-> CloudBackupManager.backupNow()

SettingsScreen
-> SettingsViewModel.restoreFromCloud()
-> SettingsFeatureGateway.restoreFromCloud()
-> CloudBackupManager.restoreLatest()
```

### 4.2 云端资源布局

云备份固定目录：

```text
我的坚果云/FluxBackups
```

远端资源：

| 资源 | 路径 | 说明 |
| :--- | :--- | :--- |
| 备份 zip | `我的坚果云/FluxBackups/FluxBackup_<timestamp>.zip` | 完整本地备份包 |
| 最新备份索引 | `我的坚果云/FluxBackups/latest_backup.json` | 指向最新备份 zip |

云备份不会写入以下多端同步资源：

```text
FluxSync_manifest.json
FluxSync_db_latest.zip
FluxSync_attachments_manifest.json
```

因此它不会改变多端同步状态，也不会触发多端同步冲突。

### 4.3 latest_backup.json

上传云备份后，会写入：

```json
{
  "fileName": "FluxBackup_20260430T210000.zip",
  "remotePath": "我的坚果云/FluxBackups/FluxBackup_20260430T210000.zip",
  "createdAt": "2026-04-30T21:00:00",
  "sha256": "...",
  "sizeBytes": 123456
}
```

恢复时优先读取 `latest_backup.json` 中的 `remotePath`。如果这个文件不存在，则退化为扫描 `FluxBackups` 下最新的 `FluxBackup_*.zip`。

### 4.4 云备份逻辑

点击“备份到云端 WebDAV”：

```text
读取 WebDAV 账号和应用密码
构造固定目录 FluxBackups
映射为 我的坚果云/FluxBackups
确保远端目录存在
导出本地备份 zip 到缓存目录
上传 FluxBackup_<timestamp>.zip
计算 sha256 和大小
写入 latest_backup.json
删除本地临时 zip
Toast 显示结果
```

对应核心代码：

```kotlin
val config = stateStore.getConfig().copy(
    enabled = true,
    baseUrl = JIANGUOYUN_WEBDAV_URL,
    remoteDir = "FluxBackups"
)
val client = WebDavClient(config)
client.ensureDirectory("我的坚果云/FluxBackups", skipLeadingSegments = 1)
exportBackupUseCase.toFile(context, localFile)
client.uploadFile(remoteFile, localFile)
client.writeText("我的坚果云/FluxBackups/latest_backup.json", json)
```

### 4.5 云恢复逻辑

点击“从云端恢复备份”：

```text
读取 WebDAV 账号和应用密码
定位 我的坚果云/FluxBackups
读取 latest_backup.json
找到最新备份 zip
下载 zip 到本地缓存
根据用户选择执行恢复：
  - Replace：全量替换
  - Merge：增量合并
删除本地临时 zip
Toast 显示结果
```

恢复模式：

| 模式 | 枚举 | 行为 |
| :--- | :--- | :--- |
| 全量恢复 | `ImportBackupMode.Replace` | 用云端备份中的 `data/` 替换本机 `data/` |
| 增量恢复 | `ImportBackupMode.Merge` | 将云端备份数据库和文件合并进本机数据 |

恢复前会弹出确认框。本机当前 `data/` 会先复制一份副本，避免恢复失败后完全丢失当前数据。

## 5. 本地备份导入导出能力

云备份依赖已有本地备份能力：

| 类 | 作用 |
| :--- | :--- |
| `ExportBackupUseCase` | 导出 `data/` 到 zip，可导出到 Uri 或临时 File |
| `ImportBackupUseCase` | 从 zip 恢复 `data/`，支持 Replace 和 Merge |
| `DatabaseSnapshotManager` | 多端同步专用的数据库快照创建、hash、导入封装 |

本地导出：

```kotlin
exportBackupUseCase(context, destUri)
exportBackupUseCase.toFile(context, destFile)
```

本地导入：

```kotlin
importBackupUseCase(context, sourceUri, ImportBackupMode.Replace)
importBackupUseCase.fromFile(context, sourceFile, ImportBackupMode.Merge)
```

## 6. WebDAV 调用链

### 6.1 多端同步测试

```text
PROPFIND https://dav.jianguoyun.com/dav/
PROPFIND 我的坚果云/
PROPFIND 我的坚果云/FluxSync/
MKCOL    我的坚果云/FluxSync/              # 如果不存在
PUT      我的坚果云/FluxSync/FluxSync_connection_test.json
DELETE   我的坚果云/FluxSync/FluxSync_connection_test.json
```

### 6.2 多端同步执行

```text
PROPFIND 我的坚果云/FluxSync/
GET      我的坚果云/FluxSync/FluxSync_manifest.json
PUT      我的坚果云/FluxSync/FluxSync_db_history_<snapshotId>.zip
PUT      我的坚果云/FluxSync/FluxSync_db_latest.zip
PUT      我的坚果云/FluxSync/FluxSync_db_latest.meta.json
PUT      我的坚果云/FluxSync/FluxSync_manifest.json
GET      我的坚果云/FluxSync/FluxSync_attachments_manifest.json
PUT/GET  我的坚果云/FluxSync/FluxSync_att_<base64url-relativePath>
PUT      我的坚果云/FluxSync/FluxSync_attachments_manifest.json
```

具体会调用哪些方法，取决于本地/云端变化判断。

### 6.3 云备份

```text
PROPFIND https://dav.jianguoyun.com/dav/
PROPFIND 我的坚果云/
PROPFIND 我的坚果云/FluxBackups/
MKCOL    我的坚果云/FluxBackups/           # 如果不存在
PUT      我的坚果云/FluxBackups/FluxBackup_<timestamp>.zip
PUT      我的坚果云/FluxBackups/latest_backup.json
```

### 6.4 云恢复

```text
GET      我的坚果云/FluxBackups/latest_backup.json
PROPFIND 我的坚果云/FluxBackups/           # latest_backup.json 不存在时用于扫描
GET      我的坚果云/FluxBackups/FluxBackup_<timestamp>.zip
```

## 7. 错误处理

| 状态码 | 常见含义 | 多端同步影响 | 云备份影响 | 建议 |
| :--- | :--- | :--- | :--- | :--- |
| 401 | 账号或应用密码错误 | 无法测试/同步 | 无法备份/恢复 | 检查坚果云账号和第三方应用密码 |
| 403 | 无权限访问或创建目录 | 目录不可写，无法同步 | 目录不可写，无法上传备份 | 检查应用密码权限和坚果云目录权限 |
| 404 | 路径不存在 | manifest 或快照缺失 | 最新备份索引或 zip 缺失 | 确认路径映射，必要时先执行备份或同步 |
| 409 | 父目录不存在或路径冲突 | 创建目录失败 | 创建目录失败 | 确认 `我的坚果云` 可访问 |
| 5xx | 坚果云服务异常 | 同步失败 | 备份/恢复失败 | 稍后重试 |

常见提示解释：

| 提示 | 说明 |
| :--- | :--- |
| `WebDAV 账号或应用密码验证失败` | 401，通常是密码不是第三方应用密码 |
| `无权限访问目录` | 403，通常是路径或应用密码权限问题 |
| `远端目录不存在` | 404，可能是目录尚未创建或路径映射错误 |
| `Remote database snapshot checksum mismatch` | 下载到的数据库快照 hash 与 manifest 记录不一致 |

## 8. 测试方法

### 8.1 构建

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

### 8.2 同步核心单元测试

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest --tests "com.example.flux.core.sync.SyncCoreTest"
```

当前 `SyncCoreTest` 覆盖：

- 坚果云目录映射。
- 路径规范化。
- 配置完整性判断。
- 同步 manifest JSON 往返。
- 附件 tombstone manifest 往返。
- sha256 文件/字节一致性。

### 8.3 手机端多端同步验证

1. 安装最新 debug APK。
2. 打开设置页。
3. 进入“多端同步”。
4. 填写坚果云账号和第三方应用密码。
5. 同步目录保持默认 `FluxSync`。
6. 点击“测试”。
7. 点击“立即同步”。
8. 在坚果云中确认 `我的坚果云/FluxSync` 下出现 manifest、数据库快照、附件清单。
9. 新建日记/Todo/事件，再同步。
10. 清空本地数据后再同步，观察是否恢复到最新云端快照。

### 8.4 手机端云备份验证

1. 设置页先确保 WebDAV 账号和应用密码已经填写。
2. 点击“备份到云端 WebDAV”。
3. 在坚果云中确认 `我的坚果云/FluxBackups` 下出现：
   - `FluxBackup_*.zip`
   - `latest_backup.json`
4. 修改本地数据。
5. 点击“从云端恢复备份”。
6. 选择全量恢复或增量恢复。
7. 恢复完成后重启 App 查看数据。

## 9. 网盘资源清单

### 9.1 多端同步目录

```text
我的坚果云/
└── FluxSync/
    ├── FluxSync_manifest.json
    ├── FluxSync_db_latest.zip
    ├── FluxSync_db_latest.meta.json
    ├── FluxSync_db_history_<snapshotId>.zip
    ├── FluxSync_attachments_manifest.json
    └── FluxSync_att_<base64url-relativePath>
```

### 9.2 云备份目录

```text
我的坚果云/
└── FluxBackups/
    ├── FluxBackup_<timestamp>.zip
    └── latest_backup.json
```

### 9.3 不应混用的资源

不要手动把 `FluxBackups` 里的 zip 移到 `FluxSync`。

不要手动修改：

```text
FluxSync_manifest.json
FluxSync_attachments_manifest.json
latest_backup.json
```

这些文件是 App 判断状态和定位资源的索引文件，手动修改可能导致同步或恢复失败。

## 10. 后续改进

- 增加实体级 tombstone，覆盖 diary、todo、event，避免旧设备把已彻底删除的数据重新带回。
- 云备份增加历史列表，可以选择任意版本恢复。
- 云备份增加自动清理策略，例如保留最近 N 个版本。
- 同步状态页展示上传/下载数量、耗时、文件名和错误详情。
- 附件 hash 缓存命中率统计。
- 支持更多 WebDAV 服务商，而不只固定坚果云。
- 增加真实 WebDAV mock/integration test，覆盖 PUT、GET、PROPFIND、MKCOL、DELETE 调用链。
