
package com.joe.taskmanager.data.backup

import kotlinx.serialization.Serializable

/**
 * PRD 7.13: a single versioned JSON file containing everything. The schema
 * version lets the importer migrate older backups instead of rejecting them.
 */
@Serializable
data class BackupFile(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAt: Long,
    val tasks: List<TaskDto> = emptyList(),
    val lists: List<ListDto> = emptyList(),
    val folders: List<FolderDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    val subtasks: List<SubtaskDto> = emptyList(),
    val reminders: List<ReminderDto> = emptyList(),
    val taskTags: List<TaskTagDto> = emptyList(),
    val taskSeries: List<TaskSeriesDto> = emptyList(),
    val occurrenceMarkers: List<OccurrenceMarkerDto> = emptyList(),
    val habits: List<HabitDto> = emptyList(),
    val habitLogs: List<HabitLogDto> = emptyList(),
    val focusSessions: List<FocusSessionDto> = emptyList(),
    val events: List<EventDto> = emptyList(),
    val ledger: List<LedgerDto> = emptyList(),
    val rewards: List<RewardDto> = emptyList(),
    val badges: List<BadgeDto> = emptyList(),
    val redemptions: List<RedemptionDto> = emptyList(),
    val settings: Map<String, String> = emptyMap()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}

@Serializable
data class FolderDto(
    val id: Long, val name: String, val color: Int, val sortOrder: Int
)

@Serializable
data class ListDto(
    val id: Long, val name: String, val color: Int,
    val folderId: Long?, val parentListId: Long?, val sortOrder: Int, val archived: Boolean
)

@Serializable
data class TaskDto(
    val id: Long, val title: String, val notes: String,
    val listId: Long?, val dueDate: Long?, val hasTime: Boolean,
    val priority: String, val status: String, val completedAt: Long?,
    val seriesId: Long?, val occurrenceDate: Long?, val sortOrder: Int,
    val createdAt: Long, val updatedAt: Long, val deletedAt: Long?
)

@Serializable
data class SubtaskDto(
    val id: Long, val taskId: Long, val title: String, val done: Boolean, val sortOrder: Int
)

@Serializable
data class TagDto(val id: Long, val name: String, val color: Int)

@Serializable
data class ReminderDto(
    val id: Long, val taskId: Long, val type: String,
    val offsetMinutes: Int?, val absoluteTime: Long?, val scheduledAt: Long?
)

@Serializable data class TaskTagDto(val taskId: Long, val tagId: Long)

@Serializable data class TaskSeriesDto(
    val id: Long, val title: String, val notes: String, val listId: Long?, val priority: String,
    val startDate: Long, val endDate: Long?, val intervalUnit: String?, val intervalAmount: Int,
    val weekdaysMask: Int, val monthlyDayOfMonth: Int?, val monthlyIsLastDay: Boolean,
    val yearlyMonth: Int?, val yearlyDayOfMonth: Int?, val hasTime: Boolean
)

@Serializable data class OccurrenceMarkerDto(val id: Long, val seriesId: Long, val occurrenceDate: Long)

@Serializable data class HabitDto(
    val id: Long, val name: String, val type: String, val targetValue: Double?, val unit: String?,
    val minThresholdPercent: Int, val scheduleDays: Int, val reminderEnabled: Boolean,
    val archived: Boolean, val createdAt: Long
)

@Serializable data class HabitLogDto(
    val id: Long, val habitId: Long, val date: Long, val value: Double,
    val completed: Boolean, val countsForStreak: Boolean, val freezeUsed: Boolean
)

@Serializable data class FocusSessionDto(
    val id: Long, val taskId: Long?, val startedAt: Long, val plannedMinutes: Int,
    val actualMinutes: Int, val status: String
)

@Serializable data class RedemptionDto(
    val id: Long, val rewardId: Long, val cost: Int, val redeemedAt: Long, val ledgerId: Long?
)

@Serializable
data class EventDto(
    val id: Long, val type: String, val entityType: String,
    val entityId: Long?, val occurredAt: Long, val payloadJson: String
)

@Serializable
data class LedgerDto(
    val id: Long, val delta: Int, val reason: String, val entityType: String,
    val entityId: Long?, val dedupeKey: String, val createdAt: Long
)

@Serializable
data class RewardDto(
    val id: Long, val name: String, val emoji: String?, val cost: Int,
    val note: String?, val archived: Boolean
)

@Serializable
data class BadgeDto(val badgeCode: String, val unlockedAt: Long)
