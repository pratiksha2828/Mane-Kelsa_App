package com.manekelsa.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.manekelsa.ui.model.SkillOption
import com.manekelsa.utils.TranslationUtils

@Composable
fun SkillChipLabel(skill: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val normalized = SkillOption.normalize(skill)
    val res = SkillOption.labelRes(normalized)
    val text = if (res != null) stringResource(res) else TranslationUtils.getTranslatedSkill(skill)
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color
    )
}
