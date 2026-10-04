
package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.joe.taskmanager.data.local.entity.TaskStatus
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.ui.components.TaskRow
import com.joe.taskmanager.ui.screens.today.TodayViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    onAddTask: () -> Unit,
    onTaskClick: (Long) -> Unit,
    onSeeOverdue: () -> Unit,
    onOpenList: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onSearch: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val completedMessage = stringResource(R.string.task_completed)
    val undoLabel = stringResource(R.string.action_undo)

    ScreenScaffold(
        title = stringResource(R.string.nav_today),
        snackbarHostState = viewModel.snackbarHostState,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_add_task)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onSearch) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = stringResource(R.string.cd_search)
                        )
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            androidx.compose.material.icons.Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.nav_settings)
                        )
                    }
                }
            }

            // PRD OQ-1: a compact red banner surfaces overdue counts without
            // moving the tasks out of their original positions.
            if (state.overdueCount > 0) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .clickable(onClick = onSeeOverdue),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.overdue_banner, state.overdueCount),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            if (state.todayTasks.isEmpty()) {
                item {
                    EmptyState(
                        title = stringResource(R.string.empty_today_title),
                        body = stringResource(R.string.empty_today_body)
                    )
                }
            }

            items(state.todayTasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    showListName = state.listNames[task.listId],
                    onToggleComplete = {
                        val completing = task.status == TaskStatus.OPEN
                        viewModel.toggleComplete(task.id, task.status)
                        if (completing) {
                            // PRD 7.1: completing offers Undo.
                            viewModel.showUndoSnackbar(
                                taskId = task.id,
                                message = completedMessage,
                                actionLabel = undoLabel
                            )
                        }
                    },
                    onClick = { onTaskClick(task.id) }
                )
            }
        }
    }
}
