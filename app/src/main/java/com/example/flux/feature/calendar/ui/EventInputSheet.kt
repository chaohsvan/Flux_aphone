package com.example.flux.feature.calendar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.util.RecurrenceUtil
import com.example.flux.core.util.TimeUtil

data class CalendarEventInput(
    val title: String,
    val description: String,
    val startAt: String,
    val endAt: String,
    val allDay: Boolean,
    val color: String,
    val locationName: String?,
    val reminderMinutes: Int?,
    val recurrenceRule: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventInputSheet(
    targetDate: String,
    event: CalendarEventEntity? = null,
    onDismiss: () -> Unit,
    onSubmit: (CalendarEventInput) -> Unit
) {
    val colorOptions = listOf("#4A90E2", "#E57373", "#66BB6A", "#FFB74D")
    val recurrenceOptions = listOf("none", "daily", "weekly", "monthly", "yearly")
    val initialRecurrence = remember(event?.id) { RecurrenceUtil.parseSpec(event?.recurrenceRule) }
    var title by remember { mutableStateOf(event?.title.orEmpty()) }
    var description by remember { mutableStateOf(event?.description.orEmpty()) }
    var startTimeText by remember { mutableStateOf(event?.startAt?.takeIf { it.length >= 16 }?.substring(11, 16) ?: "09:00") }
    var endTimeText by remember { mutableStateOf(event?.endAt?.takeIf { it.length >= 16 }?.substring(11, 16) ?: startTimeText) }
    var allDay by remember { mutableStateOf(event?.allDay == 1) }
    var color by remember { mutableStateOf(event?.color ?: colorOptions.first()) }
    var location by remember { mutableStateOf(event?.locationName.orEmpty()) }
    var reminderMinutesText by remember { mutableStateOf(event?.reminderMinutes?.toString().orEmpty()) }
    var recurrenceRule by remember { mutableStateOf(initialRecurrence?.rule ?: "none") }
    var recurrenceIntervalText by remember { mutableStateOf(initialRecurrence?.interval?.toString() ?: "1") }
    var recurrenceUntil by remember { mutableStateOf(initialRecurrence?.until.orEmpty()) }
    val isTimeValid = allDay || (startTimeText.isValidClockTime() && endTimeText.isValidClockTime())
    val isRecurrenceUntilValid = recurrenceUntil.isBlank() || TimeUtil.isValidDate(recurrenceUntil)
    val canSave = title.isNotBlank() && isTimeValid && isRecurrenceUntilValid

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (event == null) "新建事件 - $targetDate" else "编辑事件 - $targetDate",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("事件标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("描述") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("位置") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = startTimeText,
                    onValueChange = { startTimeText = it },
                    label = { Text("开始") },
                    placeholder = { Text("HH:mm") },
                    modifier = Modifier.weight(1f),
                    enabled = !allDay,
                    singleLine = true
                )
                OutlinedTextField(
                    value = endTimeText,
                    onValueChange = { endTimeText = it },
                    label = { Text("结束") },
                    placeholder = { Text("HH:mm") },
                    modifier = Modifier.weight(1f),
                    enabled = !allDay,
                    singleLine = true
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = allDay, onCheckedChange = { allDay = it })
                Text("全天")
            }

            OutlinedTextField(
                value = reminderMinutesText,
                onValueChange = { reminderMinutesText = it.filter(Char::isDigit).take(5) },
                label = { Text("提前提醒分钟") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("重复规则", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(recurrenceOptions) { option ->
                    FilterChip(
                        selected = recurrenceRule == option,
                        onClick = {
                            recurrenceRule = option
                            if (option == "none") {
                                recurrenceIntervalText = "1"
                                recurrenceUntil = ""
                            }
                        },
                        label = { Text(RecurrenceUtil.label(option)) }
                    )
                }
            }
            if (recurrenceRule != "none") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = recurrenceIntervalText,
                        onValueChange = { recurrenceIntervalText = it.filter(Char::isDigit).take(3) },
                        label = { Text("重复间隔") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = recurrenceUntil,
                        onValueChange = { recurrenceUntil = it.take(10) },
                        label = { Text("重复截止") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                if (!isRecurrenceUntilValid) {
                    Text(
                        text = "重复截止日期应为 YYYY-MM-DD",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (!isTimeValid) {
                Text(
                    text = "时间应为 HH:mm",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text("颜色", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(colorOptions) { option ->
                    FilterChip(
                        selected = color == option,
                        onClick = { color = option },
                        label = { Text(" ") },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .height(12.dp)
                                    .width(12.dp)
                                    .background(Color(android.graphics.Color.parseColor(option)), CircleShape)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("取消") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (!canSave) return@Button
                        val startAt = if (allDay) "${targetDate}T00:00:00" else "${targetDate}T${startTimeText}:00"
                        val endAt = if (allDay) "${targetDate}T23:59:00" else "${targetDate}T${endTimeText}:00"
                        val recurrence = RecurrenceUtil.buildSpec(
                            rule = recurrenceRule,
                            interval = recurrenceIntervalText.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            until = recurrenceUntil.trim().ifBlank { null }
                        )
                        onSubmit(
                            CalendarEventInput(
                                title = title.trim(),
                                description = description.trim(),
                                startAt = startAt,
                                endAt = endAt,
                                allDay = allDay,
                                color = color,
                                locationName = location.trim().ifBlank { null },
                                reminderMinutes = reminderMinutesText.toIntOrNull(),
                                recurrenceRule = recurrence
                            )
                        )
                    },
                    enabled = canSave
                ) {
                    Text("保存")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun String.isValidClockTime(): Boolean {
    val match = Regex("(\\d{2}):(\\d{2})").matchEntire(this) ?: return false
    val hour = match.groupValues[1].toInt()
    val minute = match.groupValues[2].toInt()
    return hour in 0..23 && minute in 0..59
}
