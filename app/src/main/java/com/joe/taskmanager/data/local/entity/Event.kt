
package com.joe.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * PRD 1.3 principle 5 and 9: event logging ships in v1.0 even though the
 * analytics screens ship in v1.3. "Analytics can only analyze what was logged."
 */
@Entity(
    tableName = "events",
    indices = [Index(value = ["type", "occurredAt"]), Index("entityId")]
)
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: EventType,
    val entityType: String,
    val entityId: Long?,
    val occurredAt: Long = System.currentTimeMillis(),
    val payloadJson: String = "{}"
)

enum class EventType {
    TASK_CREATED,
    TASK_COMPLETED,
    TASK_UNCOMPLETED,
    TASK_POSTPONED,
    TASK_DELETED,
    TASK_EDITED,
    HABIT_LOGGED,
    FOCUS_STARTED,
    FOCUS_COMPLETED,
    FOCUS_ABORTED,
    POINTS_AWARDED,
    POINTS_PENALTY_APPLIED,
    REMINDER_FIRED,
    HABIT_MISSED,
    BADGE_UNLOCKED,
    REWARD_REDEEMED,
    BACKUP_COMPLETED,
    BACKUP_FAILED
}
