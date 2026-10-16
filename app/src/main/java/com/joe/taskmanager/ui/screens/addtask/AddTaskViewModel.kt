package com.joe.taskmanager.ui.screens.addtask

import androidx.lifecycle.SavedStateHandle
import com.joe.taskmanager.ui.navigation.longArg
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.local.dao.TagDao
import com.joe.taskmanager.data.local.dao.TaskListDao
import com.joe.taskmanager.data.local.entity.Priority
import com.joe.taskmanager.data.local.entity.ReminderType
import com.joe.taskmanager.data.local.entity.Tag
import com.joe.taskmanager.data.local.entity.TaskList
import com.joe.taskmanager.data.repository.TaskRepository
import com.joe.taskmanager.util.DateUtils
import com.joe.taskmanager.util.QuickAddParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderDraft(
    val type: ReminderType,
    val offsetMinutes: Int? = null,
    val absoluteTime: Long? = null
)

data class AddTaskUiState(
    val title: String = "",
    val notes: String = "",
    val priority: Priority = Priority.NONE,
    val dueDate: Long? = null,
    val hasTime: Boolean = false,
    val listId: Long? = null,
    val subtasks: List<String> = emptyList(),
    val newSubtask: String = "",
    val reminders: List<ReminderDraft> = emptyList(),
    val reminderChanged: Boolean = false,
    val availableLists: List<TaskList> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val selectedTagIds: Set<Long> = emptySet(),
    val saving: Boolean = false,
    val quickAddParsed: Boolean = false,
    val dateChipLabel: String = "Date",
    val priorityLabel: String = "Priority",
    val reminderLabel: String = "Reminder"
)

