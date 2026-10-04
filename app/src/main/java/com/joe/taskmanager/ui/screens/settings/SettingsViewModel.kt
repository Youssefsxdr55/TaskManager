
package com.joe.taskmanager.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.R
import com.joe.taskmanager.data.settings.SettingsRepository
import com.joe.taskmanager.data.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themeLabelRes: Int = R.string.theme_system,
    val language: String = "system",
    val snoozeMinutes: Int = 10
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = repository.settings
        .map { s ->
            SettingsUiState(
                themeMode = s.themeMode,
                themeLabelRes = when (s.themeMode) {
                    ThemeMode.SYSTEM -> R.string.theme_system
                    ThemeMode.LIGHT -> R.string.theme_light
                    ThemeMode.DARK -> R.string.theme_dark
                },
                language = s.language,
                snoozeMinutes = s.defaultSnoozeMinutes
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun cycleTheme() {
        val next = when (uiState.value.themeMode) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        viewModelScope.launch { repository.setTheme(next) }
    }

    /**
     * Cycles system -> English -> Arabic. The Android per-app language API is
     * applied by the system on next activity recreation, so the value is stored
     * here and read at startup.
     */
    fun cycleLanguage() {
        val next = when (uiState.value.language) {
            "system" -> "en"
            "en" -> "ar"
            else -> "system"
        }
        viewModelScope.launch { repository.setLanguage(next) }
    }

    fun cycleSnooze() {
        val next = when (uiState.value.snoozeMinutes) {
            5 -> 10
            10 -> 15
            15 -> 30
            30 -> 60
            else -> 5
        }
        viewModelScope.launch { repository.setSnoozeMinutes(next) }
    }
}
