
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
    val events: List<EventDto> = emptyList(),
    val ledger: List<LedgerDto> = emptyList(),
    val rewards: List<RewardDto> = emptyList(),
    val badges: List<BadgeDto> = emptyList(),
    val settings: Map<String, String> = emptyMap()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
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
