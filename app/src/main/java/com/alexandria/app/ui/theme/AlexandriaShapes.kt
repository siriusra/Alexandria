package com.alexandria.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

data class AlexandriaShapes(
    val cardShape: RoundedCornerShape = RoundedCornerShape(12.dp),
    val cardShapeSmall: RoundedCornerShape = RoundedCornerShape(8.dp),
    val dialogShape: RoundedCornerShape = RoundedCornerShape(16.dp),
    val chipShape: RoundedCornerShape = RoundedCornerShape(8.dp),
    val buttonShape: RoundedCornerShape = RoundedCornerShape(12.dp),
    val largeShape: RoundedCornerShape = RoundedCornerShape(20.dp),
    val shelfCardShape: RoundedCornerShape = RoundedCornerShape(4.dp),
    val tabShape: RoundedCornerShape = RoundedCornerShape(12.dp)
) {
    companion object {
        val Classic = AlexandriaShapes()

        val Immersive = AlexandriaShapes(
            cardShape = RoundedCornerShape(16.dp),
            cardShapeSmall = RoundedCornerShape(12.dp),
            dialogShape = RoundedCornerShape(20.dp),
            chipShape = RoundedCornerShape(10.dp),
            buttonShape = RoundedCornerShape(14.dp),
            largeShape = RoundedCornerShape(24.dp),
            shelfCardShape = RoundedCornerShape(2.dp),
            tabShape = RoundedCornerShape(14.dp)
        )
    }
}

val LocalAlexandriaShapes = staticCompositionLocalOf { AlexandriaShapes.Classic }
