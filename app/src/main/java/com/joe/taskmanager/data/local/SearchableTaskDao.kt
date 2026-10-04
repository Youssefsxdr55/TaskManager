
package com.joe.taskmanager.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchableTaskDao {

    /**
     * FTS4 MATCH. The expression comes from SearchQueryBuilder, which escapes user
     * input; raw user text is never interpolated unescaped into FTS syntax.
     *
     * The JOIN back to tasks applies the same deletedAt IS NULL filter as every
     * other query, so trashed tasks never appear in results.
     */
    @Query(
        """
        SELECT t.id AS id,
               t.title AS title,
               t.status AS status,
               t.dueDate AS dueDate,
               t.hasTime AS hasTime,
               t.priority AS priority,
               t.listId AS listId
        FROM searchable_task
        INNER JOIN tasks t ON t.id = searchable_task.rowid
        WHERE searchable_task MATCH :match
          AND t.deletedAt IS NULL
        ORDER BY searchable_task.rowid
        LIMIT :limit
        """
    )
    fun search(match: String, limit: Int): Flow<List<SearchResultRow>>
}

/** Projection of a task that matched the FTS query. */
data class SearchResultRow(
    val id: Long,
    val title: String,
    val status: String,
    val dueDate: Long?,
    val hasTime: Boolean,
    val priority: String,
    val listId: Long?
)
