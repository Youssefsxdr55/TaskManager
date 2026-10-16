package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.IconButton
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.data.local.entity.Habit
import com.joe.taskmanager.data.local.entity.HabitLog
import com.joe.taskmanager.ui.screens.habits.HabitsViewModel
import com.joe.taskmanager.util.DateUtils

@Composable
fun HabitsScreen(viewModel: HabitsViewModel = hiltViewModel()) {
    val habits by viewModel.active.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    ScreenScaffold(
        title = stringResource(R.string.nav_habits),
        floatingActionButton = { FloatingActionButton(onClick = { showCreate = true }) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.habit_add))
        } }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().then(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text(stringResource(R.string.habit_points, balance), style = MaterialTheme.typography.titleMedium) }
            if (habits.isEmpty()) item { Text(stringResource(R.string.habits_empty), modifier = Modifier.padding(top = 24.dp)) }
            items(habits, key = { it.id }) { habit -> HabitCard(habit, viewModel) }
        }
    }
    if (showCreate) HabitCreateDialog(onDismiss = { showCreate = false }, onSave = { name, type, target, unit, days, threshold ->
        viewModel.create(name, type, target, unit, days, threshold); showCreate = false
    })
}

@Composable
private fun HabitCard(habit: Habit, viewModel: HabitsViewModel) {
    val logs by viewModel.logs(habit.id).collectAsStateWithLifecycle(initialValue = emptyList())
    val today = DateUtils.startOfToday()
    val todayLog = logs.firstOrNull { it.date == today }
    var numericText by remember(habit.id, todayLog?.value) { mutableStateOf(todayLog?.value?.takeIf { it > 0 }?.toString().orEmpty()) }
    val stats = remember(logs, habit.scheduleDays) { calculateStats(logs, habit.scheduleDays) }
    val streak = stats.first
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(habit.name, style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.habit_streak, streak), style = MaterialTheme.typography.bodySmall)
                    Text(stringResource(R.string.habit_best_rate, stats.second, stats.third), style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = { viewModel.archive(habit.id) }) { Text(stringResource(R.string.action_archive)) }
            }
            HabitHistoryGrid(logs, habit.scheduleDays)
            if (habit.type == Habit.HabitType.BOOLEAN) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = todayLog?.completed == true, onCheckedChange = { viewModel.log(habit, it) })
                    Text(stringResource(if (todayLog?.completed == true) R.string.habit_done_today else R.string.habit_mark_done))
                }
            } else {
                val target = habit.targetValue ?: 1.0
                LinearProgressIndicator(progress = { ((todayLog?.value ?: 0.0) / target).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = numericText, onValueChange = { numericText = it },
                        label = { Text(stringResource(R.string.habit_progress)) }, modifier = Modifier.weight(1f), singleLine = true)
                    IconButton(onClick = {
                        val next = ((numericText.toDoubleOrNull() ?: todayLog?.value ?: 0.0) - 1.0).coerceAtLeast(0.0)
                        numericText = next.toString(); viewModel.log(habit, false, next)
                    }) { Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.habit_decrease)) }
                    IconButton(onClick = {
                        val next = (numericText.toDoubleOrNull() ?: todayLog?.value ?: 0.0) + 1.0
                        numericText = next.toString(); viewModel.log(habit, false, next)
                    }) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.habit_increase)) }
                    TextButton(onClick = { viewModel.log(habit, false, numericText.toDoubleOrNull() ?: 0.0) }) {
                        Text(stringResource(R.string.action_save))
                    }
                    Text("${todayLog?.value ?: 0.0} / $target ${habit.unit.orEmpty()}")
                }
            }
        }
    }
}

