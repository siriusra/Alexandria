package com.alexandria.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TrueBlack = Color(0xFF000000)

fun buildAccentColorScheme(darkTheme: Boolean, accentIndex: Int): ColorScheme {
    val accent = accentColors.getOrElse(accentIndex) { accentColors[0] }
    val primaryColor = if (darkTheme) accent.dark else accent.light
    return if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            onPrimary = KindleNearBlack,
            primaryContainer = primaryColor.copy(alpha = 0.2f),
            onPrimaryContainer = primaryColor,
            secondary = KindlePurple,
            onSecondary = KindleNearBlack,
            background = TrueBlack,
            onBackground = Color(0xFFE0E0E0),
            surface = Color(0xFF1A1A1A),
            onSurface = Color(0xFFE0E0E0),
            surfaceVariant = Color(0xFF1A1A1A),
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
            background = Color.White,
            onBackground = KindleNearBlack,
            surface = KindleLightGray,
            onSurface = KindleNearBlack,
            surfaceVariant = KindleLightGray,
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
    val colorScheme = buildAccentColorScheme(darkTheme, accentIndex)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KindleTypography,
        content = content
    )
}
