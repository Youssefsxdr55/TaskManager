
package com.joe.taskmanager.ui.screens

import androidx.compose.runtime.Composable

/** PRD 7.1 All Tasks view: everything open, grouped by list with sorting. */
@Composable
fun AllTasksScreen(
    onTaskClick: (Long) -> Unit,
    onAddTask: () -> Unit
) {
    TaskListScreen(
        listId = null,
        onTaskClick = onTaskClick,
        onAddTask = onAddTask,
        title = null
    )
}
