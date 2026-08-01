package com.alexandria.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed interface PickerItem {
    val name: String

    data class EmojiItem(val emoji: NamedEmoji) : PickerItem {
        override val name get() = emoji.name
    }

    data class IconItem(val icon: GameIcon) : PickerItem {
        override val name get() = icon.label
    }
}

@Composable
fun CharacterIconPickerDialog(
    initialIconType: String = ICON_TYPE_EMOJI,
    initialIconKey: String = "⭐",
    onDismiss: () -> Unit,
    onPick: (iconType: String, iconKey: String) -> Unit
) {
    var mode by remember { mutableIntStateOf(0) }
    var emojiCat by remember { mutableIntStateOf(0) }
    var iconCat by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }

    val emojiCategories = CharacterEmojis.categories
    val iconCategories = CharacterIcons.categoryKeys

    val items: List<PickerItem> = when {
        query.isNotBlank() -> {
            CharacterEmojis.search(query).map { PickerItem.EmojiItem(it) } +
                CharacterIcons.search(query).map { PickerItem.IconItem(it) }
        }
        mode == 0 -> emojiCategories.getOrNull(emojiCat)?.items?.map { PickerItem.EmojiItem(it) }.orEmpty()
        else -> iconCategories.getOrNull(iconCat)
            ?.let { c -> CharacterIcons.forCategory(c).map { PickerItem.IconItem(it) } }
            .orEmpty()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Elegir icono")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Buscar (ej. detective, mago, mujer…)") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (query.isNotBlank()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                            }
                        }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        text = {
            Column {
                TabRow(selectedTabIndex = mode) {
                    Tab(selected = mode == 0, onClick = { mode = 0 }, text = { Text("Emojis") })
                    Tab(selected = mode == 1, onClick = { mode = 1 }, text = { Text("Iconos") })
                }

                if (query.isBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (mode == 0) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(emojiCategories.size) { index ->
                                FilterChip(
                                    selected = index == emojiCat,
                                    onClick = { emojiCat = index },
                                    label = { Text(emojiCategories[index].label) }
                                )
                            }
                        }
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(iconCategories.size) { index ->
                                val catKey = iconCategories[index]
                                FilterChip(
                                    selected = index == iconCat,
                                    onClick = { iconCat = index },
                                    label = { Text(CharacterIcons.categoryNames[catKey] ?: catKey) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sin resultados", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.fillMaxWidth().height(300.dp)
                    ) {
                        items(items.size) { index ->
                            val item = items[index]
                            val selected = when (item) {
                                is PickerItem.EmojiItem ->
                                    initialIconType == ICON_TYPE_EMOJI && initialIconKey == item.emoji.emoji
                                is PickerItem.IconItem ->
                                    initialIconType == ICON_TYPE_ICON && initialIconKey == item.icon.key
                            }
                            PickerCell(item = item, selected = selected) {
                                when (item) {
                                    is PickerItem.EmojiItem -> onPick(ICON_TYPE_EMOJI, item.emoji.emoji)
                                    is PickerItem.IconItem -> onPick(ICON_TYPE_ICON, item.icon.key)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun PickerCell(item: PickerItem, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (item) {
                is PickerItem.EmojiItem -> {
                    Text(text = item.emoji.emoji, fontSize = 26.sp)
                }
                is PickerItem.IconItem -> {
                    CharacterAvatar(
                        iconType = ICON_TYPE_ICON,
                        iconKey = item.icon.key,
                        size = 40.dp,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
            }
        }
    }
}
