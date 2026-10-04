
package com.joe.taskmanager.data.local

import com.joe.taskmanager.data.local.dao.SearchIndexDao
import com.joe.taskmanager.data.local.dao.TaskIndexQueriesDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the FTS index in step with the real rows (PRD 7.1: search covers titles,
 * notes, subtask titles, and tags).
 *
 * Recomputing from the source tables is deliberate: an index rebuilt from the
 * source can never drift out of sync with the data it indexes. Every write goes
 * through a DAO, so Room's table invalidation is respected.
 */
@Singleton
class SearchIndexMaintainer @Inject constructor(
    private val indexQueries: TaskIndexQueriesDao,
    private val searchIndexDao: SearchIndexDao
) {
    /** Cheap change signal; emits whenever the tasks table changes. */
    private val changeSignal: Flow<Unit> = indexQueries.observeIndexSignal().map { }

    suspend fun rebuildAll() {
        searchIndexDao.clear()
        indexQueries.indexSourceRows().forEach { row ->
            searchIndexDao.upsert(
                SearchableTask(
                    rowid = row.id,
                    title = row.title,
                    notes = row.notes,
                    subtasks = row.subtasks,
                    tags = row.tags
                )
            )
        }
    }

    /**
     * collectLatest cancels an in-flight rebuild when a newer change arrives, so
     * rapid editing does not queue many rebuilds.
     */
    fun observeAndMaintain(scope: CoroutineScope) {
        changeSignal.collectLatest { rebuildAll() }
    }
}
