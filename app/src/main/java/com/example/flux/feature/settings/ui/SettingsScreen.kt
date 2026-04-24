package com.example.flux.feature.settings.ui

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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
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
    val isExportingBackup by viewModel.isExportingBackup.collectAsState()
    val isImportingBackup by viewModel.isImportingBackup.collectAsState()
    val trashSummary by viewModel.trashSummary.collectAsState()
    var pendingImportUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip"),
        onResult = { uri ->
            if (uri != null) {
                viewModel.exportBackup(context, uri) { success ->
                    Toast.makeText(
                        context,
                        if (success) "\u5df2\u5bfc\u51fa\u672c\u5730\u5907\u4efd" else "\u5907\u4efd\u5bfc\u51fa\u5931\u8d25",
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
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "统一搜索"
                        )
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
            ListItem(
                headlineContent = { Text("附件管理") },
                supportingContent = { Text("查看引用来源，清理未引用附件") },
                leadingContent = {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToAttachmentManager)
            )
            ListItem(
                headlineContent = { Text("\u56de\u6536\u7ad9") },
                supportingContent = {
                    Text(
                        if (trashSummary.total > 0) {
                            "\u5f53\u524d\u53ef\u6062\u590d ${trashSummary.total} \u9879\uff1a\u65e5\u8bb0 ${trashSummary.diaries} / \u5f85\u529e ${trashSummary.todos} / \u4e8b\u4ef6 ${trashSummary.events}"
                        } else {
                            "\u5f53\u524d\u6ca1\u6709\u5f85\u6062\u590d\u5185\u5bb9"
                        }
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToTrash)
            )
            ListItem(
                headlineContent = { Text("\u5bfc\u51fa\u672c\u5730\u5907\u4efd") },
                supportingContent = {
                    Text(if (isExportingBackup) "\u6b63\u5728\u751f\u6210\u5907\u4efd..." else "\u6253\u5305\u6570\u636e\u5e93\u548c\u672c\u5730\u9644\u4ef6")
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isExportingBackup) {
                        backupLauncher.launch("FluxBackup_${TimeUtil.getCurrentDate()}.zip")
                    }
            )
            ListItem(
                headlineContent = { Text("\u5bfc\u5165\u672c\u5730\u5907\u4efd") },
                supportingContent = {
                    Text(if (isImportingBackup) "\u6b63\u5728\u6062\u590d\u5907\u4efd..." else "\u4ece data \u538b\u7f29\u5305\u6062\u590d\uff0c\u5b8c\u6210\u540e\u9700\u8981\u91cd\u542f\u5e94\u7528")
                },
                leadingContent = {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isImportingBackup) {
                        importLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                    }
            )
        }
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("\u5bfc\u5165\u672c\u5730\u5907\u4efd") },
            text = { Text("\u5c06\u7528\u5907\u4efd\u5305\u4e2d\u7684 data \u76ee\u5f55\u66ff\u6362\u5f53\u524d\u79c1\u6709\u6570\u636e\u3002\u5f53\u524d data \u4f1a\u5148\u4fdd\u7559\u4e00\u4efd\u526f\u672c\u3002") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingImportUri = null
                        viewModel.importBackup(context, uri) { success ->
                            Toast.makeText(
                                context,
                                if (success) "\u5df2\u5bfc\u5165\u5907\u4efd\uff0c\u8bf7\u91cd\u542f\u5e94\u7528" else "\u5907\u4efd\u5bfc\u5165\u5931\u8d25",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                ) {
                    Text("瀵煎叆")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text("鍙栨秷")
                }
            }
        )
    }
}
