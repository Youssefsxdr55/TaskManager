
package com.joe.taskmanager

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.joe.taskmanager.data.settings.SettingsRepository
import com.joe.taskmanager.notification.NotificationChannels
import com.joe.taskmanager.notification.ReminderRescheduler
import com.joe.taskmanager.work.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class TaskManagerApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationChannels: NotificationChannels
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var workScheduler: WorkScheduler
    @Inject lateinit var reminderRescheduler: ReminderRescheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        notificationChannels.ensureAll()

        appScope.launch {
            // Apply the stored theme before the first frame is composed.
            settingsRepository.applyStoredTheme()
        }

        appScope.launch {
            // PRD 10: enqueue the daily Day Close job and the trash purge. Without
            // this the workers exist but are never scheduled, so penalties, streak
            // evaluation, and automatic backup would silently never run.
            workScheduler.scheduleDaily()
            workScheduler.scheduleTrashPurge()

            // PRD 7.3: alarms do not survive an app update, so rebuild the whole
            // reminder schedule at startup as well as on the explicit broadcasts.
            reminderRescheduler.rescheduleEverything()
        }
    }
}
