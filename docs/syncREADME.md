# Flux WebDAV 云备份与恢复说明

本文只描述当前保留的 WebDAV 功能：**备份到 WebDAV** 与 **从 WebDAV 恢复**。

多端同步功能已经移除：

- 不再进入 App 自动同步。
- 不再退出 App 自动同步。
- 设置页不再提供“多端同步 / 立即同步 / 测试同步”入口。
- 不再使用 `FluxSync` 目录、同步 manifest、数据库同步快照、附件同步 manifest。

当前 WebDAV 只作为手动云备份空间使用。

## 1. 功能目标

WebDAV 云备份用于把当前 App 私有数据打包成一个完整备份 zip，上传到坚果云。

WebDAV 云恢复用于从坚果云下载最新备份 zip，然后恢复到本机。

它解决的是：

| 场景 | 行为 |
| :--- | :--- |
| 换手机 | 在旧手机备份到云端，在新手机从云端恢复 |
| 重装 App | 重装前备份，重装后恢复 |
| 手动保存安全点 | 随时上传一个当前数据快照 |
| 回到最新云备份 | 下载最新备份 zip 并恢复 |

它不解决：

- 多设备自动合并。
- 多设备冲突处理。
- 实时同步。
- 后台同步。
- 附件级双向增量同步。

## 2. WebDAV 配置

当前固定使用坚果云 WebDAV：

```text
https://dav.jianguoyun.com/dav/
```

用户需要在设置页的 **WebDAV 云备份配置** 中填写：

| 配置项 | 说明 |
| :--- | :--- |
| 账号 | 坚果云账号邮箱 |
| 应用密码 | 坚果云第三方应用密码，不是登录密码 |

云备份目录固定为：

```text
FluxBackups
```

坚果云 WebDAV 的实际用户根目录是：

```text
我的坚果云
```

所以 App 实际访问：

```text
我的坚果云/FluxBackups
```

`我的坚果云` 是坚果云内置根目录，App 只访问它，不创建它；App 只会尝试创建其下的 `FluxBackups` 目录。

## 3. 本地备份包原理

Flux 的核心数据位于 App 私有数据目录：

```text
files/
└── data/
    ├── flux.db
    ├── attachments/...
    └── 其他被纳入备份的数据文件
```

导出的备份 zip 结构为：

```text
data/flux.db
data/attachments/...
```

云备份上传的就是这个 zip。也就是说，云备份与“导出本地备份”使用同一套备份格式。

相关代码：

| 类 | 作用 |
| :--- | :--- |
| `ExportBackupUseCase` | 把本地 `data/` 打包成 zip，可导出到文件或系统 Uri |
| `ImportBackupUseCase` | 从 zip 恢复数据，支持全量替换和增量合并 |
| `CloudBackupManager` | 负责 WebDAV 目录创建、上传备份、下载最新备份 |
| `WebDavClient` | 封装 WebDAV 的 `PROPFIND`、`MKCOL`、`PUT`、`GET`、`DELETE` |
| `SyncStateStore` | 当前仅保存 WebDAV 账号和应用密码 |

## 4. 网盘资源

云端目录结构：

```text
我的坚果云/
└── FluxBackups/
    ├── FluxBackup_<timestamp>.zip
    └── latest_backup.json
```

### 4.1 备份 zip

命名格式：

```text
FluxBackup_<timestamp>.zip
```

示例：

```text
FluxBackup_20260430T210000.zip
```

每个 zip 都是一份完整备份。

### 4.2 latest_backup.json

每次成功上传云备份后，App 会更新：

```text
我的坚果云/FluxBackups/latest_backup.json
```

内容示例：

```json
{
  "fileName": "FluxBackup_20260430T210000.zip",
  "remotePath": "我的坚果云/FluxBackups/FluxBackup_20260430T210000.zip",
  "createdAt": "2026-04-30T21:00:00",
  "sha256": "...",
  "sizeBytes": 123456
}
```

恢复时优先读取 `latest_backup.json` 中的 `remotePath`。

如果 `latest_backup.json` 不存在，App 会扫描 `FluxBackups` 目录下最新的 `FluxBackup_*.zip` 作为恢复目标。

## 5. 备份逻辑

设置页点击 **备份到云端 WebDAV** 后：

```text
读取 WebDAV 账号和应用密码
构造坚果云固定地址 https://dav.jianguoyun.com/dav/
使用固定目录 FluxBackups
映射为 我的坚果云/FluxBackups
检查或创建远端目录
把本地 data/ 打包成临时 zip
上传 FluxBackup_<timestamp>.zip
计算 zip 的 sha256 和大小
写入 latest_backup.json
删除本地临时 zip
通过 Toast 显示结果
```

核心调用链：

```text
SettingsScreen
-> SettingsViewModel.backupToCloud()
-> SettingsFeatureGateway.backupToCloud()
-> CloudBackupManager.backupNow()
-> ExportBackupUseCase.toFile()
-> WebDavClient.uploadFile()
-> WebDavClient.writeText(latest_backup.json)
```

核心代码入口：

```kotlin
suspend fun backupNow(): CloudBackupResult
```

## 6. 恢复逻辑

设置页点击 **从云端恢复备份** 后：

