
package com.joe.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * PRD 7.5. The rule plus template fields. Occurrences are ordinary Task rows
 * linked by seriesId + occurrenceDate, so completion, overdue state, points and
 * events all work per occurrence with no special cases.
 *
 * Ships in v1.0 schema (empty) so adding the v1.1 engine needs no migration.
 */
@Entity(tableName = "task_series")
data class TaskSeries(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val listId: Long? = null,
    val priority: Priority = Priority.NONE,
    val startDate: Long,
    val endDate: Long? = null,

    // --- Custom interval: every N days / weeks / months ---
    val intervalUnit: RecurrenceUnit? = null,
    val intervalAmount: Int = 1,

    // --- Specific weekdays: bitmask, bit 0 = Saturday (week starts Saturday) ---
    val weekdaysMask: Int = 0,

    // --- Monthly ---
    val monthlyDayOfMonth: Int? = null,   // 1..31
    val monthlyIsLastDay: Boolean = false, // "last day of month"

    // --- Yearly ---
    val yearlyMonth: Int? = null,          // 1..12
    val yearlyDayOfMonth: Int? = null,

    val hasTime: Boolean = false
)
