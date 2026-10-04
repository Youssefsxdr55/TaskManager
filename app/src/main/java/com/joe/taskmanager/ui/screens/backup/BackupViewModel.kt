
package com.joe.taskmanager.ui.screens.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.data.backup.BackupManager
import com.joe.taskmanager.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class BackupUiState(
    val folderSummary: String = "",
    val message: String? = null,
    val error: Boolean = false
)

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager,
    private val settings: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val uri = settings.settings.first().backupFolderUri
            _uiState.value = _uiState.value.copy(folderSummary = uri ?: "")
        }
    }

    /** Called with the folder the user picked via the Storage Access Framework. */
    fun onFolderChosen(uri: Uri) {
        viewModelScope.launch {
            settings.setBackupFolder(uri.toString())
            backupManager.persistPermission(uri)
            _uiState.value = _uiState.value.copy(folderSummary = uri.toString())
        }
    }

    fun exportNow() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { backupManager.exportToConfiguredFolder() }
            _uiState.value = _uiState.value.copy(
                message = if (result.isSuccess) MSG_BACKUP_OK else MSG_BACKUP_FAIL,
                error = result.isFailure
            )
        }
    }

    /**
     * PRD 7.13 / OQ-4: v1 import is replace-all, and it must only run after an
     * explicit confirmation. [confirmReplaceAll] must be true; the Composable owns
     * the confirmation dialog so a destructive action is never one tap away.
     */
    fun importLatest(confirmReplaceAll: Boolean) {
        if (!confirmReplaceAll) return
        performImport()
    }

    private fun performImport() {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { backupManager.importLatestReplaceAll() }
            _uiState.value = _uiState.value.copy(
                message = if (result.isSuccess) MSG_IMPORT_OK else MSG_IMPORT_FAIL,
                error = result.isFailure
            )
        }
    }

    companion object {
        /**
         * Result tokens rather than display text: the Composable maps these onto
         * string resources so the messages are translated (PRD 3, bilingual UI).
         */
        const val MSG_BACKUP_OK = "backup_ok"
        const val MSG_BACKUP_FAIL = "backup_fail"
        const val MSG_IMPORT_OK = "import_ok"
        const val MSG_IMPORT_FAIL = "import_fail"
    }
}
