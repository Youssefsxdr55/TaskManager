
package com.joe.taskmanager.data.local.dao
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.joe.taskmanager.data.local.entity.Event
import com.joe.taskmanager.data.local.entity.Priority
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.data.local.entity.TaskStatus
import kotlinx.coroutines.flow.Flow
/**
 * Queries are indexed on dueDate, status, listId, seriesId, createdAt (PRD 9).
 * Soft-deleted rows (deletedAt IS NOT NULL) are excluded everywhere: they live in
 * the 30-day trash (PRD OQ-8) and are purged by TrashPurgeWorker.
 */
@Dao
interface TaskDao {
    @Insert
    suspend fun insert(task: Task): Long
    @Update
    suspend fun update(task: Task)
    @Upsert
    suspend fun upsert(task: Task): Long
    @Delete
    suspend fun delete(task: Task)
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): Task?
    @Query("SELECT COUNT(*) FROM tasks WHERE deletedAt IS NULL AND dueDate BETWEEN :from AND :to")
    suspend fun countDueBetween(from: Long, to: Long): Int
    @Query("SELECT COUNT(*) FROM tasks WHERE deletedAt IS NULL AND status = 'COMPLETED' AND dueDate BETWEEN :from AND :to")
    suspend fun countCompletedDueBetween(from: Long, to: Long): Int
    @Query("SELECT * FROM events WHERE type = 'DAY_BALANCE_SNAPSHOT' AND entityId = :dayStart LIMIT 1")
    suspend fun dayBalanceSnapshot(dayStart: Long): Event?
    @Query("SELECT * FROM events WHERE type = 'DAY_BALANCE_SNAPSHOT' AND entityId < :beforeDay ORDER BY entityId DESC LIMIT :limit")
    suspend fun recentDayBalanceSnapshots(beforeDay: Long, limit: Int): List<Event>
    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: Long): Flow<Task?>
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND dueDate BETWEEN :from AND :to ORDER BY dueDate ASC, priority DESC")
    fun observeDueBetween(from: Long, to: Long): Flow<List<Task>>
    // ---------- Today ----------
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND status = 'OPEN' AND dueDate BETWEEN :startOfToday AND :endOfToday ORDER BY CASE WHEN hasTime THEN 0 ELSE 1 END, dueDate ASC, priority DESC LIMIT :limit")
    suspend fun getDueToday(startOfToday: Long, endOfToday: Long, limit: Int): List<Task>
    /**
     * Today view: tasks due today. Per PRD 7.1 overdue tasks stay in their
     * original position rendered in red, and PRD OQ-1 surfaces them via a
     * separate "Overdue (N)" banner rather than mixing them into this list.
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL
          AND status = 'OPEN'
          AND dueDate IS NOT NULL
          AND dueDate >= :startOfToday AND dueDate <= :endOfToday
        ORDER BY
            CASE WHEN hasTime THEN 0 ELSE 1 END,
            dueDate ASC,
            priority DESC,
            sortOrder ASC
        """
    )
    fun observeDueToday(startOfToday: Long, endOfToday: Long): Flow<List<Task>>
    /** PRD OQ-1: count of open tasks whose due instant has passed. */
    @Query(
        """
        SELECT COUNT(*) FROM tasks
        WHERE deletedAt IS NULL AND status = 'OPEN'
          AND dueDate IS NOT NULL
          AND ((hasTime = 1 AND dueDate < :now) OR (hasTime = 0 AND dueDate < :endOfToday))
        """
    )
    fun observeOverdueCount(now: Long, endOfToday: Long): Flow<Int>
    /** Full overdue list, oldest due first. */
    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL AND status = 'OPEN'
          AND dueDate IS NOT NULL
          AND ((hasTime = 1 AND dueDate < :now) OR (hasTime = 0 AND dueDate < :endOfToday))
        ORDER BY dueDate ASC, priority DESC
        """
    )
    fun observeOverdue(now: Long, endOfToday: Long): Flow<List<Task>>
    // ---------- Upcoming ----------
    /**
     * PRD 7.1: grouped by date, next 7 days by default, scrollable further.
     * Groups are formed in the repository from this flat, date-ordered list.
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL AND status = 'OPEN'
          AND dueDate IS NOT NULL AND dueDate > :endOfToday
        ORDER BY dueDate ASC, priority DESC, sortOrder ASC
        """
    )
    fun observeUpcoming(endOfToday: Long): Flow<List<Task>>
    // ---------- All / completed / trash ----------
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND status = 'OPEN' ORDER BY createdAt DESC")
    fun observeAllOpen(): Flow<List<Task>>
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND status = 'OPEN' AND listId IS NULL ORDER BY createdAt DESC")
    fun observeInboxOpen(): Flow<List<Task>>
    @Query(
        """
        SELECT * FROM tasks WHERE deletedAt IS NULL AND status = 'COMPLETED'
        ORDER BY completedAt DESC
        """
    )
    fun observeCompleted(): Flow<List<Task>>
    @Query("SELECT * FROM tasks WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<Task>>
    // ---------- Per list / tag ----------
    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL AND listId = :listId AND status = 'OPEN'
        ORDER BY sortOrder ASC, createdAt ASC
        """
    )
    fun observeByList(listId: Long): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE listId IN (:listIds)")
    suspend fun allForLists(listIds: List<Long>): List<Task>
    @Query(
        """
        SELECT t.* FROM tasks t
        INNER JOIN task_tags tt ON tt.taskId = t.id
        WHERE t.deletedAt IS NULL AND tt.tagId = :tagId AND t.status = 'OPEN'
        ORDER BY t.dueDate IS NULL, t.dueDate ASC, t.createdAt DESC
        """
    )
    fun observeByTag(tagId: Long): Flow<List<Task>>
    // ---------- Filters & sorting ----------
    /**
     * All Tasks with the PRD 7.1 sorting options. SQLite cannot parameterize an
     * ORDER BY column, so the sort mode is selected via a safe whitelist of
     * inlined literals rather than string concatenation of user input.
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL AND status = 'OPEN'
          AND (:listId IS NULL OR listId = :listId)
          AND (:priority IS NULL OR priority = :priority)
          AND (:dueFrom IS NULL OR dueDate >= :dueFrom)
          AND (:dueTo IS NULL OR dueDate <= :dueTo)
        ORDER BY
            CASE WHEN :sort = 'DUE' THEN (dueDate IS NULL) END ASC,
            CASE WHEN :sort = 'DUE' THEN dueDate END ASC,
            CASE WHEN :sort = 'PRIORITY' THEN
                CASE priority WHEN 'HIGH' THEN 3 WHEN 'MEDIUM' THEN 2 WHEN 'LOW' THEN 1 ELSE 0 END
            END DESC,
            CASE WHEN :sort = 'CREATED' THEN createdAt END DESC,
            CASE WHEN :sort = 'MANUAL' THEN sortOrder END ASC
        """
    )
    fun observeFiltered(
        listId: Long?,
        priority: Priority?,
        dueFrom: Long?,
        dueTo: Long?,
        sort: String
    ): Flow<List<Task>>
    // ---------- Mutations ----------
    @Query("UPDATE tasks SET status = :status, completedAt = :completedAt, updatedAt = :now WHERE id = :id")
    suspend fun setStatus(id: Long, status: TaskStatus, completedAt: Long?, now: Long)
    @Query("UPDATE tasks SET dueDate = :dueDate, hasTime = :hasTime, updatedAt = :now WHERE id = :id")
    suspend fun setDue(id: Long, dueDate: Long?, hasTime: Boolean, now: Long)
    @Query("UPDATE tasks SET deletedAt = :deletedAt, updatedAt = :now WHERE id = :id")
    suspend fun setDeletedAt(id: Long, deletedAt: Long?, now: Long)
    /** PRD OQ-8: purge trashed rows older than the retention window. */
    @Query("DELETE FROM tasks WHERE deletedAt IS NOT NULL AND deletedAt < :cutoff")
    suspend fun purgeTrash(cutoff: Long): Int
    @Query("SELECT COUNT(*) FROM tasks WHERE deletedAt IS NULL AND status = 'OPEN'")
    fun observeOpenCount(): Flow<Int>
    /**
     * Event writes go through TaskDao as well as EventDao so code paths that
     * already hold a TaskDao (the points engine, the reminder presenter) do not
     * need a second dependency just to log an event.
     */
    @Insert
    suspend fun logEvent(event: Event): Long
    // ---------- Recurrence materialization (v1.1 engine, schema ready now) ----------
    @Query("SELECT * FROM tasks WHERE seriesId = :seriesId AND occurrenceDate >= :fromDate AND status = 'OPEN' AND deletedAt IS NULL")
    suspend fun openOccurrencesFrom(seriesId: Long, fromDate: Long): List<Task>
    @Query("UPDATE tasks SET deletedAt = :now, updatedAt = :now WHERE seriesId = :seriesId AND occurrenceDate >= :fromDate AND status = 'OPEN' AND deletedAt IS NULL")
    suspend fun trashOpenOccurrencesFrom(seriesId: Long, fromDate: Long, now: Long): Int
    @Query("SELECT COUNT(*) FROM tasks WHERE seriesId = :seriesId AND occurrenceDate = :occurrenceDate AND deletedAt IS NULL")
    suspend fun countOccurrence(seriesId: Long, occurrenceDate: Long): Int
    @Query("SELECT * FROM tasks WHERE seriesId = :seriesId ORDER BY occurrenceDate ASC")
    fun observeOccurrences(seriesId: Long): Flow<List<Task>>
    /**
     * PRD 10 / 7.8: open tasks that were overdue on the given calendar day, used
     * by Day Close to apply the per-day overdue penalty.
     */
    @Query(
        """
        SELECT * FROM tasks
        WHERE deletedAt IS NULL AND status = 'OPEN'
          AND dueDate IS NOT NULL AND dueDate < :dayStart
          AND ((hasTime = 0) OR (dueDate < :dayEnd))
        """
    )
    suspend fun overdueAsOf(dayStart: Long, dayEnd: Long): List<Task>
}
