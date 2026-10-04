
package com.joe.taskmanager.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.joe.taskmanager.data.repository.TaskRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * PRD OQ-8: trashed tasks are deleted after 30 days. Runs daily alongside Day
 * Close; the cutoff is recomputed each run so nothing depends on run history.
 */
@HiltWorker
class TrashPurgeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: TaskRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        repository.purgeTrashOlderThan(cutoff)
        return Result.success()
    }

    companion object {
        const val RETENTION_MS = 30L * 24 * 60 * 60 * 1000
        const val UNIQUE_NAME = "trash-purge"
    }
}
