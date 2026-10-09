package com.joe.taskmanager.data.local

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchableTaskDao {

    /**
     * FTS4 MATCH using a subquery instead of JOIN.
     * Room verifies subqueries against entities more reliably than FTS4 JOINs.
     *
     * The subquery finds matching rowids from the FTS4 table, then the outer
     * query selects from tasks with the deletedAt filter.
     * Ordered by the FTS rowid to preserve relevance ranking.
     */
    @Query(
        """
        SELECT id, title, status, dueDate, hasTime, priority, listId
        FROM tasks
        WHERE id IN (
            SELECT rowid FROM searchable_task WHERE searchable_task MATCH :match
        )
          AND deletedAt IS NULL
        ORDER BY CASE id WHEN (
            SELECT rowid FROM searchable_task WHERE searchable_task MATCH :match
            ORDER BY rowid LIMIT 1
        ) THEN 0 ELSE 1 END, id
        LIMIT :limit
        """
    )
    fun search(match: String, limit: Int): Flow<List<SearchResultRow>>
}
