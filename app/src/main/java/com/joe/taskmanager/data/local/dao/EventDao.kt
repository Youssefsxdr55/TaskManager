
package com.joe.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.joe.taskmanager.data.local.entity.Event
import com.joe.taskmanager.data.local.entity.EventType
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert suspend fun insert(event: Event): Long
    @Insert suspend fun insertAll(events: List<Event>)

    @Query("SELECT * FROM events WHERE type = :type AND occurredAt >= :from ORDER BY occurredAt DESC")
    suspend fun since(type: EventType, from: Long): List<Event>

    /**
     * PRD 7.11 smart postponement alert: how many times has this task been
     * postponed, and when was the last one. Derived from TASK_POSTPONED events
     * rather than a counter column, so it stays correct across restore.
     */
    @Query(
        """
        SELECT COUNT(*) FROM events
        WHERE type = 'TASK_POSTPONED' AND entityId = :taskId
        """
    )
    suspend fun postponeCount(taskId: Long): Int

    @Query(
        """
        SELECT MAX(occurredAt) FROM events
        WHERE type = 'TASK_POSTPONED' AND entityId = :taskId
        """
    )
    suspend fun lastPostponedAt(taskId: Long): Long?

    @Query("SELECT COUNT(*) FROM events WHERE type = :type AND occurredAt BETWEEN :from AND :to")
    suspend fun countBetween(type: EventType, from: Long, to: Long): Int

    /** Truncate the log so the app cannot grow without bound. Keeps 12 months. */
    @Query("DELETE FROM events WHERE occurredAt < :cutoff")
    suspend fun prune(cutoff: Long): Int

    @Query("SELECT * FROM events ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<Event>>
}
