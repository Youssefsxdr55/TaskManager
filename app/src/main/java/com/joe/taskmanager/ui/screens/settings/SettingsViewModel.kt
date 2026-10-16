
package com.joe.taskmanager.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.R
import com.joe.taskmanager.data.settings.SettingsRepository
import com.joe.taskmanager.data.gamification.PointsEngine
import com.joe.taskmanager.notification.HabitReminderScheduler
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
    val accentColor: String = "terracotta",
    val accentLabelRes: Int = R.string.accent_terracotta,
    val dynamicColor: Boolean = false,
    val textScale: String = "standard",
    val textScaleLabelRes: Int = R.string.text_scale_standard,
    val language: String = "system",
    val snoozeMinutes: Int = 10,
    val postponeThreshold: Int = 3,
    val habitReminderHour: Int = 21
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val pointsEngine: PointsEngine,
    private val habitReminderScheduler: HabitReminderScheduler
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
                accentColor = s.accentColor,
                accentLabelRes = when (s.accentColor) {
                    "ocean" -> R.string.accent_ocean
                    "forest" -> R.string.accent_forest
                    "violet" -> R.string.accent_violet
                    "rose" -> R.string.accent_rose
                    "amber" -> R.string.accent_amber
                    else -> R.string.accent_terracotta
                },
                dynamicColor = s.dynamicColor,
                textScale = s.textScale,
                textScaleLabelRes = when (s.textScale) {
                    "compact" -> R.string.text_scale_compact
                    "large" -> R.string.text_scale_large
                    else -> R.string.text_scale_standard
                },
                language = s.language,
                snoozeMinutes = s.defaultSnoozeMinutes,
                postponeThreshold = s.postponeAlertThreshold,
                habitReminderHour = s.habitReminderHour
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

    fun setAccentColor(color: String) { viewModelScope.launch { repository.setAccentColor(color) } }

    fun toggleDynamicColor() {
        viewModelScope.launch { repository.setDynamicColor(!uiState.value.dynamicColor) }
    }

    fun setTextScale(scale: String) { viewModelScope.launch { repository.setTextScale(scale) } }

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

    fun cycleHabitReminderTime() {
        val next = when (uiState.value.habitReminderHour) { 18 -> 19; 19 -> 20; 20 -> 21; 21 -> 22; 22 -> 23; else -> 18 }
        viewModelScope.launch { repository.setHabitReminderTime(next, 0); habitReminderScheduler.scheduleNext() }
    }

    fun cyclePostponeThreshold() {
        val next = when (uiState.value.postponeThreshold) { 2 -> 3; 3 -> 5; 5 -> 10; else -> 2 }
        viewModelScope.launch { repository.setPostponeAlertThreshold(next) }
    }

    fun resetPoints() { viewModelScope.launch { pointsEngine.resetPoints() } }

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
