package com.joe.taskmanager.ui.screens
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.data.local.entity.Tag
import com.joe.taskmanager.ui.screens.tags.TagsViewModel
private data class TagColorOption(val label: Int, val color: Int)
private val tagColors = listOf(
    TagColorOption(R.string.color_brown, 0xFF8B5E3C.toInt()),
    TagColorOption(R.string.color_blue, 0xFF3973AC.toInt()),
    TagColorOption(R.string.color_green, 0xFF548B57.toInt()),
    TagColorOption(R.string.color_purple, 0xFF7956A8.toInt()),
    TagColorOption(R.string.color_red, 0xFFC62828.toInt())
)
/** PRD 7.2: create, rename, recolor, delete, and open tasks for a tag. */
@Composable
fun TagsScreen(
    onTagClick: (Long) -> Unit,
    viewModel: TagsViewModel = hiltViewModel()
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Tag?>(null) }
    var creating by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<Tag?>(null) }
    ScreenScaffold(
        title = stringResource(R.string.nav_tags),
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; creating = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.tag_add))
            }
        }
    ) { padding ->
        if (tags.isEmpty()) {
            EmptyState(stringResource(R.string.tags_empty), modifier = Modifier.then(padding))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().then(padding)) {
                items(tags, key = { it.id }) { tag ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onTagClick(tag.id) }
                            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(12.dp).background(Color(tag.color), CircleShape))
                        Text(tag.name, style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f).padding(start = 12.dp))
                        IconButton(onClick = { editing = tag; creating = false }) {
                            Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.tag_edit, tag.name))
                        }
                    }
                }
            }
        }
    }
    if (creating || editing != null) {
        TagEditorDialog(
            tag = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { name, color -> viewModel.save(editing, name, color); creating = false; editing = null },
            onDelete = editing?.let { tag -> { pendingDelete = tag; editing = null } }
        )
    }
    pendingDelete?.let { tag ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.tag_delete_title)) },
            text = { Text(stringResource(R.string.tag_delete_confirm, tag.name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(tag); pendingDelete = null; editing = null }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}
@Composable
private fun TagEditorDialog(
    tag: Tag?,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
    onDelete: (() -> Unit)?
) {
    var name by remember(tag?.id) { mutableStateOf(tag?.name.orEmpty()) }
    var color by remember(tag?.id) { mutableStateOf(tag?.color ?: tagColors.first().color) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (tag == null) R.string.tag_add else R.string.tag_edit_title)) },
        text = {
            androidx.compose.foundation.layout.Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.tag_name)) },
                    singleLine = true
                )
                Text(stringResource(R.string.tag_color), modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                androidx.compose.foundation.layout.Column {
                    tagColors.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { option ->
                                FilterChip(
                                    selected = color == option.color,
                                    onClick = { color = option.color },
                                    label = { Text(stringResource(option.label)) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, color) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    )
}
