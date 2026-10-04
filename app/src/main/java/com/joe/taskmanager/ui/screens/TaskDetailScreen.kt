
package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.ui.components.SnoozeSheet
import com.joe.taskmanager.ui.screens.detail.TaskDetailViewModel

@Composable
fun TaskDetailScreen(
    taskId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    viewModel: TaskDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = state.title.ifBlank { stringResource(R.string.task_title_label) },
        onBack = onBack
    ) { padding ->
        val task = state.task
        if (task == null) {
            EmptyState(stringResource(R.string.empty_search_title), modifier = Modifier.padding(padding))
            return@ScreenScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineMedium,
                color = if (task.isOverdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onBackground
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { viewModel.toggleComplete() }) {
                    Text(stringResource(if (state.completed) R.string.cd_mark_open else R.string.cd_mark_done))
                }
                TextButton(onClick = { viewModel.showSnooze = true }) {
                    Text(stringResource(R.string.action_snooze))
                }
                TextButton(onClick = onEdit) {
                    Text(stringResource(R.string.action_save))
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (task.notes.isNotBlank()) {
                Text(
                    text = task.notes,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (state.subtasks.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.task_subtasks),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                )
                state.subtasks.forEach { sub ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = sub.done,
                            onCheckedChange = { viewModel.toggleSubtask(sub.id, it) }
                        )
                        Text(
                            text = sub.title,
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (sub.done)
                                androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        )
                    }
                }
            }

            if (state.tags.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.chip_tags) + ": " +
                        state.tags.joinToString(", ") { it.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
            }

            if (state.reminders.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.chip_reminder) + ": " +
                        state.reminders.joinToString(", ") { it.type.name },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            TextButton(
                onClick = { viewModel.moveToTrash(onDeleted = onBack) },
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Text(
                    text = stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (state.showSnooze) {
            SnoozeSheet(
                onDismiss = { viewModel.showSnooze = false },
                onPick = { viewModel.snooze(it) }
            )
        }
    }
}
