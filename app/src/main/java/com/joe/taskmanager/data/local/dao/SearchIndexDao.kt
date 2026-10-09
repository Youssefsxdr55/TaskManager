
package com.joe.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/**
 * Write side of the FTS index. The read side lives in SearchableTaskDao.
 * Room manages the FTS4 virtual table's shadow tables, so writes go through
 * @Upsert rather than raw SQL.
 */
@Dao
interface SearchIndexDao {

    @Upsert
    suspend fun upsert(row: SearchableTask)

    @Query("DELETE FROM searchable_task")
    suspend fun clear()
}
