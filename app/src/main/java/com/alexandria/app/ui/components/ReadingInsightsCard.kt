package com.alexandria.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ReadingInsights(
    val totalBooks: Int = 0,
    val totalPagesRead: Int = 0,
    val averageRating: Float = 0f,
    val topGenre: String? = null,
    val booksThisMonth: Int = 0
)

@Composable
fun ReadingInsightsCard(
    insights: ReadingInsights,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Estadísticas de lectura",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                InsightStat(
                    value = insights.booksThisMonth.toString(),
                    label = "Este mes"
                )
                InsightStat(
                    value = "${insights.totalPagesRead}",
                    label = "Páginas"
                )
                InsightStat(
                    value = "%.1f".format(insights.averageRating),
                    label = "Media ★"
                )
                InsightStat(
                    value = insights.totalBooks.toString(),
                    label = "Total"
                )
            }

            if (insights.topGenre != null) {
                Text(
                    text = "Género favorito: ${insights.topGenre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InsightStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
