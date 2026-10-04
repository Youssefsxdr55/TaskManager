
package com.joe.taskmanager.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.content.getSystemService
import javax.inject.Inject
import javax.inject.Singleton

/** PRD 7.4. Channel ids are stable so user customizations survive app updates. */
@Singleton
class NotificationChannels @Inject constructor(private val context: Context) {

    companion object {
        const val TASK_REMINDERS = "task_reminders"
        const val HIGH_PRIORITY_ALARMS = "high_priority_alarms"
        const val HABIT_REMINDERS = "habit_reminders"
        const val FOCUS_TIMER = "focus_timer"
        const val BACKUP = "backup"
    }

    fun ensureAll() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService<NotificationManager>() ?: return

        val channels = listOf(
            NotificationChannel(
                TASK_REMINDERS, "Task reminders", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Standard reminders for tasks with a due time"
                enableVibration(true)
                setShowBadge(true)
            },
            NotificationChannel(
                HIGH_PRIORITY_ALARMS, "High-priority alarms", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen alarm for high-priority tasks"
                enableVibration(true)
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            },
            NotificationChannel(
                HABIT_REMINDERS, "Habit reminders", NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Nightly habit prompts"
            },
            NotificationChannel(
                FOCUS_TIMER, "Focus timer", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing Pomodoro timer notification"
                setShowBadge(false)
            },
            NotificationChannel(
                BACKUP, "Backup", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Backup status and problems"
            }
        )
        nm.createNotificationChannels(channels)
    }
}
