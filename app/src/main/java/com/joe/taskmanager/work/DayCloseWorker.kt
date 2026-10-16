
package com.joe.taskmanager.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.joe.taskmanager.data.gamification.PointsEngine
import com.joe.taskmanager.data.backup.BackupManager
import com.joe.taskmanager.data.settings.SettingsRepository
import com.joe.taskmanager.notification.ReminderRescheduler
import com.joe.taskmanager.util.DateUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * PRD 10 "Day Close". Runs shortly after local midnight and is idempotent, so a
 * missed run is caught up on next app open without double-applying points.
 *
 * Every write uses a dedupeKey, which is what makes a rerun safe.
 */
private fun dayStartForKey(key: String): Long? = runCatching {
    java.time.LocalDate.parse(key).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
}.getOrNull()

@HiltWorker
class DayCloseWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val pointsEngine: PointsEngine,
    private val rescheduler: ReminderRescheduler,
    private val settings: SettingsRepository,
    private val backupManager: BackupManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val key = SettingsRepository.todayKey()
        val settingsValue = settings.settings.first()

        if (settingsValue.lastDayCloseDate != key) {
            val today = DateUtils.startOfToday()
            var day = settingsValue.lastDayCloseDate?.let(::dayStartForKey)?.let { DateUtils.plusDays(it, 1) } ?: today
            var processed = 0
            while (day <= today && processed < 3660) {
                pointsEngine.runDayClose(day)
                day = DateUtils.plusDays(day, 1)
                processed++
            }
            rescheduler.rescheduleEverything()
            settings.setLastDayCloseDate(key)
        }

        val latestSettings = settings.settings.first()
        if (latestSettings.backupFolderUri != null && latestSettings.lastBackupDate != key) {
            val backupResult = backupManager.exportToConfiguredFolder()
            if (backupResult.isFailure) return Result.retry()
        }
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "day-close"
        const val CATCH_UP_NAME = "day-close-catchup"
    }
}

@Singleton
class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager get() = WorkManager.getInstance(context)

    /** Scheduled a little after midnight so "yesterday" is unambiguous. */
    fun scheduleDaily() {
        val request = PeriodicWorkRequestBuilder<DayCloseWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(computeInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            DayCloseWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun scheduleTrashPurge() {
        val request = PeriodicWorkRequestBuilder<TrashPurgeWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(computeInitialDelay() + TimeUnit.HOURS.toMillis(1), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            TrashPurgeWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun runCatchUpNow() {
        workManager.enqueueUniqueWork(
            DayCloseWorker.CATCH_UP_NAME,
            androidx.work.ExistingWorkPolicy.REPLACE,
            androidx.work.OneTimeWorkRequestBuilder<DayCloseWorker>().build()
        )
    }

    private fun computeInitialDelay(): Long {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 5)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (cal.timeInMillis - now).coerceAtLeast(TimeUnit.HOURS.toMillis(1))
    }
}
