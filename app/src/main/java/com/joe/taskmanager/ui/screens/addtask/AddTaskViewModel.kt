
package com.joe.taskmanager.ui.screens.addtask

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.local.dao.TaskListDao
import com.joe.taskmanager.data.local.entity.Priority
import com.joe.taskmanager.data.local.entity.ReminderType
import com.joe.taskmanager.data.repository.TaskRepository
import com.joe.taskmanager.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddTaskUiState(
    val title: String = "",
    val notes: String = "",
    val priority: Priority = Priority.NONE,
    val dueDate: Long? = null,
    val hasTime: Boolean = false,
    val listId: Long? = null,
    val subtasks: List<String> = emptyList(),
    val newSubtask: String = "",
    val reminder: ReminderType? = null,
    val saving: Boolean = false,
    val dateChipLabel: String = "Date",
    val priorityLabel: String = "Priority",
    val reminderLabel: String = "Reminder"
)

@HiltViewModel
class AddTaskViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val listDao: TaskListDao,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTaskUiState())
    val uiState: StateFlow<AddTaskUiState> = _uiState.asStateFlow()

    private var taskId: Long? = null
    private var reminderOffsetMinutes: Int? = null

    init {
        val id = savedStateHandle.get<String>("taskId")?.toLongOrNull()
        val listId = savedStateHandle.get<String>("listId")?.toLongOrNull()
        taskId = id
        if (id != null) loadExisting(id) else _uiState.update { it.copy(listId = listId) }
    }

    private fun loadExisting(id: Long) {
        viewModelScope.launch {
            val task = repository.getTask(id) ?: return@launch
            _uiState.update {
                it.copy(
                    title = task.title,
                    notes = task.notes,
                    priority = task.priority,
                    dueDate = task.dueDate,
                    hasTime = task.hasTime,
                    listId = task.listId
                )
            }
        }
    }

    fun onTitleChange(v: String) = _uiState.update { it.copy(title = v) }
    fun onNotesChange(v: String) = _uiState.update { it.copy(notes = v) }
    fun onNewSubtaskChange(v: String) = _uiState.update { it.copy(newSubtask = v) }

    fun onPrioritySelected(name: String) = _uiState.update {
        it.copy(priority = Priority.valueOf(name), priorityLabel = name.lowercase().replaceFirstChar { c -> c.uppercase() })
    }

    /**
     * Fast-capture affordances: tapping a chip cycles through the useful presets
     * so a common task needs no dialog. Setter methods remain for finer control.
     */
    fun onCycleDateChip() = _uiState.update {
        val now = System.currentTimeMillis()
        val next = when {
            it.dueDate == null -> DateUtils.startOfToday()
            DateUtils.isToday(it.dueDate!!) -> DateUtils.plusDays(now, 1)
            DateUtils.isTomorrow(it.dueDate!!) -> DateUtils.plusDays(now, 7)
            else -> null
        }
        it.copy(
            dueDate = next,
            hasTime = false,
            dateChipLabel = when {
                next == null -> "Date"
                DateUtils.isToday(next) -> "Today"
                DateUtils.isTomorrow(next) -> "Tomorrow"
                else -> "Next week"
            }
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

    fun onCycleReminder() = _uiState.update {
        when (it.reminder) {
            null -> it.copy(reminder = ReminderType.AT_DUE_TIME, hasTime = true, reminderLabel = "At due time")
            ReminderType.AT_DUE_TIME -> it.copy(reminder = ReminderType.BEFORE, reminderOffsetMinutes = 30, reminderLabel = "30 min before")
            ReminderType.BEFORE -> it.copy(reminder = null, reminderOffsetMinutes = null, reminderLabel = "Reminder")
            else -> it.copy(reminder = null, reminderOffsetMinutes = null, reminderLabel = "Reminder")
        }
    }

    fun addSubtask() = _uiState.update {
        if (it.newSubtask.isBlank()) it
        else it.copy(subtasks = it.subtasks + it.newSubtask.trim(), newSubtask = "")
    }

    fun removeSubtask(title: String) = _uiState.update {
        it.copy(subtasks = it.subtasks.filterNot { t -> t == title })
    }

    fun save(onSaved: () -> Unit) {
        val state = _uiState.value
        if (state.title.isBlank() || state.saving) return
        _uiState.update { it.copy(saving = true) }

        viewModelScope.launch {
            val existing = taskId
            if (existing == null) {
                val reminder = state.reminder?.let { type ->
                    TaskRepository.NewReminder(
                        type = type,
                        offsetMinutes = reminderOffsetMinutes
                    )
                }
                repository.createTask(
                    title = state.title,
                    notes = state.notes,
                    listId = state.listId,
                    dueDate = state.dueDate,
                    hasTime = state.hasTime,
                    priority = state.priority,
                    subtasks = state.subtasks,
                    reminders = listOfNotNull(reminder)
                )
            } else {
                repository.updateTask(
                    id = existing,
                    title = state.title,
                    notes = state.notes,
                    listId = state.listId,
                    dueDate = state.dueDate,
                    clearDue = state.dueDate == null,
                    hasTime = state.hasTime,
                    priority = state.priority
                )
            }
            _uiState.update { it.copy(saving = false) }
            onSaved()
        }
    }
}
