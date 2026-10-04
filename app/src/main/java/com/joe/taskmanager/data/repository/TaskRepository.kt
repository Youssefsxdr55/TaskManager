
package com.joe.taskmanager.data.repository

import com.joe.taskmanager.data.local.dao.EventDao
import com.joe.taskmanager.data.local.dao.ReminderDao
import com.joe.taskmanager.data.local.dao.SubtaskDao
import com.joe.taskmanager.data.local.dao.TagDao
import com.joe.taskmanager.data.local.dao.TaskDao
import com.joe.taskmanager.data.local.dao.TaskListDao
import com.joe.taskmanager.data.local.entity.EventType
import com.joe.taskmanager.data.local.entity.Priority
import com.joe.taskmanager.data.local.entity.Reminder
import com.joe.taskmanager.data.local.entity.ReminderType
import com.joe.taskmanager.data.local.entity.Subtask
import com.joe.taskmanager.data.local.entity.Tag
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.data.local.entity.TaskList
import com.joe.taskmanager.data.local.entity.TaskStatus
import com.joe.taskmanager.data.local.entity.TaskTag
import com.joe.taskmanager.notification.ReminderScheduler
import com.joe.taskmanager.util.ReminderMath
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single write path for tasks (PRD 4.1 MVVM + repository + unidirectional flow).
 *
 * Every mutation here also: updates `updatedAt`, writes an event, and keeps the
 * reminder schedule in sync. That coupling is the reason callers never touch DAOs
 * directly.
 */
