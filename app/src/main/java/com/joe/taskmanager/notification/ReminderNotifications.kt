
package com.joe.taskmanager.notification

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.joe.taskmanager.MainActivity
import com.joe.taskmanager.R
import com.joe.taskmanager.reminder.ReminderActionReceiver
import com.joe.taskmanager.reminder.ReminderContract
import com.joe.taskmanager.reminder.AlarmActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Builds the standard reminder notification with Done and Snooze actions. */
@Singleton
class ReminderNotifications @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun buildStandard(
        taskId: Long,
        title: String,
        body: String?,
        snoozeMinutes: Int
    ): Notification {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReminderContract.EXTRA_TASK_ID, taskId)
        }
        val openPi = PendingIntent.getActivity(
            context, taskId.toInt(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val donePi = broadcast(
            context, taskId, ReminderContract.ACTION_DONE, "done", snoozeMinutes = 0, mutable = true
        )
        val snoozePi = broadcast(
            context, taskId, ReminderContract.ACTION_SNOOZE, "snooze", snoozeMinutes, mutable = true
        )

        return NotificationCompat.Builder(context, NotificationChannels.TASK_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body ?: ""))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPi)
            .addAction(R.drawable.ic_check, context.getString(R.string.action_done), donePi)
            .addAction(
                R.drawable.ic_snooze,
                context.getString(R.string.action_snooze_fmt, snoozeMinutes),
                snoozePi
            )
            .build()
    }

    /**
     * Full-screen-alarm path for high-priority tasks (PRD 7.3). Uses
     * setFullScreenIntent with a high-priority channel so the alarm activity is
     * shown over the lock screen rather than as a heads-up notification.
     */
    fun buildHighPriority(
        taskId: Long,
        title: String,
        body: String?,
        snoozeMinutes: Int
    ): Notification {
        val fullScreen = Intent(context, AlarmActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(ReminderContract.EXTRA_TASK_ID, taskId)
            putExtra(ReminderContract.EXTRA_TASK_TITLE, title)
            putExtra(ReminderContract.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        val fullPi = PendingIntent.getActivity(
            context, taskId.toInt(), fullScreen,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val donePi = broadcast(
            context, taskId, ReminderContract.ACTION_DONE, "done", 0, mutable = true
        )
        val snoozePi = broadcast(
            context, taskId, ReminderContract.ACTION_SNOOZE, "snooze", snoozeMinutes, mutable = true
        )

        return NotificationCompat.Builder(context, NotificationChannels.HIGH_PRIORITY_ALARMS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(true)
            .setFullScreenIntent(fullPi, true)
            .setContentIntent(fullPi)
            .addAction(R.drawable.ic_check, context.getString(R.string.action_done), donePi)
            .addAction(
                R.drawable.ic_snooze,
                context.getString(R.string.action_snooze_fmt, snoozeMinutes),
                snoozePi
            )
            .build()
    }

    private fun broadcast(
        context: Context,
        taskId: Long,
        action: String,
        tag: String,
        snoozeMinutes: Int,
        mutable: Boolean
    ): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderContract.EXTRA_TASK_ID, taskId)
            putExtra(ReminderContract.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        // Distinct requestCode per action per task so Done and Snooze do not
        // collapse into the same PendingIntent.
        val requestCode = (taskId.toInt() * 31) + action.hashCode()
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
