
package com.joe.taskmanager.reminder

/**
 * Shared constants for the reminder path: receiver actions, notification ids,
 * and request codes. Kept in one file so the notification builder, the alarm
 * activity, and the action receiver cannot drift apart.
 */
object ReminderContract {
    const val ACTION_DONE = "com.joe.taskmanager.action.REMINDER_DONE"
    const val ACTION_SNOOZE = "com.joe.taskmanager.action.REMINDER_SNOOZE"

    const val EXTRA_TASK_ID = "task_id"
    const val EXTRA_TASK_TITLE = "task_title"
    const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
    const val EXTRA_HIGH_PRIORITY = "high_priority"

    /** Notification id is derived from the task id so re-firing replaces, not stacks. */
    fun notificationId(taskId: Long): Int = (taskId % Int.MAX_VALUE).toInt()

    const val NOTIFICATION_ID_ALARM = 999_001
}
