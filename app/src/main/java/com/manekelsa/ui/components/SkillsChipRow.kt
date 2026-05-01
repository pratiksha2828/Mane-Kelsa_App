package com.manekelsa.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SkillsChipRow(
    skills: List<String>,
    selectedSkills: Set<String>,
    onSkillSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val saffronColor = Color(0xFFFF9933)
    val lightGreyColor = Color(0xFFE0E0E0)

    WrapRow(
        modifier = modifier
            .fillMaxWidth(),
        horizontalSpacing = 8.dp,
        verticalSpacing = 4.dp
    ) {
        skills.forEach { skill ->
            val isSelected = selectedSkills.contains(skill)
            FilterChip(
                selected = isSelected,
                onClick = { onSkillSelected(skill) },
                label = {
                    Text(
                        text = skill,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = lightGreyColor,
                    selectedContainerColor = saffronColor,
                    labelColor = Color.Black,
                    selectedLabelColor = Color.White
                ),
                border = null
            )
        }
    }
}
