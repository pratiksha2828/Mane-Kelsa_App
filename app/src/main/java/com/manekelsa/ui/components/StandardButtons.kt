package com.manekelsa.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manekelsa.ui.theme.LocalHighContrastMode

@Composable
fun LargeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    colors: ButtonColors? = null
) {
    val isHighContrast = LocalHighContrastMode.current
    
    val defaultColors = if (isHighContrast) {
        ButtonDefaults.buttonColors(
            containerColor = Color(0xFF004D00), // Very dark green
            contentColor = Color.White
        )
    } else {
        ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF9933), // Saffron
            contentColor = Color.White
        )
    }

    Button(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .widthIn(min = 160.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = enabled && !isLoading,
        colors = colors ?: defaultColors,
        border = if (isHighContrast) BorderStroke(3.dp, Color.Black) else null
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = (colors ?: defaultColors).contentColor,
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = text,
                fontSize = if (isHighContrast) 20.sp else 18.sp,
                fontWeight = if (isHighContrast) androidx.compose.ui.text.font.FontWeight.Bold else null
            )
        }
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val isHighContrast = LocalHighContrastMode.current
    
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(56.dp)
            .widthIn(min = 160.dp),
        shape = RoundedCornerShape(16.dp),
        enabled = enabled,
        border = BorderStroke(
            width = if (isHighContrast) 3.dp else 1.dp,
            color = if (isHighContrast) Color.Black else if (enabled) Color.Gray else Color.Gray.copy(alpha = 0.38f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isHighContrast) Color.Black else if (enabled) Color.Gray else Color.Gray.copy(alpha = 0.38f)
        )
    ) {
        Text(
            text = text,
            fontSize = if (isHighContrast) 20.sp else 18.sp,
            fontWeight = if (isHighContrast) androidx.compose.ui.text.font.FontWeight.Bold else null
        )
    }
}
