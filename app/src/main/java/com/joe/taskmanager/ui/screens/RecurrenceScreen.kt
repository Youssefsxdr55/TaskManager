package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.data.local.entity.RecurrenceUnit
import com.joe.taskmanager.data.local.entity.TaskSeries
import com.joe.taskmanager.ui.screens.recurrence.RecurrenceViewModel

@Composable
fun RecurrenceScreen(viewModel: RecurrenceViewModel = hiltViewModel()) {
    val series by viewModel.active.collectAsStateWithLifecycle()
    var add by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TaskSeries?>(null) }
    var stopping by remember { mutableStateOf<TaskSeries?>(null) }
    ScreenScaffold(title = stringResource(R.string.nav_recurrence), floatingActionButton = {
        FloatingActionButton(onClick = { add = true }) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.recurrence_add)) }
    }) { contentModifier ->
        LazyColumn(contentModifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (series.isEmpty()) item { Text(stringResource(R.string.recurrence_empty)) }
            items(series, key = { it.id }) { item ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(recurrenceDescription(item), style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { editing = item }) { Text(stringResource(R.string.action_edit)) }
                        TextButton(onClick = { stopping = item }) { Text(stringResource(R.string.recurrence_stop)) }
                    }
                } }
            }
        }
    }
    if (add) RecurrenceDialog(onDismiss = { add = false }, onSave = { title, pattern, interval, unit, mask, day, month, lastDay ->
        viewModel.create(title, pattern, interval, unit, mask, day, month, lastDay); add = false
    })
    editing?.let { item -> RecurrenceDialog(initial = item, onDismiss = { editing = null }, onSave = { title, pattern, interval, unit, mask, day, month, lastDay ->
        viewModel.edit(item.id, title, pattern, interval, unit, mask, day, month, lastDay); editing = null
    }) }
    stopping?.let { item -> AlertDialog(onDismissRequest = { stopping = null },
        title = { Text(stringResource(R.string.recurrence_stop_title)) },
        text = { Text(stringResource(R.string.recurrence_stop_confirm, item.title)) },
        confirmButton = { TextButton(onClick = { viewModel.stop(item.id); stopping = null }) { Text(stringResource(R.string.recurrence_stop)) } },
        dismissButton = { TextButton(onClick = { stopping = null }) { Text(stringResource(R.string.action_cancel)) } }) }
}

@Composable
private fun recurrenceDescription(series: TaskSeries): String = when {
    series.intervalUnit != null -> stringResource(R.string.recurrence_desc_interval, series.intervalAmount,
        stringResource(when (series.intervalUnit) {
            RecurrenceUnit.DAY -> R.string.recurrence_unit_day
            RecurrenceUnit.WEEK -> R.string.recurrence_unit_week
            RecurrenceUnit.MONTH -> R.string.recurrence_unit_month
        }))
    series.weekdaysMask != 0 -> stringResource(R.string.recurrence_desc_weekdays)
    series.monthlyIsLastDay -> stringResource(R.string.recurrence_desc_last_day)
    series.monthlyDayOfMonth != null -> stringResource(R.string.recurrence_desc_month_day, series.monthlyDayOfMonth)
    series.yearlyMonth != null -> stringResource(R.string.recurrence_desc_yearly, series.yearlyMonth, series.yearlyDayOfMonth ?: 1)
    else -> stringResource(R.string.recurrence_desc_default)
}

@Composable
private fun RecurrenceDialog(initial: TaskSeries? = null, onDismiss: () -> Unit,
    onSave: (String, Int, Int, RecurrenceUnit, Int, Int, Int, Boolean) -> Unit) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var pattern by remember(initial?.id) { mutableIntStateOf(when {
        initial?.intervalUnit != null -> 0
        initial != null && initial.weekdaysMask != 0 -> 1
        initial?.yearlyMonth != null -> 3
        else -> 2
    }) }
    var interval by remember(initial?.id) { mutableStateOf((initial?.intervalAmount ?: 1).toString()) }
    var unit by remember(initial?.id) { mutableStateOf(initial?.intervalUnit ?: RecurrenceUnit.DAY) }
    var mask by remember(initial?.id) { mutableIntStateOf(initial?.weekdaysMask?.takeIf { it != 0 } ?: 0b1111111) }
    var day by remember(initial?.id) { mutableStateOf((initial?.monthlyDayOfMonth ?: initial?.yearlyDayOfMonth ?: 1).toString()) }
    var month by remember(initial?.id) { mutableStateOf((initial?.yearlyMonth ?: java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1).toString()) }
    var lastDay by remember(initial?.id) { mutableStateOf(initial?.monthlyIsLastDay ?: false) }
    val patterns = listOf(R.string.recurrence_interval, R.string.recurrence_weekdays, R.string.recurrence_monthly, R.string.recurrence_yearly)
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(if (initial == null) R.string.recurrence_add else R.string.action_edit)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(title, { title = it }, label = { Text(stringResource(R.string.habit_name)) }, singleLine = true)
            patterns.forEachIndexed { index, label ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    RadioButton(selected = pattern == index, onClick = { pattern = index })
                    Text(stringResource(label))
                }
            }
            when (pattern) {
                0 -> {
                    OutlinedTextField(interval, { interval = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.recurrence_every)) }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { RecurrenceUnit.entries.forEach { choice ->
                        FilterChip(selected = unit == choice, onClick = { unit = choice }, label = { Text(choice.name.lowercase()) })
                    } }
                }
                1 -> Row { (0..6).forEach { index ->
                    FilterChip(selected = mask and (1 shl index) != 0, onClick = { mask = mask xor (1 shl index) },
                        label = { Text(stringResource(dayLabels[index])) })
                } }
                2, 3 -> {
                    OutlinedTextField(day, { day = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(if (pattern == 2) R.string.recurrence_day else R.string.recurrence_year_day)) }, singleLine = true)
                    if (pattern == 2) Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = lastDay, onCheckedChange = { lastDay = it })
                        Text(stringResource(R.string.recurrence_last_day))
                    }
                    if (pattern == 3) OutlinedTextField(month, { month = it.filter(Char::isDigit).take(2) },
                        label = { Text(stringResource(R.string.recurrence_month)) }, singleLine = true)
                }
            }
        } },
        confirmButton = { TextButton(enabled = title.isNotBlank() && (pattern != 1 || mask != 0), onClick = {
            onSave(title, pattern, interval.toIntOrNull() ?: 1, unit, mask, day.toIntOrNull() ?: 1, month.toIntOrNull() ?: 1, lastDay)
        }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } })
}

private val dayLabels = listOf(R.string.weekday_sat, R.string.weekday_sun, R.string.weekday_mon, R.string.weekday_tue,
    R.string.weekday_wed, R.string.weekday_thu, R.string.weekday_fri)

