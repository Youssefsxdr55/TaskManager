
package com.joe.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * PRD 7.3. Multiple reminders per task.
 *
 * `scheduledAt` is the absolute UTC epoch millis the alarm is booked for; it is
 * recomputed whenever the task's due date changes so reminders never fire stale.
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId"), Index("scheduledAt")]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long,
    val type: ReminderType,
    val offsetMinutes: Int? = null,
    val absoluteTime: Long? = null,
    val scheduledAt: Long? = null
) {
    val isValid: Boolean
        get() = when (type) {
            ReminderType.AT_DUE_TIME -> scheduledAt != null
            ReminderType.BEFORE -> offsetMinutes != null && scheduledAt != null
            ReminderType.ABSOLUTE -> absoluteTime != null
        }
}

enum class ReminderType {
    /** At the due instant. Only valid when the task has an explicit time. */
    AT_DUE_TIME,

    /** N minutes/hours/days before the due instant. */
    BEFORE,

    /** A fixed wall-clock time chosen by the user. */
    ABSOLUTE
}
