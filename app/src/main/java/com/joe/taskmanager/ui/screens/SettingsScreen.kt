
package com.joe.taskmanager.ui.screens
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    onOpenOrganization: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmReset by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showAccentPicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var showTextScalePicker by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val accentOptions = listOf(
        "terracotta" to R.string.accent_terracotta,
        "ocean" to R.string.accent_ocean,
        "forest" to R.string.accent_forest,
        "violet" to R.string.accent_violet,
        "rose" to R.string.accent_rose,
        "amber" to R.string.accent_amber
    )
    ScreenScaffold(title = stringResource(R.string.nav_settings), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionHeader(stringResource(R.string.settings_general))
            SettingRow(
                title = stringResource(R.string.settings_theme),
                value = stringResource(state.themeLabelRes),
                onClick = viewModel::cycleTheme
            )
            SectionHeader(stringResource(R.string.settings_personalization))
            SettingRow(
                title = stringResource(R.string.settings_accent_color),
                value = stringResource(state.accentLabelRes),
                onClick = { showAccentPicker = true }
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) SettingRow(
                title = stringResource(R.string.settings_dynamic_color),
                value = stringResource(if (state.dynamicColor) R.string.settings_on else R.string.settings_off),
                onClick = viewModel::toggleDynamicColor
            )
            SettingRow(
                title = stringResource(R.string.settings_text_size),
                value = stringResource(state.textScaleLabelRes),
                onClick = { showTextScalePicker = true }
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
            SettingRow(stringResource(R.string.settings_postpone_threshold), "${state.postponeThreshold}", onClick = viewModel::cyclePostponeThreshold)
            SectionHeader(stringResource(R.string.settings_reminders))
            SettingRow(stringResource(R.string.settings_habit_reminder), String.format(java.util.Locale.US, "%02d:00", state.habitReminderHour), onClick = viewModel::cycleHabitReminderTime)
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
            SettingRow(stringResource(R.string.nav_organization), "", onClick = onOpenOrganization)
            SettingRow(stringResource(R.string.nav_completed), "", onClick = onOpenCompleted)
            SettingRow(stringResource(R.string.nav_tags), "", onClick = onOpenTags)
            SettingRow(stringResource(R.string.nav_trash), "", onClick = onOpenTrash)
            SectionHeader(stringResource(R.string.nav_rewards))
            SettingRow(stringResource(R.string.points_reset), "", onClick = { confirmReset = true })
            SectionHeader(stringResource(R.string.settings_about))
            SettingRow(stringResource(R.string.app_name), "1.0.0", onClick = {})
            SettingRow(
                title = stringResource(R.string.onboarding_start),
                value = "",
                onClick = onStartOnboarding
            )
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text(stringResource(R.string.points_reset)) },
        text = { Text(stringResource(R.string.points_reset_confirm)) },
        confirmButton = { TextButton(onClick = { viewModel.resetPoints(); confirmReset = false }) { Text(stringResource(R.string.points_reset)) } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.action_cancel)) } }
    )
    if (showAccentPicker) AlertDialog(
        onDismissRequest = { showAccentPicker = false },
        title = { Text(stringResource(R.string.settings_accent_color)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                accentOptions.chunked(3).forEach { optionsRow ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        optionsRow.forEach { (key, labelRes) ->
                            val selected = state.accentColor == key
                            Surface(
                                modifier = Modifier.weight(1f).clickable {
                                    viewModel.setAccentColor(key)
                                    showAccentPicker = false
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(
                                    if (selected) 2.dp else 1.dp,
                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(Modifier.size(30.dp).background(com.joe.taskmanager.ui.theme.AppColors.accent(key, isSystemInDarkTheme()).first, CircleShape))
                                    Text(stringResource(labelRes), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                    if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    else Spacer(Modifier.size(16.dp))
                                }
                            }
                        }
                        if (optionsRow.size < 3) Spacer(Modifier.weight((3 - optionsRow.size).toFloat()))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { showAccentPicker = false }) { Text(stringResource(R.string.action_cancel)) } }
    )
    if (showTextScalePicker) AlertDialog(
        onDismissRequest = { showTextScalePicker = false },
        title = { Text(stringResource(R.string.settings_text_size)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    "compact" to R.string.text_scale_compact,
                    "standard" to R.string.text_scale_standard,
                    "large" to R.string.text_scale_large
                ).forEach { (scale, labelRes) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            viewModel.setTextScale(scale)
                            showTextScalePicker = false
                        }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(labelRes), modifier = Modifier.weight(1f))
                        androidx.compose.material3.RadioButton(
                            selected = state.textScale == scale,
                            onClick = { viewModel.setTextScale(scale); showTextScalePicker = false }
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { showTextScalePicker = false }) { Text(stringResource(R.string.action_cancel)) } }
    )
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
