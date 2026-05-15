package com.manekelsa.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.glassmorphism(
    cornerRadius: Dp,
    fillAlpha: Float = 0.2f,
    borderAlpha: Float = 0.3f
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    return this
        .clip(shape)
        .background(Color.White.copy(alpha = fillAlpha))
        .border(BorderStroke(1.dp, Color.White.copy(alpha = borderAlpha)), shape)
}
