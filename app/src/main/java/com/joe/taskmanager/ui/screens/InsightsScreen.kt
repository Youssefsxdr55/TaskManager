package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.ui.screens.insights.InsightsViewModel

@Composable
fun InsightsScreen(onTaskClick: (Long) -> Unit, onBack: () -> Unit, viewModel: InsightsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ScreenScaffold(title = stringResource(R.string.nav_insights), onBack = onBack) { content ->
        LazyColumn(content.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { InsightCard(stringResource(R.string.insight_tasks), state.thisWeekTasks, state.previousWeekTasks) }
            item { InsightCard(stringResource(R.string.insight_focus), state.thisWeekFocus, state.previousWeekFocus, suffix = stringResource(R.string.minutes_short)) }
            item { InsightCard(stringResource(R.string.insight_habits), state.thisWeekHabitRate, state.previousWeekHabitRate, suffix = "%") }
            item { InsightCard(stringResource(R.string.insight_points), state.thisWeekPoints, state.previousWeekPoints) }
            state.bestWeekday?.let { item { Text(stringResource(R.string.insight_best_day, weekdayName(it))) } }
            state.bestHour?.let { item { Text(stringResource(R.string.insight_best_hour, it)) } }
            item { Text(stringResource(R.string.insight_habit_heatmap), style = MaterialTheme.typography.titleMedium) }
            items(4) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (0 until 7).forEach { col ->
                        val percent = state.habitHeatmap.getOrElse(row * 7 + col) { 0 }
                        val color = when {
                            percent >= 75 -> MaterialTheme.colorScheme.primary
                            percent >= 40 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                            percent > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        Surface(Modifier.weight(1f).height(26.dp), color = color, shape = RoundedCornerShape(5.dp)) {}
                    }
                }
            }
            item { Text(stringResource(R.string.insight_postponed), style = MaterialTheme.typography.titleMedium) }
            if (state.alerts.isEmpty()) item { Text(stringResource(R.string.insight_no_alerts)) }
            items(state.alerts, key = { it.taskId }) { alert ->
                Text(stringResource(R.string.insight_postponed_item, alert.count), modifier = Modifier.fillMaxWidth().clickable { onTaskClick(alert.taskId) }.padding(12.dp))
            }
        }
    }
}

@Composable
private fun InsightCard(title: String, current: Int, previous: Int, suffix: String = "") {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.insight_comparison, current, suffix, previous, suffix))
    } }
}

@Composable
private fun weekdayName(day: Int): String = stringResource(when(day) {
    java.util.Calendar.SATURDAY -> R.string.weekday_sat
    java.util.Calendar.SUNDAY -> R.string.weekday_sun
    java.util.Calendar.MONDAY -> R.string.weekday_mon
    java.util.Calendar.TUESDAY -> R.string.weekday_tue
    java.util.Calendar.WEDNESDAY -> R.string.weekday_wed
    java.util.Calendar.THURSDAY -> R.string.weekday_thu
    else -> R.string.weekday_fri
})

