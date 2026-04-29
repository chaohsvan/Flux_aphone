package com.example.flux.feature.settings.presentation

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.flux.core.database.entity.CalendarSubscriptionEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSubscriptionSettingsScreen(
    onNavigateUp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingSubscription by remember { mutableStateOf<CalendarSubscriptionEntity?>(null) }
    var deletingSubscription by remember { mutableStateOf<CalendarSubscriptionEntity?>(null) }
    var subscriptionName by remember { mutableStateOf("") }
    var subscriptionUrl by remember { mutableStateOf("") }

    fun openEditor(subscription: CalendarSubscriptionEntity?) {
        editingSubscription = subscription
        subscriptionName = subscription?.name.orEmpty()
        subscriptionUrl = subscription?.icsUrl.orEmpty()
        showDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("ICS 日历订阅") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { openEditor(null) },
                content = {
                    Icon(Icons.Default.Add, contentDescription = "添加 ICS 订阅")
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
            if (uiState.calendarSubscriptions.isEmpty()) {
                Text(
                    text = "还没有订阅的 ICS 日历",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                uiState.calendarSubscriptions.forEach { subscription ->
                    CalendarSubscriptionRow(
                        subscription = subscription,
                        isSyncing = uiState.isSyncingCalendar,
                        onSync = {
                            viewModel.syncCalendarSubscription(subscription.id) { success, message ->
                                Toast.makeText(context, message, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                            }
                        },
                        onEdit = { openEditor(subscription) },
                        onToggleEnabled = {
                            viewModel.setCalendarSubscriptionEnabled(
                                subscription.id,
                                subscription.enabled != 1
                            ) { success, message ->
                                Toast.makeText(context, message, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                            }
                        },
                        onDelete = { deletingSubscription = subscription }
                    )
                }
            }
        }
    }

    if (showDialog) {
        val editing = editingSubscription
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (editing == null) "添加 ICS 日历订阅" else "编辑 ICS 日历订阅") },
            text = {
                Column {
                    OutlinedTextField(
                        value = subscriptionName,
                        onValueChange = { subscriptionName = it },
                        label = { Text("名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subscriptionUrl,
                        onValueChange = { subscriptionUrl = it },
                        label = { Text("ICS 地址") },
                        minLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !uiState.isSyncingCalendar,
                    onClick = {
                        if (editing == null) {
                            viewModel.addCalendarSubscription(subscriptionName, subscriptionUrl) { success, message ->
                                Toast.makeText(context, message, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                                if (success) {
                                    subscriptionName = ""
                                    subscriptionUrl = ""
                                    showDialog = false
                                }
                            }
                        } else {
                            viewModel.updateCalendarSubscription(editing.id, subscriptionName, subscriptionUrl) { success, message ->
                                Toast.makeText(context, message, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                                if (success) {
                                    subscriptionName = ""
                                    subscriptionUrl = ""
                                    editingSubscription = null
                                    showDialog = false
                                }
                            }
                        }
                    }
                ) {
                    Text(
                        when {
                            uiState.isSyncingCalendar -> "同步中..."
                            editing == null -> "添加并同步"
                            else -> "保存"
                        }
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    deletingSubscription?.let { subscription ->
        AlertDialog(
            onDismissRequest = { deletingSubscription = null },
            title = { Text("删除 ICS 订阅") },
            text = { Text("将删除「${subscription.name}」以及它导入到本地日历中的事件。此操作不会影响原始日历。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCalendarSubscription(subscription.id) { success, message ->
                            Toast.makeText(context, message, if (success) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
                            if (success) deletingSubscription = null
                        }
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingSubscription = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
private fun CalendarSubscriptionRow(
    subscription: CalendarSubscriptionEntity,
    isSyncing: Boolean,
    onSync: () -> Unit,
    onEdit: () -> Unit,
    onToggleEnabled: () -> Unit,
    onDelete: () -> Unit
) {
    val enabled = subscription.enabled == 1
    val status = buildList {
        add(if (enabled) "已启用" else "已停用")
        subscription.lastSyncTime?.let { add("上次同步：$it") }
        subscription.lastError?.let { add("错误：$it") }
    }.joinToString(" / ")

    ListItem(
        headlineContent = { Text(subscription.name) },
        supportingContent = { Text(status.ifBlank { "尚未同步" }) },
        leadingContent = {
            Icon(
                Icons.Default.Refresh,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingContent = {
            Column {
                Row {
                    TextButton(enabled = enabled && !isSyncing, onClick = onSync) {
                        Text("同步")
                    }
                    TextButton(onClick = onEdit) {
                        Text("编辑")
                    }
                }
                Row {
                    TextButton(onClick = onToggleEnabled) {
                        Text(if (enabled) "停用" else "启用")
                    }
                    TextButton(onClick = onDelete) {
                        Text("删除")
                    }
                }
            }
        }
    )
}
