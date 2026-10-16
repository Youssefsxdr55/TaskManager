
package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.ui.screens.search.SearchViewModel
import com.joe.taskmanager.ui.util.DateText
import com.joe.taskmanager.util.DateUtils

/** PRD 7.1 search: full-text over titles, notes, subtasks, and tags. */
@Composable
fun SearchScreen(
    onTaskClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenScaffold(title = stringResource(R.string.search_hint), onBack = onBack) { padding ->
        Column(modifier = Modifier.fillMaxSize().then(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = viewModel::clear) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                        }
                    }
                }
            )

            val anyLabel = stringResource(R.string.filter_any)
            val listLabel = stringResource(R.string.filter_list)
            val tagLabel = stringResource(R.string.filter_tag)
            val priorityLabel = stringResource(R.string.filter_priority)
            val statusLabel = stringResource(R.string.filter_status)
            val dateLabel = stringResource(R.string.filter_date)
            val filterOptions = listOf(
                listLabel to (state.lists.map { it.name to it.id.toString() } + listOf(stringResource(R.string.nav_inbox) to "-1")),
                tagLabel to state.tags.map { it.name to it.id.toString() },
                priorityLabel to listOf(
                    stringResource(R.string.priority_none) to "NONE",
                    stringResource(R.string.priority_low) to "LOW",
                    stringResource(R.string.priority_medium) to "MEDIUM",
                    stringResource(R.string.priority_high) to "HIGH"
                ),
                statusLabel to listOf(
                    stringResource(R.string.filter_open) to "OPEN",
                    stringResource(R.string.filter_completed) to "COMPLETED"
                ),
                dateLabel to listOf(
                    stringResource(R.string.filter_today) to "TODAY",
                    stringResource(R.string.filter_next_7_days) to "NEXT7",
                    stringResource(R.string.filter_overdue) to "OVERDUE"
                )
            )
            LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                items(filterOptions) { (label, options) ->
                    val selected = when (label) {
                        listLabel -> state.filters.listId?.toString()
                        tagLabel -> state.filters.tagId?.toString()
                        priorityLabel -> state.filters.priority
                        statusLabel -> state.filters.status
                        else -> when {
                            state.filters.dueFrom == null && state.filters.dueTo == null -> null
                            state.filters.dueFrom == DateUtils.startOfToday() && state.filters.dueTo == DateUtils.endOfToday() -> "TODAY"
                            state.filters.dueFrom == null -> "OVERDUE"
                            else -> "NEXT7"
                        }
                    }
                    val shown = options.firstOrNull { it.second == selected }?.first ?: anyLabel
                    SearchFilterMenu(label, shown, options, anyLabel) { value ->
                        when (label) {
                            listLabel -> viewModel.setList(value?.toLongOrNull())
                            tagLabel -> viewModel.setTag(value?.toLongOrNull())
                            priorityLabel -> viewModel.setPriority(value)
                            statusLabel -> viewModel.setStatus(value)
                            else -> when (value) {
                                "TODAY" -> viewModel.setDateRange(DateUtils.startOfToday(), DateUtils.endOfToday())
                                "NEXT7" -> viewModel.setDateRange(DateUtils.startOfToday(), DateUtils.endOfDay(DateUtils.plusDays(System.currentTimeMillis(), 7)))
                                "OVERDUE" -> viewModel.setDateRange(null, System.currentTimeMillis())
                                else -> viewModel.setDateRange(null, null)
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = viewModel::clearFilters) { Text(stringResource(R.string.action_clear)) }
                }
            }

            if (state.results.isEmpty()) {
                EmptyState(
                    title = if (state.query.isBlank()) stringResource(R.string.search_hint)
                            else stringResource(R.string.empty_search_title),
                    modifier = Modifier.padding(top = 32.dp)
                )
                return@Column
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(state.results, key = { it.id }) { result ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTaskClick(result.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = result.title,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (result.dueDate != null) {
                                Text(
                                    text = if (result.hasTime) DateText.format(result.dueDate)
                                           else DateText.dateOnly(result.dueDate),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun SearchFilterMenu(
    label: String,
    selected: String,
    options: List<Pair<String, String?>>,
    anyLabel: String,
    onSelected: (String?) -> Unit
) {
    var expanded by remember(label) { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box {
        TextButton(onClick = { expanded = true }) { Text("$label: $selected") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(anyLabel) }, onClick = { expanded = false; onSelected(null) })
            options.forEach { (text, value) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { expanded = false; onSelected(value) })
            }
        }
    }
}
