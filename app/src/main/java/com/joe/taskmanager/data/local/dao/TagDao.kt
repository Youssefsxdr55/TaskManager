
package com.joe.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.joe.taskmanager.data.local.entity.Tag
import com.joe.taskmanager.data.local.entity.TaskTag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert suspend fun insert(tag: Tag): Long
    @Update suspend fun update(tag: Tag)
    @Delete suspend fun delete(tag: Tag)
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun observeAll(): Flow<List<Tag>>
    @Query("SELECT * FROM tags WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Tag?
    @Query("SELECT * FROM tags WHERE id = :id")
    fun observeById(id: Long): Flow<Tag?>
    @Query("SELECT t.* FROM tags t INNER JOIN task_tags tt ON tt.tagId = t.id WHERE tt.taskId = :taskId ORDER BY t.name")
    fun observeForTask(taskId: Long): Flow<List<Tag>>

    @Upsert suspend fun upsertLink(link: TaskTag)
    @Query("DELETE FROM task_tags WHERE taskId = :taskId AND tagId = :tagId")
    suspend fun unlink(taskId: Long, tagId: Long)
    @Query("DELETE FROM task_tags WHERE taskId = :taskId")
    suspend fun unlinkAll(taskId: Long)
    @Query("SELECT tagId FROM task_tags WHERE taskId = :taskId")
    suspend fun tagIdsForTask(taskId: Long): List<Long>
}
