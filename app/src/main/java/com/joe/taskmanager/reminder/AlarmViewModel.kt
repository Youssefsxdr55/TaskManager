
package com.joe.taskmanager.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.reminder.ReminderPresenter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlarmUiState(val title: String = "")

@HiltViewModel
class AlarmViewModel @Inject constructor(
    private val presenter: ReminderPresenter
) : ViewModel() {
    private val _uiState = MutableStateFlow(AlarmUiState())
    val uiState: StateFlow<AlarmUiState> = _uiState.asStateFlow()

    private var taskId: Long = -1L
    private var snoozeMinutes: Int = 10
    private var finished = false

    fun bind(taskId: Long, title: String, snoozeMinutes: Int) {
        this.taskId = taskId
        this.snoozeMinutes = snoozeMinutes
        _uiState.value = AlarmUiState(title = title)
    }

    fun onDone() {
        if (finished) return
        finished = true
        viewModelScope.launch { presenter.completeFromNotification(taskId) }
    }

    fun onSnooze() {
        if (finished) return
        finished = true
        viewModelScope.launch { presenter.snoozeFromNotification(taskId, snoozeMinutes) }
    }
}
