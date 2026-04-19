package com.example.flux.feature.calendar.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventInputSheet(
    targetDate: String,
    onDismiss: () -> Unit,
    onSubmit: (title: String, startAt: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var timeStr by remember { mutableStateOf("09:00") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("新建事件 - $targetDate", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("事件标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = timeStr,
                onValueChange = { timeStr = it },
                label = { Text("时间 (HH:mm)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank() && timeStr.isNotBlank()) {
                            val startAt = "${targetDate}T${timeStr}:00"
                            onSubmit(title, startAt)
                        }
                    },
                    enabled = title.isNotBlank()
                ) {
                    Text("保存")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
