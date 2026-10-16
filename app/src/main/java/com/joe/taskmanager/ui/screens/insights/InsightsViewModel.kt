package com.joe.taskmanager.ui.screens.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.local.dao.EventDao
import com.joe.taskmanager.data.local.dao.FocusDao
import com.joe.taskmanager.data.local.dao.GamificationDao
import com.joe.taskmanager.data.local.dao.HabitDao
import com.joe.taskmanager.data.local.entity.Event
import com.joe.taskmanager.data.local.entity.EventType
import com.joe.taskmanager.data.local.entity.Habit
import com.joe.taskmanager.data.local.entity.HabitLog
import com.joe.taskmanager.data.local.entity.FocusSession
import com.joe.taskmanager.data.settings.SettingsRepository
import com.joe.taskmanager.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar

data class PostponeAlert(val taskId: Long, val count: Int)
data class InsightsState(
    val thisWeekTasks: Int = 0, val previousWeekTasks: Int = 0,
    val thisWeekFocus: Int = 0, val previousWeekFocus: Int = 0,
    val thisWeekPoints: Int = 0, val previousWeekPoints: Int = 0,
    val thisWeekHabitRate: Int = 0, val previousWeekHabitRate: Int = 0,
    val alerts: List<PostponeAlert> = emptyList(), val bestWeekday: Int? = null,
    val bestHour: Int? = null, val habitHeatmap: List<Int> = emptyList()
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    events: EventDao, focus: FocusDao, points: GamificationDao, habits: HabitDao, settings: SettingsRepository
) : ViewModel() {
    private val now = System.currentTimeMillis()
    private val thisStart = DateUtils.startOfWeek(now)
    private val previousStart = DateUtils.plusDays(thisStart, -7)
    private val previousEnd = DateUtils.endOfDay(DateUtils.plusDays(thisStart, -1))
    private val eventFlow = events.observeRecent(3000)
    private val heatmapStart = DateUtils.plusDays(DateUtils.startOfWeek(now), -21)
    private val sessionFlow = focus.observeBetween(previousStart, now)
    private data class Base(val events: List<Event>, val sessions: List<FocusSession>, val currentPoints: Int, val previousPoints: Int)
    private val base = combine(eventFlow, sessionFlow,
        points.observeSumBetween(thisStart, now), points.observeSumBetween(previousStart, previousEnd)) { e, f, p, pp -> Base(e, f, p, pp) }
    val state: StateFlow<InsightsState> = combine(base, habits.observeActive(), habits.observeAllLogsBetween(heatmapStart, now), settings.settings) { data, active, logs, pref ->
        val currentEvents = data.events.filter { it.occurredAt >= thisStart && it.occurredAt <= now }
        val previousEvents = data.events.filter { it.occurredAt >= previousStart && it.occurredAt <= previousEnd }
        val currentTasks = currentEvents.count { it.type == EventType.TASK_COMPLETED }
        val previousTasks = previousEvents.count { it.type == EventType.TASK_COMPLETED }
        val currentFocus = data.sessions.filter { it.startedAt >= thisStart && it.status.name == "COMPLETED" }.sumOf { it.actualMinutes }
        val previousFocus = data.sessions.filter { it.startedAt in previousStart..previousEnd && it.status.name == "COMPLETED" }.sumOf { it.actualMinutes }
        val currentRate = adherence(active, logs, thisStart, now)
        val previousRate = adherence(active, logs, previousStart, previousEnd)
        val alerts = data.events.filter { it.type == EventType.TASK_POSTPONED && it.occurredAt >= thisStart }
            .groupingBy { it.entityId }.eachCount().filter { it.key != null && it.value >= pref.postponeAlertThreshold }
            .map { PostponeAlert(it.key!!, it.value) }.sortedByDescending { it.count }
        val completions = currentEvents.filter { it.type == EventType.TASK_COMPLETED }
        val weekday = completions.takeIf { it.size >= 5 }
            ?.groupingBy { Calendar.getInstance().apply { timeInMillis = it.occurredAt }.get(Calendar.DAY_OF_WEEK) }
            ?.eachCount()?.maxByOrNull { it.value }?.key
        val hour = completions.takeIf { it.size >= 5 }
            ?.groupingBy { Calendar.getInstance().apply { timeInMillis = it.occurredAt }.get(Calendar.HOUR_OF_DAY) }
            ?.eachCount()?.maxByOrNull { it.value }?.key
        val byKey = logs.associateBy { it.habitId to it.date }
        val heatmap = (0 until 28).map { index ->
            val day = DateUtils.plusDays(heatmapStart, index)
            val scheduled = active.filter { it.scheduleDays and (1 shl DateUtils.saturdayStartDayIndex(day)) != 0 }
            if (scheduled.isEmpty()) 0 else scheduled.count { byKey[it.id to day]?.completed == true } * 100 / scheduled.size
        }
        InsightsState(currentTasks, previousTasks, currentFocus, previousFocus, data.currentPoints,
            data.previousPoints, currentRate, previousRate, alerts, weekday, hour, heatmap)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InsightsState())

    private fun adherence(habits: List<Habit>, logs: List<HabitLog>, from: Long, to: Long): Int {
        var scheduled = 0; var done = 0
        val byKey = logs.associateBy { it.habitId to it.date }
        habits.forEach { habit ->
            var day = DateUtils.startOfDay(from)
            while (day <= to) {
                if (habit.scheduleDays and (1 shl DateUtils.saturdayStartDayIndex(day)) != 0) {
                    scheduled++
                    if (byKey[habit.id to day]?.completed == true) done++
                }
                day = DateUtils.plusDays(day, 1)
            }
        }
        return if (scheduled == 0) 0 else done * 100 / scheduled
    }
}

