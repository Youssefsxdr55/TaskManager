
package com.joe.taskmanager.ui.screens.list

import androidx.lifecycle.SavedStateHandle
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GenericListUiState(
    val tasks: List<Task> = emptyList(),
    val emptyTextRes: Int = R.string.empty_all_title
)

@HiltViewModel
class GenericTaskListViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val taskDao: TaskDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val snackbarHostState = SnackbarHostState()

    private val listId: Long? = savedStateHandle.get<String>("listId")?.toLongOrNull()
    private val tagId: Long? = savedStateHandle.get<String>("tagId")?.toLongOrNull()

    private val source: Flow<List<Task>> = when {
        tagId != null -> repository.observeByTag(tagId)
        listId != null -> repository.observeByList(listId)
        else -> repository.observeAllOpen()
    }

    val uiState: StateFlow<GenericListUiState> = source
        .map { GenericListUiState(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GenericListUiState())

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
}
