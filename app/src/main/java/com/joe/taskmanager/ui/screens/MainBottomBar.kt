package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composeunstyled.Button
import com.joe.taskmanager.R

enum class MainTab { TODAY, UPCOMING, CALENDAR }

@Composable
fun MainBottomBar(
    selected: MainTab,
    onToday: () -> Unit,
    onUpcoming: () -> Unit,
    onCalendar: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 5.dp
    ) {
        Row(
            modifier = Modifier.padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTabButton(
                modifier = Modifier.weight(1f),
                selected = selected == MainTab.TODAY,
                label = stringResource(R.string.nav_today),
                icon = { Icon(Icons.Filled.Today, contentDescription = null) },
                onClick = onToday
            )
            MainTabButton(
                modifier = Modifier.weight(1f),
                selected = selected == MainTab.UPCOMING,
                label = stringResource(R.string.nav_upcoming),
                icon = { Icon(Icons.Filled.ViewAgenda, contentDescription = null) },
                onClick = onUpcoming
            )
            MainTabButton(
                modifier = Modifier.weight(1f),
                selected = selected == MainTab.CALENDAR,
                label = stringResource(R.string.nav_calendar),
                icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                onClick = onCalendar
            )
        }
    }
}

@Composable
private fun MainTabButton(
    modifier: Modifier,
    selected: Boolean,
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) colors.primary.copy(alpha = 0.12f) else colors.surface),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 5.dp),
        indication = LocalIndication.current,
        contentColor = if (selected) colors.primary else colors.onSurfaceVariant,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            icon()
            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}
