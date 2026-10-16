package com.joe.taskmanager.data.recurrence

import androidx.room.withTransaction
import com.joe.taskmanager.data.local.AppDatabase
import com.joe.taskmanager.data.local.dao.SeriesDao
import com.joe.taskmanager.data.local.dao.TaskDao
import com.joe.taskmanager.data.local.entity.Event
import com.joe.taskmanager.data.local.entity.EventType
import com.joe.taskmanager.data.local.entity.OccurrenceMarker
import com.joe.taskmanager.data.local.entity.RecurrenceUnit
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.data.local.entity.TaskSeries
import com.joe.taskmanager.util.DateUtils
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/** Creates ordinary task rows for each series date and keeps missed dates visible. */
@Singleton
class RecurrenceMaterializer @Inject constructor(
    private val seriesDao: SeriesDao,
    private val taskDao: TaskDao,
    private val database: AppDatabase
) {
    suspend fun materialize(now: Long = System.currentTimeMillis()) {
        val today = DateUtils.startOfDay(now)
        val horizon = DateUtils.plusDays(today, RecurrenceEngine.FORWARD_HORIZON_DAYS)
        seriesDao.observeActive(today).first().forEach { series -> materialize(series, horizon) }
    }

    private suspend fun materialize(series: TaskSeries, horizon: Long) {
        val rule = series.rule() ?: return
        val normalizedStart = DateUtils.startOfDay(series.startDate)
        val dates = RecurrenceEngine.occurrencesBetween(rule, normalizedStart, normalizedStart, horizon, series.endDate)
        dates.forEach { date ->
            database.withTransaction {
                if (seriesDao.occurrenceExists(series.id, date) == 0 && taskDao.countOccurrence(series.id, date) == 0) {
                    val due = if (series.hasTime) {
                        val start = Calendar.getInstance().apply { timeInMillis = series.startDate }
                        val occurrence = Calendar.getInstance().apply { timeInMillis = date }
                        occurrence.set(Calendar.HOUR_OF_DAY, start.get(Calendar.HOUR_OF_DAY))
                        occurrence.set(Calendar.MINUTE, start.get(Calendar.MINUTE))
                        occurrence.timeInMillis
                    } else date
                    val id = taskDao.insert(Task(title = series.title, notes = series.notes, listId = series.listId,
                        dueDate = due, hasTime = series.hasTime, priority = series.priority,
                        seriesId = series.id, occurrenceDate = date))
                    seriesDao.markOccurrence(OccurrenceMarker(seriesId = series.id, occurrenceDate = date))
                    taskDao.logEvent(Event(type = EventType.TASK_CREATED, entityType = "task", entityId = id,
                        payloadJson = """{"seriesId":${series.id},"occurrenceDate":$date}"""))
                }
            }
        }
    }

    private fun TaskSeries.rule(): RecurrenceRule? = when {
        intervalUnit != null -> {
            val anchor = Calendar.getInstance().apply { timeInMillis = startDate }
            RecurrenceRule.Interval(intervalUnit, intervalAmount,
                anchorDayOfMonth = anchor.get(Calendar.DAY_OF_MONTH),
                anchorIsLastDay = anchor.get(Calendar.DAY_OF_MONTH) == anchor.getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        weekdaysMask != 0 -> RecurrenceRule.Weekdays(weekdaysMask)
        monthlyIsLastDay -> RecurrenceRule.MonthlyByDate(31, true)
        monthlyDayOfMonth != null -> RecurrenceRule.MonthlyByDate(monthlyDayOfMonth, false)
        yearlyMonth != null && yearlyDayOfMonth != null -> RecurrenceRule.Yearly(yearlyMonth - 1, yearlyDayOfMonth)
        else -> null
    }
}

