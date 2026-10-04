
package com.joe.taskmanager.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.getSystemService
import com.joe.taskmanager.reminder.ReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PRD 7.3: "Reminders are scheduled with exact alarms and must be rescheduled
 * after: device reboot, time or time zone change, app update, and permission
 * changes."
 *
 * All scheduling funnels through this class so there is exactly one place that
 * knows how to book an alarm. Every alarm uses a data URI carrying the reminder
 * id, which keeps PendingIntents distinct even when two reminders share a
 * request code, and makes cancel() able to target the right one.
 */
@Singleton
class ReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val channels: NotificationChannels
) {
    private val alarmManager: AlarmManager? = context.getSystemService()

    companion object {
        private const val TAG = "ReminderScheduler"

        /** Alarms further out than this are batched by the OS to save battery. */
        private const val INEXACT_AFTER_MS = 60 * 60 * 1000L
    }

    /** True when the OS will honour a setExactAndAllowWhileIdle call. */
    fun canScheduleExact(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val am = alarmManager ?: return false
        return am.canScheduleExactAlarms()
    }

    /** Sends the user to the system screen where exact alarms are granted. */
    fun exactAlarmSettingsIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else null

    fun batteryOptimizationSettingsIntent(): Intent? =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun schedule(reminderId: Long, triggerAtMillis: Long, isHighPriority: Boolean) {
        val am = alarmManager ?: return
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.w(TAG, "Refusing to schedule reminder $reminderId in the past")
            return
        }
        val operation = buildPendingIntent(reminderId, isHighPriority, mutable = false)
        try {
            if (canScheduleExact()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation)
            } else {
                // Degrade to a windowed alarm rather than dropping the reminder.
                am.setWindow(
                    AlarmManager.RTC_WAKEUP, triggerAtMillis,
                    INEXACT_AFTER_MS, operation
                )
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Exact alarm denied, falling back to inexact", e)
            am.setWindow(AlarmManager.RTC_WAKEUP, triggerAtMillis, INEXACT_AFTER_MS, operation)
        }
    }

    fun cancel(reminderId: Long) {
        val am = alarmManager ?: return
        val pending = buildPendingIntent(reminderId, isHighPriority = false, mutable = false)
        am.cancel(pending)
        pending.cancel()
    }

    /** Reschedule a single task's reminders from their current DB state. */
    fun rescheduleAll(reminders: List<Pair<Long, Long>>) {
        // Pair is (reminderId, triggerAtMillis). isHighPriority is re-derived by
        // the caller-provided list from the task it belongs to.
        reminders.forEach { (id, at) ->
            if (at > System.currentTimeMillis()) schedule(id, at, isHighPriority = false)
        }
    }

    private fun buildPendingIntent(reminderId: Long, isHighPriority: Boolean, mutable: Boolean): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            // Distinct data per reminder keeps filterEquals() (and therefore
            // alarm identity) correct when request codes collide.
            data = Uri.parse("taskmanager://reminder/$reminderId")
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderReceiver.EXTRA_HIGH_PRIORITY, isHighPriority)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (mutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, reminderId.toInt(), intent, flags)
    }
}
