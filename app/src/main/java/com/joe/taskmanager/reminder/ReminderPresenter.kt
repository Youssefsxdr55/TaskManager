
package com.joe.taskmanager.reminder

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.joe.taskmanager.data.local.dao.EventDao
import com.joe.taskmanager.data.local.dao.ReminderDao
import com.joe.taskmanager.data.local.dao.TaskDao
import com.joe.taskmanager.data.local.entity.Event
import com.joe.taskmanager.data.local.entity.EventType
import com.joe.taskmanager.data.local.entity.Reminder
import com.joe.taskmanager.data.local.entity.ReminderType
import com.joe.taskmanager.data.local.entity.TaskStatus
import com.joe.taskmanager.data.repository.TaskRepository
import com.joe.taskmanager.notification.NotificationChannels
import com.joe.taskmanager.notification.ReminderNotifications
import com.joe.taskmanager.notification.ReminderRescheduler
import com.joe.taskmanager.notification.ReminderScheduler
import com.joe.taskmanager.util.ReminderMath
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Business logic for a reminder firing. Kept out of the receiver so it is
 * testable and so both the notification path and the alarm activity share it.
 */
@Singleton
class ReminderPresenter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val taskDao: TaskDao,
    private val reminderDao: ReminderDao,
    private val eventDao: EventDao,
    private val taskRepository: TaskRepository,
    private val notifications: ReminderNotifications,
    private val scheduler: ReminderScheduler,
    private val rescheduler: ReminderRescheduler,
    private val channels: NotificationChannels,
    private val settings: com.joe.taskmanager.data.settings.SettingsRepository
) {
    suspend fun onAlarmFired(reminderId: Long, highPriorityRequested: Boolean) {
        val reminder = reminderDao.forTaskId(reminderId) ?: return
        val task = taskDao.getById(reminder.taskId) ?: return

        // A reminder that fires for a deleted, trashed, or completed task must
        // not surface.
        if (task.deletedAt != null || task.status == TaskStatus.COMPLETED) {
            cancelForTask(task.id)
            return
        }

        val snoozeMinutes = currentSnoozeMinutes()
        val body = task.notes.takeIf { it.isNotBlank() }

        // Trust the DB over the intent extra: priority may have changed since booking.
        val useAlarmPath = highPriorityRequested &&
            task.priority == com.joe.taskmanager.data.local.entity.Priority.HIGH

        val notification = if (useAlarmPath) {
            notifications.buildHighPriority(task.id, task.title, body, snoozeMinutes)
        } else {
            notifications.buildStandard(task.id, task.title, body, snoozeMinutes)
        }

        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        manager.notify(ReminderContract.notificationId(task.id), notification)

        rescheduler.logFired(reminderId, task.id)
    }

    suspend fun completeFromNotification(taskId: Long) {
        taskRepository.setCompleted(taskId, completed = true)
        NotificationManagerCompat.from(context).cancel(ReminderContract.notificationId(taskId))
    }

    /**
     * Snoozing records a TASK_POSTPONED event, which is what feeds both the v1.3
     * analytics and the smart postponement alert (PRD 7.3, 7.11).
     */
    suspend fun snoozeFromNotification(taskId: Long, minutes: Int) {
        val task = taskDao.getById(taskId) ?: return
        val newDue = ReminderMath.addMinutes(
            task.dueDate ?: System.currentTimeMillis(), minutes
        )

        taskDao.setDue(taskId, newDue, hasTime = true, now = System.currentTimeMillis())
        eventDao.insert(
            Event(
                type = EventType.TASK_POSTPONED,
                entityType = "task",
                entityId = taskId,
                payloadJson = """{"minutes":$minutes,"from":"notification"}"""
            )
        )

        // Re-book every reminder for the task against the new due instant.
        rescheduleForTask(taskId)
        NotificationManagerCompat.from(context).cancel(ReminderContract.notificationId(taskId))
    }

    suspend fun rescheduleForTask(taskId: Long) {
        val task = taskDao.getById(taskId) ?: return
        val reminders = reminderDao.forTask(taskId)
        reminders.forEach { reminder ->
            val at = rescheduler.recompute(reminder)
            if (at != null && at > System.currentTimeMillis()) {
                reminderDao.setScheduledAt(reminder.id, at)
                scheduler.schedule(
                    reminder.id, at,
                    isHighPriority = task.priority == com.joe.taskmanager.data.local.entity.Priority.HIGH
                )
            } else {
                reminderDao.setScheduledAt(reminder.id, null)
                scheduler.cancel(reminder.id)
            }
        }
    }

    suspend fun cancelForTask(taskId: Long) {
        val reminders = reminderDao.forTask(taskId)
        reminders.forEach { scheduler.cancel(it.id) }
        NotificationManagerCompat.from(context).cancel(ReminderContract.notificationId(taskId))
    }

    /** PRD 7.3: snooze duration is user-configurable (default 10 minutes). */
    private suspend fun currentSnoozeMinutes(): Int =
        settings.settings.first().defaultSnoozeMinutes

    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 10
    }
}
