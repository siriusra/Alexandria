package com.alexandria.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import com.alexandria.app.domain.model.VisualMode

@Composable
fun VignetteOverlay(visualMode: VisualMode) {
    if (visualMode != VisualMode.IMMERSIVE) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = ShaderBrush(
                RadialGradientShader(
                    center = Offset(size.width / 2f, size.height / 3f),
                    radius = size.height * 0.8f,
                    colors = listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.25f)
                    ),
                    colorStops = listOf(0.5f, 0.8f, 1.0f)
                )
            ),
            size = size
        )
    }
}
