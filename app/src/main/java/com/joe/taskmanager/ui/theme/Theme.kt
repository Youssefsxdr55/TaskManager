
package com.joe.taskmanager.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
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
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.joe.taskmanager.R
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

private val GoogleFontsProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

/** Cairo is downloaded through AndroidX Google Fonts and falls back cleanly if offline. */
private val Cairo = FontFamily(
    Font(
        googleFont = GoogleFont("Cairo"),
        fontProvider = GoogleFontsProvider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("Cairo"),
        fontProvider = GoogleFontsProvider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("Cairo"),
        fontProvider = GoogleFontsProvider,
        weight = FontWeight.SemiBold
    ),
    Font(
        googleFont = GoogleFont("Cairo"),
        fontProvider = GoogleFontsProvider,
        weight = FontWeight.Bold
    )
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Bold, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Bold, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.SemiBold, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(
        fontFamily = Cairo,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(
        fontFamily = Cairo,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Cairo,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    titleSmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(
        fontFamily = Cairo,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Cairo,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(
        fontFamily = Cairo,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Cairo,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(fontFamily = Cairo, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp)
)

@Composable
fun TaskManagerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: String = "terracotta",
    dynamicColor: Boolean = false,
    textScale: String = "standard",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val (accent, onAccent) = AppColors.accent(accentColor, darkTheme)
    val scheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkScheme.copy(primary = accent, onPrimary = onAccent, secondary = accent, onSecondary = onAccent)
        else -> LightScheme.copy(primary = accent, onPrimary = onAccent, secondary = accent, onSecondary = onAccent)
    }
    val scale = when (textScale) { "compact" -> 0.92f; "large" -> 1.12f; else -> 1f }
    fun scaled(style: TextStyle) = style.copy(
        fontSize = (style.fontSize.value * scale).sp,
        lineHeight = (style.lineHeight.value * scale).sp
    )
    val typography = AppTypography.copy(
        displayLarge = scaled(AppTypography.displayLarge),
        displayMedium = scaled(AppTypography.displayMedium),
        displaySmall = scaled(AppTypography.displaySmall),
        headlineLarge = scaled(AppTypography.headlineLarge),
        headlineMedium = scaled(AppTypography.headlineMedium),
        headlineSmall = scaled(AppTypography.headlineSmall),
        titleLarge = scaled(AppTypography.titleLarge),
        titleMedium = scaled(AppTypography.titleMedium),
        titleSmall = scaled(AppTypography.titleSmall),
        bodyLarge = scaled(AppTypography.bodyLarge),
        bodyMedium = scaled(AppTypography.bodyMedium),
        bodySmall = scaled(AppTypography.bodySmall),
        labelLarge = scaled(AppTypography.labelLarge),
        labelMedium = scaled(AppTypography.labelMedium),
        labelSmall = scaled(AppTypography.labelSmall)
    )
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
        typography = typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(28.dp)
        ),
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
