
package com.joe.taskmanager.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joe.taskmanager.data.local.entity.EventType
import com.joe.taskmanager.notification.ReminderContract
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires when a scheduled reminder alarm elapses. Builds and posts the
 * notification, and launches the full-screen alarm for high-priority tasks.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderPresenter: ReminderPresenter

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (reminderId <= 0) return
        val highPriority = intent.getBooleanExtra(EXTRA_HIGH_PRIORITY, false)

        // BroadcastReceiver.onReceive is on the main thread with a ~10s budget;
        // the DB read and notification post go to an injected scope instead.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                reminderPresenter.onAlarmFired(reminderId, highPriority)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.joe.taskmanager.action.FIRE_REMINDER"
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_HIGH_PRIORITY = "high_priority"
    }
}
