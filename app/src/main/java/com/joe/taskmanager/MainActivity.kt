
package com.joe.taskmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.joe.taskmanager.data.settings.SettingsRepository
import com.joe.taskmanager.ui.navigation.TaskManagerNavHost
import com.joe.taskmanager.ui.theme.TaskManagerTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hold the splash until settings (theme + language) have been read, so the
        // first composed frame is already correct (PRD 2.3: cold start < 2s, and
        // no flash of the wrong theme).
        val ready = MutableStateFlow(false)
        splash.setKeepOnScreenCondition { !ready.value }

        setContent {
            val settings by settingsRepository.settings
                .stateIn(
                    scope = lifecycleScope,
                    started = SharingStarted.Eagerly,
                    initialValue = null
                )
            ready.value = settings != null

            val value = settings
            TaskManagerTheme(themeMode = value?.themeMode ?: com.joe.taskmanager.data.settings.ThemeMode.SYSTEM) {
                TaskManagerNavHost()
            }
        }
    }
}
