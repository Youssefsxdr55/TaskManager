package com.joe.taskmanager.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.joe.taskmanager.R

enum class MainTab { TODAY, UPCOMING, CALENDAR }

@Composable
fun MainBottomBar(
    selected: MainTab,
    onToday: () -> Unit,
    onUpcoming: () -> Unit,
    onCalendar: () -> Unit
) {
    NavigationBar(
        modifier = Modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        NavigationBarItem(
            selected = selected == MainTab.TODAY,
            onClick = onToday,
            icon = { Icon(Icons.Filled.Today, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_today)) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
        )
        NavigationBarItem(
            selected = selected == MainTab.UPCOMING,
            onClick = onUpcoming,
            icon = { Icon(Icons.Filled.ViewAgenda, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_upcoming)) }
        )
        NavigationBarItem(
            selected = selected == MainTab.CALENDAR,
            onClick = onCalendar,
            icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
            label = { Text(stringResource(R.string.nav_calendar)) }
        )
    }
}
