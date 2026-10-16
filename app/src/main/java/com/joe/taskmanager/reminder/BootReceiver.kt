
package com.joe.taskmanager.reminder

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joe.taskmanager.notification.ReminderRescheduler
import com.joe.taskmanager.notification.HabitReminderScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PRD 7.3: alarms do not survive reboot, app update, or time/timezone changes.
 * This receiver catches all four and rebuilds the entire schedule.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var rescheduler: ReminderRescheduler
    @Inject lateinit var habitReminderScheduler: HabitReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        rescheduler.rescheduleEverything()
                        habitReminderScheduler.scheduleNext()
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
