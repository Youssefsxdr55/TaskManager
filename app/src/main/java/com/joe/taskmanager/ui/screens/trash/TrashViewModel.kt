
package com.joe.taskmanager.ui.screens.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.local.dao.TaskDao
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PRD OQ-8: 30-day trash. Retention is enforced by TrashPurgeWorker, so this
 * ViewModel only exposes read + restore.
 */
@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: TaskRepository,
    taskDao: TaskDao
) : ViewModel() {

    val trashedTasks: StateFlow<List<Task>> = taskDao.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun restore(taskId: Long) {
        viewModelScope.launch { repository.restoreFromTrash(taskId) }
    }
}
