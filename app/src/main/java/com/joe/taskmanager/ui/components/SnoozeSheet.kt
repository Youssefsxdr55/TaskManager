
package com.joe.taskmanager.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joe.taskmanager.R
import com.joe.taskmanager.util.DateUtils

sealed class SnoozeChoice {
    data object LATER_TODAY : SnoozeChoice()
    data object TOMORROW : SnoozeChoice()
    data object NEXT_WEEK : SnoozeChoice()
    data class Custom(val dayStart: Long, val hour: Int, val minute: Int) : SnoozeChoice()
}

/**
 * PRD 7.1: swipe left opens quick options — Later today, Tomorrow, Next week,
 * Pick date/time. "Pick date/time" is offered here as the next occurrence
 * shortcut; a full picker is wired to the same Custom choice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnoozeSheet(
    onDismiss: () -> Unit,
    onPick: (SnoozeChoice) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            SheetRow(stringResource(R.string.action_snooze_later_today)) { onPick(SnoozeChoice.LATER_TODAY) }
            SheetRow(stringResource(R.string.action_snooze_tomorrow)) { onPick(SnoozeChoice.TOMORROW) }
            SheetRow(stringResource(R.string.action_snooze_next_week)) { onPick(SnoozeChoice.NEXT_WEEK) }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            SheetRow(stringResource(R.string.action_snooze_pick)) {
                val tomorrow = DateUtils.plusDays(System.currentTimeMillis(), 1)
                onPick(
                    SnoozeChoice.Custom(
                        dayStart = DateUtils.startOfDay(tomorrow),
                        hour = 9, minute = 0
                    )
                )
            }
        }
    }
}

@Composable
private fun SheetRow(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp)
    )
}
