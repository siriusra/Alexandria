package com.alexandria.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.alexandria.app.domain.model.VisualMode

private val TrueBlack = Color(0xFF000000)

private val WarmLightBg = Color(0xFFFDF8F3)
private val WarmLightSurface = Color(0xFFFFFBF7)
private val WarmLightSurfaceVariant = Color(0xFFF5EDE4)

private val WarmDarkBg = Color(0xFF1A1614)
private val WarmDarkSurface = Color(0xFF24201D)
private val WarmDarkSurfaceVariant = Color(0xFF2D2925)

fun buildAccentColorScheme(darkTheme: Boolean, accentIndex: Int, visualMode: VisualMode = VisualMode.CLASSIC): ColorScheme {
    val accent = accentColors.getOrElse(accentIndex) { accentColors[0] }
    val primaryColor = if (darkTheme) accent.dark else accent.light
    val isImmersive = visualMode == VisualMode.IMMERSIVE
    return if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = KindleNearBlack,
            primaryContainer = primaryColor.copy(alpha = 0.2f),
            onPrimaryContainer = primaryColor,
            secondary = KindlePurple,
            onSecondary = KindleNearBlack,
            background = if (isImmersive) WarmDarkBg else TrueBlack,
            onBackground = Color(0xFFE0E0E0),
            surface = if (isImmersive) WarmDarkSurface else Color(0xFF1A1A1A),
            onSurface = Color(0xFFE0E0E0),
            surfaceVariant = if (isImmersive) WarmDarkSurfaceVariant else Color(0xFF1A1A1A),
            onSurfaceVariant = Color(0xFFE0E0E0).copy(alpha = 0.7f)
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            onPrimary = Color.White,
            primaryContainer = primaryColor.copy(alpha = 0.1f),
            onPrimaryContainer = primaryColor,
            secondary = KindleDark,
            onSecondary = Color.White,
            background = if (isImmersive) WarmLightBg else Color.White,
            onBackground = KindleNearBlack,
            surface = if (isImmersive) WarmLightSurface else KindleLightGray,
            onSurface = KindleNearBlack,
            surfaceVariant = if (isImmersive) WarmLightSurfaceVariant else KindleLightGray,
            onSurfaceVariant = KindleNearBlack.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun AlexandriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val visualMode = LocalVisualMode.current
    val colorScheme = buildAccentColorScheme(darkTheme, accentIndex, visualMode)
    val shapes = if (visualMode == VisualMode.IMMERSIVE) AlexandriaShapes.Immersive else AlexandriaShapes.Classic

    CompositionLocalProvider(LocalAlexandriaShapes provides shapes) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = KindleTypography,
            content = content
        )
    }
}
