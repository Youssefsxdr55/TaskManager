
package com.joe.taskmanager.ui.screens.list

import androidx.lifecycle.SavedStateHandle
import com.joe.taskmanager.ui.navigation.longArg
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.res.stringResource
import com.joe.taskmanager.R
import com.joe.taskmanager.data.local.dao.TaskDao
import com.joe.taskmanager.data.local.entity.Priority
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.data.repository.TaskRepository
import com.joe.taskmanager.ui.components.SnoozeChoice
import com.joe.taskmanager.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GenericListUiState(
    val tasks: List<Task> = emptyList(),
    val emptyTextRes: Int = R.string.empty_all_title,
    val sortMode: String = "DUE"
)

@HiltViewModel
class GenericTaskListViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val taskDao: TaskDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val snackbarHostState = SnackbarHostState()

    private val listId: Long? = savedStateHandle.longArg("listId")
    private val tagId: Long? = savedStateHandle.longArg("tagId")
    private val inboxOnly: Boolean = savedStateHandle.get<Boolean>("inboxOnly") ?: false
    private val sortMode = MutableStateFlow("DUE")

    private val source: Flow<List<Task>> = when {
        inboxOnly -> repository.observeInboxOpen()
        tagId != null -> repository.observeByTag(tagId)
        listId != null -> repository.observeByList(listId)
        else -> sortMode.flatMapLatest { repository.observeFiltered(null, null, null, null, it) }
    }

    val uiState: StateFlow<GenericListUiState> = combine(source, sortMode) { tasks, sort ->
        GenericListUiState(tasks, sortMode = sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GenericListUiState())

    fun setSortMode(mode: String) {
        if (mode in setOf("DUE", "PRIORITY", "CREATED", "MANUAL")) sortMode.value = mode
    }

    /**
     * Completing shows an Undo snackbar (PRD 7.1). Deleting is available from the
     * detail screen; the swipe-to-complete path is the one that must be undoable
     * here since it is one gesture away from being accidental.
     */
    fun toggleComplete(taskId: Long) {
        viewModelScope.launch {
            repository.setCompleted(taskId, completed = true)
            val result = snackbarHostState.showSnackbar(
                message = "Done",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                repository.setCompleted(taskId, completed = false)
            }
        }
    }
    fun snooze(taskId: Long, choice: SnoozeChoice) {
        viewModelScope.launch {
            val task = repository.getTask(taskId) ?: return@launch
            val now = System.currentTimeMillis()
            val newDue = when (choice) {
                SnoozeChoice.LATER_TODAY -> if ((task.dueDate ?: 0L) < DateUtils.endOfToday())
                    DateUtils.combine(DateUtils.startOfToday(), 18, 0)
                else DateUtils.combine(DateUtils.plusDays(now, 1), 9, 0)
                SnoozeChoice.TOMORROW -> DateUtils.combine(DateUtils.plusDays(now, 1), 9, 0)
                SnoozeChoice.NEXT_WEEK -> DateUtils.combine(DateUtils.plusDays(now, 7), 9, 0)
                is SnoozeChoice.Custom -> DateUtils.combine(choice.dayStart, choice.hour, choice.minute)
            }
            repository.postpone(taskId, newDue, hasTime = true, source = "swipe")
        }
    }

}
