package com.joe.taskmanager.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.joe.taskmanager.MainActivity
import com.joe.taskmanager.R
import com.joe.taskmanager.data.local.dao.HabitDao
import com.joe.taskmanager.data.local.dao.EventDao
import com.joe.taskmanager.data.local.entity.Event
import com.joe.taskmanager.data.local.entity.EventType
import com.joe.taskmanager.data.settings.SettingsRepository
import com.joe.taskmanager.notification.HabitReminderScheduler
import com.joe.taskmanager.notification.NotificationChannels
import com.joe.taskmanager.util.DateUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HabitReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var habits: HabitDao
    @Inject lateinit var events: EventDao
    @Inject lateinit var scheduler: HabitReminderScheduler

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != HabitReminderScheduler.ACTION) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val today = DateUtils.startOfToday()
                val due = habits.activeHabits().filter { habit ->
                    if (!habit.reminderEnabled) return@filter false
                    val scheduled = habit.scheduleDays and (1 shl DateUtils.saturdayStartDayIndex(today)) != 0
                    if (!scheduled) return@filter false
                    val log = habits.logFor(habit.id, today)
                    val progress = log?.value ?: 0.0
                    val threshold = (habit.targetValue ?: 1.0) * habit.minThresholdPercent / 100.0
                    if (habit.type.name == "BOOLEAN") log?.completed != true else progress < threshold
                }
                if (due.isNotEmpty()) {
                    post(context, due.size)
                    due.forEach { events.insert(Event(type = EventType.REMINDER_FIRED, entityType = "habit", entityId = it.id,
                        payloadJson = """{"date":$today,"kind":"habit"}""")) }
                }
                scheduler.scheduleNext()
            } finally { pending.finish() }
        }
    }

    private fun post(context: Context, count: Int) {
        val manager = context.getSystemService<NotificationManager>() ?: return
        val open = PendingIntent.getActivity(context, 4702, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, NotificationChannels.HABIT_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.habit_reminder_title))
            .setContentText(context.resources.getQuantityString(R.plurals.habit_reminder_body, count, count))
            .setContentIntent(open).setAutoCancel(true).build()
        manager.notify(4703, notification)
    }
}
