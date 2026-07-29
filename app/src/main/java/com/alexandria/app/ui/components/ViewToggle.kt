package com.alexandria.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.alexandria.app.domain.model.VisualMode
import com.alexandria.app.ui.theme.LocalVisualMode

enum class ViewMode { GRID, LIST, CAROUSEL, SHELF }

@Composable
fun ViewToggle(
    viewMode: ViewMode,
    onModeSelected: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val visualMode = LocalVisualMode.current

    IconButton(onClick = {
        val modes = if (visualMode == VisualMode.IMMERSIVE) {
            listOf(ViewMode.GRID, ViewMode.LIST, ViewMode.CAROUSEL, ViewMode.SHELF)
        } else {
            listOf(ViewMode.GRID, ViewMode.LIST, ViewMode.CAROUSEL)
        }
        val currentIndex = modes.indexOf(viewMode)
        val nextIndex = (currentIndex + 1) % modes.size
        onModeSelected(modes[nextIndex])
    }) {
        Icon(
            imageVector = when (viewMode) {
                ViewMode.GRID -> Icons.Default.ViewList
                ViewMode.LIST -> Icons.Default.ViewCarousel
                ViewMode.CAROUSEL -> Icons.Default.GridView
                ViewMode.SHELF -> Icons.Default.MenuBook
            },
            contentDescription = when (viewMode) {
                ViewMode.GRID -> "Vista lista"
                ViewMode.LIST -> "Vista carrusel"
                ViewMode.CAROUSEL -> "Vista cuadrícula"
                ViewMode.SHELF -> "Vista estantería"
            }
        )
    }
}
