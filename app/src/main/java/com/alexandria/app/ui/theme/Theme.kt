package com.alexandria.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = KindleTeal,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = KindleTeal.copy(alpha = 0.1f),
    onPrimaryContainer = KindleTeal,
    secondary = KindleDark,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = KindleDark.copy(alpha = 0.1f),
    onSecondaryContainer = KindleDark,
    tertiary = KindleOrange,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    background = androidx.compose.ui.graphics.Color.White,
    onBackground = KindleNearBlack,
    surface = KindleLightGray,
    onSurface = KindleNearBlack,
    surfaceVariant = KindleLightGray,
    onSurfaceVariant = KindleNearBlack.copy(alpha = 0.7f)
)

private val DarkColorScheme = darkColorScheme(
    primary = KindleLightTeal,
    onPrimary = KindleNearBlack,
    primaryContainer = KindleLightTeal.copy(alpha = 0.2f),
    onPrimaryContainer = KindleLightTeal,
    secondary = KindlePurple,
    onSecondary = KindleNearBlack,
    secondaryContainer = KindlePurple.copy(alpha = 0.2f),
    onSecondaryContainer = KindlePurple,
    tertiary = KindleOrangeLight,
    onTertiary = KindleNearBlack,
    background = androidx.compose.ui.graphics.Color(0xFF1A1A1A),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
    surface = androidx.compose.ui.graphics.Color(0xFF2D2D2D),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2D2D2D),
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFE0E0E0).copy(alpha = 0.7f)
)

@Composable
fun AlexandriaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KindleTypography,
        content = content
    )
}
