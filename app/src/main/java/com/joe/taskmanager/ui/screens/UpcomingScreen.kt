
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
    onTaskClick: (Long) -> Unit,
    onAddTask: () -> Unit,
    viewModel: UpcomingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenScaffold(title = stringResource(R.string.nav_upcoming)) { padding ->
        if (state.groups.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.empty_upcoming_title),
                modifier = Modifier.padding(padding)
            )
            return@ScreenScaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // PRD 7.1: Upcoming is grouped by date.
            state.groups.forEach { group ->
                item(key = "header_${group.dateStart}") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp)
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
                        onToggleComplete = { viewModel.toggleComplete(task.id, task.status) },
                        onClick = { onTaskClick(task.id) }
                    )
                }
            }
        }
    }
}
