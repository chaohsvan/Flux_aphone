package com.example.flux.feature.trash.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.flux.core.domain.diary.AttachmentKind
import com.example.flux.core.domain.diary.AttachmentReferenceSource
import com.example.flux.core.domain.diary.ManagedAttachment
import com.example.flux.feature.trash.domain.AttachmentDateFilter
import com.example.flux.feature.trash.domain.AttachmentReferenceFilter
import com.example.flux.feature.trash.domain.AttachmentSizeFilter
import com.example.flux.feature.trash.domain.AttachmentSortMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AttachmentSummary(
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
        Text("共 $totalCount 个附件，当前筛选 $filteredCount 个", style = MaterialTheme.typography.titleMedium)
        Text("总大小 ${formatSize(totalSizeBytes)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("已引用 $referencedCount 个 / ${formatSize(referencedSizeBytes)}", color = MaterialTheme.colorScheme.primary)
        Text("未引用 $unreferencedCount 个 / ${formatSize(unreferencedSizeBytes)}", color = MaterialTheme.colorScheme.error)
        if (freedSpaceBytes > 0) {
            Text("本次已释放 ${formatSize(freedSpaceBytes)}", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AttachmentSelectionActions(
    selectionMode: Boolean,
    selectedCount: Int,
    hasFilteredItems: Boolean,
    onToggleSelectionMode: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelection: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            OutlinedButton(onClick = onDeleteSelection, enabled = selectedCount > 0) {
                Text("删除 $selectedCount")
            }
        }
    }
}

@Composable
fun AttachmentKindChips(selected: AttachmentKind?, onSelected: (AttachmentKind?) -> Unit) {
    FilterRow {
        FilterChip(selected = selected == null, onClick = { onSelected(null) }, label = { Text("全部类型") })
        AttachmentKind.entries.forEach { kind ->
            FilterChip(selected = selected == kind, onClick = { onSelected(kind) }, label = { Text(kind.label()) })
        }
    }
}

@Composable
fun AttachmentReferenceChips(selected: AttachmentReferenceFilter, onSelected: (AttachmentReferenceFilter) -> Unit) {
    FilterRow {
        AttachmentReferenceFilter.entries.forEach { filter ->
            FilterChip(selected = selected == filter, onClick = { onSelected(filter) }, label = { Text(filter.label()) })
        }
    }
}

@Composable
fun AttachmentSizeChips(selected: AttachmentSizeFilter, onSelected: (AttachmentSizeFilter) -> Unit) {
    FilterRow {
        AttachmentSizeFilter.entries.forEach { filter ->
            FilterChip(selected = selected == filter, onClick = { onSelected(filter) }, label = { Text(filter.label()) })
        }
    }
}

@Composable
fun AttachmentDateChips(selected: AttachmentDateFilter, onSelected: (AttachmentDateFilter) -> Unit) {
    FilterRow {
        AttachmentDateFilter.entries.forEach { filter ->
            FilterChip(selected = selected == filter, onClick = { onSelected(filter) }, label = { Text(filter.label()) })
        }
    }
}

@Composable
fun AttachmentSortChips(selected: AttachmentSortMode, onSelected: (AttachmentSortMode) -> Unit) {
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
            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

@Composable
fun AttachmentRow(
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
            Text(text = attachment.file.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = attachment.relativePath, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        color = if (source.isDeleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(enabled = !source.isDeleted) { onOpenDiary(source.diaryId) }
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
                        color = if (attachment.isReferenced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = onOpenAttachment) { Text("打开") }
                    TextButton(onClick = onDelete) { Text("删除") }
                }
            }
        }
    )
}

@Composable
fun DeleteAttachmentDialog(
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
fun AttachmentReferencesSheet(
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
            Text(attachment.file.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
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
                        color = if (source.isDeleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
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

fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(Locale.US, kb)
    return "%.1f MB".format(Locale.US, kb / 1024.0)
}

private fun formatDateTime(timestamp: Long): String {
    if (timestamp <= 0L) return "-"
    return SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))
}
