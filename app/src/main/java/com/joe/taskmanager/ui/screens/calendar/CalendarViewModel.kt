package com.joe.taskmanager.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.local.dao.TaskDao
import com.joe.taskmanager.data.local.dao.HabitDao
import com.joe.taskmanager.data.repository.TaskRepository
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.data.local.entity.Habit
import com.joe.taskmanager.data.local.entity.HabitLog
import com.joe.taskmanager.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

@HiltViewModel
class CalendarViewModel @Inject constructor(private val dao: TaskDao, private val repository: TaskRepository, private val habitDao: HabitDao) : ViewModel() {
    private val month = MutableStateFlow(DateUtils.startOfMonth(System.currentTimeMillis()))
    val monthStart: StateFlow<Long> = month
    val tasks: StateFlow<List<Task>> = month.flatMapLatest { start ->
        val end = Calendar.getInstance().apply { timeInMillis = start; add(Calendar.MONTH, 1); add(Calendar.MILLISECOND, -1) }.timeInMillis
        dao.observeDueBetween(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val habits: StateFlow<List<Habit>> = habitDao.observeActive().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val habitLogs: StateFlow<List<HabitLog>> = month.flatMapLatest { start ->
        val end = Calendar.getInstance().apply { timeInMillis = start; add(Calendar.MONTH, 1); add(Calendar.MILLISECOND, -1) }.timeInMillis
        habitDao.observeAllLogsBetween(start, end)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    fun moveMonth(delta: Int): Long { val next = DateUtils.startOfMonth(Calendar.getInstance().apply { timeInMillis = month.value; add(Calendar.MONTH, delta) }.timeInMillis); month.value = next; return next }
    fun selectMonth(date: Long) { month.value = DateUtils.startOfMonth(date) }
    fun toggle(task: Task) { viewModelScope.launch { repository.setCompleted(task.id, task.status.name != "COMPLETED") } }
}
