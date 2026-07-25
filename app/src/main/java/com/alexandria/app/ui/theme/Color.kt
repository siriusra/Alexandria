package com.alexandria.app.ui.theme

import androidx.compose.ui.graphics.Color

val KindleTeal = Color(0xFF007185)
val KindleDark = Color(0xFF232F3E)
val KindleOrange = Color(0xFFFF9900)
val KindleLightGray = Color(0xFFF5F5F5)
val KindleNearBlack = Color(0xFF111111)

val KindlePurple = Color(0xFFBB86FC)
val KindleLightTeal = Color(0xFF4DD0E1)
val KindleOrangeLight = Color(0xFFFFAB40)

val StatusReading = Color(0xFF4CAF50)
val StatusFinished = Color(0xFF2196F3)
val StatusPending = Color(0xFFFF9800)

val BookShadow = Color(0x1A000000)

val AccentTeal = Color(0xFF007185)
val AccentViolet = Color(0xFF7B1FA2)
val AccentBlue = Color(0xFF1565C0)
val AccentGreen = Color(0xFF2E7D32)
val AccentOrange = Color(0xFFE65100)
val AccentPink = Color(0xFFC2185B)

val AccentTealDark = Color(0xFF4DD0E1)
val AccentVioletDark = Color(0xFFCE93D8)
val AccentBlueDark = Color(0xFF90CAF9)
val AccentGreenDark = Color(0xFFA5D6A7)
val AccentOrangeDark = Color(0xFFFFAB40)
val AccentPinkDark = Color(0xFFF48FB1)

data class AccentColorOption(
    val name: String,
    val light: Color,
    val dark: Color
)

val accentColors = listOf(
    AccentColorOption("Teal", AccentTeal, AccentTealDark),
    AccentColorOption("Violeta", AccentViolet, AccentVioletDark),
    AccentColorOption("Azul", AccentBlue, AccentBlueDark),
    AccentColorOption("Verde", AccentGreen, AccentGreenDark),
    AccentColorOption("Naranja", AccentOrange, AccentOrangeDark),
    AccentColorOption("Rosa", AccentPink, AccentPinkDark)
)
