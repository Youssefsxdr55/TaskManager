
package com.joe.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.joe.taskmanager.data.local.entity.FocusSession
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDao {
    @Insert suspend fun insert(session: FocusSession): Long
    @Query("SELECT * FROM focus_sessions WHERE startedAt BETWEEN :from AND :to ORDER BY startedAt ASC")
    fun observeBetween(from: Long, to: Long): Flow<List<FocusSession>>
    @Query("SELECT COALESCE(SUM(actualMinutes), 0) FROM focus_sessions WHERE status = 'COMPLETED' AND startedAt BETWEEN :from AND :to")
    fun observeMinutesBetween(from: Long, to: Long): Flow<Int>
    @Query("SELECT COUNT(*) FROM focus_sessions WHERE status = 'COMPLETED' AND startedAt BETWEEN :from AND :to")
    suspend fun completedCountBetween(from: Long, to: Long): Int
    @Query("SELECT * FROM focus_sessions WHERE taskId = :taskId ORDER BY startedAt DESC")
    fun observeForTask(taskId: Long): Flow<List<FocusSession>>
}
