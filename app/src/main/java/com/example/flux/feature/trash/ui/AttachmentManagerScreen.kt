package com.example.flux.feature.trash.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flux.core.domain.diary.AttachmentKind
import com.example.flux.core.domain.diary.AttachmentReferenceSource
import com.example.flux.core.domain.diary.ManagedAttachment
import com.example.flux.core.util.AttachmentOpener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentManagerScreen(
    onNavigateUp: () -> Unit,
    onOpenDiary: (String) -> Unit,
    initialQuery: String = "",
    viewModel: AttachmentManagerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val attachments by viewModel.attachments.collectAsState()
    val filteredAttachments by viewModel.filteredAttachments.collectAsState()
    val kindFilter by viewModel.kindFilter.collectAsState()
    val referenceFilter by viewModel.referenceFilter.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sizeFilter by viewModel.sizeFilter.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val selectionMode by viewModel.selectionMode.collectAsState()
    val selectedPaths by viewModel.selectedPaths.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val freedSpaceBytes by viewModel.freedSpaceBytes.collectAsState()

    val referencedCount = attachments.count { it.isReferenced }
    val unreferencedCount = attachments.size - referencedCount
    val referencedSizeBytes = attachments.filter { it.isReferenced }.sumOf { it.sizeBytes }
    val unreferencedSizeBytes = attachments.filterNot { it.isReferenced }.sumOf { it.sizeBytes }
    val selectedAttachments = attachments.filter { it.relativePath in selectedPaths }
    val selectedReferencedCount = selectedAttachments.count { it.isReferenced }

    var referenceSheetAttachment by remember { mutableStateOf<ManagedAttachment?>(null) }
    var pendingDeleteAttachment by remember { mutableStateOf<ManagedAttachment?>(null) }
    var pendingDeleteSelected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.scan(context)
    }

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank()) {
            viewModel.updateSearchQuery(initialQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("附件管理") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isScanning) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("正在扫描附件...")
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AttachmentSummary(
                totalCount = attachments.size,
                filteredCount = filteredAttachments.size,
                referencedCount = referencedCount,
                unreferencedCount = unreferencedCount,
                totalSizeBytes = attachments.sumOf { it.sizeBytes },
                referencedSizeBytes = referencedSizeBytes,
                unreferencedSizeBytes = unreferencedSizeBytes,
                freedSpaceBytes = freedSpaceBytes
            )

            AttachmentKindChips(selected = kindFilter, onSelected = viewModel::setKindFilter)

            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("搜索附件名、路径、哈希或引用内容") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") }
            )

            AttachmentReferenceChips(selected = referenceFilter, onSelected = viewModel::setReferenceFilter)
            AttachmentSizeChips(selected = sizeFilter, onSelected = viewModel::setSizeFilter)
            AttachmentDateChips(selected = dateFilter, onSelected = viewModel::setDateFilter)
            AttachmentSortChips(selected = sortMode, onSelected = viewModel::setSortMode)

            AttachmentSelectionActions(
                selectionMode = selectionMode,
                selectedCount = selectedPaths.size,
                hasFilteredItems = filteredAttachments.isNotEmpty(),
                onToggleSelectionMode = viewModel::toggleSelectionMode,
                onSelectAll = viewModel::selectAllFiltered,
                onClearSelection = viewModel::clearSelection,
                onDeleteSelection = { pendingDeleteSelected = true }
            )

            Button(
                onClick = {
                    viewModel.clean(context) { freed ->
                        val message = if (freed > 0) {
                            "已清理未引用附件，释放 ${formatSize(freed)}"
                        } else {
                            "没有可清理的未引用附件"
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = unreferencedCount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("清理未引用附件")
            }

            if (filteredAttachments.isEmpty()) {
                Text(
                    text = if (attachments.isEmpty()) "暂无本地附件" else "当前筛选下没有附件",
                    style = MaterialTheme.typography.bodyLarge
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filteredAttachments, key = { it.relativePath }) { attachment ->
                        AttachmentRow(
                            attachment = attachment,
                            selectionMode = selectionMode,
                            selected = attachment.relativePath in selectedPaths,
                            onToggleSelection = { viewModel.toggleSelection(attachment.relativePath) },
                            onOpenDiary = onOpenDiary,
                            onOpenAttachment = {
                                if (!AttachmentOpener.open(context, attachment.file)) {
                                    Toast.makeText(context, "无法打开附件", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onShowReferences = { referenceSheetAttachment = attachment },
                            onDelete = {
                                if (attachment.isReferenced) {
                                    pendingDeleteAttachment = attachment
                                } else {
                                    viewModel.deleteOne(attachment) { freed ->
                                        if (freed > 0) {
                                            Toast.makeText(context, "已删除未引用附件", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    referenceSheetAttachment?.let { attachment ->
        AttachmentReferencesSheet(
            attachment = attachment,
            onOpenDiary = onOpenDiary,
            onDismiss = { referenceSheetAttachment = null }
        )
    }

    pendingDeleteAttachment?.let { attachment ->
        DeleteAttachmentDialog(
            title = "删除已引用附件",
            message = "这个附件被 ${attachment.references.size} 处日记引用。删除文件不会修改 Markdown，原位置会变成失效链接。",
            onDismiss = { pendingDeleteAttachment = null },
            onConfirm = {
                viewModel.deleteOne(attachment, allowReferenced = true) { freed ->
                    if (freed > 0) {
                        Toast.makeText(context, "已删除附件，原引用会保留为失效链接", Toast.LENGTH_LONG).show()
                    }
                }
                pendingDeleteAttachment = null
            }
        )
    }

    if (pendingDeleteSelected) {
        DeleteAttachmentDialog(
            title = "批量删除附件",
            message = buildString {
                append("将删除 ${selectedPaths.size} 个附件")
                if (selectedReferencedCount > 0) {
                    append("，其中 ${selectedReferencedCount} 个仍被日记引用，删除后会留下失效链接")
                }
                append("。")
            },
            onDismiss = { pendingDeleteSelected = false },
            onConfirm = {
                viewModel.deleteSelected(context, allowReferenced = true) { result ->
                    val message = buildString {
                        append("已删除 ${result.deletedCount} 个附件")
                        if (result.freedSpaceBytes > 0) {
                            append("，释放 ${formatSize(result.freedSpaceBytes)}")
                        }
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
                pendingDeleteSelected = false
            }
        )
    }
}

@Composable
private fun AttachmentSummary(
    totalCount: Int,
    filteredCount: Int,
    referencedCount: Int,
    unreferencedCount: Int,
    totalSizeBytes: Long,
    referencedSizeBytes: Long,
    unreferencedSizeBytes: Long,
    freedSpaceBytes: Long
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "共 $totalCount 个附件，当前显示 $filteredCount 个",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "已引用 $referencedCount 个，未引用 $unreferencedCount 个",
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "占用 ${formatSize(totalSizeBytes)}" +
                if (freedSpaceBytes > 0) "，本次已释放 ${formatSize(freedSpaceBytes)}" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "已引用 ${formatSize(referencedSizeBytes)} / 未引用 ${formatSize(unreferencedSizeBytes)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AttachmentSelectionActions(
    selectionMode: Boolean,
    selectedCount: Int,
    hasFilteredItems: Boolean,
    onToggleSelectionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelection: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onToggleSelectionMode) {
            Text(if (selectionMode) "退出多选" else "多选")
        }
        if (selectionMode) {
            OutlinedButton(onClick = onSelectAll, enabled = hasFilteredItems) {
                Text("全选")
            }
            OutlinedButton(onClick = onClearSelection, enabled = selectedCount > 0) {
                Text("清空")
            }
            Button(
                onClick = onDeleteSelection,
                enabled = selectedCount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("删除选中 ($selectedCount)")
            }
        }
    }
}

@Composable
private fun AttachmentKindChips(
    selected: AttachmentKind?,
    onSelected: (AttachmentKind?) -> Unit
) {
    FilterRow {
        FilterChip(selected = selected == null, onClick = { onSelected(null) }, label = { Text("全部类型") })
        AttachmentKind.entries.forEach { kind ->
            FilterChip(selected = selected == kind, onClick = { onSelected(kind) }, label = { Text(kind.label()) })
        }
    }
}

@Composable
private fun AttachmentReferenceChips(
    selected: AttachmentReferenceFilter,
    onSelected: (AttachmentReferenceFilter) -> Unit
) {
    FilterRow {
        AttachmentReferenceFilter.entries.forEach { filter ->
            FilterChip(selected = selected == filter, onClick = { onSelected(filter) }, label = { Text(filter.label()) })
        }
    }
}

@Composable
private fun AttachmentSizeChips(
    selected: AttachmentSizeFilter,
    onSelected: (AttachmentSizeFilter) -> Unit
) {
    FilterRow {
        AttachmentSizeFilter.entries.forEach { filter ->
            FilterChip(selected = selected == filter, onClick = { onSelected(filter) }, label = { Text(filter.label()) })
        }
    }
}

@Composable
private fun AttachmentDateChips(
    selected: AttachmentDateFilter,
    onSelected: (AttachmentDateFilter) -> Unit
) {
    FilterRow {
        AttachmentDateFilter.entries.forEach { filter ->
            FilterChip(selected = selected == filter, onClick = { onSelected(filter) }, label = { Text(filter.label()) })
        }
    }
}

@Composable
private fun AttachmentSortChips(
    selected: AttachmentSortMode,
    onSelected: (AttachmentSortMode) -> Unit
) {
    FilterRow {
        AttachmentSortMode.entries.forEach { mode ->
            FilterChip(selected = selected == mode, onClick = { onSelected(mode) }, label = { Text(mode.label()) })
        }
    }
}

@Composable
private fun FilterRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
private fun AttachmentRow(
    attachment: ManagedAttachment,
    selectionMode: Boolean,
    selected: Boolean,
    onToggleSelection: () -> Unit,
    onOpenDiary: (String) -> Unit,
    onOpenAttachment: () -> Unit,
    onShowReferences: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        modifier = if (selectionMode) Modifier.clickable(onClick = onToggleSelection) else Modifier,
        leadingContent = {
            if (selectionMode) {
                Checkbox(checked = selected, onCheckedChange = { onToggleSelection() })
            }
        },
        headlineContent = {
            Text(
                text = attachment.file.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = attachment.relativePath,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("${attachment.kind.label()} / ${formatSize(attachment.sizeBytes)} / ${formatDateTime(attachment.modifiedAt)}")
                Text(
                    text = "SHA-256 ${attachment.sha256.take(12)}...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                attachment.references.take(2).forEach { source ->
                    Text(
                        text = source.label(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (source.isDeleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.clickable(enabled = !source.isDeleted) {
                            onOpenDiary(source.diaryId)
                        }
                    )
                }
                if (attachment.references.size > 2) {
                    TextButton(onClick = onShowReferences) {
                        Text("查看全部 ${attachment.references.size} 处引用")
                    }
                }
            }
        },
        trailingContent = {
            if (!selectionMode) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (attachment.isReferenced) "已引用 ${attachment.references.size}" else "未引用",
                        color = if (attachment.isReferenced) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onOpenAttachment) {
                        Text("打开")
                    }
                    TextButton(onClick = onDelete) {
                        Text("删除")
                    }
                }
            }
        }
    )
}

@Composable
private fun DeleteAttachmentDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AttachmentReferencesSheet(
    attachment: ManagedAttachment,
    onOpenDiary: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = attachment.file.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${attachment.relativePath} / ${formatSize(attachment.sizeBytes)} / ${formatDateTime(attachment.modifiedAt)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "SHA-256 ${attachment.sha256}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider()
            attachment.references.forEach { source ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !source.isDeleted) {
                            onDismiss()
                            onOpenDiary(source.diaryId)
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = source.label(),
                        color = if (source.isDeleted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                    if (source.snippet.isNotBlank()) {
                        Text(
                            text = source.snippet,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun AttachmentReferenceFilter.label(): String {
    return when (this) {
        AttachmentReferenceFilter.ALL -> "全部引用状态"
        AttachmentReferenceFilter.REFERENCED -> "已引用"
        AttachmentReferenceFilter.UNREFERENCED -> "未引用"
    }
}

private fun AttachmentSortMode.label(): String {
    return when (this) {
        AttachmentSortMode.NAME -> "按名称"
        AttachmentSortMode.SIZE_DESC -> "体积从大到小"
        AttachmentSortMode.SIZE_ASC -> "体积从小到大"
        AttachmentSortMode.DATE_DESC -> "最近修改"
        AttachmentSortMode.DATE_ASC -> "最早修改"
        AttachmentSortMode.REFERENCE_COUNT -> "引用最多"
    }
}

private fun AttachmentSizeFilter.label(): String {
    return when (this) {
        AttachmentSizeFilter.ALL -> "全部大小"
        AttachmentSizeFilter.UNDER_1_MB -> "1MB 以下"
        AttachmentSizeFilter.MB_1_TO_10 -> "1MB - 10MB"
        AttachmentSizeFilter.ABOVE_10_MB -> "10MB 以上"
    }
}

private fun AttachmentDateFilter.label(): String {
    return when (this) {
        AttachmentDateFilter.ALL -> "全部日期"
        AttachmentDateFilter.LAST_7_DAYS -> "7 天内"
        AttachmentDateFilter.LAST_30_DAYS -> "30 天内"
        AttachmentDateFilter.THIS_YEAR -> "今年"
        AttachmentDateFilter.BEFORE_THIS_YEAR -> "今年以前"
    }
}

private fun AttachmentKind.label(): String {
    return when (this) {
        AttachmentKind.IMAGE -> "图片"
        AttachmentKind.AUDIO -> "音频"
        AttachmentKind.FILE -> "文件"
    }
}

private fun AttachmentReferenceSource.label(): String {
    val timeText = diaryTime?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""
    val deletedText = if (isDeleted) "（回收站）" else ""
    return "引用：$diaryDate$timeText / 第 $lineNumber 行 $deletedText $snippet"
}

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(Locale.US, kb)
    return "%.1f MB".format(Locale.US, kb / 1024.0)
}

private fun formatDateTime(timestamp: Long): String {
    if (timestamp <= 0L) return "-"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