private fun calculateStats(logs: List<HabitLog>, schedule: Int): Triple<Int, Int, Int> {
    val byDate = logs.associateBy { it.date }
    fun scheduled(day: Long) = schedule and (1 shl DateUtils.saturdayStartDayIndex(day)) != 0
    var today = DateUtils.startOfToday()
    if (scheduled(today) && byDate[today]?.countsForStreak != true) today = DateUtils.plusDays(today, -1)
    var current = 0
    var probe = today
    var inspected = 0
    while (inspected < 370) {
        if (scheduled(probe)) {
            if (byDate[probe]?.countsForStreak != true) break
            current++
        }
        probe = DateUtils.plusDays(probe, -1); inspected++
    }
    var best = 0
    var run = 0
    var day = logs.minOfOrNull { it.date }?.let { DateUtils.startOfDay(it) } ?: DateUtils.startOfToday()
    val end = DateUtils.startOfToday()
    while (day <= end) {
        if (scheduled(day)) {
            if (byDate[day]?.countsForStreak == true) { run++; best = maxOf(best, run) } else run = 0
        }
        day = DateUtils.plusDays(day, 1)
    }
    var possible = 0
    var achieved = 0
    repeat(30) { offset ->
        val date = DateUtils.plusDays(DateUtils.startOfToday(), -offset)
        if (scheduled(date)) { possible++; if (byDate[date]?.completed == true) achieved++ }
    }
    val rate = if (possible == 0) 0 else achieved * 100 / possible
    return Triple(current, best, rate)
}

@Composable
private fun HabitHistoryGrid(logs: List<HabitLog>, schedule: Int) {
    val history = remember(logs, schedule) {
        val byDate = logs.associateBy { it.date }
        (29 downTo 0).map { offset ->
            val day = DateUtils.plusDays(DateUtils.startOfToday(), -offset)
            val item = byDate[day]
            when {
                schedule and (1 shl DateUtils.saturdayStartDayIndex(day)) == 0 -> 0
                item?.completed == true -> 2
                item?.freezeUsed == true -> 3
                item != null -> 1
                else -> 0
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        history.chunked(10).forEach { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { status ->
                    val color = when (status) {
                        2 -> MaterialTheme.colorScheme.primary
                        3 -> MaterialTheme.colorScheme.tertiary
                        1 -> MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    androidx.compose.foundation.layout.Box(Modifier.size(14.dp).background(color, androidx.compose.foundation.shape.CircleShape))
                }
            }
        }
    }
}

@Composable
private fun HabitCreateDialog(onDismiss: () -> Unit, onSave: (String, Habit.HabitType, Double?, String, Int, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var numeric by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("") }
    var days by remember { mutableIntStateOf(Habit.ALL_DAYS) }
    var threshold by remember { mutableStateOf("100") }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.habit_add)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.habit_name)) }, singleLine = true)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(numeric, { numeric = it }); Text(stringResource(R.string.habit_numeric))
            }
            if (numeric) {
                OutlinedTextField(target, { target = it }, label = { Text(stringResource(R.string.habit_target)) }, singleLine = true)
                OutlinedTextField(unit, { unit = it }, label = { Text(stringResource(R.string.habit_unit)) }, singleLine = true)
                OutlinedTextField(threshold, { threshold = it.filter(Char::isDigit).take(3) }, label = { Text(stringResource(R.string.habit_threshold)) }, singleLine = true)
            }
            Text(stringResource(R.string.habit_schedule))
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                habitDayLabels.forEachIndexed { index, label ->
                    FilterChip(selected = days and (1 shl index) != 0, onClick = { days = days xor (1 shl index) },
                        label = { Text(stringResource(label)) })
                }
            }
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank() && days != 0 && (!numeric || (target.toDoubleOrNull() ?: 0.0) > 0), onClick = {
            onSave(name, if (numeric) Habit.HabitType.NUMERIC else Habit.HabitType.BOOLEAN, target.toDoubleOrNull(), unit, days, threshold.toIntOrNull() ?: 100)
        }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

private val habitDayLabels = listOf(R.string.weekday_sat, R.string.weekday_sun, R.string.weekday_mon, R.string.weekday_tue,
    R.string.weekday_wed, R.string.weekday_thu, R.string.weekday_fri)
