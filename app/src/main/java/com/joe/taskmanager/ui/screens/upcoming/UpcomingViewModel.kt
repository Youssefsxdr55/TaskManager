
package com.joe.taskmanager.ui.screens.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.data.local.entity.TaskStatus
import com.joe.taskmanager.data.repository.TaskRepository
import com.joe.taskmanager.ui.components.SnoozeChoice
import com.joe.taskmanager.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class UpcomingGroup(
    val dateStart: Long,
    val relativeLabel: String,
    val tasks: List<Task>
)

data class UpcomingUiState(val groups: List<UpcomingGroup> = emptyList())

@HiltViewModel
class UpcomingViewModel @Inject constructor(
    private val repository: TaskRepository
) : ViewModel() {

    /**
     * PRD 7.1: grouped by date, next 7 days by default, scrollable further.
     * Flat list -> groups on the current day boundary, which also means the
     * grouping stays correct across midnight without extra scheduling.
     */
    val uiState: StateFlow<UpcomingUiState> = repository.observeUpcoming()
        .map { tasks -> UpcomingUiState(tasks.groupBy { DateUtils.startOfDay(it.dueDate!!) }
            .toSortedMap()
            .map { (dateStart, group) ->
                UpcomingGroup(
                    dateStart = dateStart,
                    relativeLabel = "${DateUtils.daysBetween(System.currentTimeMillis(), dateStart)} d",
                    tasks = group.sortedWith(
                        compareBy({ !it.hasTime }, { it.dueDate }, { it.priority.ordinal })
                    )
                )
            }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UpcomingUiState())

    val snackbarHostState = androidx.compose.material3.SnackbarHostState()

    fun toggleComplete(taskId: Long, status: TaskStatus) {
        viewModelScope.launch { repository.setCompleted(taskId, status == TaskStatus.OPEN) }
    }

    fun showUndoSnackbar(taskId: Long, message: String, actionLabel: String) {
        viewModelScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                duration = androidx.compose.material3.SnackbarDuration.Short
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
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
