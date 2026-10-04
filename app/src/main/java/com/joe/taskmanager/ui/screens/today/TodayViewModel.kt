
package com.joe.taskmanager.ui.screens.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.local.dao.TaskListDao
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.data.local.entity.TaskStatus
import com.joe.taskmanager.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodayUiState(
    val todayTasks: List<Task> = emptyList(),
    val overdueCount: Int = 0,
    val listNames: Map<Long?, String> = emptyMap()
)

@HiltViewModel
class TodayViewModel @Inject constructor(
    private val repository: TaskRepository,
    private val listDao: TaskListDao
) : ViewModel() {

    /**
     * A real SnackbarHostState, created once and held by the ViewModel so the
     * screen can pass it to the scaffold and drive the Undo action from here.
     * It must not be a StateFlow: the scaffold expects the state object itself.
     */
    val snackbarHostState = androidx.compose.material3.SnackbarHostState()

    /** Which task the visible snackbar refers to, so Undo cannot act on a newer task. */
    private var pendingUndoTaskId: Long? = null

    val uiState: StateFlow<TodayUiState> =
        combine(
            repository.observeDueToday(),
            repository.observeOverdueCount(),
            listDao.observeAll()
        ) { today, overdue, lists ->
            TodayUiState(
                todayTasks = today,
                overdueCount = overdue,
                listNames = lists.associate { it.id to it.name }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /**
     * Completing shows an Undo snackbar (PRD 7.1). The undo reverts the status
     * only; points are neither re-awarded nor removed, because the ledger is keyed
     * on the task identity rather than on a completion count (PRD 7.8).
     */
    fun toggleComplete(taskId: Long, currentStatus: TaskStatus) {
        if (currentStatus == TaskStatus.COMPLETED) {
            viewModelScope.launch { repository.setCompleted(taskId, completed = false) }
            return
        }
        viewModelScope.launch { repository.setCompleted(taskId, completed = true) }
    }

    /**
     * Called by the screen after a completion so the snackbar can offer Undo.
     * Kept out of toggleComplete because the message and action label are
     * string resources owned by the UI layer.
     */
    fun showUndoSnackbar(taskId: Long, message: String, actionLabel: String) {
        pendingUndoTaskId = taskId
        viewModelScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                pendingUndoTaskId?.let { id ->
                    repository.setCompleted(id, completed = false)
                }
            }
            pendingUndoTaskId = null
        }
    }
}
