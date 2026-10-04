
package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.ui.screens.addtask.AddTaskViewModel

/**
 * PRD 7.1 Add Task screen: full screen, opens with the keyboard up and focus on
 * the title field; below the title are quick-action chips (Date, Priority, List,
 * Tags, Reminder) where everything except the title is optional. A title-only
 * save must be possible in one tap after typing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskScreen(
    taskId: Long?,
    initialListId: Long?,
    onClose: () -> Unit,
    viewModel: AddTaskViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Auto-focus the title with the keyboard up, per PRD 7.1.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close))
                }
                Text(
                    text = stringResource(
                        if (taskId == null) R.string.action_add_task else R.string.action_save
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    // PRD 7.1: title-only save is one tap. Guarded so an empty
                    // title cannot create a junk row.
                    enabled = state.title.isNotBlank() && !state.saving,
                    onClick = {
                        keyboard?.hide()
                        viewModel.save(onSaved = onClose)
                    }
                ) {
                    Text(stringResource(R.string.action_save))
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.task_title_hint)) },
                textStyle = MaterialTheme.typography.titleMedium,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboard?.hide()
                        viewModel.save(onSaved = onClose)
                    }
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = viewModel::onCycleDateChip,
                    label = { Text(state.dateChipLabel) }
                )
                AssistChip(
                    onClick = viewModel::onCyclePriority,
                    label = { Text(state.priorityLabel) }
                )
                AssistChip(
                    onClick = viewModel::onCycleReminder,
                    label = { Text(state.reminderLabel) }
                )
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::onNotesChange,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text(stringResource(R.string.task_notes_label)) },
                minLines = 3
            )

            // Priority chooser shown only when priority != NONE, so the default
            // state stays minimal (PRD 8).
            if (state.priority.name != "NONE") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("LOW", "MEDIUM", "HIGH").forEach { p ->
                        FilterChip(
                            selected = state.priority.name == p,
                            onClick = { viewModel.onPrioritySelected(p) },
                            label = { Text(
                                when (p) {
                                    "LOW" -> stringResource(R.string.priority_low)
                                    "MEDIUM" -> stringResource(R.string.priority_medium)
                                    else -> stringResource(R.string.priority_high)
                                }
                            ) }
                        )
                    }
                }
            }

            Text(
                text = stringResource(R.string.task_subtasks),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            state.subtasks.forEach { sub ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(sub, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.removeSubtask(sub) }) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = state.newSubtask,
                    onValueChange = viewModel::onNewSubtaskChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.task_subtask_hint)) },
                    singleLine = true
                )
                Button(
                    onClick = viewModel::addSubtask,
                    enabled = state.newSubtask.isNotBlank(),
                    modifier = Modifier.padding(start = 8.dp)
                ) { Text("+") }
            }

            Button(
                onClick = {
                    keyboard?.hide()
                    viewModel.save(onSaved = onClose)
                },
                enabled = state.title.isNotBlank() && !state.saving,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp).height(48.dp)
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}
