
package com.joe.taskmanager.ui.screens
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.joe.taskmanager.R
/** PRD 7.1 Completed view: completed tasks, newest first. */
@Composable
fun CompletedScreen(onTaskClick: (Long) -> Unit) {
    TaskListScreen(
        listId = null,
        onTaskClick = onTaskClick,
        onAddTask = {},
        title = stringResource(R.string.nav_completed)
    )
}
