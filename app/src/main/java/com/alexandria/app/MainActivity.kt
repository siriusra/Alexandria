package com.alexandria.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexandria.app.data.local.PreferencesManager
import com.alexandria.app.ui.navigation.MainNavGraph
import com.alexandria.app.ui.theme.AlexandriaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val crashLog = CrashHandler.getCrashLog(this)

        setContent {
            val isDarkTheme by preferencesManager.isDarkTheme.collectAsState(initial = false)
            val accentIndex by preferencesManager.accentColorIndex.collectAsState(initial = 0)

            AlexandriaTheme(
                darkTheme = isDarkTheme,
                accentIndex = accentIndex
            ) {
                if (crashLog != null) {
                    CrashDialog(
                        crashLog = crashLog,
                        onDismiss = { /* dialog auto-dismisses */ }
                    )
                }
                MainNavGraph()
            }
        }
    }
}

@Composable
private fun CrashDialog(
    crashLog: String,
    onDismiss: () -> Unit
) {
    var showDialog by remember { mutableStateOf(true) }
    val context = LocalContext.current

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false; onDismiss() },
            title = { Text("La app falló inesperadamente") },
            text = {
                Column {
                    Text(
                        text = "Ha ocurrido un error. Puedes copiar el reporte y enviármelo para ayudarme a arreglarlo:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                    ) {
                        Text(
                            text = crashLog,
                            modifier = Modifier
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState()),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Crash Report", crashLog)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copiado al portapapeles", Toast.LENGTH_SHORT).show()
                        showDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Copiar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        onDismiss()
                    }
                ) {
                    Text("Cerrar")
                }
            }
        )
    }
}
