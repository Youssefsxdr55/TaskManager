package com.joe.taskmanager.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskWidgetUpdater @Inject constructor(@ApplicationContext private val context: Context) {
    suspend fun updateToday() = runCatching { TodayTasksWidget().updateAll(context) }
    suspend fun updatePomodoro() = runCatching { PomodoroWidget().updateAll(context) }
}
