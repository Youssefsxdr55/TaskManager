package com.joe.taskmanager.ui.screens.habits

import androidx.room.withTransaction
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.local.AppDatabase
import com.joe.taskmanager.data.local.dao.EventDao
import com.joe.taskmanager.data.local.dao.GamificationDao
import com.joe.taskmanager.data.local.dao.HabitDao
import com.joe.taskmanager.data.gamification.PointRules
import com.joe.taskmanager.data.local.entity.Event
import com.joe.taskmanager.data.local.entity.EventType
import com.joe.taskmanager.data.local.entity.Habit
import com.joe.taskmanager.data.local.entity.HabitLog
import com.joe.taskmanager.data.local.entity.PointsLedgerEntry
import com.joe.taskmanager.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HabitsViewModel @Inject constructor(
    private val habits: HabitDao,
    private val points: GamificationDao,
    private val events: EventDao,
    private val database: AppDatabase
) : ViewModel() {
    val active: StateFlow<List<Habit>> = habits.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val balance: StateFlow<Int> = points.observeBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    fun logs(id: Long): Flow<List<HabitLog>> = habits.observeLogs(id)

    fun create(name: String, type: Habit.HabitType, target: Double?, unit: String, days: Int, threshold: Int) {
        if (name.isBlank() || (type == Habit.HabitType.NUMERIC && (target == null || target <= 0))) return
        viewModelScope.launch {
            habits.insert(Habit(name = name.trim(), type = type, targetValue = target,
                unit = unit.trim().takeIf { it.isNotEmpty() }, scheduleDays = days, minThresholdPercent = threshold.coerceIn(1, 100)))
        }
    }

    fun log(habit: Habit, complete: Boolean, currentValue: Double = 0.0) {
        viewModelScope.launch {
            val day = DateUtils.startOfToday()
            val value = if (habit.type == Habit.HabitType.BOOLEAN) if (complete) 1.0 else 0.0 else currentValue
            val achieved = if (habit.type == Habit.HabitType.BOOLEAN) complete
                else value >= (habit.targetValue ?: Double.MAX_VALUE) * habit.minThresholdPercent / 100.0
            database.withTransaction {
                val old = habits.logFor(habit.id, day)
                val row = HabitLog(id = old?.id ?: 0, habitId = habit.id, date = day, value = value,
                    completed = achieved, countsForStreak = achieved)
                if (old == null) habits.upsertLog(row) else habits.updateLog(habit.id, day, value, achieved, achieved, old.freezeUsed)
                events.insert(Event(type = EventType.HABIT_LOGGED, entityType = "habit", entityId = habit.id,
                    payloadJson = """{"date":$day,"value":$value,"completed":$achieved}"""))
                val pointCap = if (habit.type == Habit.HabitType.BOOLEAN) 3 else
                    PointRules.habitNumericAward(value / (habit.targetValue ?: 1.0))
                val oldCap = if (habit.type == Habit.HabitType.BOOLEAN) if (old?.completed == true) 3 else 0 else
                    PointRules.habitNumericAward((old?.value ?: 0.0) / (habit.targetValue ?: 1.0))
                for (point in (oldCap + 1)..pointCap) {
                    val ledgerId = points.append(PointsLedgerEntry(
                        delta = 1, reason = "Habit progress", entityType = "habit", entityId = habit.id,
                        dedupeKey = "habit:${habit.id}:$day:$point"))
                    if (ledgerId != -1L) events.insert(Event(type = EventType.POINTS_AWARDED, entityType = "habit", entityId = habit.id,
                        payloadJson = """{"points":1,"date":$day}"""))
                }
            }
        }
    }

    fun archive(id: Long) { viewModelScope.launch { habits.archive(id) } }
}
