
package com.joe.taskmanager.notification

import com.joe.taskmanager.data.local.dao.EventDao
import com.joe.taskmanager.data.local.dao.ReminderDao
import com.joe.taskmanager.data.local.dao.TaskDao
import com.joe.taskmanager.data.local.entity.Event
import com.joe.taskmanager.data.local.entity.EventType
import com.joe.taskmanager.data.local.entity.Reminder
import com.joe.taskmanager.data.local.entity.ReminderType
import com.joe.taskmanager.data.local.entity.TaskStatus
import com.joe.taskmanager.data.settings.SettingsRepository
import com.joe.taskmanager.util.ReminderMath
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Recomputes and re-books every future reminder. Called after reboot, app update,
 * time/timezone change, and permission changes (PRD 7.3).
 */
@Singleton
class ReminderRescheduler @Inject constructor(
    private val reminderDao: ReminderDao,
    private val taskDao: TaskDao,
    private val eventDao: EventDao,
    private val scheduler: ReminderScheduler,
    private val settings: SettingsRepository
) {
    suspend fun rescheduleEverything() {
        val now = System.currentTimeMillis()
        val horizon = now + THIRTY_DAYS_MS

        // Widen the net: fetch every reminder booked in the window, then recompute
        // its absolute time from the owning task so a due-date change is honoured.
        val reminders = reminderDao.remindersBetween(0L, horizon)
        reminders.forEach { reminder ->
            val recomputed = recompute(reminder)
            if (recomputed != null && recomputed > now) {
                reminderDao.setScheduledAt(reminder.id, recomputed)
                scheduler.schedule(reminder.id, recomputed, isHighPriority = false)
            } else {
                reminderDao.setScheduledAt(reminder.id, null)
                scheduler.cancel(reminder.id)
            }
        }
    }

    /**
     * Recompute one reminder's absolute instant from its task.
     * Returns null when the reminder is not valid (PRD 7.3: "a date with no time
     * produces no reminder unless a time is explicitly set").
     */
    suspend fun recompute(reminder: Reminder): Long? {
        val task = taskDao.getById(reminder.taskId) ?: return null
        if (task.deletedAt != null || task.status == TaskStatus.COMPLETED) return null

        return when (reminder.type) {
            ReminderType.ABSOLUTE -> reminder.absoluteTime
            ReminderType.AT_DUE_TIME ->
                if (task.hasTime && task.dueDate != null) task.dueDate else null
            ReminderType.BEFORE -> {
                val offset = reminder.offsetMinutes ?: return null
                if (!task.hasTime || task.dueDate == null) null
                else ReminderMath.subtractMinutes(task.dueDate, offset)
            }
        }
    }

    suspend fun logFired(reminderId: Long, taskId: Long) {
        eventDao.insert(
            Event(
                type = EventType.REMINDER_FIRED,
                entityType = "reminder",
                entityId = reminderId,
                payloadJson = """{"taskId":$taskId}"""
            )
        )
    }

    /** Kept in one place so the snooze default is consistent everywhere. */
    suspend fun defaultSnoozeMinutes(): Int = settings.settings.first().defaultSnoozeMinutes

    companion object {
        const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1000
    }
}
