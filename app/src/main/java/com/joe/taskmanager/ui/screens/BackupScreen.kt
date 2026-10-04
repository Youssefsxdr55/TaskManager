package com.joe.taskmanager.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.joe.taskmanager.ui.screens.backup.BackupViewModel

/**
 * PRD 7.13 Backup and Restore.
 *
 * The automatic backup writes into a folder the user picks with the Storage
 * Access Framework, so this screen owns an OpenDocumentTree launcher and passes the
 * resulting URI back to the ViewModel. Manual import is replace-all, so it is
 * gated behind an explicit confirmation dialog.
 */
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmImport by remember { mutableStateOf(false) }

    // PRD 7.13: a folder chosen through SAF with a persisted URI grant, so backups
    // survive uninstalling the app.
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) viewModel.onFolderChosen(uri)
    }

    ScreenScaffold(title = stringResource(R.string.settings_backup), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.backup_choose_folder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = state.folderSummary.ifBlank { stringResource(R.string.backup_no_folder) },
                style = MaterialTheme.typography.bodyLarge
            )

            Button(
                onClick = { folderPicker.launch(null) },
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.backup_choose_folder)) }

            Button(
                onClick = viewModel::exportNow,
                enabled = state.folderSummary.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_export)) }

            Button(
                onClick = { confirmImport = true },
                enabled = state.folderSummary.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(stringResource(R.string.settings_import)) }

            val message = state.message
            if (message != null) {
                Text(
                    text = stringResource(message.toRes()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (state.error) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text(stringResource(R.string.settings_import)) },
            text = { Text(stringResource(R.string.backup_import_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmImport = false
                    viewModel.importLatest(confirmReplaceAll = true)
                }) {
                    Text(stringResource(R.string.settings_import))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

/** Maps a ViewModel result token onto its localized string resource. */
private fun String.toRes(): Int = when (this) {
    BackupViewModel.MSG_BACKUP_OK -> R.string.backup_complete
    BackupViewModel.MSG_BACKUP_FAIL -> R.string.backup_failed
    BackupViewModel.MSG_IMPORT_OK -> R.string.backup_import_complete
    BackupViewModel.MSG_IMPORT_FAIL -> R.string.backup_import_failed
    else -> R.string.backup_failed
}
