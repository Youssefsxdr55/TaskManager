package com.joe.taskmanager.ui.screens

import android.app.NotificationManager
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.focus.FocusTimerService
import com.joe.taskmanager.focus.TimerSnapshot
import com.joe.taskmanager.ui.screens.focus.FocusViewModel
import kotlinx.coroutines.delay

@Composable
fun FocusScreen(onBack: () -> Unit, viewModel: FocusViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var snapshot by remember { mutableStateOf(FocusTimerService.activeSnapshot(context)) }
    var focus by remember { mutableStateOf("25") }
    var short by remember { mutableStateOf("5") }
    var long by remember { mutableStateOf("15") }
    var sessions by remember { mutableStateOf("4") }
    var auto by remember { mutableStateOf(false) }
    var dnd by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var taskMenu by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { while (true) { snapshot = FocusTimerService.activeSnapshot(context); delay(1000) } }
    val notificationManager = context.getSystemService<NotificationManager>()
    ScreenScaffold(title = stringResource(R.string.nav_focus), onBack = onBack) { content ->
        Column(content.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(if (snapshot.phase == FocusTimerService.PHASE_BREAK) R.string.focus_break_phase else R.string.focus_phase), style = MaterialTheme.typography.titleLarge)
            Text(formatRemaining(snapshot.remainingMillis), style = MaterialTheme.typography.displayLarge)
            if (snapshot.active) {
                Text(stringResource(R.string.focus_sessions_completed, snapshot.completedSessions))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (snapshot.paused) Button(onClick = { viewModel.command(FocusTimerService.ACTION_RESUME) }) { Text(stringResource(R.string.focus_resume)) }
                    else Button(onClick = { viewModel.command(FocusTimerService.ACTION_PAUSE) }) { Text(stringResource(R.string.focus_pause)) }
                    OutlinedButton(onClick = { viewModel.command(FocusTimerService.ACTION_STOP) }) { Text(stringResource(R.string.focus_stop)) }
                }
            } else if (snapshot.pending) {
                Button(onClick = { viewModel.command(FocusTimerService.ACTION_NEXT) }) { Text(stringResource(R.string.focus_start_next)) }
                OutlinedButton(onClick = { viewModel.command(FocusTimerService.ACTION_STOP) }) { Text(stringResource(R.string.focus_stop)) }
            } else {
                OutlinedTextField(focus, { focus = it.filter(Char::isDigit).take(3) }, label = { Text(stringResource(R.string.focus_minutes)) }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(short, { short = it.filter(Char::isDigit).take(2) }, label = { Text(stringResource(R.string.focus_short_break)) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(long, { long = it.filter(Char::isDigit).take(2) }, label = { Text(stringResource(R.string.focus_long_break)) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(sessions, { sessions = it.filter(Char::isDigit).take(2) }, label = { Text(stringResource(R.string.focus_sessions_before_long)) }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Box {
                    OutlinedButton(onClick = { taskMenu = true }) { Text(selectedTask?.title ?: stringResource(R.string.focus_link_task)) }
                    DropdownMenu(expanded = taskMenu, onDismissRequest = { taskMenu = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.focus_no_task)) }, onClick = { selectedTask = null; taskMenu = false })
                        tasks.forEach { task -> DropdownMenuItem(text = { Text(task.title) }, onClick = { selectedTask = task; taskMenu = false }) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(dnd, { dnd = it })
                    Text(stringResource(R.string.focus_do_not_disturb))
                }
                if (dnd && notificationManager?.isNotificationPolicyAccessGranted != true) {
                    TextButton(onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }) {
                        Text(stringResource(R.string.focus_grant_dnd))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(auto, { auto = it }); Text(stringResource(R.string.focus_auto_start))
                }
                Button(enabled = (focus.toIntOrNull() ?: 0) > 0 && (short.toIntOrNull() ?: 0) > 0,
                    onClick = { viewModel.start(focus.toIntOrNull() ?: 25, short.toIntOrNull() ?: 5,
                        long.toIntOrNull() ?: 15, sessions.toIntOrNull() ?: 4, auto,
                        dnd && notificationManager?.isNotificationPolicyAccessGranted == true, selectedTask?.id) }) {
                    Text(stringResource(R.string.focus_start))
                }
            }
        }
    }
}

private fun formatRemaining(ms: Long): String {
    val seconds = (ms / 1000).coerceAtLeast(0)
    return String.format(java.util.Locale.US, "%02d:%02d", seconds / 60, seconds % 60)
}
