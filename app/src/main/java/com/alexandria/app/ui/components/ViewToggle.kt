package com.alexandria.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ViewToggle(
    isGridView: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = { onToggle(!isGridView) }) {
        Icon(
            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
            contentDescription = if (isGridView) "Vista lista" else "Vista cuadrícula"
        )
    }
}