@HiltViewModel
class AddTaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val listDao: TaskListDao,
    private val tagDao: TagDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTaskUiState())
    val uiState: StateFlow<AddTaskUiState> = _uiState.asStateFlow()
    private var taskId: Long? = null

    init {
        val id = savedStateHandle.longArg("taskId")?.takeIf { it > 0 }
        val initialListId = savedStateHandle.longArg("listId")?.takeIf { it > 0 }
        val initialDueDate = savedStateHandle.longArg("dueDate")?.takeIf { it > 0 }
        taskId = id
        viewModelScope.launch {
            combine(listDao.observeAll(), tagDao.observeAll()) { lists, tags -> lists to tags }
                .collect { (lists, tags) -> _uiState.update { it.copy(availableLists = lists, availableTags = tags) } }
        }
        if (id != null) loadExisting(id) else _uiState.update { it.copy(listId = initialListId, dueDate = initialDueDate) }
    }

    private fun loadExisting(id: Long) {
        viewModelScope.launch {
            val task = repository.getTask(id) ?: return@launch
            val subtasks = repository.observeSubtasks(id).first().map { it.title }
            val tags = repository.observeTagsForTask(id).first().map { it.id }.toSet()
            val reminders = repository.observeReminders(id).first().map {
                ReminderDraft(it.type, it.offsetMinutes, it.absoluteTime)
            }
            _uiState.update {
                it.copy(
                    title = task.title,
                    notes = task.notes,
                    priority = task.priority,
                    dueDate = task.dueDate,
                    hasTime = task.hasTime,
                    listId = task.listId,
                    subtasks = subtasks,
                    selectedTagIds = tags,
                    reminders = reminders
                )
            }
        }
    }

    fun onTitleChange(value: String) = _uiState.update { it.copy(title = value, quickAddParsed = false) }
    fun parseQuickAdd() = _uiState.update { state ->
        val result = QuickAddParser.parse(state.title)
        state.copy(title = result.title, dueDate = result.dueDate ?: state.dueDate,
            hasTime = result.hasTime, quickAddParsed = true)
    }
    fun onNotesChange(value: String) = _uiState.update { it.copy(notes = value) }
    fun onNewSubtaskChange(value: String) = _uiState.update { it.copy(newSubtask = value) }
    fun selectList(id: Long?) = _uiState.update { it.copy(listId = id) }
    fun toggleTag(id: Long) = _uiState.update {
        val ids = if (id in it.selectedTagIds) it.selectedTagIds - id else it.selectedTagIds + id
        it.copy(selectedTagIds = ids)
    }

    fun onPrioritySelected(name: String) = _uiState.update {
        val priority = Priority.valueOf(name)
        it.copy(priority = priority, priorityLabel = priority.name.lowercase().replaceFirstChar { c -> c.uppercase() })
    }

    fun onCycleDateChip() = _uiState.update {
        val now = System.currentTimeMillis()
        val next = when {
            it.dueDate == null -> DateUtils.startOfToday()
            DateUtils.isToday(it.dueDate!!) -> DateUtils.plusDays(now, 1)
            DateUtils.isTomorrow(it.dueDate!!) -> DateUtils.plusDays(now, 7)
            else -> null
        }
        it.copy(dueDate = next, hasTime = false)
    }

    fun onDateSelected(dayMillis: Long) = _uiState.update {
        it.copy(dueDate = DateUtils.startOfDay(dayMillis), hasTime = false)
    }

    fun onTimeSelected(hour: Int, minute: Int) = _uiState.update {
        val day = it.dueDate ?: DateUtils.startOfToday()
        it.copy(dueDate = DateUtils.combine(day, hour, minute), hasTime = true)
    }

    fun clearDueDate() = _uiState.update {
        it.copy(dueDate = null, hasTime = false, reminders = it.reminders.filter { reminder -> reminder.type == ReminderType.ABSOLUTE }, reminderLabel = "Reminder", reminderChanged = true)
    }

    /** Multiple reminders are allowed, but relative reminders require an explicit due time. */
    fun toggleReminder(type: ReminderType, offsetMinutes: Int? = null) = _uiState.update { state ->
        if (type != ReminderType.ABSOLUTE && (state.dueDate == null || !state.hasTime)) return@update state
        val draft = ReminderDraft(type, offsetMinutes)
        val next = if (draft in state.reminders) state.reminders - draft else state.reminders + draft
        state.copy(reminders = next, reminderChanged = true)
    }

    fun addAbsoluteReminder(time: Long) = _uiState.update { state ->
        val draft = ReminderDraft(ReminderType.ABSOLUTE, absoluteTime = time)
        if (draft in state.reminders) state else state.copy(
            reminders = state.reminders + draft,
            reminderChanged = true
        )
    }


    fun removeAbsoluteReminder(time: Long) = _uiState.update { state ->
        state.copy(
            reminders = state.reminders.filterNot { it.type == ReminderType.ABSOLUTE && it.absoluteTime == time },
            reminderChanged = true
        )
    }

    fun onCyclePriority() = _uiState.update {
        val next = when (it.priority) {
            Priority.NONE -> Priority.LOW
            Priority.LOW -> Priority.MEDIUM
            Priority.MEDIUM -> Priority.HIGH
            Priority.HIGH -> Priority.NONE
        }
        it.copy(priority = next, priorityLabel = if (next == Priority.NONE) "Priority" else next.name.lowercase().replaceFirstChar { c -> c.uppercase() })
    }

    fun addSubtask() = _uiState.update {
        if (it.newSubtask.isBlank()) it else it.copy(subtasks = it.subtasks + it.newSubtask.trim(), newSubtask = "")
    }

    fun removeSubtask(title: String) = _uiState.update { it.copy(subtasks = it.subtasks.filterNot { item -> item == title }) }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank() || state.saving) return
        _uiState.update { it.copy(saving = true) }
        viewModelScope.launch {
            val existing = taskId
            if (existing == null) {
                repository.createTask(
                    title = state.title,
                    notes = state.notes,
                    listId = state.listId,
                    dueDate = state.dueDate,
                    hasTime = state.hasTime,
                    priority = state.priority,
                    tags = state.selectedTagIds.toList(),
                    subtasks = state.subtasks,
                    reminders = state.reminders.map {
                        TaskRepository.NewReminder(it.type, it.offsetMinutes, it.absoluteTime)
                    }
                )
            } else {
                repository.updateTask(
                    id = existing,
                    title = state.title,
                    notes = state.notes,
                    listId = state.listId,
                    clearDue = state.dueDate == null,
                    dueDate = state.dueDate,
                    hasTime = state.hasTime,
                    priority = state.priority
                )
                repository.replaceTaskTags(existing, state.selectedTagIds)
                repository.replaceSubtasks(existing, state.subtasks)
                if (state.reminderChanged) {
                    repository.replaceReminders(existing, state.reminders.map {
                        TaskRepository.NewReminder(it.type, it.offsetMinutes, it.absoluteTime)
                    })
                }
            }
            _uiState.update { it.copy(saving = false) }
            onSaved()
        }
    }
}
