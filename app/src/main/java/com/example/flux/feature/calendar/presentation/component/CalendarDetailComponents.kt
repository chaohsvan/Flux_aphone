package com.example.flux.feature.calendar.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.flux.core.database.entity.CalendarEventEntity
import com.example.flux.core.database.entity.TodoEntity
import com.example.flux.core.util.TimeUtil
import com.example.flux.feature.calendar.presentation.CalendarDateDetails
import com.example.flux.feature.calendar.presentation.CalendarMonthHistory
import com.example.flux.ui.theme.FluxDiaryYellow
import com.example.flux.ui.theme.FluxHolidayOrange
import com.example.flux.ui.theme.FluxTodoRed

@Composable
fun CalendarDayTimelineView(
    selectedDate: String,
    details: CalendarDateDetails?,
    showEvents: Boolean,
    showTrash: Boolean,
    onWriteDiary: (String?) -> Unit,
    onAddTodo: () -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (CalendarEventEntity) -> Unit,
    onDeleteEvent: (String, String) -> Unit,
    onRestoreDiary: (String) -> Unit,
    onRestoreTodo: (String) -> Unit,
    onRestoreEvent: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                Text(selectedDate, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (details?.holidayLabel != null) {
                    Text(
                        text = details.holidayLabel,
                        color = if (details.isHoliday) FluxHolidayOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = { onWriteDiary(details?.diary?.id) }, modifier = Modifier.weight(1f)) {
                        Text(if (details?.diary == null) "写日记" else "打开日记")
                    }
                    Button(onClick = onAddTodo, modifier = Modifier.weight(1f)) {
                        Text("加待办")
                    }
                    Button(onClick = onAddEvent, modifier = Modifier.weight(1f)) {
                        Text("加事件")
                    }
                }
            }
        }

        if (details == null) {
            item {
                Text(
                    text = "正在加载这一天的安排...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
            return@LazyColumn
        }

        val allDayEvents = if (showEvents) details.events.filter { it.allDay == 1 } else emptyList()
        if (details.diary != null || allDayEvents.isNotEmpty()) {
            item {
                Text("全天", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            details.diary?.let { diary ->
                item {
                    TimelineRow(
                        time = "日记",
                        title = diary.title.ifBlank { "无标题日记" },
                        subtitle = diary.contentMd.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty(),
                        color = FluxDiaryYellow,
                        onClick = { onWriteDiary(diary.id) }
                    )
                }
            }
            items(allDayEvents, key = { it.id }) { event ->
                TimelineRow(
                    time = "全天",
                    title = event.title,
                    subtitle = event.eventSubtitle(),
                    color = event.safeColor(),
                    onClick = { onEditEvent(event) },
                    trailing = {
                        IconButton(onClick = { onDeleteEvent(event.id, selectedDate) }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除事件", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        }

        val deletedCount = details.deletedDiaries.size + details.deletedTodos.size + details.deletedEvents.size
        if (showTrash && deletedCount > 0) {
            item {
                Text("回收站", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            details.deletedDiaries.forEach { diary ->
                item(key = "deleted_diary:${diary.id}") {
                    TimelineRow(
                        time = "删",
                        title = diary.title.ifBlank { "无标题日记" },
                        subtitle = diary.entryDate,
                        color = Color.Gray,
                        textDecoration = TextDecoration.LineThrough,
                        trailing = {
                            TextButton(onClick = { onRestoreDiary(diary.id) }) {
                                Text("恢复")
                            }
                        }
                    )
                }
            }
            details.deletedTodos.forEach { todo ->
                item(key = "deleted_todo:${todo.id}") {
                    TimelineRow(
                        time = "删",
                        title = todo.title,
                        subtitle = todo.dueAt ?: TimeUtil.formatTimestampForDisplay(todo.createdAt),
                        color = Color.Gray,
                        textDecoration = TextDecoration.LineThrough,
                        trailing = {
                            TextButton(onClick = { onRestoreTodo(todo.id) }) {
                                Text("恢复")
                            }
                        }
                    )
                }
            }
            details.deletedEvents.forEach { event ->
                item(key = "deleted_event:${event.id}") {
                    TimelineRow(
                        time = "删",
                        title = event.title,
                        subtitle = event.startAt,
                        color = Color.Gray,
                        textDecoration = TextDecoration.LineThrough,
                        trailing = {
                            TextButton(onClick = { onRestoreEvent(event.id) }) {
                                Text("恢复")
                            }
                        }
                    )
                }
            }
        }

        val timedEvents = if (showEvents) details.events.filterNot { it.allDay == 1 } else emptyList()
        val timelineItems = buildList {
            timedEvents.forEach { event ->
                add(CalendarTimelineEntry.Event(event.timelineTime(), event))
            }
            details.todos.forEach { todo ->
                add(CalendarTimelineEntry.Todo(todo.timelineTime(), todo))
            }
        }.sortedWith(compareBy<CalendarTimelineEntry> { it.sortKey }.thenBy { it.title })

        item {
            Text("时间轴", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        if (timelineItems.isEmpty()) {
            item {
                Text(
                    text = "这一天还没有定时安排。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            items(timelineItems, key = { it.key }) { entry ->
                when (entry) {
                    is CalendarTimelineEntry.Event -> TimelineRow(
                        time = entry.time,
                        title = entry.event.title,
                        subtitle = entry.event.eventSubtitle(),
                        color = entry.event.safeColor(),
                        onClick = { onEditEvent(entry.event) },
                        trailing = {
                            IconButton(onClick = { onDeleteEvent(entry.event.id, selectedDate) }) {
                                Icon(Icons.Default.Delete, contentDescription = "删除事件", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    )

                    is CalendarTimelineEntry.Todo -> TimelineRow(
                        time = entry.time,
                        title = entry.todo.title,
                        subtitle = entry.todo.todoSubtitle(),
                        color = if (entry.todo.status == "completed") Color.Gray else FluxTodoRed,
                        textDecoration = if (entry.todo.status == "completed") TextDecoration.LineThrough else null
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineRow(
    time: String,
    title: String,
    subtitle: String,
    color: Color,
    textDecoration: TextDecoration? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = time,
            modifier = Modifier.width(58.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, textDecoration = textDecoration)
            if (subtitle.isNotBlank()) {
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}

private sealed class CalendarTimelineEntry(
    val key: String,
    val time: String,
    val sortKey: String,
    val title: String
) {
    class Event(time: String, val event: CalendarEventEntity) :
        CalendarTimelineEntry("event:${event.id}", time, time.sortableTime(), event.title)

    class Todo(time: String, val todo: TodoEntity) :
        CalendarTimelineEntry("todo:${todo.id}", time, time.sortableTime(), todo.title)
}

@Composable
fun CalendarMonthHistorySheet(
    selectedDate: String,
    history: CalendarMonthHistory
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        val month = selectedDate.takeIf { it.length >= 7 }?.substring(5, 7).orEmpty()
        Text("那年这月 | $month 月", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        val totalCount = history.diaries.size + history.todos.size + history.events.size
        if (totalCount == 0) {
            Text("没有找到历史同月记录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(
                text = "${history.diaries.size} 篇日记 | ${history.todos.size} 个待办 | ${history.events.size} 个事件",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            history.diaries.take(5).forEach { diary ->
                Text("日记 ${diary.entryDate}  ${diary.title.ifBlank { diary.contentMd.take(20) }}")
            }
            history.todos.take(5).forEach { todo ->
                Text("待办 ${todo.dueAt?.take(10).orEmpty()}  ${todo.title}")
            }
            history.events.take(5).forEach { event ->
                Text("事件 ${event.startAt.take(10)}  ${event.title}")
            }

            val hiddenCount = totalCount - history.diaries.take(5).size - history.todos.take(5).size - history.events.take(5).size
            if (hiddenCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("还有 $hiddenCount 条未显示。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun CalendarDateDetailsSheet(
    selectedDate: String,
    details: CalendarDateDetails?,
    showTrash: Boolean,
    onWriteDiary: (String?) -> Unit,
    onAddTodo: () -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (CalendarEventEntity) -> Unit,
    onDeleteEvent: (String, String) -> Unit,
    onRestoreDiary: (String) -> Unit,
    onRestoreTodo: (String) -> Unit,
    onRestoreEvent: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(text = selectedDate, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))

        if (details != null) {
            if (details.holidayLabel != null) {
                Text(
                    text = details.holidayLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (details.isHoliday) FluxHolidayOrange else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = { onWriteDiary(details.diary?.id) }, modifier = Modifier.weight(1f)) {
                    Text(if (details.diary == null) "写日记" else "打开日记")
                }
                Button(onClick = onAddTodo, modifier = Modifier.weight(1f)) {
                    Text("加待办")
                }
                Button(onClick = onAddEvent, modifier = Modifier.weight(1f)) {
                    Text("加事件")
                }
            }

            if (details.diary != null) {
                CalendarDetailSectionTitle("日记", FluxDiaryYellow)
                CalendarDetailItemRow(
                    title = details.diary.title.ifBlank { "无标题日记" },
                    subtitle = details.diary.contentMd.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty(),
                    color = FluxDiaryYellow,
                    onClick = { onWriteDiary(details.diary.id) }
                )
            }

            if (details.todos.isNotEmpty()) {
                CalendarDetailSectionTitle("待办", FluxTodoRed)
                details.todos.forEach { todo ->
                    val todoColor = if (todo.status == "completed") Color.Gray else FluxTodoRed
                    CalendarDetailItemRow(
                        title = todo.title,
                        subtitle = todo.todoSubtitle(),
                        color = todoColor,
                        textDecoration = if (todo.status == "completed") TextDecoration.LineThrough else null,
                    )
                }
            }

            if (details.events.isNotEmpty()) {
                CalendarDetailSectionTitle("事件", TimelineFallbackColor)
                details.events.forEach { event ->
                    CalendarDetailItemRow(
                        title = event.title,
                        subtitle = event.eventSubtitle().ifBlank { event.label().removePrefix("- ") },
                        color = event.safeColor(),
                        onClick = { onEditEvent(event) },
                        trailing = {
                            IconButton(onClick = { onDeleteEvent(event.id, selectedDate) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "删除事件",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    )
                }
            }

            val deletedCount = details.deletedDiaries.size + details.deletedTodos.size + details.deletedEvents.size
            if (showTrash && deletedCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("回收站", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                details.deletedDiaries.forEach { diary ->
                    DeletedItemRow("删 | 日记 ${diary.title.ifBlank { diary.entryDate }}") { onRestoreDiary(diary.id) }
                }
                details.deletedTodos.forEach { todo ->
                    DeletedItemRow("删 | 待办 ${todo.title}") { onRestoreTodo(todo.id) }
                }
                details.deletedEvents.forEach { event ->
                    DeletedItemRow("删 | 事件 ${event.title}") { onRestoreEvent(event.id) }
                }
            }

            if (details.events.isEmpty() && details.diary == null && details.todos.isEmpty() && (!showTrash || deletedCount == 0)) {
                Text("这一天还没有记录。")
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun CalendarDetailSectionTitle(
    title: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CalendarDetailItemRow(
    title: String,
    subtitle: String,
    color: Color,
    textDecoration: TextDecoration? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textDecoration = textDecoration
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun DeletedItemRow(
    label: String,
    onRestore: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray)
        TextButton(onClick = onRestore) {
            Text("恢复")
        }
    }
}
