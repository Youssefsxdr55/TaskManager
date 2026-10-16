
package com.joe.taskmanager.data.settings

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * PRD 7.14 Settings. Week start is fixed to Saturday and is deliberately not
 * configurable (PRD 7.14 "week start fixed to Saturday").
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")           // "system" | "ar" | "en"
        val SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
        val FULL_SCREEN_ENABLED = booleanPreferencesKey("full_screen_alarm_enabled")
        val HABIT_REMINDER_HOUR = intPreferencesKey("habit_reminder_hour")
        val HABIT_REMINDER_MINUTE = intPreferencesKey("habit_reminder_minute")
        val POSTPONE_ALERT_THRESHOLD = intPreferencesKey("postpone_alert_threshold")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_complete")
        val BACKUP_FOLDER_URI = stringPreferencesKey("backup_folder_uri")
        val BACKUP_RETENTION = intPreferencesKey("backup_retention_count")
        val LAST_BACKUP_DATE = stringPreferencesKey("last_backup_date")
        val LAST_DAY_CLOSE_DATE = stringPreferencesKey("last_day_close_date")
    }

    data class Settings(
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        val language: String = "system",
        val defaultSnoozeMinutes: Int = 10,
        val fullScreenAlarmEnabled: Boolean = true,
        val habitReminderHour: Int = 21,
        val habitReminderMinute: Int = 0,
        val postponeAlertThreshold: Int = 3,
        val onboardingComplete: Boolean = false,
        val backupFolderUri: String? = null,
        val backupRetention: Int = 7,
        val lastBackupDate: String? = null,
        val lastDayCloseDate: String? = null
    )

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            themeMode = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            language = p[Keys.LANGUAGE] ?: "system",
            defaultSnoozeMinutes = p[Keys.SNOOZE_MINUTES] ?: 10,
            fullScreenAlarmEnabled = p[Keys.FULL_SCREEN_ENABLED] ?: true,
            habitReminderHour = p[Keys.HABIT_REMINDER_HOUR] ?: 21,
            habitReminderMinute = p[Keys.HABIT_REMINDER_MINUTE] ?: 0,
            postponeAlertThreshold = p[Keys.POSTPONE_ALERT_THRESHOLD] ?: 3,
            onboardingComplete = p[Keys.ONBOARDING_DONE] ?: false,
            backupFolderUri = p[Keys.BACKUP_FOLDER_URI],
            backupRetention = p[Keys.BACKUP_RETENTION] ?: 7,
            lastBackupDate = p[Keys.LAST_BACKUP_DATE],
            lastDayCloseDate = p[Keys.LAST_DAY_CLOSE_DATE]
        )
    }

    suspend fun setTheme(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME] = mode.name }

    suspend fun setLanguage(tag: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = tag }
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(if (tag == "system") "" else tag))
    }

    suspend fun setSnoozeMinutes(minutes: Int) = context.dataStore.edit { it[Keys.SNOOZE_MINUTES] = minutes }

    suspend fun setFullScreenAlarmEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.FULL_SCREEN_ENABLED] = enabled }

    suspend fun setHabitReminderTime(hour: Int, minute: Int) = context.dataStore.edit {
        it[Keys.HABIT_REMINDER_HOUR] = hour
        it[Keys.HABIT_REMINDER_MINUTE] = minute
    }

    suspend fun setPostponeAlertThreshold(n: Int) = context.dataStore.edit { it[Keys.POSTPONE_ALERT_THRESHOLD] = n }

    suspend fun setOnboardingComplete(done: Boolean) = context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }

    suspend fun setBackupFolder(uri: String?) = context.dataStore.edit {
        if (uri == null) it.remove(Keys.BACKUP_FOLDER_URI) else it[Keys.BACKUP_FOLDER_URI] = uri
    }

    suspend fun setBackupRetention(count: Int) = context.dataStore.edit { it[Keys.BACKUP_RETENTION] = count }

    suspend fun setLastBackupDate(date: String?) = context.dataStore.edit {
        if (date == null) it.remove(Keys.LAST_BACKUP_DATE) else it[Keys.LAST_BACKUP_DATE] = date
    }

    suspend fun setLastDayCloseDate(date: String?) = context.dataStore.edit {
        if (date == null) it.remove(Keys.LAST_DAY_CLOSE_DATE) else it[Keys.LAST_DAY_CLOSE_DATE] = date
    }

    /** Stable string representation used by the versioned backup format. */
    suspend fun exportableSettings(): Map<String, String> {
        val value = settings.first()
        return buildMap {
            put("themeMode", value.themeMode.name)
            put("language", value.language)
            put("defaultSnoozeMinutes", value.defaultSnoozeMinutes.toString())
            put("fullScreenAlarmEnabled", value.fullScreenAlarmEnabled.toString())
            put("habitReminderHour", value.habitReminderHour.toString())
            put("habitReminderMinute", value.habitReminderMinute.toString())
            put("postponeAlertThreshold", value.postponeAlertThreshold.toString())
            put("onboardingComplete", value.onboardingComplete.toString())
            put("backupRetention", value.backupRetention.toString())
            value.backupFolderUri?.let { put("backupFolderUri", it) }
            value.lastBackupDate?.let { put("lastBackupDate", it) }
            value.lastDayCloseDate?.let { put("lastDayCloseDate", it) }
        }
    }

    suspend fun restoreExportableSettings(values: Map<String, String>) = context.dataStore.edit { p ->
        p.remove(Keys.THEME)
        p.remove(Keys.LANGUAGE)
        p.remove(Keys.SNOOZE_MINUTES)
        p.remove(Keys.FULL_SCREEN_ENABLED)
        p.remove(Keys.HABIT_REMINDER_HOUR)
        p.remove(Keys.HABIT_REMINDER_MINUTE)
        p.remove(Keys.POSTPONE_ALERT_THRESHOLD)
        p.remove(Keys.ONBOARDING_DONE)
        p.remove(Keys.BACKUP_FOLDER_URI)
        p.remove(Keys.BACKUP_RETENTION)
        p.remove(Keys.LAST_BACKUP_DATE)
        p.remove(Keys.LAST_DAY_CLOSE_DATE)
        values["themeMode"]?.let { raw -> runCatching { ThemeMode.valueOf(raw) }.getOrNull()?.let { p[Keys.THEME] = it.name } }
        values["language"]?.let { p[Keys.LANGUAGE] = it }
        values["defaultSnoozeMinutes"]?.toIntOrNull()?.coerceIn(1, 120)?.let { p[Keys.SNOOZE_MINUTES] = it }
        values["fullScreenAlarmEnabled"]?.toBooleanStrictOrNull()?.let { p[Keys.FULL_SCREEN_ENABLED] = it }
        values["habitReminderHour"]?.toIntOrNull()?.takeIf { it in 0..23 }?.let { p[Keys.HABIT_REMINDER_HOUR] = it }
        values["habitReminderMinute"]?.toIntOrNull()?.takeIf { it in 0..59 }?.let { p[Keys.HABIT_REMINDER_MINUTE] = it }
        values["postponeAlertThreshold"]?.toIntOrNull()?.coerceAtLeast(1)?.let { p[Keys.POSTPONE_ALERT_THRESHOLD] = it }
        values["onboardingComplete"]?.toBooleanStrictOrNull()?.let { p[Keys.ONBOARDING_DONE] = it }
        values["backupRetention"]?.toIntOrNull()?.coerceIn(1, 90)?.let { p[Keys.BACKUP_RETENTION] = it }
        values["backupFolderUri"]?.let { uri ->
            // SAF grants are tied to this install/device; restore the URI only if
            // Android still holds the matching persisted write permission.
            val hasGrant = context.contentResolver.persistedUriPermissions.any {
                it.uri.toString() == uri && it.isReadPermission && it.isWritePermission
            }
            if (hasGrant) p[Keys.BACKUP_FOLDER_URI] = uri
        }
        values["lastBackupDate"]?.let { p[Keys.LAST_BACKUP_DATE] = it }
        values["lastDayCloseDate"]?.let { p[Keys.LAST_DAY_CLOSE_DATE] = it }
    }

    /** Called once at startup so the very first frame already uses the right theme. */
    suspend fun applyStoredTheme() {
        val mode = settings.let { flow ->
            var result = ThemeMode.SYSTEM
            flow.collect { result = it.themeMode; return@collect }
            result
        }
        AppCompatDelegate.setDefaultNightMode(
            when (mode) {
                ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            }
        )
        val language = settings.first().language
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(if (language == "system") "" else language))
    }

    companion object {
        /** YYYY-MM-DD in the device's current locale-independent calendar. */
        fun todayKey(cal: Calendar = Calendar.getInstance()): String =
            String.format(
                Locale.US, "%04d-%02d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH)
            )
    }
}
