
package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.joe.taskmanager.ui.screens.trash.TrashViewModel

/**
 * PRD OQ-8: a 30-day trash with restore. Reads directly from the trash query
 * rather than reusing TaskListScreen, because trash rows are excluded from every
 * other list by the deletedAt IS NULL filter.
 */
@Composable
fun TrashScreen(
    onTaskClick: (Long) -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val tasks by viewModel.trashedTasks.collectAsStateWithLifecycle()

    ScreenScaffold(title = stringResource(R.string.nav_trash)) { padding ->
        if (tasks.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.empty_trash_title),
                body = stringResource(R.string.trash_retention),
                modifier = Modifier.padding(padding)
            )
            return@ScreenScaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    TextButton(onClick = { viewModel.restore(task.id) }) {
                        Text(stringResource(R.string.action_restore))
                    }
                }
            }
        }
    }
}
