
package com.joe.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * PRD 9 Tasks.
 *
 * Date handling follows PRD 9: date-only values are stored as a local-date epoch
 * millis value plus `hasTime = false`, so a task due "tomorrow" does not shift
 * when the device crosses a time zone. `dueDate` is therefore the start-of-day
 * local millis when hasTime == false, and the absolute instant when hasTime == true.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("dueDate"),
        Index("status"),
        Index("listId"),
        Index("seriesId"),
        Index("createdAt")
    ]
)
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val listId: Long? = null,
    val dueDate: Long? = null,
    val hasTime: Boolean = false,
    val priority: Priority = Priority.NONE,
    val status: TaskStatus = TaskStatus.OPEN,
    val completedAt: Long? = null,
    // v1.1 recurrence. Kept in the v1 schema so migration to series needs no rewrite.
    val seriesId: Long? = null,
    val occurrenceDate: Long? = null,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Soft delete. PRD OQ-8: Undo snackbar plus a 30-day trash.
    val deletedAt: Long? = null
) {
    val isOverdue: Boolean
        get() = status == TaskStatus.OPEN &&
            dueDate != null &&
            isPast(dueDate, hasTime)

    companion object {
        private fun isPast(due: Long, hasTime: Boolean): Boolean {
            val now = System.currentTimeMillis()
            return if (hasTime) now > due else now > endOfToday()
        }
        private fun endOfToday(): Long {
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 23)
                set(java.util.Calendar.MINUTE, 59)
                set(java.util.Calendar.SECOND, 59)
                set(java.util.Calendar.MILLISECOND, 999)
            }
            return cal.timeInMillis
        }
    }
}

enum class Priority { NONE, LOW, MEDIUM, HIGH }

enum class TaskStatus { OPEN, COMPLETED }