```text
弹出确认框
用户选择全量恢复或增量恢复
读取 WebDAV 账号和应用密码
定位 我的坚果云/FluxBackups
读取 latest_backup.json
如果 latest_backup.json 不存在，则扫描 FluxBackup_*.zip
下载最新 zip 到本地缓存目录
调用本地备份恢复逻辑
删除本地临时 zip
通过 Toast 显示结果
```

核心调用链：

```text
SettingsScreen
-> SettingsViewModel.restoreFromCloud()
-> SettingsFeatureGateway.restoreFromCloud()
-> CloudBackupManager.restoreLatest()
-> WebDavClient.readText(latest_backup.json)
-> WebDavClient.downloadFile()
-> ImportBackupUseCase.fromFile()
```

核心代码入口：

```kotlin
suspend fun restoreLatest(mode: ImportBackupMode): CloudBackupResult
```

恢复模式：

| 模式 | 枚举 | 行为 |
| :--- | :--- | :--- |
| 全量恢复 | `ImportBackupMode.Replace` | 用云端备份里的 `data/` 替换本机 `data/` |
| 增量恢复 | `ImportBackupMode.Merge` | 把云端备份中的数据库和文件合并到本机 |

恢复前，本机当前 `data/` 会先保留一份副本。

## 7. WebDAV 调用方法

底层客户端：

```text
app/src/main/java/com/example/flux/core/sync/webdav/WebDavClient.kt
```

使用 OkHttp 发送 WebDAV 请求。

### 7.1 创建或检查目录

```text
PROPFIND https://dav.jianguoyun.com/dav/
PROPFIND 我的坚果云/
PROPFIND 我的坚果云/FluxBackups/
MKCOL    我的坚果云/FluxBackups/      # 如果不存在
```

### 7.2 上传备份

```text
PUT 我的坚果云/FluxBackups/FluxBackup_<timestamp>.zip
PUT 我的坚果云/FluxBackups/latest_backup.json
```

### 7.3 恢复备份

```text
GET      我的坚果云/FluxBackups/latest_backup.json
PROPFIND 我的坚果云/FluxBackups/      # latest_backup.json 不存在时用于扫描
GET      我的坚果云/FluxBackups/FluxBackup_<timestamp>.zip
```

### 7.4 客户端方法对应关系

| Kotlin 方法 | WebDAV 方法 | 说明 |
| :--- | :--- | :--- |
| `testConnection()` | `PROPFIND` | 检查 WebDAV 根地址 |
| `ensureDirectory()` | `PROPFIND` + `MKCOL` | 确保远端目录存在 |
| `readText()` | `GET` | 读取 `latest_backup.json` |
| `writeText()` | `PUT` | 写入 `latest_backup.json` |
| `uploadFile()` | `PUT` | 上传备份 zip |
| `downloadFile()` | `GET` | 下载备份 zip |
| `listFiles()` | `PROPFIND Depth: 1` | 扫描 `FluxBackup_*.zip` |

## 8. 错误处理

| 状态码 | 含义 | 建议 |
| :--- | :--- | :--- |
| 401 | 账号或应用密码错误 | 确认使用坚果云第三方应用密码 |
| 403 | 无权限访问或创建目录 | 检查应用密码权限和坚果云目录权限 |
| 404 | 目录或备份文件不存在 | 先执行一次云备份，或检查网盘目录 |
| 409 | 父目录不存在或路径冲突 | 检查 `我的坚果云/FluxBackups` 路径 |
| 5xx | 坚果云服务端异常 | 稍后重试 |

常见提示：

| 提示 | 说明 |
| :--- | :--- |
| `请先在多端同步中填写 WebDAV 账号和应用密码` | 旧提示文案，后续应改为“请先配置 WebDAV 账号和应用密码” |
| `云端没有可恢复的备份` | `FluxBackups` 下没有 `latest_backup.json`，也没有 `FluxBackup_*.zip` |
| `WebDAV 账号或应用密码验证失败` | 401，账号或应用密码错误 |
| `WebDAV 无权限访问云备份目录` | 403，目录不可写或权限不足 |

## 9. 测试方法

### 9.1 构建

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

### 9.2 单元测试

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest --tests "com.example.flux.core.sync.SyncCoreTest"
```

当前测试覆盖：

- 坚果云路径映射。
- WebDAV 配置完整性。
- 路径规范化。
- sha256 计算。

### 9.3 手机端验证

1. 打开设置页。
2. 进入 `WebDAV 云备份配置`。
3. 填写坚果云账号和应用密码。
4. 保存配置。
5. 点击 `备份到云端 WebDAV`。
6. 到坚果云确认 `我的坚果云/FluxBackups` 下出现：
   - `FluxBackup_*.zip`
   - `latest_backup.json`
7. 点击 `从云端恢复备份`。
8. 选择全量恢复或增量恢复。
9. 恢复完成后重启 App 查看数据。

## 10. 后续改进

- 云备份页面展示历史备份列表。
- 支持选择指定备份版本恢复。
- 自动清理旧备份，例如保留最近 N 个版本。
- 修正旧错误文案中的“多端同步”字样。
- 增加 WebDAV mock/integration test。
