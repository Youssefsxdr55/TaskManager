
package com.joe.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.joe.taskmanager.data.local.entity.Habit
import com.joe.taskmanager.data.local.entity.HabitLog
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Insert suspend fun insert(habit: Habit): Long
    @Update suspend fun update(habit: Habit)
    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<Habit>>
    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun habitById(id: Long): Habit?
    @Query("UPDATE habits SET archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)

    /**
     * One row per (habit, date) via the unique index, so logging the same day
     * twice updates rather than duplicating. IGNORE + explicit update below
     * keeps the write idempotent.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun upsertLog(log: HabitLog): Long
    @Query("UPDATE habit_logs SET value = :value, completed = :completed, countsForStreak = :counts, freezeUsed = :freezeUsed WHERE habitId = :habitId AND date = :date")
    suspend fun updateLog(habitId: Long, date: Long, value: Double, completed: Boolean, counts: Boolean, freezeUsed: Boolean)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun logFor(habitId: Long, date: Long): HabitLog?

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC")
    fun observeLogs(habitId: Long): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date BETWEEN :from AND :to ORDER BY date ASC")
    suspend fun logsBetween(habitId: Long, from: Long, to: Long): List<HabitLog>

    @Query("SELECT * FROM habit_logs WHERE date = :date")
    suspend fun logsOn(date: Long): List<HabitLog>
}
