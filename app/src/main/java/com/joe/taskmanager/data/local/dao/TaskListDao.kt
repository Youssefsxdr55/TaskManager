
package com.joe.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.joe.taskmanager.data.local.entity.Folder
import com.joe.taskmanager.data.local.entity.TaskList
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Insert suspend fun insert(folder: Folder): Long
    @Update suspend fun update(folder: Folder)
    @Delete suspend fun delete(folder: Folder)
    @Query("SELECT * FROM folders ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<Folder>>
    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getById(id: Long): Folder?
    @Query("SELECT COUNT(*) FROM task_lists WHERE folderId = :folderId")
    suspend fun countLists(folderId: Long): Int
}

@Dao
interface TaskListDao {
    @Insert suspend fun insert(list: TaskList): Long
    @Update suspend fun update(list: TaskList)
    @Delete suspend fun delete(list: TaskList)
    @Query("SELECT * FROM task_lists WHERE archived = 0 ORDER BY sortOrder ASC, name ASC")
    fun observeAll(): Flow<List<TaskList>>
    @Query("SELECT * FROM task_lists WHERE id = :id")
    suspend fun getById(id: Long): TaskList?
    @Query("SELECT * FROM task_lists WHERE parentListId = :parentListId AND archived = 0")
    fun observeSublists(parentListId: Long): Flow<List<TaskList>>
    @Query("SELECT * FROM task_lists WHERE folderId = :folderId AND archived = 0")
    fun observeInFolder(folderId: Long): Flow<List<TaskList>>

    /** Guard for the Folder > List > Sublist depth limit (PRD 6.2). */
    @Query("SELECT parentListId FROM task_lists WHERE id = :id")
    suspend fun parentOf(id: Long): Long?
}
