
package com.joe.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.joe.taskmanager.data.local.entity.OccurrenceMarker
import com.joe.taskmanager.data.local.entity.TaskSeries
import kotlinx.coroutines.flow.Flow

@Dao
interface SeriesDao {
    @Insert suspend fun insert(series: TaskSeries): Long
    @Update suspend fun update(series: TaskSeries)
    @Query("SELECT * FROM task_series WHERE endDate IS NULL OR endDate >= :today")
    fun observeActive(today: Long): Flow<List<TaskSeries>>
    @Query("SELECT * FROM task_series WHERE id = :id")
    suspend fun byId(id: Long): TaskSeries?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markOccurrence(marker: OccurrenceMarker): Long
    @Query("SELECT COUNT(*) FROM occurrence_markers WHERE seriesId = :seriesId AND occurrenceDate = :date")
    suspend fun occurrenceExists(seriesId: Long, date: Long): Int
    @Query("SELECT * FROM occurrence_markers WHERE seriesId = :seriesId ORDER BY occurrenceDate ASC")
    fun observeMarkers(seriesId: Long): Flow<List<OccurrenceMarker>>
}
