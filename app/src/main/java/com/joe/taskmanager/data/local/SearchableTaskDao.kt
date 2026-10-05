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
     * The JOIN back to tasks applies the same deletedAt IS NULL filter every other
     * query uses, so trashed tasks never appear in results.
     *
     * Ordered by rowid rather than bm25(): bm25() is an FTS4-specific function
     * that the compiler must resolve against the FTS table, and keeping the
     * query free of FTS-only functions makes Room's verification simpler.
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
        JOIN tasks t ON t.id = searchable_task.rowid
        WHERE searchable_task MATCH :match
          AND t.deletedAt IS NULL
        ORDER BY searchable_task.rowid
        LIMIT :limit
        """
    )
    fun search(match: String, limit: Int): Flow<List<SearchResultRow>>
}
