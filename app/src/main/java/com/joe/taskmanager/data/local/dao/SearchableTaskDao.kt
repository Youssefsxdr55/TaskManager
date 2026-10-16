package com.joe.taskmanager.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

import com.joe.taskmanager.data.local.entity.Task

@Dao
interface SearchableTaskDao {
    @Query(
        """
        SELECT * FROM tasks
        WHERE id IN (
            SELECT rowid FROM searchable_task WHERE searchable_task MATCH :match
        )
          AND deletedAt IS NULL
          AND (:listId IS NULL OR (:listId = -1 AND listId IS NULL) OR listId = :listId)
          AND (:tagId IS NULL OR id IN (SELECT taskId FROM task_tags WHERE tagId = :tagId))
          AND (:priority IS NULL OR priority = :priority)
          AND (:status IS NULL OR status = :status)
          AND (:dueFrom IS NULL OR dueDate >= :dueFrom)
          AND (:dueTo IS NULL OR dueDate <= :dueTo)
        ORDER BY CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END, dueDate ASC
        LIMIT :limit
        """
    )
    fun search(
        match: String,
        listId: Long?,
        tagId: Long?,
        priority: String?,
        status: String?,
        dueFrom: Long?,
        dueTo: Long?,
        limit: Int
    ): Flow<List<Task>>

    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL
          AND (:listId IS NULL OR (:listId = -1 AND listId IS NULL) OR listId = :listId)
          AND (:tagId IS NULL OR id IN (SELECT taskId FROM task_tags WHERE tagId = :tagId))
          AND (:priority IS NULL OR priority = :priority)
          AND (:status IS NULL OR status = :status)
          AND (:dueFrom IS NULL OR dueDate >= :dueFrom)
          AND (:dueTo IS NULL OR dueDate <= :dueTo)
        ORDER BY CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END, dueDate ASC
        LIMIT :limit
        """
    )
    fun filter(
        listId: Long?, tagId: Long?, priority: String?, status: String?,
        dueFrom: Long?, dueTo: Long?, limit: Int
    ): Flow<List<Task>>

}
