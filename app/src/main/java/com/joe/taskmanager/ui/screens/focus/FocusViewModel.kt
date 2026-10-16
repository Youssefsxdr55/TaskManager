package com.joe.taskmanager.ui.screens.focus

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.data.repository.TaskRepository
import com.joe.taskmanager.focus.FocusTimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class FocusViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    repository: TaskRepository
) : ViewModel() {
    val tasks: StateFlow<List<Task>> = repository.observeAllOpen()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun start(focus: Int, short: Int, long: Int, sessions: Int, auto: Boolean, dnd: Boolean, taskId: Long?) {
        val extras = Intent().putExtra(FocusTimerService.EXTRA_FOCUS, focus)
            .putExtra(FocusTimerService.EXTRA_SHORT, short).putExtra(FocusTimerService.EXTRA_LONG, long)
            .putExtra(FocusTimerService.EXTRA_SESSIONS, sessions).putExtra(FocusTimerService.EXTRA_AUTO, auto)
            .putExtra(FocusTimerService.EXTRA_DND, dnd).putExtra(FocusTimerService.EXTRA_TASK, taskId ?: -1L)
        FocusTimerService.command(context, FocusTimerService.ACTION_START, extras)
    }
    fun command(action: String) = FocusTimerService.command(context, action)
}
