package com.joe.taskmanager.data.local

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions
import androidx.room.PrimaryKey

/**
 * Standalone FTS4 index for task search (PRD 7.1: full-text across titles,
 * notes, subtask titles and tags).
 *
 * Deliberately NOT an external-content FTS table. Room's external-content mode
 * expects the content entity to be maintained by Room's own sync triggers, but
 * this index is rebuilt in Kotlin by SearchIndexMaintainer, so the two models
 * conflict. A standalone FTS4 table has no such constraint and keeps
 * SearchIndexMaintainer the single source of truth.
 *
 * unicode61 is the tokenizer that segments Arabic text without a separate
 * dictionary, which the bilingual UI needs.
 */
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "searchable_task")
data class SearchableTask(
    @PrimaryKey val rowid: Long,
    val title: String,
    val notes: String,
    val subtasks: String,
    val tags: String
)