
package com.joe.taskmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.joe.taskmanager.R
import com.joe.taskmanager.ui.components.SnoozeChoice
import com.joe.taskmanager.data.local.entity.Priority
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.data.local.entity.TaskStatus
import com.joe.taskmanager.ui.theme.priorityColor
import com.joe.taskmanager.ui.util.DateText

/**
 * One task row.
 *
 * PRD 7.1: an overdue task stays in its original position and is rendered in red
 * rather than being moved or hidden. Priority is signalled by a leading bar, not
 * by bolding the title, so the title remains readable at small font scales.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TaskRow(
    task: Task,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showListName: String? = null,
    onSnooze: (SnoozeChoice) -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var showSnooze by remember(task.id) { mutableStateOf(false) }
    var showDeleteConfirmation by remember(task.id) { mutableStateOf(false) }
    var showCelebration by remember(task.id) { mutableStateOf(false) }
    val completed = task.status == TaskStatus.COMPLETED
    val celebration by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.task_complete))
    LaunchedEffect(showCelebration) {
        if (showCelebration) {
            kotlinx.coroutines.delay(1_100)
            showCelebration = false
        }
    }
    val complete = {
        if (!completed) showCelebration = true
        onToggleComplete()
    }
    val dismissState = rememberSwipeToDismissBoxState(confirmValueChange = { target ->
        when (target) {
            SwipeToDismissBoxValue.StartToEnd -> { if (!completed) complete(); false }
            SwipeToDismissBoxValue.EndToStart -> { showDeleteConfirmation = true; false }
            SwipeToDismissBoxValue.Settled -> true
        }
    })
    // PRD 8: overdue red is only for genuinely overdue, still-open tasks.
    val titleColor = when {
        completed -> MaterialTheme.colorScheme.onSurfaceVariant
        task.isOverdue -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onBackground
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = !completed,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                androidx.compose.material3.SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                androidx.compose.material3.SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                androidx.compose.material3.SwipeToDismissBoxValue.Settled -> MaterialTheme.colorScheme.surface
            }
            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                Alignment.CenterEnd else Alignment.CenterStart
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(color).padding(16.dp),
                contentAlignment = alignment
            ) {
                Text(
                    text = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                        stringResource(R.string.cd_mark_done) else stringResource(R.string.action_delete),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    ) {
      ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
      ) {
       Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
       ) {
        // Priority bar
        Box(
            modifier = Modifier
                .padding(top = 4.dp, end = 10.dp)
                .width(3.dp)
                .height(if (completed) 18.dp else 34.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    if (task.priority == Priority.NONE || completed)
                        Color.Transparent
                    else priorityColor(task.priority)
                )
        )

        IconButton(
            onClick = complete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = when {
                    completed -> Icons.Filled.CheckCircle
                    else -> Icons.Outlined.Circle
                },
                contentDescription = stringResource(
                    if (completed) R.string.cd_mark_open else R.string.cd_mark_done
                ),
                tint = when {
                    completed -> MaterialTheme.colorScheme.primary
                    task.priority == Priority.HIGH -> priorityColor(Priority.HIGH)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (completed) TextDecoration.LineThrough else null,
                fontWeight = if (task.priority == Priority.HIGH) FontWeight.SemiBold else FontWeight.Normal
            )

            if (showListName != null) {
                Text(
                    text = showListName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (task.priority != Priority.NONE || task.hasTime) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    when (task.priority) {
                        Priority.HIGH -> R.string.priority_high
                        Priority.MEDIUM -> R.string.priority_medium
                        Priority.LOW -> R.string.priority_low
                        Priority.NONE -> null
                    }?.let { label ->
                        Text(
                            text = stringResource(label),
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor(task.priority),
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(
                                priorityColor(task.priority).copy(alpha = 0.14f)
                            ).padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    val dueDate = task.dueDate
                    val timeLabel = if (task.hasTime && dueDate != null) DateText.format(dueDate) else null
                    if (!timeLabel.isNullOrEmpty()) {
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (task.isOverdue && !completed) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        if (showCelebration) {
            LottieAnimation(
                composition = celebration,
                iterations = 1,
                modifier = Modifier.size(48.dp)
            )
        }
        if (!completed) {
            IconButton(onClick = { showSnooze = true }, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = stringResource(R.string.action_snooze),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
       }
      }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(stringResource(R.string.task_delete_confirm_title)) },
            text = { Text(stringResource(R.string.task_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete()
                }) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (showSnooze) {
        SnoozeSheet(
            onDismiss = { showSnooze = false },
            onPick = { choice ->
                showSnooze = false
                onSnooze(choice)
            }
        )
    }
}
