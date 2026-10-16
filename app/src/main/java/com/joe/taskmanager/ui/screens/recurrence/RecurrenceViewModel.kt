package com.joe.taskmanager.ui.screens.recurrence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import com.joe.taskmanager.data.local.AppDatabase
import com.joe.taskmanager.data.local.dao.SeriesDao
import com.joe.taskmanager.data.local.dao.TaskDao
import com.joe.taskmanager.data.local.entity.Event
import com.joe.taskmanager.data.local.entity.EventType
import com.joe.taskmanager.data.local.entity.RecurrenceUnit
import com.joe.taskmanager.data.local.entity.TaskSeries
import com.joe.taskmanager.data.recurrence.RecurrenceMaterializer
import com.joe.taskmanager.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class RecurrenceViewModel @Inject constructor(
    private val series: SeriesDao,
    private val tasks: TaskDao,
    private val database: AppDatabase,
    private val materializer: RecurrenceMaterializer
) : ViewModel() {
    val active: StateFlow<List<TaskSeries>> = series.observeActive(DateUtils.startOfToday())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(title: String, pattern: Int, interval: Int, unit: RecurrenceUnit, mask: Int, day: Int, month: Int, lastDay: Boolean) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val today = DateUtils.startOfToday()
            series.insert(buildSeries(title, pattern, interval, unit, mask, day, month, lastDay, today))
            materializer.materialize()
        }
    }

    fun edit(id: Long, title: String, pattern: Int, interval: Int, unit: RecurrenceUnit, mask: Int, day: Int, month: Int, lastDay: Boolean) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val existing = series.byId(id) ?: return@launch
            val today = DateUtils.startOfToday()
            val updated = buildSeries(title, pattern, interval, unit, mask, day, month, lastDay, today).copy(id = id,
                listId = existing.listId, priority = existing.priority, notes = existing.notes, hasTime = existing.hasTime)
            database.withTransaction {
                removeOpenFuture(id, today)
                series.update(updated)
            }
            materializer.materialize()
        }
    }

    fun stop(id: Long) {
        viewModelScope.launch {
            val existing = series.byId(id) ?: return@launch
            val today = DateUtils.startOfToday()
            database.withTransaction {
                removeOpenFuture(id, today)
                series.update(existing.copy(endDate = DateUtils.plusDays(today, -1)))
            }
        }
    }

    private suspend fun removeOpenFuture(id: Long, from: Long) {
        val affected = tasks.openOccurrencesFrom(id, from)
        val now = System.currentTimeMillis()
        tasks.trashOpenOccurrencesFrom(id, from, now)
        series.clearMarkersFrom(id, from)
        affected.forEach { task ->
            tasks.logEvent(Event(type = EventType.TASK_DELETED, entityType = "task", entityId = task.id,
                payloadJson = """{"seriesId":$id,"occurrenceDate":${task.occurrenceDate},"reason":"series-edited"}"""))
        }
    }

    private fun buildSeries(
        title: String, pattern: Int, interval: Int, unit: RecurrenceUnit, mask: Int,
        day: Int, month: Int, lastDay: Boolean, startDate: Long
    ): TaskSeries {
        val validDay = day.coerceIn(1, 31)
        return when (pattern) {
            0 -> TaskSeries(title = title.trim(), startDate = startDate, intervalUnit = unit, intervalAmount = interval.coerceAtLeast(1))
            1 -> TaskSeries(title = title.trim(), startDate = startDate, weekdaysMask = mask)
            2 -> TaskSeries(title = title.trim(), startDate = startDate,
                monthlyDayOfMonth = if (lastDay) null else validDay, monthlyIsLastDay = lastDay)
            else -> TaskSeries(title = title.trim(), startDate = startDate,
                yearlyMonth = month.coerceIn(1, 12), yearlyDayOfMonth = validDay)
        }
    }
}
