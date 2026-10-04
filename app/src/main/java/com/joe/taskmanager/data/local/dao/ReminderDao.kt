
package com.joe.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.joe.taskmanager.data.local.entity.Reminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Insert suspend fun insert(reminder: Reminder): Long
    @Update suspend fun update(reminder: Reminder)
    @Delete suspend fun delete(reminder: Reminder)

    @Query("SELECT * FROM reminders WHERE taskId = :taskId ORDER BY scheduledAt ASC")
    fun observeForTask(taskId: Long): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE taskId = :taskId")
    suspend fun forTask(taskId: Long): List<Reminder>

    /** The alarm intent carries the reminder id, so look it up directly. */
    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun forTaskId(reminderId: Long): Reminder?

    @Query("UPDATE reminders SET scheduledAt = :scheduledAt WHERE id = :id")
    suspend fun setScheduledAt(id: Long, scheduledAt: Long?)

    @Query("SELECT * FROM reminders WHERE scheduledAt IS NOT NULL AND scheduledAt > :now")
    suspend fun futureReminders(now: Long): List<Reminder>

    @Query("SELECT * FROM reminders WHERE scheduledAt IS NOT NULL AND scheduledAt BETWEEN :from AND :to")
    suspend fun remindersBetween(from: Long, to: Long): List<Reminder>

    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: Long)
}
