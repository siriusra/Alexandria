package com.alexandria.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlaceholderPortada(
    titulo: String,
    autor: String?,
    modifier: Modifier = Modifier
) {
    val bgColor = colorFromTitle(titulo)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = titulo.take(1).uppercase(),
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = titulo,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (!autor.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = autor,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun colorFromTitle(title: String): Color {
    val colors = listOf(
        Color(0xFF1B5E20), Color(0xFF0D47A1), Color(0xFF4A148C), Color(0xFFBF360C),
        Color(0xFF006064), Color(0xFF880E4F), Color(0xFF1A237E), Color(0xFF33691E),
        Color(0xFFE65100), Color(0xFF01579B), Color(0xFF311B92), Color(0xFF827717),
        Color(0xFF004D40), Color(0xFFAD1457), Color(0xFF283593), Color(0xFF558B2F)
    )
    val hash = title.hashCode().let { if (it < 0) -it else it }
    return colors[hash % colors.size]
}
