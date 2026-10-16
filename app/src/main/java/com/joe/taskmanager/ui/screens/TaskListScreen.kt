
package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.ui.components.TaskRow
import com.joe.taskmanager.ui.screens.list.GenericTaskListViewModel

/**
 * One screen serves All Tasks, a single list, and a tag view: they differ only in
 * which Flow is observed. Keeps swipe/row behaviour identical everywhere.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    listId: Long?,
    onTaskClick: (Long) -> Unit,
    onAddTask: () -> Unit,
    tagId: Long? = null,
    title: String? = null,
    viewModel: GenericTaskListViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSortMenu by remember { mutableStateOf(false) }

    ScreenScaffold(
        title = title ?: stringResource(R.string.nav_all_tasks),
        snackbarHostState = viewModel.snackbarHostState,
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTask) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.cd_add_task))
            }
        }
    ) { padding ->
        if (state.tasks.isEmpty()) {
            EmptyState(
                title = stringResource(state.emptyTextRes),
                modifier = Modifier.then(padding)
            )
            return@ScreenScaffold
        }

        Column(modifier = Modifier.fillMaxSize().then(padding)) {
        if (title == null && listId == null && tagId == null) {
            androidx.compose.foundation.layout.Box {
                TextButton(onClick = { showSortMenu = true }) {
                    Text("${stringResource(R.string.filter_sort)}: ${stringResource(when (state.sortMode) {
                        "PRIORITY" -> R.string.sort_priority
                        "CREATED" -> R.string.sort_created
                        "MANUAL" -> R.string.sort_manual
                        else -> R.string.sort_due
                    })}")
                }
                DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                    listOf("DUE" to R.string.sort_due, "PRIORITY" to R.string.sort_priority,
                        "CREATED" to R.string.sort_created, "MANUAL" to R.string.sort_manual).forEach { (mode, label) ->
                        DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = {
                            viewModel.setSortMode(mode); showSortMenu = false
                        })
                    }
                }
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().weight(1f),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(state.tasks, key = { it.id }) { task ->
                TaskRow(
                    task = task,
                    onToggleComplete = { viewModel.toggleComplete(task.id) },
                    onClick = { onTaskClick(task.id) },
                    modifier = if (tagId != null) Modifier else Modifier,
                    onSnooze = { viewModel.snooze(task.id, it) }
                )
            }
        }
        }
    }
}
