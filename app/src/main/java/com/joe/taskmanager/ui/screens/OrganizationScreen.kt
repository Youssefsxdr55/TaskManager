package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.data.local.entity.TaskList
import com.joe.taskmanager.ui.screens.organization.OrganizationViewModel

private data class DeleteTarget(val folderId: Long? = null, val listId: Long? = null)

private data class AddTarget(val folderId: Long? = null, val parentListId: Long? = null, val isFolder: Boolean = false)

/** Folder > List > Sublist organization UI for PRD 6.2. */
@Composable
fun OrganizationScreen(
    onBack: () -> Unit,
    onOpenList: (Long) -> Unit,
    viewModel: OrganizationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var target by remember { mutableStateOf<AddTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<DeleteTarget?>(null) }
    var name by remember { mutableStateOf("") }

    ScreenScaffold(title = stringResource(R.string.nav_organization), onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().then(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { target = AddTarget(isFolder = true); name = "" }) {
                    Text(stringResource(R.string.organization_add_folder))
                }
                Button(onClick = { target = AddTarget(); name = "" }) {
                    Text(stringResource(R.string.organization_add_list))
                }
            }

            val rootLists = state.lists.filter { it.parentListId == null }
            rootLists.filter { it.folderId == null }.forEach { list ->
                OrganizationListRow(list, 0, onOpenList,
                    onAddSublist = { target = AddTarget(parentListId = list.id); name = "" },
                    onDelete = { deleteTarget = DeleteTarget(listId = list.id) })
                state.lists.filter { it.parentListId == list.id }.forEach { sub ->
                    OrganizationListRow(sub, 1, onOpenList, onAddSublist = {},
                        onDelete = { deleteTarget = DeleteTarget(listId = sub.id) })
                }
            }

            state.folders.forEach { folder ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(folder.name, style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f).padding(top = 12.dp))
                    IconButton(onClick = { deleteTarget = DeleteTarget(folderId = folder.id) }) {
                        Text("×", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
                    }
                }
                state.lists.filter { it.folderId == folder.id && it.parentListId == null }.forEach { list ->
                    OrganizationListRow(list, 0, onOpenList,
                    onAddSublist = { target = AddTarget(parentListId = list.id); name = "" },
                    onDelete = { deleteTarget = DeleteTarget(listId = list.id) })
                    state.lists.filter { it.parentListId == list.id }.forEach { sub ->
                        OrganizationListRow(sub, 1, onOpenList, onAddSublist = {},
                        onDelete = { deleteTarget = DeleteTarget(listId = sub.id) })
                    }
                }
                TextButton(onClick = { target = AddTarget(folderId = folder.id); name = "" }) {
                    Text(stringResource(R.string.organization_add_list))
                }
                HorizontalDivider()
            }

            if (state.folders.isEmpty() && rootLists.isEmpty()) {
                EmptyState(stringResource(R.string.organization_empty), modifier = Modifier.fillMaxWidth())
            }
        }
    }

    deleteTarget?.let { targetToDelete ->
        val label = targetToDelete.folderId?.let { id -> state.folders.firstOrNull { it.id == id }?.name }
            ?: targetToDelete.listId?.let { id -> state.lists.firstOrNull { it.id == id }?.name }.orEmpty()
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.organization_delete_title, label)) },
            text = { Text(stringResource(R.string.organization_delete_body)) },
            confirmButton = {
                Column {
                    TextButton(onClick = {
                        targetToDelete.folderId?.let { viewModel.deleteFolder(it, deleteTasks = false) }
                        targetToDelete.listId?.let { viewModel.deleteList(it, deleteTasks = false) }
                        deleteTarget = null
                    }) { Text(stringResource(R.string.organization_move_to_inbox)) }
                    TextButton(onClick = {
                        targetToDelete.folderId?.let { viewModel.deleteFolder(it, deleteTasks = true) }
                        targetToDelete.listId?.let { viewModel.deleteList(it, deleteTasks = true) }
                        deleteTarget = null
                    }) { Text(stringResource(R.string.organization_delete_tasks)) }
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }

    target?.let { addTarget ->
        AlertDialog(
            onDismissRequest = { target = null },
            title = { Text(stringResource(if (addTarget.isFolder) R.string.organization_add_folder else R.string.organization_add_list)) },
            text = {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.organization_name)) }, singleLine = true)
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    if (addTarget.isFolder) viewModel.addFolder(name)
                    else viewModel.addList(name, addTarget.folderId, addTarget.parentListId)
                    target = null
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = { TextButton(onClick = { target = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@Composable
private fun OrganizationListRow(
    list: TaskList,
    depth: Int,
    onOpen: (Long) -> Unit,
    onAddSublist: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(list.id) }
            .padding(start = (depth * 24).dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(list.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(vertical = 10.dp))
        if (depth == 0) IconButton(onClick = onAddSublist) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.organization_add_sublist))
        }
        IconButton(onClick = onDelete) {
            Text("×", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
        }
    }
}
