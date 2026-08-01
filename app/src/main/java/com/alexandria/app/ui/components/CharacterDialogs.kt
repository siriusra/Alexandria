package com.alexandria.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CharacterEditDialog(
    title: String,
    initialName: String,
    initialIconType: String,
    initialIconKey: String,
    onDismiss: () -> Unit,
    onSave: (name: String, iconType: String, iconKey: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var iconType by remember { mutableStateOf(initialIconType) }
    var iconKey by remember { mutableStateOf(initialIconKey) }
    var showPicker by remember { mutableStateOf(false) }

    if (showPicker) {
        CharacterIconPickerDialog(
            initialIconType = iconType,
            initialIconKey = iconKey,
            onDismiss = { showPicker = false },
            onPick = { type, key ->
                iconType = type
                iconKey = key
                showPicker = false
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                            CharacterAvatar(
                                iconType = iconType,
                                iconKey = iconKey,
                                size = 48.dp,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(onClick = { showPicker = true }) {
                            Text("Cambiar icono")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onSave(name.trim(), iconType, iconKey) },
                    enabled = name.isNotBlank()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun CharacterSuggestionsDialog(
    candidates: List<String>,
    onDismiss: () -> Unit,
    onAdd: (List<Pair<String, String>>) -> Unit
) {
    var selectedNames by remember(candidates) { mutableStateOf(candidates.toSet()) }

    fun toggle(name: String) {
        val current = selectedNames
        selectedNames = if (name in current) current - name else current + name
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Personajes encontrados")
                Text(
                    text = "Selecciona los que quieras añadir",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            if (candidates.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se encontraron personajes automáticamente. Puedes añadirlos a mano.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(candidates.size) { index ->
                        val name = candidates[index]
                        val checked = name in selectedNames
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { toggle(name) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = checked,
                                onCheckedChange = null
                            )
                            Text(text = CharacterEmojis.defaultForName(name), fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAdd(selectedNames.map { it to CharacterEmojis.defaultForName(it) })
                    onDismiss()
                },
                enabled = selectedNames.isNotEmpty()
            ) {
                Text("Añadir (${selectedNames.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
