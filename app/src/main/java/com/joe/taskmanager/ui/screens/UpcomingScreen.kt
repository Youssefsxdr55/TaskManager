
package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.ui.components.TaskRow
import com.joe.taskmanager.ui.util.DateText
import com.joe.taskmanager.ui.screens.upcoming.UpcomingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingScreen(
    onOpenDrawer: () -> Unit,
    onOpenToday: () -> Unit,
    onOpenUpcoming: () -> Unit,
    onOpenCalendar: () -> Unit,
    onTaskClick: (Long) -> Unit,
    onAddTask: () -> Unit,
    viewModel: UpcomingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val completedMessage = stringResource(R.string.task_completed)
    val undoLabel = stringResource(R.string.action_undo)

    ScreenScaffold(
        title = stringResource(R.string.nav_upcoming),
        snackbarHostState = viewModel.snackbarHostState,
        bottomBar = { MainBottomBar(MainTab.UPCOMING, onOpenToday, onOpenUpcoming, onOpenCalendar) },
        topBarActions = { IconButton(onClick = onOpenDrawer) {
            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_tasks))
        } },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_task))
            }
        }
    ) { padding ->
        if (state.groups.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.empty_upcoming_title),
                modifier = Modifier.then(padding)
            )
            return@ScreenScaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().then(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // PRD 7.1: Upcoming is grouped by date.
            state.groups.forEach { group ->
                item(key = "header_${group.dateStart}") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = DateText.dateOnly(group.dateStart),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = group.relativeLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(group.tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        modifier = Modifier.animateItem(),
                        onToggleComplete = {
                            val completing = task.status == com.joe.taskmanager.data.local.entity.TaskStatus.OPEN
                            viewModel.toggleComplete(task.id, task.status)
                            if (completing) viewModel.showUndoSnackbar(task.id, completedMessage, undoLabel)
                        },
                        onClick = { onTaskClick(task.id) },
                        onSnooze = { viewModel.snooze(task.id, it) },
                        onDelete = { viewModel.moveToTrash(task.id) }
                    )
                }
            }
        }
    }
}
