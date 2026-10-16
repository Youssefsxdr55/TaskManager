
package com.joe.taskmanager.ui.screens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.text.format.DateFormat
import java.util.Calendar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.ui.screens.addtask.AddTaskViewModel
import com.joe.taskmanager.ui.util.DateText
import com.joe.taskmanager.util.DateUtils
import com.joe.taskmanager.data.local.entity.ReminderType
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
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var showListMenu by remember { mutableStateOf(false) }
    var showTagMenu by remember { mutableStateOf(false) }
    var showReminderMenu by remember { mutableStateOf(false) }
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
            TextButton(onClick = viewModel::parseQuickAdd, enabled = state.title.isNotBlank()) {
                Text(stringResource(R.string.quick_add_parse))
            }
            if (state.quickAddParsed) Text(stringResource(R.string.quick_add_review), style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        val calendar = Calendar.getInstance().apply { state.dueDate?.let { timeInMillis = it } }
                        DatePickerDialog(context, { _, year, month, day ->
                            val selected = Calendar.getInstance().apply {
                                set(year, month, day, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            viewModel.onDateSelected(selected.timeInMillis)
                        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    label = { Text(state.dueDate?.let { DateText.dateOnly(it) } ?: stringResource(R.string.chip_date)) }
                )
                AssistChip(
                    enabled = state.dueDate != null,
                    onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = state.dueDate ?: System.currentTimeMillis() }
                        TimePickerDialog(context, { _, hour, minute -> viewModel.onTimeSelected(hour, minute) },
                            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), DateFormat.is24HourFormat(context)).show()
                    },
                    label = { Text(state.dueDate?.takeIf { state.hasTime }?.let { DateText.format(it) } ?: stringResource(R.string.chip_time)) }
                )
                if (state.dueDate != null) TextButton(onClick = viewModel::clearDueDate) {
                    Text(stringResource(R.string.action_clear))
                }
                AssistChip(
                    onClick = viewModel::onCyclePriority,
                    label = { Text(stringResource(when (state.priority) {
                        com.joe.taskmanager.data.local.entity.Priority.NONE -> R.string.chip_priority
                        com.joe.taskmanager.data.local.entity.Priority.LOW -> R.string.priority_low
                        com.joe.taskmanager.data.local.entity.Priority.MEDIUM -> R.string.priority_medium
                        com.joe.taskmanager.data.local.entity.Priority.HIGH -> R.string.priority_high
                    })) }
                )
                androidx.compose.foundation.layout.Box {
                    AssistChip(
                        onClick = { showListMenu = true },
                        label = { Text(state.availableLists.firstOrNull { it.id == state.listId }?.name ?: stringResource(R.string.chip_list)) }
                    )
                    DropdownMenu(expanded = showListMenu, onDismissRequest = { showListMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.nav_inbox)) }, onClick = {
                            viewModel.selectList(null); showListMenu = false
                        })
                        state.availableLists.forEach { list ->
                            DropdownMenuItem(text = { Text(list.name) }, onClick = {
                                viewModel.selectList(list.id); showListMenu = false
                            })
                        }
                    }
                }
                androidx.compose.foundation.layout.Box {
                    AssistChip(
                        onClick = { showTagMenu = true },
                        label = { Text("${stringResource(R.string.chip_tags)} (${state.selectedTagIds.size})") }
                    )
                    DropdownMenu(expanded = showTagMenu, onDismissRequest = { showTagMenu = false }) {
                        if (state.availableTags.isEmpty()) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.tags_empty)) }, onClick = { showTagMenu = false })
                        } else state.availableTags.forEach { tag ->
                            DropdownMenuItem(
                                text = { Text((if (tag.id in state.selectedTagIds) "✓ " else "") + tag.name) },
                                onClick = { viewModel.toggleTag(tag.id) }
                            )
                        }
                    }
                }
                androidx.compose.foundation.layout.Box {
                    AssistChip(
                        onClick = { showReminderMenu = true },
                        label = { Text(if (state.reminders.isEmpty()) stringResource(R.string.chip_reminder)
                            else stringResource(R.string.chip_reminder_count, state.reminders.size)) }
                    )
                    DropdownMenu(expanded = showReminderMenu, onDismissRequest = { showReminderMenu = false }) {
                        val choices = listOf(
                            ReminderType.AT_DUE_TIME to null,
                            ReminderType.BEFORE to 5,
                            ReminderType.BEFORE to 15,
                            ReminderType.BEFORE to 30,
                            ReminderType.BEFORE to 60,
                            ReminderType.BEFORE to 1440
                        )
                        choices.forEach { (type, offset) ->
                            val draft = com.joe.taskmanager.ui.screens.addtask.ReminderDraft(type, offset)
                            val selected = draft in state.reminders
                            val label = if (type == ReminderType.AT_DUE_TIME) stringResource(R.string.reminder_at_due) else when (offset) {
                                60 -> stringResource(R.string.reminder_before_hour)
                                1440 -> stringResource(R.string.reminder_before_day)
                                else -> stringResource(R.string.reminder_before_minutes, offset ?: 30)
                            }
                            DropdownMenuItem(
                                enabled = state.dueDate != null && state.hasTime,
                                text = { Text((if (selected) "✓ " else "") + label) },
                                onClick = { viewModel.toggleReminder(type, offset) }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.reminder_absolute)) },
                            onClick = {
                                showReminderMenu = false
                                val now = Calendar.getInstance()
                                DatePickerDialog(context, { _, year, month, day ->
                                    val chosen = Calendar.getInstance().apply {
                                        set(year, month, day, 0, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    TimePickerDialog(context, { _, hour, minute ->
                                        viewModel.addAbsoluteReminder(DateUtils.combine(chosen.timeInMillis, hour, minute))
                                    }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), DateFormat.is24HourFormat(context)).show()
                                }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
                            }
                        )
                        state.reminders.filter { it.type == ReminderType.ABSOLUTE }.forEach { reminder ->
                            DropdownMenuItem(
                                text = { Text("✓ ${reminder.absoluteTime?.let { DateText.format(it) } ?: stringResource(R.string.reminder_absolute)}") },
                                onClick = { viewModel.removeAbsoluteReminder(reminder.absoluteTime ?: 0L) }
                            )
                        }
                    }
                }
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
