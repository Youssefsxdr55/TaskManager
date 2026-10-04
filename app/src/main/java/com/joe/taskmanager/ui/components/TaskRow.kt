
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.joe.taskmanager.R
import com.joe.taskmanager.data.local.entity.Priority
import com.joe.taskmanager.data.local.entity.Task
import com.joe.taskmanager.data.local.entity.TaskStatus
import com.joe.taskmanager.ui.theme.priorityColor

/**
 * One task row.
 *
 * PRD 7.1: an overdue task stays in its original position and is rendered in red
 * rather than being moved or hidden. Priority is signalled by a leading bar, not
 * by bolding the title, so the title remains readable at small font scales.
 */
@Composable
fun TaskRow(
    task: Task,
    onToggleComplete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showListName: String? = null
) {
    val completed = task.status == TaskStatus.COMPLETED
    // PRD 8: overdue red is only for genuinely overdue, still-open tasks.
    val titleColor = when {
        completed -> MaterialTheme.colorScheme.onSurfaceVariant
        task.isOverdue -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onBackground
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
            onClick = onToggleComplete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = when {
                    completed -> Icons.Filled.CheckCircle
                    task.priority == Priority.HIGH -> Icons.Filled.Circle
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

            val meta = buildMetaLine(task, completed)
            if (meta.isNotEmpty()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (task.isOverdue && !completed)
                        MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Builds the "meta" line under the title. Kept as a plain function so the same
 * formatting rules are shared by the row and the task detail screen.
 */
@Composable
private fun buildMetaLine(task: Task, completed: Boolean): String {
    val parts = mutableListOf<String>()

    when (task.priority) {
        Priority.HIGH -> parts += stringResource(R.string.priority_high)
        Priority.MEDIUM -> parts += stringResource(R.string.priority_medium)
        Priority.LOW -> parts += stringResource(R.string.priority_low)
        Priority.NONE -> Unit
    }

    if (task.hasTime) {
        parts += com.joe.taskmanager.ui.util.DateText.format(task.dueDate ?: return "")
    }

    return parts.joinToString("  ·  ")
}
