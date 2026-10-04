
package com.joe.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * PRD 7.6. Ships in v1.0 schema so v1.1 habits need no destructive migration.
 * `scheduleDays` is a bitmask; bit 0 = Saturday because the week starts Saturday.
 */
@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: HabitType,
    val targetValue: Double? = null,
    val unit: String? = null,
    val minThresholdPercent: Int = 100,
    val scheduleDays: Int = ALL_DAYS,
    val reminderEnabled: Boolean = true,
    val archived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    enum class HabitType { BOOLEAN, NUMERIC }
    companion object {
        const val ALL_DAYS = 0b1111111
    }
}

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habitId", "date"], unique = true)]
)
data class HabitLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val date: Long,
    val value: Double,
    val completed: Boolean,
    val countsForStreak: Boolean,
    val freezeUsed: Boolean = false
)
