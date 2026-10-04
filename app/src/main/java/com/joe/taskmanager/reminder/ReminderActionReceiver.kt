
package com.joe.taskmanager.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.joe.taskmanager.notification.ReminderContract
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Handles the Done and Snooze action buttons on reminder notifications. */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderPresenter: ReminderPresenter

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(ReminderContract.EXTRA_TASK_ID, -1L)
        if (taskId <= 0) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ReminderContract.ACTION_DONE -> reminderPresenter.completeFromNotification(taskId)
                    ReminderContract.ACTION_SNOOZE -> {
                        val minutes = intent.getIntExtra(
                            ReminderContract.EXTRA_SNOOZE_MINUTES, DEFAULT_SNOOZE_MINUTES
                        )
                        reminderPresenter.snoozeFromNotification(taskId, minutes)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val DEFAULT_SNOOZE_MINUTES = 10
    }
}
