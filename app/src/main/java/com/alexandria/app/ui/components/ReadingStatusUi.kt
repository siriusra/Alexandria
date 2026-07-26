package com.alexandria.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.alexandria.app.domain.model.ReadingStatus

data class StatusUiConfig(
    val color: Color,
    val icon: ImageVector
)

fun ReadingStatus.uiConfig(): StatusUiConfig = when (this) {
    ReadingStatus.QUIERO_LEER -> StatusUiConfig(Color(0xFFFF9800), Icons.Default.Bookmark)
    ReadingStatus.LEYENDO -> StatusUiConfig(Color(0xFF4CAF50), Icons.Default.MenuBook)
    ReadingStatus.PAUSADO -> StatusUiConfig(Color(0xFFFFA000), Icons.Default.Pause)
    ReadingStatus.RELEYENDO -> StatusUiConfig(Color(0xFF9C27B0), Icons.Default.Refresh)
    ReadingStatus.ABANDONADO -> StatusUiConfig(Color(0xFF757575), Icons.Default.Cancel)
    ReadingStatus.TERMINADO -> StatusUiConfig(Color(0xFF2196F3), Icons.Default.CheckCircle)
    ReadingStatus.FAVORITOS -> StatusUiConfig(Color(0xFFE91E63), Icons.Default.Favorite)
}
