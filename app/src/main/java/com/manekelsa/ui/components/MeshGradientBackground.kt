package com.manekelsa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun MeshGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val background = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFF3E0),
            Color(0xFFE3F2FD),
            Color(0xFFEDE7F6)
        )
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
    ) {
        content()
    }
}
