
package com.joe.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** PRD 7.7. Ships in v1.0 schema; the timer UI itself is v1.2. */
@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("startedAt"), Index("taskId")]
)
data class FocusSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val status: FocusStatus
)

enum class FocusStatus { COMPLETED, ABORTED }
