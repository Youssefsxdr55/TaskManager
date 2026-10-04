
package com.joe.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.joe.taskmanager.data.local.entity.Subtask
import kotlinx.coroutines.flow.Flow

@Dao
interface SubtaskDao {
    @Insert suspend fun insert(subtask: Subtask): Long
    @Update suspend fun update(subtask: Subtask)
    @Delete suspend fun delete(subtask: Subtask)
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC, id ASC")
    fun observeForTask(taskId: Long): Flow<List<Subtask>>
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY sortOrder ASC, id ASC")
    suspend fun forTask(taskId: Long): List<Subtask>
    @Query("UPDATE subtasks SET done = :done WHERE id = :id")
    suspend fun setDone(id: Long, done: Boolean)
    @Query("SELECT COUNT(*) FROM subtasks WHERE taskId = :taskId AND done = 0")
    fun observePendingCount(taskId: Long): Flow<Int>
}
