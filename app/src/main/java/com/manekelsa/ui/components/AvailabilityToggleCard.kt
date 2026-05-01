package com.manekelsa.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manekelsa.R

@Composable
fun AvailabilityToggleCard(
    isAvailable: Boolean,
    onAvailableChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isAvailable) Color(0xFF4CAF50) else Color(0xFF9E9E9E),
        label = "backgroundColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 100.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isAvailable) "✅ " else "❌ ",
                        fontSize = 24.sp
                    )
                    Text(
                        text = stringResource(R.string.available_today),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = stringResource(R.string.availability_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(start = 32.dp)
                )
            }

            Switch(
                checked = isAvailable,
                onCheckedChange = onAvailableChange,
                modifier = Modifier
                    .size(width = 64.dp, height = 48.dp), // Standard Switch size is fixed, but we can set constraints
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Color.Black.copy(alpha = 0.2f),
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = Color.Black.copy(alpha = 0.2f),
                    uncheckedBorderColor = Color.Transparent
                )
            )
        }
    }
}
