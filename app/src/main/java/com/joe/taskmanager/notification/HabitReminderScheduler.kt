package com.joe.taskmanager.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.joe.taskmanager.reminder.HabitReminderReceiver
import com.joe.taskmanager.data.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import java.util.Calendar

@Singleton
class HabitReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository
) {
    suspend fun scheduleNext() {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val pref = settings.settings.first()
        val fireAt = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, pref.habitReminderHour)
            set(Calendar.MINUTE, pref.habitReminderMinute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }.timeInMillis
        val intent = PendingIntent.getBroadcast(context, REQUEST_CODE,
            Intent(context, HabitReminderReceiver::class.java).setAction(ACTION), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT < 31 || alarm.canScheduleExactAlarms()) {
            runCatching { alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, intent) }
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, fireAt, intent)
        }
    }
    fun cancel() {
        val alarm = context.getSystemService(AlarmManager::class.java) ?: return
        val intent = PendingIntent.getBroadcast(context, REQUEST_CODE, Intent(context, HabitReminderReceiver::class.java).setAction(ACTION), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        intent?.let(alarm::cancel)
    }
    companion object { const val ACTION = "com.joe.taskmanager.HABIT_REMINDER"; private const val REQUEST_CODE = 4701 }
}
