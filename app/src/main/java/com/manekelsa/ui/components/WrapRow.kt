package com.manekelsa.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun WrapRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 0.dp,
    verticalSpacing: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val hSpacePx = horizontalSpacing.roundToPx()
        val vSpacePx = verticalSpacing.roundToPx()

        val placeables = measurables.map { measurable ->
            measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }

        val availableWidth = if (constraints.maxWidth == Constraints.Infinity) {
            placeables.fold(0) { acc, placeable -> acc + placeable.width + hSpacePx }
                .let { if (it > 0) it - hSpacePx else 0 }
        } else {
            constraints.maxWidth
        }

        var x = 0
        var y = 0
        var rowHeight = 0
        val positions = ArrayList<Triple<Int, Int, androidx.compose.ui.layout.Placeable>>(placeables.size)

        placeables.forEach { placeable ->
            if (x > 0 && x + placeable.width > availableWidth) {
                x = 0
                y += rowHeight + vSpacePx
                rowHeight = 0
            }

            positions.add(Triple(x, y, placeable))
            x += placeable.width + hSpacePx
            rowHeight = max(rowHeight, placeable.height)
        }

        val layoutWidth = availableWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeight = (y + rowHeight).coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(layoutWidth, layoutHeight) {
            positions.forEach { (px, py, placeable) ->
                placeable.placeRelative(px, py)
            }
        }
    }
}
