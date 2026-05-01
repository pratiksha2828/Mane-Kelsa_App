package com.manekelsa.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun RatingBar(
    rating: Float,
    totalRatings: Int,
    modifier: Modifier = Modifier
) {
    val goldColor = Color(0xFFFFD700)
    val greyColor = Color.LightGray

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(5) { index ->
            val starIndex = index + 1
            val icon = when {
                rating >= starIndex -> Icons.Default.Star
                rating >= starIndex - 0.5f -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Default.StarBorder
            }
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (rating >= starIndex - 0.5f) goldColor else greyColor,
                modifier = Modifier.size(18.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = String.format(Locale.getDefault(), "%.1f (%d ratings total)", rating, totalRatings),
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
