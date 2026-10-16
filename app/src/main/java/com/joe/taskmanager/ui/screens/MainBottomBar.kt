package com.joe.taskmanager.ui.screens

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.joe.taskmanager.R

/** The v1.0 main tabs from PRD 6.1. */
@Composable
fun MainBottomBar(showToday: Boolean, onToday: () -> Unit, onUpcoming: () -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = showToday,
            onClick = onToday,
            icon = {},
            label = { Text(stringResource(R.string.nav_today)) }
        )
        NavigationBarItem(
            selected = !showToday,
            onClick = onUpcoming,
            icon = {},
            label = { Text(stringResource(R.string.nav_upcoming)) }
        )
    }
}
