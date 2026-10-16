
package com.joe.taskmanager.ui.screens.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import com.joe.taskmanager.ui.navigation.longArg
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.local.entity.Reminder
import com.joe.taskmanager.data.local.entity.Subtask
import com.joe.taskmanager.data.local.entity.Tag
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.data.repository.TaskRepository
import com.joe.taskmanager.ui.components.SnoozeChoice
import com.joe.taskmanager.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskDetailUiState(
    val task: Task? = null,
    val subtasks: List<Subtask> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val reminders: List<Reminder> = emptyList()
) {
    val title: String get() = task?.title ?: ""
    val completed: Boolean get() = task?.status?.name == "COMPLETED"
}

@HiltViewModel
class TaskDetailViewModel @Inject constructor(
    private val repository: TaskRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    var showSnooze by mutableStateOf(false)
        private set

    fun openSnooze() { showSnooze = true }
    fun closeSnooze() { showSnooze = false }

    private val taskId: Long = savedStateHandle.longArg("taskId") ?: -1L

    val uiState: StateFlow<TaskDetailUiState> = combine(
        repository.observeTask(taskId),
        repository.observeSubtasks(taskId),
        repository.observeTagsForTask(taskId),
        repository.observeReminders(taskId)
    ) { task, subtasks, tags, reminders ->
        TaskDetailUiState(task, subtasks, tags, reminders)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskDetailUiState())

    fun toggleComplete() {
        val state = uiState.value
        viewModelScope.launch {
            repository.setCompleted(taskId, completed = !state.completed)
        }
    }

    fun toggleSubtask(id: Long, done: Boolean) {
        viewModelScope.launch { repository.setSubtaskDone(id, done) }
    }

    /**
     * Swipe-left snooze quick options (PRD 7.1). Every path records a
     * TASK_POSTPONED event so postponement analytics and the v1.3 alert work.
     */
    fun snooze(choice: SnoozeChoice) {
        closeSnooze()
        viewModelScope.launch {
            val task = repository.getTask(taskId) ?: return@launch
            val now = System.currentTimeMillis()
            val newDue = when (choice) {
                SnoozeChoice.LATER_TODAY -> {
                    val today = DateUtils.startOfToday()
                    val base = task.dueDate ?: today
                    if (base < DateUtils.endOfToday()) {
                        DateUtils.combine(today, 18, 0)
                    } else DateUtils.combine(DateUtils.plusDays(today, 1), 9, 0)
                }
                SnoozeChoice.TOMORROW -> DateUtils.combine(DateUtils.plusDays(now, 1), 9, 0)
                SnoozeChoice.NEXT_WEEK -> DateUtils.combine(DateUtils.plusDays(now, 7), 9, 0)
                is SnoozeChoice.Custom -> DateUtils.combine(choice.dayStart, choice.hour, choice.minute)
            }
            repository.postpone(taskId, newDue, hasTime = true, source = "swipe")
        }
    }

    fun moveToTrash(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.moveToTrash(taskId)
            onDeleted()
        }
    }
}
