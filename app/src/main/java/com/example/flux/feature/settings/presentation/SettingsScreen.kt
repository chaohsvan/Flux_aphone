package com.example.flux.feature.settings.presentation

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Recycling
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flux.core.sync.WebDavSyncConfig
import com.example.flux.core.sync.JIANGUOYUN_WEBDAV_URL
import com.example.flux.core.util.TimeUtil
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAttachmentManager: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToCalendarSubscriptions: () -> Unit,
    onNavigateToWeatherAppBinding: () -> Unit,
    onOpenGlobalSearch: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var incrementalImport by remember { mutableStateOf(false) }
    var showWebDavSettings by remember { mutableStateOf(false) }
    var showCloudRestoreConfirm by remember { mutableStateOf(false) }
    var incrementalCloudRestore by remember { mutableStateOf(false) }
    var showWeekStartDialog by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri != null) {
                viewModel.exportBackup(context, uri) { success ->
                    Toast.makeText(
                        context,
                        if (success) "已导出本地备份" else "备份导出失败",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    )
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) pendingImportUri = uri
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                actions = {
                    IconButton(onClick = onOpenGlobalSearch) {
                        Icon(Icons.Default.Search, contentDescription = "全局搜索")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            SettingsActionItem(
                title = "WebDAV \u4e91\u5907\u4efd\u914d\u7f6e",
                subtitle = if (uiState.webDavConfig.username.isNotBlank()) {
                    "\u5df2\u914d\u7f6e\u8d26\u53f7\uff1a${uiState.webDavConfig.username}"
                } else {
                    "\u586b\u5199\u575a\u679c\u4e91\u8d26\u53f7\u548c\u7b2c\u4e09\u65b9\u5e94\u7528\u5bc6\u7801"
                },
                icon = Icons.Default.Refresh,
                tint = MaterialTheme.colorScheme.primary,
                onClick = { showWebDavSettings = true }
            )
            SettingsActionItem(
                title = "\u5907\u4efd\u5230\u4e91\u7aef WebDAV",
                subtitle = if (uiState.isExportingBackup) {
                    "\u6b63\u5728\u4e0a\u4f20\u4e91\u5907\u4efd..."
                } else {
                    "\u4f7f\u7528\u72ec\u7acb\u76ee\u5f55 FluxBackups"
                },
                icon = Icons.Default.Share,
                tint = MaterialTheme.colorScheme.primary,
                enabled = !uiState.isExportingBackup,
                onClick = {
                    viewModel.backupToCloud { success, message ->
                        Toast.makeText(
                            context,
                            message,
                            if (success) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
            SettingsActionItem(
                title = "\u4ece\u4e91\u7aef\u6062\u590d\u5907\u4efd",
                subtitle = if (uiState.isImportingBackup) {
                    "\u6b63\u5728\u4ece\u4e91\u7aef\u6062\u590d..."
                } else {
                    "\u6062\u590d FluxBackups \u4e2d\u7684\u6700\u65b0\u5907\u4efd"
                },
                icon = Icons.Default.Refresh,
                tint = MaterialTheme.colorScheme.primary,
                enabled = !uiState.isImportingBackup,
                onClick = { showCloudRestoreConfirm = true }
            )
            SettingsActionItem(
                title = "天气 App 绑定",
                subtitle = uiState.weatherAppBinding?.let { "已绑定：${it.displayName}" }
                    ?: "绑定后，日历顶部天气按钮会打开对应 App",
                icon = Icons.Default.WbSunny,
                tint = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToWeatherAppBinding
            )
            SettingsActionItem(
                title = "ICS 日历订阅",
                subtitle = if (uiState.calendarSubscriptions.isEmpty()) {
                    "管理已订阅日历，或添加新的 ICS 链接"
                } else {
                    "已添加 ${uiState.calendarSubscriptions.size} 个订阅"
                },
                icon = Icons.Default.DateRange,
                tint = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToCalendarSubscriptions
            )
            SettingsActionItem(
                title = "附件管理",
                subtitle = "查看引用来源，清理未引用附件",
                icon = Icons.Default.AttachFile,
                tint = MaterialTheme.colorScheme.primary,
                onClick = onNavigateToAttachmentManager
            )
            SettingsActionItem(
                title = "回收站",
                subtitle = if (uiState.trashSummary.total > 0) {
                    "当前可恢复 ${uiState.trashSummary.total} 项：日记 ${uiState.trashSummary.diaries} / 待办 ${uiState.trashSummary.todos} / 事件 ${uiState.trashSummary.events}"
                } else {
                    "当前没有待恢复内容"
                },
                icon = Icons.Default.Recycling,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                onClick = onNavigateToTrash
            )
            SettingsActionItem(
                title = "导出本地备份",
                subtitle = if (uiState.isExportingBackup) "正在生成备份..." else "打包数据库和本地附件",
                icon = Icons.Default.Share,
                tint = MaterialTheme.colorScheme.primary,
                enabled = !uiState.isExportingBackup,
                onClick = { backupLauncher.launch("FluxBackup_${TimeUtil.getCurrentDate()}.zip") }
            )
            SettingsActionItem(
                title = "导入本地备份",
                subtitle = if (uiState.isImportingBackup) "正在恢复备份..." else "从 data 压缩包恢复，完成后需要重启应用",
                icon = Icons.Default.Refresh,
                tint = MaterialTheme.colorScheme.primary,
                enabled = !uiState.isImportingBackup,
                onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }
            )
            WeekStartSetting(
                selected = uiState.weekStartDay,
                onClick = { showWeekStartDialog = true }
            )
            ReminderSoundSetting(
                enabled = uiState.reminderSoundEnabled,
                onEnabledChange = viewModel::setReminderSoundEnabled
            )
        }
    }

    if (showWebDavSettings) {
        WebDavSettingsDialog(
            config = uiState.webDavConfig,
            onDismiss = { showWebDavSettings = false },
            onSave = { config ->
                viewModel.saveWebDavConfig(config) { success, message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    if (success) showWebDavSettings = false
                }
            }
        )
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("导入本地备份") },
            text = {
                Column {
                    Text(
                        if (incrementalImport) {
                            "只把备份中的记录合并到当前数据中，不删除当前已有内容。当前 data 仍会先保留一份副本。"
                        } else {
                            "将用备份包中的 data 目录替换当前私有数据。当前 data 会先保留一份副本。"
                        }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clickable { incrementalImport = !incrementalImport },
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Checkbox(
                            checked = incrementalImport,
                            onCheckedChange = { incrementalImport = it }
                        )
                        Text(
                            text = "只进行增量导入",
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportUri = null
                        viewModel.importBackup(context, uri, incrementalImport) { success ->
                            Toast.makeText(
                                context,
                                if (success) {
                                    if (incrementalImport) {
                                        "\u5df2\u589e\u91cf\u5bfc\u5165\u5907\u4efd\uff0c\u8bf7\u91cd\u542f\u5e94\u7528\u540e\u67e5\u770b\u6700\u65b0\u6570\u636e"
                                    } else {
                                        "\u5df2\u5bfc\u5165\u5907\u4efd\uff0c\u8bf7\u91cd\u542f\u5e94\u7528\u540e\u67e5\u770b\u6700\u65b0\u6570\u636e"
                                    }
                                } else {
                                    "\u5907\u4efd\u5bfc\u5165\u5931\u8d25"
                                },
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Text("导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text("取消")
                }
            }
        )
    }

    if (showCloudRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showCloudRestoreConfirm = false },
            title = { Text("\u4ece\u4e91\u7aef\u6062\u590d\u5907\u4efd") },
            text = {
                Column {
                    Text(
                        if (incrementalCloudRestore) {
                            "\u5c06\u4e91\u7aef\u6700\u65b0\u5907\u4efd\u589e\u91cf\u5408\u5e76\u5230\u672c\u673a\uff0c\u672c\u673a\u73b0\u6709\u6570\u636e\u4f1a\u5148\u4fdd\u7559\u526f\u672c\u3002"
                        } else {
                            "\u5c06\u7528\u4e91\u7aef\u6700\u65b0\u5907\u4efd\u66ff\u6362\u672c\u673a\u6570\u636e\uff0c\u672c\u673a\u73b0\u6709\u6570\u636e\u4f1a\u5148\u4fdd\u7559\u526f\u672c\u3002"
                        }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clickable { incrementalCloudRestore = !incrementalCloudRestore },
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Checkbox(
                            checked = incrementalCloudRestore,
                            onCheckedChange = { incrementalCloudRestore = it }
                        )
                        Text(
                            text = "\u53ea\u8fdb\u884c\u589e\u91cf\u6062\u590d",
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isImportingBackup,
                    onClick = {
                        showCloudRestoreConfirm = false
                        viewModel.restoreFromCloud(incrementalCloudRestore) { success, message ->
                            Toast.makeText(
                                context,
                                message,
                                if (success) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text("\u6062\u590d")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloudRestoreConfirm = false }) {
                    Text("\u53d6\u6d88")
                }
            }
        )
    }

    if (showWeekStartDialog) {
        WeekStartDialog(
            selected = uiState.weekStartDay,
            onDismiss = { showWeekStartDialog = false },
            onSelected = { day ->
                viewModel.setWeekStartDay(day)
                showWeekStartDialog = false
            }
        )
    }
}

@Composable
private fun WebDavSettingsDialog(
    config: WebDavSyncConfig,
    onDismiss: () -> Unit,
    onSave: (WebDavSyncConfig) -> Unit
) {
    var username by remember(config) { mutableStateOf(config.username) }
    var password by remember(config) { mutableStateOf(config.password) }

    fun currentConfig(): WebDavSyncConfig {
        return WebDavSyncConfig(
            enabled = true,
            baseUrl = JIANGUOYUN_WEBDAV_URL,
            username = username,
            password = password,
            remoteDir = config.remoteDir
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("WebDAV \u4e91\u5907\u4efd\u914d\u7f6e") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "WebDAV \u5730\u5740\uff1a$JIANGUOYUN_WEBDAV_URL",
                    modifier = Modifier.padding(top = 12.dp)
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("\u8d26\u53f7") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("\u5e94\u7528\u5bc6\u7801") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "\u4e91\u5907\u4efd\u76ee\u5f55\u56fa\u5b9a\u4e3a\uff1a\u6211\u7684\u575a\u679c\u4e91/FluxBackups",
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(currentConfig()) }) {
                Text("\u4fdd\u5b58")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("\u5173\u95ed")
            }
        }
    )
}

@Composable
private fun ReminderSoundSetting(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text("\u63d0\u9192\u58f0\u97f3") },
        supportingContent = {
            Text(if (enabled) "\u5f85\u529e\u548c\u65e5\u5386\u4e8b\u4ef6\u63d0\u9192\u65f6\u53d1\u51fa\u58f0\u97f3" else "\u63d0\u9192\u4ec5\u663e\u793a\u901a\u77e5\uff0c\u4e0d\u4e3b\u52a8\u53d1\u58f0")
        },
        trailingContent = {
            Switch(
                checked = enabled,
                onCheckedChange = onEnabledChange
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEnabledChange(!enabled) }
    )
}

@Composable
private fun WeekStartSetting(
    selected: Int,
    onSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = "每周起始日",
            style = MaterialTheme.typography.titleMedium
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelected(Calendar.SUNDAY) },
            horizontalArrangement = Arrangement.Start
        ) {
            RadioButton(
                selected = selected != Calendar.MONDAY,
                onClick = { onSelected(Calendar.SUNDAY) }
            )
            Text(
                text = "周日",
                modifier = Modifier.padding(top = 12.dp)
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelected(Calendar.MONDAY) },
            horizontalArrangement = Arrangement.Start
        ) {
            RadioButton(
                selected = selected == Calendar.MONDAY,
                onClick = { onSelected(Calendar.MONDAY) }
            )
            Text(
                text = "周一",
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

@Composable
private fun WeekStartSetting(
    selected: Int,
    onClick: () -> Unit
) {
    SettingsActionItem(
        title = "\u6bcf\u5468\u5f00\u59cb\u65e5",
        subtitle = if (selected == Calendar.MONDAY) "\u5468\u4e00" else "\u5468\u65e5",
        icon = Icons.Default.DateRange,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = onClick
    )
}

@Composable
private fun WeekStartDialog(
    selected: Int,
    onDismiss: () -> Unit,
    onSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("\u6bcf\u5468\u5f00\u59cb\u65e5") },
        text = {
            Column {
                WeekStartOption(
                    text = "\u5468\u65e5",
                    selected = selected != Calendar.MONDAY,
                    onClick = { onSelected(Calendar.SUNDAY) }
                )
                WeekStartOption(
                    text = "\u5468\u4e00",
                    selected = selected == Calendar.MONDAY,
                    onClick = { onSelected(Calendar.MONDAY) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("\u53d6\u6d88")
            }
        }
    )
}

@Composable
private fun WeekStartOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Start
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick
        )
        Text(
            text = text,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null, tint = tint) },
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
    )
}
