
package com.joe.taskmanager.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * PRD 8 "Proposed starting tokens": gray surfaces with brown as the accent.
 * Priority colors must stay distinct from list colors and legible in both themes.
 */
object AppColors {
    // Light
    val LightBackground = Color(0xFFF4F3F2)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceVariant = Color(0xFFE7E5E3)
    val LightAccent = Color(0xFF8B5E3C)
    val LightOnAccent = Color(0xFFFFFFFF)
    val LightOnBackground = Color(0xFF201F1E)
    val LightOnSurfaceVariant = Color(0xFF5F5B57)
    val LightOverdue = Color(0xFFC62828)
    val LightOutline = Color(0xFFD3CEC9)

    // Dark
    val DarkBackground = Color(0xFF161515)
    val DarkSurface = Color(0xFF201F1E)
    val DarkSurfaceVariant = Color(0xFF2B2927)
    val DarkAccent = Color(0xFFC89B77)
    val DarkOnAccent = Color(0xFF1A1209)
    val DarkOnBackground = Color(0xFFEDE8E3)
    val DarkOnSurfaceVariant = Color(0xFFB5ACA5)
    val DarkOverdue = Color(0xFFEF6B6B)
    val DarkOutline = Color(0xFF4A4540)

    // Priority. Hue-separated from the brown accent and from list colors so a
    // priority is never confused with a container (PRD 8).
    val PriorityNone = Color(0xFF9A938C)
    val PriorityLow = Color(0xFF3F7D58)
    val PriorityMedium = Color(0xFFB8860B)
    val PriorityHigh = Color(0xFFC62828)
    val PriorityHighDark = Color(0xFFEF6B6B)

    // Default list palette offered when creating a list (PRD 6.2: each list has a color).
    val ListPaletteLight = listOf(
        Color(0xFF8B5E3C), Color(0xFF3F7D58), Color(0xFF2F6F8F),
        Color(0xFF8C4A6B), Color(0xFF6B5B95), Color(0xFF9A6B2F)
    )
    val ListPaletteDark = listOf(
        Color(0xFFC89B77), Color(0xFF6FAF87), Color(0xFF6FA8C4),
        Color(0xFFC98BA5), Color(0xFFA695CE), Color(0xFFD0A66A)
    )
}
