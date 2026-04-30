# Flux 多端同步功能说明

本文档描述 Flux 当前多端同步功能的目标、同步范围、WebDAV 调用规则、冲突策略、错误处理和测试方式。同步功能仍以“本地优先”为原则：本地数据库和附件是 App 的主数据，云端只保存可恢复、可合并的同步快照与附件文件。

## 1. 功能目标

多端同步用于在多台设备之间同步当前本地备份已经覆盖的数据：

- SQLite 数据库快照。
- 本地附件文件。
- 附件删除状态。
- 最近历史数据库快照。
- 同步状态与错误日志。

同步不做后台定时轮询。当前触发时机为：

- 进入 App 时同步一次。
- 退出 App 时同步一次。
- 设置页点击“立即同步”时同步一次。
- 设置页点击“测试”时只测试 WebDAV 连接、目录创建和临时文件写入删除。

## 2. WebDAV 服务

当前 App 侧 WebDAV 服务商固定为坚果云，不再允许用户手动编辑 WebDAV 地址。

固定地址：

```text
https://dav.jianguoyun.com/dav/
```

用户只需要配置：

| 配置项 | 说明 |
| :--- | :--- |
| 账号 | 坚果云账号邮箱。 |
| 应用密码 | 坚果云第三方应用密码，不是登录密码。 |
| 同步目录 | 默认 `FluxSync`。 |

坚果云 WebDAV 的实际用户文件根目录是：

```text
我的坚果云/
```

因此用户填写 `FluxSync` 时，App 内部会映射为：

```text
我的坚果云/FluxSync
```

`我的坚果云` 是坚果云内置根目录，只能访问，不能创建。同步代码会跳过这一层，只检查或创建 `FluxSync` 目录。

## 3. 远端文件布局

如果同步目录为 `FluxSync`，远端实际路径如下：

```text
我的坚果云/FluxSync/FluxSync_manifest.json
我的坚果云/FluxSync/FluxSync_db_latest.zip
我的坚果云/FluxSync/FluxSync_db_latest.meta.json
我的坚果云/FluxSync/FluxSync_db_history_<snapshotId>.zip
我的坚果云/FluxSync/FluxSync_attachments_manifest.json
我的坚果云/FluxSync/FluxSync_att_<base64url-relativePath>
```

说明：

- `FluxSync_manifest.json`：数据库快照主清单。
- `FluxSync_db_latest.zip`：最新数据库快照。
- `FluxSync_db_latest.meta.json`：最新数据库快照元数据。
- `FluxSync_db_history_*.zip`：历史数据库快照。
- `FluxSync_attachments_manifest.json`：附件同步清单。
- `FluxSync_att_*`：附件文件，文件名使用附件相对路径的 Base64 URL-safe 编码。

## 4. 同步范围

### 4.1 数据库

数据库采用快照同步。

同步内容来自本地数据库备份包中的数据库文件。每次需要上传数据库时，会生成一个新的数据库 zip 快照，并上传为：

- 最新快照：`*_db_latest.zip`
- 历史快照：`*_db_history_<snapshotId>.zip`
- 元数据：`*_db_latest.meta.json`

当前历史快照保留最近 10 个版本，旧版本会在同步时尝试清理。

### 4.2 附件

附件采用文件级增量同步。

扫描范围为 App 私有数据目录下的附件文件。同步时会计算或复用附件 SHA-256：

- 如果附件未变化，不重新上传。
- 如果本地新增或修改，上传对应附件文件。
- 如果云端有本地没有的新附件，下载到本地。
- 如果本地删除了已同步过的附件，写入 tombstone，避免其他设备把它再次同步回来。

### 4.3 不同步的内容

当前同步只覆盖“本地备份已经覆盖”的核心数据。以下内容不作为独立同步对象：

- App 安装包和运行缓存。
- 构建产物。
- 系统通知状态。
- 仅存在于内存中的页面状态。
- 未纳入本地备份的数据。

## 5. 冲突策略

当前冲突策略遵循“云端快照合并，本地继续上传新结果”。

### 5.1 数据库冲突

当本地和云端数据库都发生变化时：

1. 下载云端数据库快照。
2. 执行增量恢复合并。
3. 同 ID 冲突时云端记录优先。
4. 合并完成后生成新的本地数据库快照。
5. 上传合并后的新快照，成为最新云端状态。

### 5.2 附件冲突

附件按路径和 hash 判断：

- 本地新增或修改：上传。
- 云端新增：下载。
- 本地删除且该路径此前同步过：写入删除 tombstone，并尝试删除远端附件文件。
- 云端已有删除 tombstone：本地也删除对应文件。

当前附件冲突没有复杂的双版本保留机制；同路径不同内容时，以当前合并逻辑中的写入结果为准。

## 6. WebDAV 调用链

连接测试调用链：

```text
PROPFIND https://dav.jianguoyun.com/dav/
MKCOL/PROPFIND 我的坚果云/FluxSync
PUT 我的坚果云/FluxSync/FluxSync_connection_test.json
DELETE 我的坚果云/FluxSync/FluxSync_connection_test.json
```

同步调用链：

```text
确保同步目录存在
读取数据库 manifest
判断本地和云端数据库是否变化
按冲突策略上传、下载或合并数据库快照
读取附件 manifest
扫描本地附件
上传、下载、删除附件
上传新的附件 manifest
更新本地同步状态
```

底层 WebDAV 客户端使用 OkHttp 发送请求，避免 Android `HttpURLConnection` 对 `PROPFIND`、`MKCOL` 等 WebDAV 扩展方法支持不稳定。

## 7. 错误处理

同步代码会根据 WebDAV 操作和 HTTP 状态码生成错误信息。

| 状态码 | 常见含义 | 处理建议 |
| :--- | :--- | :--- |
| 401 | 账号或应用密码错误 | 检查坚果云账号和第三方应用密码。 |
| 403 | 无权限访问或创建目录 | 检查应用密码权限、远端路径和服务端限制。 |
| 404 | 路径不存在 | 检查同步目录映射或远端文件是否存在。 |
| 409 | 父目录不存在或路径冲突 | 确认 `我的坚果云/FluxSync` 可创建。 |
| 5xx | 服务端错误 | 稍后重试或检查坚果云服务状态。 |

测试连接成功只代表 WebDAV 认证、目录创建、临时文件写入和删除可用，不代表完整数据库合并一定成功。

## 8. 本地测试建议

### 8.1 构建

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

### 8.2 手机端测试

1. 安装最新 debug APK。
2. 打开设置页。
3. 进入“多端同步”。
4. 开启同步。
5. 填写坚果云账号和第三方应用密码。
6. 同步目录保持默认 `FluxSync`。
7. 点击“测试”。
8. 测试成功后点击“立即同步”。

### 8.3 坚果云端验证

同步成功后，坚果云中应出现：

```text
我的坚果云/FluxSync/
```

并可看到 `FluxSync_manifest.json`、数据库快照、附件清单等文件。

## 9. 后续改进

后续可继续完善：

- 更详细的同步日志页面。
- 单独展示上传/下载文件数、数据库快照状态和耗时。
- 更清晰的历史快照回滚入口。
- 附件 hash 缓存命中率统计。
- 多服务商 WebDAV 适配层。
- 冲突附件保留双版本策略。
