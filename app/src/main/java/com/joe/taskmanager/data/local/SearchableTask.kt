
package com.joe.taskmanager.data.local

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.PrimaryKey

/**
 * PRD 7.1 search: full-text across titles, notes, subtask titles and tags,
 * backed by Room FTS. Content is refreshed from the source tables by
 * SearchIndexMaintainer, so search text can never drift from the real rows.
 */
@Fts4(contentEntity = SearchableTaskContent::class)
@Entity(tableName = "searchable_task")
data class SearchableTask(
    @PrimaryKey val rowid: Long,
    val title: String,
    val notes: String,
    val subtasks: String,
    val tags: String
)

/** The content-sync table Room requires alongside an FTS4 entity. */
@Entity(tableName = "searchable_task_content")
data class SearchableTaskContent(
    @PrimaryKey val rowid: Long,
    val title: String,
    val notes: String,
    val subtasks: String,
    val tags: String
)
