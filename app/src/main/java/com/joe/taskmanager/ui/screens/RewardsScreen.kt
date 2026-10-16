package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.data.local.entity.Reward
import com.joe.taskmanager.data.gamification.PointRules
import com.joe.taskmanager.ui.screens.rewards.RewardsViewModel

@Composable
fun RewardsScreen(viewModel: RewardsViewModel = hiltViewModel()) {
    val rewards by viewModel.rewards.collectAsStateWithLifecycle()
    val redemptions by viewModel.redemptions.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val earned by viewModel.earned.collectAsStateWithLifecycle()
    val badges by viewModel.badges.collectAsStateWithLifecycle()
    var edit by remember { mutableStateOf<Reward?>(null) }
    var create by remember { mutableStateOf(false) }
    ScreenScaffold(title = stringResource(R.string.nav_rewards), floatingActionButton = {
        FloatingActionButton(onClick = { edit = null; create = true }) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.reward_add))
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().then(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.reward_balance, balance), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.reward_lifetime, earned))
                Text(stringResource(R.string.reward_level, PointRules.levelFor(earned)))
            } } }
            if (rewards.isEmpty()) item { Text(stringResource(R.string.rewards_empty)) }
            items(rewards, key = { it.id }) { reward ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) {
                    Text(listOfNotNull(reward.emoji, reward.name).joinToString(" "), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.reward_cost, reward.cost))
                    reward.note?.let { Text(it) }
                    Row {
                        TextButton(enabled = balance >= reward.cost, onClick = { viewModel.redeem(reward) }) { Text(stringResource(R.string.reward_redeem)) }
                        TextButton(onClick = { edit = reward }) { Text(stringResource(R.string.action_edit)) }
                        TextButton(onClick = { viewModel.archive(reward.id) }) { Text(stringResource(R.string.action_archive)) }
                    }
                } }
            }
            item { Text(stringResource(R.string.badges_title), style = MaterialTheme.typography.titleMedium) }
            items(badges, key = { it.badgeCode }) { badge ->
                val label = when (badge.badgeCode) {
                    "first_task" -> R.string.badge_first_task
                    "tasks_10" -> R.string.badge_tasks_10
                    "tasks_100" -> R.string.badge_tasks_100
                    "tasks_1000" -> R.string.badge_tasks_1000
                    "habit_7_day" -> R.string.badge_habit_7
                    "habit_30_day" -> R.string.badge_habit_30
                    "focus_hour" -> R.string.badge_focus_hour
                    "focus_10_hours" -> R.string.badge_focus_10_hours
                    "perfect_day" -> R.string.badge_perfect_day
                    "comeback" -> R.string.badge_comeback
                    else -> R.string.badge_generic
                }
                Text("🏅 ${stringResource(label)}")
            }
            item { Text(stringResource(R.string.reward_history), style = MaterialTheme.typography.titleMedium) }
            if (redemptions.isEmpty()) item { Text(stringResource(R.string.reward_no_redemptions)) }
            items(redemptions, key = { "redemption-${it.id}" }) { item ->
                Text(stringResource(R.string.reward_history_entry, item.cost))
            }
        }
    }
    if (create || edit != null) RewardDialog(edit, onDismiss = { create = false; edit = null }, onSave = { name, emoji, cost, note ->
        viewModel.save(edit, name, emoji, cost, note); create = false; edit = null
    })
}

@Composable
private fun RewardDialog(item: Reward?, onDismiss: () -> Unit, onSave: (String, String, Int, String) -> Unit) {
    var name by remember(item?.id) { mutableStateOf(item?.name.orEmpty()) }
    var emoji by remember(item?.id) { mutableStateOf(item?.emoji.orEmpty()) }
    var cost by remember(item?.id) { mutableStateOf(item?.cost?.toString().orEmpty()) }
    var note by remember(item?.id) { mutableStateOf(item?.note.orEmpty()) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(stringResource(if (item == null) R.string.reward_add else R.string.reward_edit)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.reward_name)) }, singleLine = true)
            OutlinedTextField(emoji, { emoji = it }, label = { Text(stringResource(R.string.reward_emoji)) }, singleLine = true)
            OutlinedTextField(cost, { cost = it.filter(Char::isDigit) }, label = { Text(stringResource(R.string.reward_cost_label)) }, singleLine = true)
            OutlinedTextField(note, { note = it }, label = { Text(stringResource(R.string.reward_note)) })
        } },
        confirmButton = { TextButton(enabled = name.isNotBlank() && (cost.toIntOrNull() ?: 0) > 0,
            onClick = { onSave(name, emoji, cost.toIntOrNull() ?: 0, note) }) { Text(stringResource(R.string.action_save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } })
}
