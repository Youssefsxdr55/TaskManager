
package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.ui.screens.settings.SettingsViewModel

/** PRD 7.14 Settings. Entries for features that have not shipped are absent. */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenReliability: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenCompleted: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenTags: () -> Unit,
    onStartOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenScaffold(title = stringResource(R.string.nav_settings), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(stringResource(R.string.settings_general))

            SettingRow(
                title = stringResource(R.string.settings_theme),
                value = stringResource(state.themeLabelRes),
                onClick = viewModel::cycleTheme
            )
            SettingRow(
                title = stringResource(R.string.settings_language),
                value = state.language.uppercase(),
                onClick = viewModel::cycleLanguage
            )
            SettingRow(
                title = stringResource(R.string.settings_snooze_duration),
                value = "${state.snoozeMinutes} min",
                onClick = viewModel::cycleSnooze
            )

            SectionHeader(stringResource(R.string.settings_reminders))
            SettingRow(
                title = stringResource(R.string.settings_reliability_check),
                value = "",
                onClick = onOpenReliability
            )

            SectionHeader(stringResource(R.string.settings_backup))
            SettingRow(
                title = stringResource(R.string.settings_backup),
                value = "",
                onClick = onOpenBackup
            )

            SectionHeader(stringResource(R.string.nav_tasks))
            SettingRow(stringResource(R.string.nav_completed), "", onClick = onOpenCompleted)
            SettingRow(stringResource(R.string.nav_tags), "", onClick = onOpenTags)
            SettingRow(stringResource(R.string.nav_trash), "", onClick = onOpenTrash)

            SectionHeader(stringResource(R.string.settings_about))
            SettingRow(stringResource(R.string.app_name), "1.0.0", onClick = {})
            SettingRow(
                title = stringResource(R.string.onboarding_start),
                value = "",
                onClick = onStartOnboarding
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}
