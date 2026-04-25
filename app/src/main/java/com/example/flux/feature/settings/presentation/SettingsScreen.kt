package com.example.flux.feature.settings.presentation

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flux.core.util.TimeUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToAttachmentManager: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onOpenGlobalSearch: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }

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
                        Icon(Icons.Default.Search, contentDescription = "统一搜索")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(vertical = 8.dp)
        ) {
            SettingsActionItem(
                title = "附件管理",
                subtitle = "查看引用来源，清理未引用附件",
                icon = Icons.Default.Warning,
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
                icon = Icons.Default.Delete,
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
        }
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("导入本地备份") },
            text = { Text("将用备份包中的 data 目录替换当前私有数据。当前 data 会先保留一份副本。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportUri = null
                        viewModel.importBackup(context, uri) { success ->
                            Toast.makeText(
                                context,
                                if (success) "已导入备份，请重启应用" else "备份导入失败",
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
}

@Composable
private fun SettingsActionItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: androidx.compose.ui.graphics.Color,
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
