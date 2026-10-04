
package com.joe.taskmanager.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * PRD 7.5: materializing occurrences must be idempotent. This table records which
 * (series, date) pairs have already been created so the daily worker can rerun
 * without producing duplicates.
 */
@Entity(
    tableName = "occurrence_markers",
    indices = [Index(value = ["seriesId", "occurrenceDate"], unique = true)]
)
data class OccurrenceMarker(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seriesId: Long,
    val occurrenceDate: Long
)
