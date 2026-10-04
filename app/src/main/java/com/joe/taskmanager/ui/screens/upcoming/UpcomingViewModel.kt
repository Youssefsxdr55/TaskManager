
package com.joe.taskmanager.ui.screens.upcoming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.data.local.entity.TaskStatus
import com.joe.taskmanager.data.repository.TaskRepository
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
        .map { tasks -> tasks.groupBy { DateUtils.startOfDay(it.dueDate!!) }
            .toSortedMap()
            .map { (dateStart, group) ->
                UpcomingGroup(
                    dateStart = dateStart,
                    relativeLabel = "${DateUtils.daysBetween(System.currentTimeMillis(), dateStart)} d",
                    tasks = group.sortedWith(
                        compareBy({ !it.hasTime }, { it.dueDate }, { it.priority.ordinal })
                    )
                )
            } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UpcomingUiState())

    fun toggleComplete(taskId: Long, status: TaskStatus) {
        viewModelScope.launch { repository.setCompleted(taskId, status == TaskStatus.OPEN) }
    }
}
