package com.example.flux.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    allowClear: Boolean = true
) {
    var showPicker by remember { mutableStateOf(false) }
    val state = rememberDatePickerState(initialSelectedDateMillis = value.toDatePickerMillis())

    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        placeholder = { Text("YYYY-MM-DD") },
        readOnly = true,
        singleLine = true,
        modifier = modifier.clickable { showPicker = true },
        trailingIcon = {
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                if (allowClear && value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "清空$label")
                    }
                }
                IconButton(onClick = { showPicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "选择$label")
                }
            }
        }
    )

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { onValueChange(it.formatDatePickerMillis()) }
                        showPicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    allowClear: Boolean = true
) {
    var showPicker by remember { mutableStateOf(false) }
    val initialHour = value.takeIf { it.length >= 5 }?.substring(0, 2)?.toIntOrNull() ?: 9
    val initialMinute = value.takeIf { it.length >= 5 }?.substring(3, 5)?.toIntOrNull() ?: 0
    val state = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = true
    )

    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        placeholder = { Text("HH:mm") },
        readOnly = true,
        enabled = enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier.clickable(enabled = enabled) { showPicker = true },
        trailingIcon = {
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                if (allowClear && value.isNotBlank()) {
                    IconButton(onClick = { onValueChange("") }, enabled = enabled) {
                        Icon(Icons.Default.Clear, contentDescription = "清空$label")
                    }
                }
                IconButton(onClick = { showPicker = true }, enabled = enabled) {
                    Icon(Icons.Default.AccessTime, contentDescription = "选择$label")
                }
            }
        }
    )

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onValueChange("%02d:%02d".format(Locale.US, state.hour, state.minute))
                        showPicker = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun DateTimeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    allowClear: Boolean = true
) {
    val date = value.takeIf { it.length >= 10 }?.take(10).orEmpty()
    val time = value.takeIf { it.length >= 16 }?.substring(11, 16).orEmpty()

    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DateField(
            value = date,
            onValueChange = { newDate ->
                onValueChange(
                    when {
                        newDate.isBlank() -> ""
                        time.isBlank() -> newDate
                        else -> "$newDate $time"
                    }
                )
            },
            label = label,
            modifier = Modifier.weight(1f),
            allowClear = allowClear
        )
        TimeField(
            value = time,
            onValueChange = { newTime ->
                val targetDate = date.ifBlank { today() }
                onValueChange(if (newTime.isBlank()) targetDate else "$targetDate $newTime")
            },
            label = "时间",
            modifier = Modifier.weight(1f),
            allowClear = false
        )
    }
}

private fun String.toDatePickerMillis(): Long? {
    if (!Regex("\\d{4}-\\d{2}-\\d{2}").matches(this)) return null
    return runCatching {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(this)?.time
    }.getOrNull()
}

private fun Long.formatDatePickerMillis(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(this)
}

private fun today(): String {
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
}
