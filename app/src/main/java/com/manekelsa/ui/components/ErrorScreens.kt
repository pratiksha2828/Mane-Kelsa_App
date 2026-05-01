package com.manekelsa.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun NetworkErrorScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErrorStateTemplate(
        icon = Icons.Default.CloudOff,
        title = "No Internet Connection",
        description = "Please check your network settings and try again.",
        modifier = modifier,
        actionButton = {
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    )
}

@Composable
fun EmptyStateScreen(
    modifier: Modifier = Modifier,
    title: String = "No Workers Found",
    description: String = "Try adjusting your filters or search terms."
) {
    ErrorStateTemplate(
        icon = Icons.Default.SearchOff,
        title = title,
        description = description,
        modifier = modifier
    )
}

@Composable
fun ServerErrorScreen(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErrorStateTemplate(
        icon = Icons.Default.ErrorOutline,
        title = "Something Went Wrong",
        description = "We're having trouble connecting to our servers. Please try again later.",
        modifier = modifier,
        actionButton = {
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    )
}

@Composable
private fun ErrorStateTemplate(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionButton: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        if (actionButton != null) {
            Spacer(modifier = Modifier.height(24.dp))
            actionButton()
        }
    }
}
