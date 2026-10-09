package com.joe.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

import com.joe.taskmanager.data.local.entity.Task

@Dao
interface SearchableTaskDao {
    @Query(
        """
        SELECT * FROM tasks
        WHERE id IN (
            SELECT rowid FROM searchable_task WHERE searchable_task MATCH :match
        )
          AND deletedAt IS NULL
        LIMIT :limit
        """
    )
    fun search(match: String, limit: Int): Flow<List<Task>>
}
