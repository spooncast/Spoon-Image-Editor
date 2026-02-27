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
    // Dim areas outside crop rect
    // Top
    drawRect(
        color = DIM_COLOR,
        topLeft = Offset.Zero,
        size = Size(size.width, cropRect.top),
    )
    // Bottom
    drawRect(
        color = DIM_COLOR,
        topLeft = Offset(0f, cropRect.bottom),
        size = Size(size.width, size.height - cropRect.bottom),
    )
    // Left
    drawRect(
        color = DIM_COLOR,
        topLeft = Offset(0f, cropRect.top),
        size = Size(cropRect.left, cropRect.height),
    )
    // Right
    drawRect(
        color = DIM_COLOR,
        topLeft = Offset(cropRect.right, cropRect.top),
        size = Size(size.width - cropRect.right, cropRect.height),
    )

    // Border — black outline behind white border for visibility on light backgrounds
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

    // Grid lines (rule of thirds)
    val thirdW = cropRect.width / 3f
    val thirdH = cropRect.height / 3f

    for (i in 1..2) {
        // Vertical lines
        val x = cropRect.left + thirdW * i
        drawLine(
            color = GRID_COLOR,
            start = Offset(x, cropRect.top),
            end = Offset(x, cropRect.bottom),
            strokeWidth = GRID_STROKE_WIDTH,
        )
        // Horizontal lines
        val y = cropRect.top + thirdH * i
        drawLine(
            color = GRID_COLOR,
            start = Offset(cropRect.left, y),
            end = Offset(cropRect.right, y),
            strokeWidth = GRID_STROKE_WIDTH,
        )
    }
}
