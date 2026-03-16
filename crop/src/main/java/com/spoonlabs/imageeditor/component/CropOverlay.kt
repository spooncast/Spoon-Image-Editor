package com.spoonlabs.imageeditor.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

private val DIM_COLOR = Color.Black.copy(alpha = 0.5f)
private val GRID_COLOR = Color.White.copy(alpha = 0.7f)
private val BORDER_OUTLINE_COLOR = Color.Black.copy(alpha = 0.5f)
private const val GRID_STROKE_WIDTH = 1.5f
private const val BORDER_STROKE_WIDTH = 2.5f
private const val BORDER_OUTLINE_WIDTH = BORDER_STROKE_WIDTH + 2f

fun DrawScope.drawCropOverlay(cropRect: Rect) {
    if (cropRect.width <= 0f || cropRect.height <= 0f || cropRect == Rect.Zero) return
    if (size.width <= 0f || size.height <= 0f) return

    drawRect(
        color = DIM_COLOR,
        topLeft = Offset.Zero,
        size = Size(size.width, cropRect.top),
    )
    drawRect(
        color = DIM_COLOR,
        topLeft = Offset(0f, cropRect.bottom),
        size = Size(size.width, size.height - cropRect.bottom),
    )
    drawRect(
        color = DIM_COLOR,
        topLeft = Offset(0f, cropRect.top),
        size = Size(cropRect.left, cropRect.height),
    )
    drawRect(
        color = DIM_COLOR,
        topLeft = Offset(cropRect.right, cropRect.top),
        size = Size(size.width - cropRect.right, cropRect.height),
    )

    drawRect(
        color = BORDER_OUTLINE_COLOR,
        topLeft = cropRect.topLeft,
        size = cropRect.size,
        style = Stroke(width = BORDER_OUTLINE_WIDTH),
    )
    drawRect(
        color = Color.White,
        topLeft = cropRect.topLeft,
        size = cropRect.size,
        style = Stroke(width = BORDER_STROKE_WIDTH),
    )

    val thirdW = cropRect.width / 3f
    val thirdH = cropRect.height / 3f

    for (i in 1..2) {
        val x = cropRect.left + thirdW * i
        drawLine(
            color = GRID_COLOR,
            start = Offset(x, cropRect.top),
            end = Offset(x, cropRect.bottom),
            strokeWidth = GRID_STROKE_WIDTH,
        )
        val y = cropRect.top + thirdH * i
        drawLine(
            color = GRID_COLOR,
            start = Offset(cropRect.left, y),
            end = Offset(cropRect.right, y),
            strokeWidth = GRID_STROKE_WIDTH,
        )
    }
}
