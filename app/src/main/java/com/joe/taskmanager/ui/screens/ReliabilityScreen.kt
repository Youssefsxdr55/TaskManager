
package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
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
import com.joe.taskmanager.ui.screens.reliability.ReliabilityViewModel

/**
 * PRD 7.3 Reliability check: verifies notification permission, exact alarm
 * permission, full-screen intent permission, and battery optimization, with
 * OEM-specific guidance.
 */
@Composable
fun ReliabilityScreen(
    onBack: () -> Unit,
    viewModel: ReliabilityViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ScreenScaffold(
        title = stringResource(R.string.reliability_title),
        onBack = onBack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            CheckRow(
                title = stringResource(R.string.perm_notifications_title),
                ok = state.notificationsGranted
            ) {
                viewModel.requestNotifications()
            }
            CheckRow(
                title = stringResource(R.string.perm_exact_alarm_title),
                ok = state.exactAlarmGranted
            ) {
                viewModel.openExactAlarmSettings()
            }
            CheckRow(
                title = stringResource(R.string.perm_full_screen_title),
                ok = state.fullScreenIntentGranted
            ) {
                viewModel.openFullScreenSettings()
            }
            CheckRow(
                title = stringResource(R.string.perm_battery_title),
                ok = state.batteryOptimizationExempt
            ) {
                viewModel.openBatterySettings()
            }

            if (!state.isIgnoringBatteryOptimizations) {
                Text(
                    text = oemGuidance(state.manufacturer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }

            TextButton(
                onClick = viewModel::refresh,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

@Composable
private fun CheckRow(title: String, ok: Boolean, onFix: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onFix)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Filled.Warning,
            contentDescription = null,
            tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
        if (!ok) {
            TextButton(onClick = onFix) {
                Text(stringResource(R.string.perm_grant))
            }
        }
    }
}

/** PRD 13: aggressive OEM battery managers are the main reliability risk. */
private fun oemGuidance(manufacturer: String): String = when (manufacturer.lowercase()) {
    "xiaomi" -> "Xiaomi: Settings > Apps > Autostart, and set Battery saver to \"No restrictions\" for this app."
    "samsung" -> "Samsung: Settings > Battery > App power management > Sleeping apps, and remove this app from Never sleeping apps."
    "oppo", "realme", "oneplus" -> "Oppo/Realme/OnePlus: Settings > Battery > App battery usage, allow Background activity and Auto launch."
    "huawei", "honor" -> "Huawei/Honor: Settings > Battery > Launch app management, enable Manual management for this app."
    "vivo" -> "Vivo: Settings > Battery > High background power consumption, allow this app."
    else -> "If reminders are delayed, allow this app to run without battery restrictions in your phone's settings."
}
