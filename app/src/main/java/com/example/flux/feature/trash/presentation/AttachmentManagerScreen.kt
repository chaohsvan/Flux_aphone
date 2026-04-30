package com.example.flux.feature.trash.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.flux.core.domain.diary.ManagedAttachment
import com.example.flux.core.util.AttachmentOpener
import com.example.flux.feature.trash.presentation.component.AttachmentDateChips
import com.example.flux.feature.trash.presentation.component.AttachmentKindChips
import com.example.flux.feature.trash.presentation.component.AttachmentReferenceChips
import com.example.flux.feature.trash.presentation.component.AttachmentReferencesSheet
import com.example.flux.feature.trash.presentation.component.AttachmentRow
import com.example.flux.feature.trash.presentation.component.AttachmentSelectionActions
import com.example.flux.feature.trash.presentation.component.AttachmentSizeChips
import com.example.flux.feature.trash.presentation.component.AttachmentSortChips
import com.example.flux.feature.trash.presentation.component.AttachmentSummary
import com.example.flux.feature.trash.presentation.component.DeleteAttachmentDialog
import com.example.flux.feature.trash.presentation.component.formatSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentManagerScreen(
    onNavigateUp: () -> Unit,
    onOpenDiary: (String) -> Unit,
    initialQuery: String = "",
    viewModel: AttachmentManagerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val attachments = uiState.attachments
    val filteredAttachments = uiState.filteredAttachments
    val filterState = uiState.filterState
    val selectionMode = uiState.selectionMode
    val selectedPaths = uiState.selectedPaths

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
        if (uiState.isScanning) {
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            item {
                AttachmentSummary(
                    totalCount = attachments.size,
                    filteredCount = filteredAttachments.size,
                    referencedCount = referencedCount,
                    unreferencedCount = unreferencedCount,
                    totalSizeBytes = attachments.sumOf { it.sizeBytes },
                    referencedSizeBytes = referencedSizeBytes,
                    unreferencedSizeBytes = unreferencedSizeBytes,
                    freedSpaceBytes = uiState.freedSpaceBytes
                )

                Spacer(modifier = Modifier.height(8.dp))
                AttachmentKindChips(selected = filterState.kind, onSelected = viewModel::setKindFilter)
                OutlinedTextField(
                    value = filterState.query,
                    onValueChange = viewModel::updateSearchQuery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    placeholder = { Text("搜索附件名、路径、哈希或引用内容") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "搜索") }
                )
                AttachmentReferenceChips(selected = filterState.reference, onSelected = viewModel::setReferenceFilter)
                AttachmentSizeChips(selected = filterState.size, onSelected = viewModel::setSizeFilter)
                AttachmentDateChips(selected = filterState.date, onSelected = viewModel::setDateFilter)
                AttachmentSortChips(selected = filterState.sort, onSelected = viewModel::setSortMode)

                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    AttachmentSelectionActions(
                        selectionMode = selectionMode,
                        selectedCount = selectedPaths.size,
                        hasFilteredItems = filteredAttachments.isNotEmpty(),
                        onToggleSelectionMode = viewModel::toggleSelectionMode,
                        onSelectAll = viewModel::selectAllFiltered,
                        onClearSelection = viewModel::clearSelection,
                        onDeleteSelection = { pendingDeleteSelected = true }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
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

                Spacer(modifier = Modifier.height(12.dp))
            }
            if (filteredAttachments.isEmpty()) {
                item {
                    Text(
                        text = if (attachments.isEmpty()) "暂无本地附件" else "当前筛选下没有附件",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
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
