package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.animateItem
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.data.local.entity.HabitLog
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.data.local.entity.TaskStatus
import com.joe.taskmanager.data.local.entity.Priority
import com.joe.taskmanager.ui.screens.calendar.CalendarViewModel
import com.joe.taskmanager.util.DateUtils
import com.joe.taskmanager.ui.theme.priorityColor
import androidx.compose.ui.text.style.TextDecoration
import java.util.Calendar

@Composable
fun CalendarScreen(
    onTaskClick: (Long) -> Unit,
    onAddTask: (Long) -> Unit,
    onOpenToday: () -> Unit,
    onOpenUpcoming: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val month by viewModel.monthStart.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val habitLogs by viewModel.habitLogs.collectAsStateWithLifecycle()
    var selected by remember { mutableLongStateOf(DateUtils.startOfToday()) }
    var weekView by remember { mutableStateOf(false) }
    val selectedTasks = tasks.filter { it.dueDate?.let { due -> DateUtils.startOfDay(due) } == selected }
    ScreenScaffold(title = stringResource(R.string.nav_calendar), bottomBar = {
        MainBottomBar(MainTab.CALENDAR, onOpenToday, onOpenUpcoming) { }
    }, floatingActionButton = {
        FloatingActionButton(onClick = { onAddTask(selected) }) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_task)) }
    }) { content ->
        LazyColumn(content.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { selected = viewModel.moveMonth(-1) }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                    Text(monthLabel(month), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = { selected = viewModel.moveMonth(1) }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null) }
                    TextButton(onClick = {
                        selected = DateUtils.startOfToday()
                        viewModel.selectMonth(selected)
                    }) { Text(stringResource(R.string.today)) }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    FilterChip(
                        selected = weekView,
                        onClick = { weekView = !weekView },
                        label = { Text(stringResource(if (weekView) R.string.calendar_month_view else R.string.calendar_week_view)) }
                    )
                }
            }
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        CalendarMonth(month, selected, tasks, habitLogs, weekView, onSelect = {
                selected = it
                val chosen = Calendar.getInstance().apply { timeInMillis = it }
                val shown = Calendar.getInstance().apply { timeInMillis = month }
                if (chosen.get(Calendar.YEAR) != shown.get(Calendar.YEAR) || chosen.get(Calendar.MONTH) != shown.get(Calendar.MONTH)) viewModel.selectMonth(it)
                        })
                    }
                }
            }
            item {
                Text(
                    com.joe.taskmanager.ui.util.DateText.dateOnly(selected),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }
            if (selectedTasks.isEmpty()) item {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.calendar_no_tasks), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(selectedTasks, key = { it.id }) { task ->
                CalendarTaskRow(task, Modifier.animateItem(), onToggle = { viewModel.toggle(task) }, onClick = { onTaskClick(task.id) })
            }
            val dailyHabitLogs = habitLogs.filter { it.date == selected }
            item { Text(stringResource(R.string.nav_habits), style = MaterialTheme.typography.titleMedium) }
            if (dailyHabitLogs.isEmpty()) item { Text(stringResource(R.string.calendar_no_habit_logs)) }
            items(dailyHabitLogs, key = { "habit-${it.habitId}" }) { log ->
                val habit = habits.firstOrNull { it.id == log.habitId }
                if (habit != null) Text("${if (log.completed) "✓" else "•"} ${habit.name}: ${log.value}${habit.unit?.let { " $it" }.orEmpty()}")
            }
        }
    }
}

@Composable
private fun CalendarMonth(month: Long, selected: Long, tasks: List<Task>, habitLogs: List<HabitLog>, weekView: Boolean, onSelect: (Long) -> Unit) {
    val first = Calendar.getInstance().apply { timeInMillis = month }
    val start = DateUtils.startOfWeek(if (weekView) selected else month)
    val days = (0 until (if (weekView) 7 else 42)).map { DateUtils.plusDays(start, it) }
    val tasksByDay = tasks.filter { it.dueDate != null }
        .groupBy { DateUtils.startOfDay(it.dueDate!!) }
    val habitsByDay = habitLogs.groupBy { it.date }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf(R.string.weekday_sat, R.string.weekday_sun, R.string.weekday_mon, R.string.weekday_tue, R.string.weekday_wed, R.string.weekday_thu, R.string.weekday_fri).forEach {
                Text(stringResource(it), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall)
            }
        }
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) { week.forEach { day ->
                val cal = Calendar.getInstance().apply { timeInMillis = day }
                val inMonth = cal.get(Calendar.MONTH) == first.get(Calendar.MONTH)
                val markers = tasksByDay[day].orEmpty().map { it.priority }.distinct().take(2)
                val hasHabit = !habitsByDay[day].isNullOrEmpty()
                Box(
                    modifier = Modifier.weight(1f).height(48.dp).clickable { onSelect(day) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .size(38.dp)
                            .then(if (day == selected) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape) else Modifier)
                            .then(if (day == DateUtils.startOfToday() && day != selected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            cal.get(Calendar.DAY_OF_MONTH).toString(),
                            color = when {
                                day == selected -> MaterialTheme.colorScheme.onPrimary
                                inMonth -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (markers.isNotEmpty() || hasHabit) {
                            Row(
                                modifier = Modifier.padding(top = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                markers.forEach { priority ->
                                    Box(Modifier.size(4.dp).background(
                                        if (day == selected) MaterialTheme.colorScheme.onPrimary else priorityColor(priority),
                                        CircleShape
                                    ))
                                }
                                if (hasHabit) Box(
                                    Modifier.size(4.dp).background(
                                        if (day == selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary,
                                        CircleShape
                                    )
                                )
                            }
                        }
                    }
                }
            } }
        }
    }
}

@Composable
private fun CalendarTaskRow(task: Task, modifier: Modifier = Modifier, onToggle: () -> Unit, onClick: () -> Unit) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggle, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = if (task.status == TaskStatus.COMPLETED) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = stringResource(if (task.status == TaskStatus.COMPLETED) R.string.cd_mark_open else R.string.cd_mark_done),
                    tint = if (task.status == TaskStatus.COMPLETED) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    color = if (task.status == TaskStatus.COMPLETED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.status == TaskStatus.COMPLETED) TextDecoration.LineThrough else null
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (task.priority != Priority.NONE) {
                        val priorityLabel = when (task.priority) {
                            Priority.HIGH -> R.string.priority_high
                            Priority.MEDIUM -> R.string.priority_medium
                            Priority.LOW -> R.string.priority_low
                            Priority.NONE -> R.string.priority_low
                        }
                        Text(stringResource(priorityLabel), style = MaterialTheme.typography.labelSmall, color = priorityColor(task.priority))
                    }
                    if (task.hasTime && task.dueDate != null) Text(
                        com.joe.taskmanager.util.DateUtils.formatTime(task.dueDate, true),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(Modifier.padding(start = 8.dp).size(8.dp).background(priorityColor(task.priority), CircleShape))
        }
    }
}

@Composable
private fun monthLabel(millis: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = millis }
    val month = stringResource(listOf(R.string.month_jan, R.string.month_feb, R.string.month_mar, R.string.month_apr,
        R.string.month_may, R.string.month_jun, R.string.month_jul, R.string.month_aug, R.string.month_sep,
        R.string.month_oct, R.string.month_nov, R.string.month_dec)[calendar.get(Calendar.MONTH)])
    return "$month ${calendar.get(Calendar.YEAR)}"
}
