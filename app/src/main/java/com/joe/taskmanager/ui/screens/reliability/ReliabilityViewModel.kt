
package com.joe.taskmanager.ui.screens.reliability

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joe.taskmanager.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReliabilityUiState(
    val notificationsGranted: Boolean = false,
    val exactAlarmGranted: Boolean = false,
    val fullScreenIntentGranted: Boolean = false,
    val batteryOptimizationExempt: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val manufacturer: String = ""
)

@HiltViewModel
class ReliabilityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduler: ReminderScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReliabilityUiState())
    val uiState: StateFlow<ReliabilityUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.value = readState()
        }
    }

    private fun readState(): ReliabilityUiState {
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val exempt = pm?.isIgnoringBatteryOptimizations(context.packageName) == true

        val canFullScreen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(NotificationManager::class.java)
                ?.canUseFullScreenIntent() == true
        } else true

        return ReliabilityUiState(
            notificationsGranted = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            exactAlarmGranted = scheduler.canScheduleExact(),
            fullScreenIntentGranted = canFullScreen,
            batteryOptimizationExempt = exempt,
            isIgnoringBatteryOptimizations = exempt,
            manufacturer = Build.MANUFACTURER ?: ""
        )
    }

    fun requestNotifications() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openExactAlarmSettings() {
        scheduler.exactAlarmSettingsIntent()?.let { intent ->
            runCatching { context.startActivity(intent) }
        }
    }

    fun openFullScreenSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT)
            .setData(Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    fun openBatterySettings() {
        val intent = scheduler.batteryOptimizationSettingsIntent()
        runCatching { context.startActivity(intent) }
            .onFailure {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
    }
}
