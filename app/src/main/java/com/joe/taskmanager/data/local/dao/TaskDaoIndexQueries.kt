
package com.joe.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** One row of the aggregated text fed into the FTS index. */
data class IndexSourceRow(
    val id: Long,
    val title: String,
    val notes: String,
    val subtasks: String,
    val tags: String
)

/**
 * The aggregation that builds FTS content. Kept in SQL (not in Kotlin) so a full
 * index rebuild is a single pass with no per-row round trip into the VM.
 */
@Dao
interface TaskIndexQueriesDao {

    @Query(
        """
        SELECT
            t.id AS id,
            t.title AS title,
            t.notes AS notes,
            COALESCE((
                SELECT GROUP_CONCAT(s.title, ' ')
                FROM subtasks s
                WHERE s.taskId = t.id
            ), '') AS subtasks,
            COALESCE((
                SELECT GROUP_CONCAT(g.name, ' ')
                FROM task_tags tt
                INNER JOIN tags g ON g.id = tt.tagId
                WHERE tt.taskId = t.id
            ), '') AS tags
        FROM tasks t
        WHERE t.deletedAt IS NULL
        """
    )
    suspend fun indexSourceRows(): List<IndexSourceRow>

    /** Cheap invalidation signal: emits whenever any task row changes. */
    @Query("SELECT COUNT(*) FROM tasks")
    fun observeIndexSignal(): Flow<Int>
}
