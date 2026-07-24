package com.alexandria.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class ViewMode { GRID, LIST, CAROUSEL }

@Composable
fun ViewToggle(
    viewMode: ViewMode,
    onModeSelected: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = {
        onModeSelected(
            when (viewMode) {
                ViewMode.GRID -> ViewMode.LIST
                ViewMode.LIST -> ViewMode.CAROUSEL
                ViewMode.CAROUSEL -> ViewMode.GRID
            }
        )
    }) {
        Icon(
            imageVector = when (viewMode) {
                ViewMode.GRID -> Icons.Default.ViewList
                ViewMode.LIST -> Icons.Default.ViewCarousel
                ViewMode.CAROUSEL -> Icons.Default.GridView
            },
            contentDescription = when (viewMode) {
                ViewMode.GRID -> "Vista lista"
                ViewMode.LIST -> "Vista carrusel"
                ViewMode.CAROUSEL -> "Vista cuadrícula"
            }
        )
    }
}