@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val tagDao: TagDao,
    private val listDao: TaskListDao,
    private val reminderDao: ReminderDao,
    private val eventDao: EventDao,
    private val eventLogger: EventLogger,
    private val scheduler: ReminderScheduler
) {
    // ---------- Reads ----------

    fun observeTask(id: Long): Flow<Task?> = taskDao.observeById(id)
    fun observeDueToday(): Flow<List<Task>> =
        taskDao.observeDueToday(com.joe.taskmanager.util.DateUtils.startOfToday(), com.joe.taskmanager.util.DateUtils.endOfToday())
    fun observeOverdue(): Flow<List<Task>> =
        taskDao.observeOverdue(System.currentTimeMillis(), com.joe.taskmanager.util.DateUtils.endOfToday())
    fun observeOverdueCount(): Flow<Int> =
        taskDao.observeOverdueCount(System.currentTimeMillis(), com.joe.taskmanager.util.DateUtils.endOfToday())
    fun observeUpcoming(): Flow<List<Task>> =
        taskDao.observeUpcoming(System.currentTimeMillis(), com.joe.taskmanager.util.DateUtils.endOfToday())
    fun observeAllOpen(): Flow<List<Task>> = taskDao.observeAllOpen()
    fun observeCompleted(): Flow<List<Task>> = taskDao.observeCompleted()
    fun observeTrash(): Flow<List<Task>> = taskDao.observeTrash()
    fun observeByList(listId: Long): Flow<List<Task>> = taskDao.observeByList(listId)
    fun observeByTag(tagId: Long): Flow<List<Task>> = taskDao.observeByTag(tagId)
    fun observeSubtasks(taskId: Long): Flow<List<Subtask>> = subtaskDao.observeForTask(taskId)
    fun observeTagsForTask(taskId: Long): Flow<List<Tag>> = tagDao.observeForTask(taskId)
    fun observeReminders(taskId: Long): Flow<List<Reminder>> = reminderDao.observeForTask(taskId)
    fun observeOpenCount(): Flow<Int> = taskDao.observeOpenCount()

    fun observeFiltered(
        listId: Long?, priority: Priority?, dueFrom: Long?, dueTo: Long?, sort: String
    ): Flow<List<Task>> = taskDao.observeFiltered(listId, priority, dueFrom, dueTo, sort)

    suspend fun getTask(id: Long): Task? = taskDao.getById(id)

    // ---------- Create ----------

    /**
     * Fast capture (PRD 1.3 principle 1): a title-only task is saved in under
     * 5 seconds, so this path does no extra queries when everything is optional.
     */
    suspend fun createTask(
        title: String,
        notes: String = "",
        listId: Long? = null,
        dueDate: Long? = null,
        hasTime: Boolean = false,
        priority: Priority = Priority.NONE,
        tags: List<Long> = emptyList(),
        subtasks: List<String> = emptyList(),
        reminders: List<NewReminder> = emptyList()
    ): Long {
        require(title.isNotBlank()) { "Task title cannot be blank" }

        val now = System.currentTimeMillis()
        val id = taskDao.insert(
            Task(
                title = title.trim(),
                notes = notes,
                listId = listId,
                dueDate = dueDate,
                hasTime = hasTime,
                priority = priority,
                createdAt = now,
                updatedAt = now
            )
        )

        tags.forEach { tagId -> tagDao.upsertLink(TaskTag(taskId = id, tagId = tagId)) }
        subtasks.forEachIndexed { index, subTitle ->
            subtaskDao.insert(Subtask(taskId = id, title = subTitle, sortOrder = index))
        }
        reminders.forEach { new ->
            addReminderInternal(id, new, dueDate, hasTime)
        }

        eventLogger.log(
            EventType.TASK_CREATED, "task", id,
            mapOf("hasDue" to (dueDate != null), "hasTime" to hasTime, "priority" to priority.name)
        )
        return id
    }

    // ---------- Update ----------

    suspend fun updateTask(
        id: Long,
        title: String? = null,
        notes: String? = null,
        listId: Long? = null,
        clearList: Boolean = false,
        dueDate: Long? = null,
        clearDue: Boolean = false,
        hasTime: Boolean? = null,
        priority: Priority? = null
    ) {
        val existing = taskDao.getById(id) ?: return
        val newDue = if (clearDue) null else (dueDate ?: existing.dueDate)
        val newHasTime = hasTime ?: existing.hasTime
        val now = System.currentTimeMillis()

        taskDao.update(
            existing.copy(
                title = title?.trim() ?: existing.title,
                notes = notes ?: existing.notes,
                listId = if (clearList) null else (listId ?: existing.listId),
                dueDate = newDue,
                hasTime = newHasTime,
                priority = priority ?: existing.priority,
                updatedAt = now
            )
        )

        // Due-date or time changes invalidate every booked alarm.
        rescheduleReminders(id)
        eventLogger.log(EventType.TASK_EDITED, "task", id, mapOf("dueChanged" to (newDue != existing.dueDate)))
    }

    /**
     * PRD 7.1: "Points are awarded once per task" is enforced in the gamification
     * layer via a dedupe key; this method only flips status and logs the event, so
     * un-completing does not remove previously awarded points.
     */
    suspend fun setCompleted(id: Long, completed: Boolean) {
        val existing = taskDao.getById(id) ?: return
        if (existing.status == (if (completed) TaskStatus.COMPLETED else TaskStatus.OPEN)) return

        val now = System.currentTimeMillis()
        taskDao.setStatus(
            id = id,
            status = if (completed) TaskStatus.COMPLETED else TaskStatus.OPEN,
            completedAt = if (completed) now else null,
            now = now
        )

        eventLogger.log(
            if (completed) EventType.TASK_COMPLETED else EventType.TASK_UNCOMPLETED,
            "task", id, mapOf("priority" to existing.priority.name)
        )

        if (completed) cancelReminders(id)
    }

    // ---------- Postpone / snooze ----------

    /**
     * Swipe-left snooze (PRD 7.1) and the notification Snooze action share this.
     * Records TASK_POSTPONED so postponement analytics work without extra columns.
     */
    suspend fun postpone(id: Long, newDueDate: Long, hasTime: Boolean, source: String) {
        val existing = taskDao.getById(id) ?: return
        taskDao.setDue(id, newDueDate, hasTime, System.currentTimeMillis())

        eventLogger.log(
            EventType.TASK_POSTPONED, "task", id,
            mapOf("source" to source, "from" to existing.dueDate, "to" to newDueDate)
        )
        rescheduleReminders(id)
    }

    suspend fun postponeByOffset(id: Long, minutes: Int, source: String) {
        val existing = taskDao.getById(id) ?: return
        val base = existing.dueDate ?: System.currentTimeMillis()
        postpone(id, ReminderMath.addMinutes(base, minutes), hasTime = true, source = source)
    }

    // ---------- Soft delete / trash (PRD OQ-8) ----------

    suspend fun moveToTrash(id: Long) {
        taskDao.setDeletedAt(id, System.currentTimeMillis(), System.currentTimeMillis())
        cancelReminders(id)
        eventLogger.log(EventType.TASK_DELETED, "task", id)
    }

    suspend fun restoreFromTrash(id: Long) {
        taskDao.setDeletedAt(id, null, System.currentTimeMillis())
        rescheduleReminders(id)
    }

    suspend fun purgeTrashOlderThan(cutoff: Long): Int = taskDao.purgeTrash(cutoff)

    // ---------- Subtasks ----------

    suspend fun addSubtask(taskId: Long, title: String) {
        val order = subtaskDao.forTask(taskId).size
        subtaskDao.insert(Subtask(taskId = taskId, title = title.trim(), sortOrder = order))
    }

    suspend fun setSubtaskDone(id: Long, done: Boolean) = subtaskDao.setDone(id, done)

    suspend fun deleteSubtask(subtask: Subtask) = subtaskDao.delete(subtask)

    suspend fun reorderSubtasks(ordered: List<Subtask>) {
        ordered.forEachIndexed { index, sub -> subtaskDao.update(sub.copy(sortOrder = index)) }
    }

    // ---------- Tags ----------

    suspend fun createTag(name: String, color: Int): Long {
        val existing = tagDao.findByName(name.trim())
        if (existing != null) return existing.id
        return tagDao.insert(Tag(name = name.trim(), color = color))
    }

    suspend fun attachTag(taskId: Long, tagId: Long) = tagDao.upsertLink(TaskTag(taskId, tagId))
    suspend fun detachTag(taskId: Long, tagId: Long) = tagDao.unlink(taskId, tagId)
    suspend fun deleteTag(tag: Tag) = tagDao.delete(tag)

    // ---------- Reminders ----------

    suspend fun addReminder(taskId: Long, reminder: NewReminder) {
        val task = taskDao.getById(taskId) ?: return
        addReminderInternal(taskId, reminder, task.dueDate, task.hasTime)
    }

    private suspend fun addReminderInternal(
        taskId: Long,
        new: NewReminder,
        dueDate: Long?,
        hasTime: Boolean
    ) {
        val row = Reminder(
            taskId = taskId,
            type = new.type,
            offsetMinutes = new.offsetMinutes,
            absoluteTime = new.absoluteTime
        )
        val scheduledAt = computeSchedule(row, dueDate, hasTime)
        if (scheduledAt == null) return  // PRD 7.3: invalid reminders are not stored.

        val id = reminderDao.insert(row.copy(scheduledAt = scheduledAt))
        if (scheduledAt > System.currentTimeMillis()) {
            scheduler.schedule(id, scheduledAt, isHighPriority = false)
        }
    }

    private fun computeSchedule(reminder: Reminder, dueDate: Long?, hasTime: Boolean): Long? =
        when (reminder.type) {
            ReminderType.ABSOLUTE -> reminder.absoluteTime
            ReminderType.AT_DUE_TIME -> if (hasTime) dueDate else null
            ReminderType.BEFORE -> {
                val offset = reminder.offsetMinutes
                if (!hasTime || dueDate == null || offset == null) null
                else ReminderMath.subtractMinutes(dueDate, offset)
            }
        }

    private suspend fun rescheduleReminders(taskId: Long) {
        val task = taskDao.getById(taskId) ?: return
        val highPriority = task.priority == Priority.HIGH
        reminderDao.forTask(taskId).forEach { reminder ->
            val at = computeSchedule(reminder, task.dueDate, task.hasTime)
            reminderDao.setScheduledAt(reminder.id, at)
            if (at != null && at > System.currentTimeMillis()) {
                scheduler.schedule(reminder.id, at, highPriority)
            } else {
                scheduler.cancel(reminder.id)
            }
        }
    }

    private suspend fun cancelReminders(taskId: Long) {
        reminderDao.forTask(taskId).forEach { scheduler.cancel(it.id) }
    }

    suspend fun deleteReminder(reminder: Reminder) {
        reminderDao.delete(reminder)
        scheduler.cancel(reminder.id)
    }

    /** PRD 6.2: Folder > List > Sublist, three levels maximum. */
    suspend fun validateListDepth(parentListId: Long?): Boolean {
        if (parentListId == null) return true
        val parent = listDao.getById(parentListId) ?: return true
        // A sublist cannot itself contain sublists.
        return parent.parentListId == null
    }

    data class NewReminder(
        val type: ReminderType,
        val offsetMinutes: Int? = null,
        val absoluteTime: Long? = null
    )
}
