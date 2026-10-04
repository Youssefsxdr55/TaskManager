
package com.joe.taskmanager.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.joe.taskmanager.data.local.entity.Priority
import com.joe.taskmanager.data.settings.ThemeMode

// Declared before the schemes: Kotlin initializes top-level properties in file
// order, so referencing these from a scheme declared earlier would read them as null.
private val Color_White = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
private val Color_Black = androidx.compose.ui.graphics.Color(0xFF000000)

private val LightScheme = lightColorScheme(
    primary = AppColors.LightAccent,
    onPrimary = AppColors.LightOnAccent,
    secondary = AppColors.LightAccent,
    onSecondary = AppColors.LightOnAccent,
    background = AppColors.LightBackground,
    onBackground = AppColors.LightOnBackground,
    surface = AppColors.LightSurface,
    onSurface = AppColors.LightOnBackground,
    surfaceVariant = AppColors.LightSurfaceVariant,
    onSurfaceVariant = AppColors.LightOnSurfaceVariant,
    error = AppColors.LightOverdue,
    onError = Color_White,
    outline = AppColors.LightOutline,
    outlineVariant = AppColors.LightOutline
)

private val DarkScheme = darkColorScheme(
    primary = AppColors.DarkAccent,
    onPrimary = AppColors.DarkOnAccent,
    secondary = AppColors.DarkAccent,
    onSecondary = AppColors.DarkOnAccent,
    background = AppColors.DarkBackground,
    onBackground = AppColors.DarkOnBackground,
    surface = AppColors.DarkSurface,
    onSurface = AppColors.DarkOnBackground,
    surfaceVariant = AppColors.DarkSurfaceVariant,
    onSurfaceVariant = AppColors.DarkOnSurfaceVariant,
    error = AppColors.DarkOverdue,
    onError = Color_Black,
    outline = AppColors.DarkOutline,
    outlineVariant = AppColors.DarkOutline
)

/** TickTick-like layout density (PRD 8: clean, simple, minimal). */
private val AppTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

@Composable
fun TaskManagerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content
    )
}

/** Priority -> color, theme aware. Kept next to the theme so colors stay consistent. */
@Composable
fun priorityColor(priority: Priority): androidx.compose.ui.graphics.Color {
    val dark = isSystemInDarkTheme()
    return when (priority) {
        Priority.NONE -> AppColors.PriorityNone
        Priority.LOW -> AppColors.PriorityLow
        Priority.MEDIUM -> AppColors.PriorityMedium
        Priority.HIGH -> if (dark) AppColors.PriorityHighDark else AppColors.PriorityHigh
    }
}

@Composable
fun listPalette(): List<androidx.compose.ui.graphics.Color> =
    if (isSystemInDarkTheme()) AppColors.ListPaletteDark else AppColors.ListPaletteLight
