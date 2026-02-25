package com.spoonlabs.crop.component

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun CropArea(
    bitmap: Bitmap,
    rotationDegrees: Float,
    aspectRatioX: Float?,
    aspectRatioY: Float?,
    externalScale: Float = 1f,
    modifier: Modifier = Modifier,
    onCropStateChanged: ((getCropRect: () -> RectF) -> Unit)? = null,
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // gestureZoom: pinch-to-zoom에 의한 추가 배율 (1f = 추가 확대 없음)
    var gestureZoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Track previous values for change detection
    var lastRotation by remember { mutableFloatStateOf(rotationDegrees) }
    var lastAspectKey by remember { mutableStateOf("${aspectRatioX}_${aspectRatioY}") }

    // Effective image dimensions after rotation (supports arbitrary angles)
    val radians = Math.toRadians(rotationDegrees.toDouble())
    val cosA = abs(cos(radians)).toFloat()
    val sinA = abs(sin(radians)).toFloat()
    val effectiveWidth = bitmap.width * cosA + bitmap.height * sinA
    val effectiveHeight = bitmap.width * sinA + bitmap.height * cosA

    // Calculate crop rect based on canvas size and aspect ratio
    val cropRect = remember(canvasSize, aspectRatioX, aspectRatioY) {
        if (canvasSize == IntSize.Zero) return@remember Rect.Zero
        calculateCropRect(canvasSize.toSize(), aspectRatioX, aspectRatioY)
    }

    // Base scale: image just fills the crop rect
    val baseScale = remember(cropRect, effectiveWidth, effectiveHeight) {
        if (cropRect == Rect.Zero || effectiveWidth <= 0f || effectiveHeight <= 0f) 1f
        else {
            val scaleToFillWidth = cropRect.width / effectiveWidth
            val scaleToFillHeight = cropRect.height / effectiveHeight
            max(scaleToFillWidth, scaleToFillHeight)
        }
    }

    // Actual rendered scale = baseScale * externalScale(슬라이더) * gestureZoom(핀치)
    val renderScale = baseScale * externalScale * gestureZoom

    // Re-fit when rotation changes
    if (lastRotation != rotationDegrees) {
        lastRotation = rotationDegrees
        gestureZoom = 1f
        offsetX = cropRect.center.x - effectiveWidth * renderScale / 2f
        offsetY = cropRect.center.y - effectiveHeight * renderScale / 2f
    }

    // Re-fit when aspect ratio changes
    val currentAspectKey = "${aspectRatioX}_${aspectRatioY}"
    if (lastAspectKey != currentAspectKey && canvasSize != IntSize.Zero) {
        lastAspectKey = currentAspectKey
        gestureZoom = 1f
        offsetX = cropRect.center.x - effectiveWidth * renderScale / 2f
        offsetY = cropRect.center.y - effectiveHeight * renderScale / 2f
    }

    // Re-clamp when external scale changes
    LaunchedEffect(externalScale) {
        if (canvasSize != IntSize.Zero) {
            val currentRenderScale = baseScale * externalScale * gestureZoom
            val imgW = effectiveWidth * currentRenderScale
            val imgH = effectiveHeight * currentRenderScale
            offsetX = clampOffset(offsetX, imgW, cropRect.left, cropRect.right)
            offsetY = clampOffset(offsetY, imgH, cropRect.top, cropRect.bottom)
        }
    }

    // Provide crop rect calculator to parent
    remember(cropRect, renderScale, rotationDegrees) {
        onCropStateChanged?.invoke {
            computeSourceCropRect(
                cropRect = cropRect,
                offsetX = offsetX,
                offsetY = offsetY,
                scale = renderScale,
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height,
                rotationDegrees = rotationDegrees,
            )
        }
    }

    Canvas(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { size ->
                canvasSize = size
                val cRect = calculateCropRect(size.toSize(), aspectRatioX, aspectRatioY)
                val initScale = baseScale * externalScale * gestureZoom
                if (gestureZoom == 1f) {
                    offsetX = cRect.center.x - effectiveWidth * initScale / 2f
                    offsetY = cRect.center.y - effectiveHeight * initScale / 2f
                }
            }
            .pointerInput(baseScale, externalScale, cropRect) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newGestureZoom = (gestureZoom * zoom).coerceIn(1f, 10f)
                    gestureZoom = newGestureZoom

                    val currentScale = baseScale * externalScale * newGestureZoom
                    val newOffsetX = offsetX + pan.x
                    val newOffsetY = offsetY + pan.y

                    val imgW = effectiveWidth * currentScale
                    val imgH = effectiveHeight * currentScale
                    offsetX = clampOffset(newOffsetX, imgW, cropRect.left, cropRect.right)
                    offsetY = clampOffset(newOffsetY, imgH, cropRect.top, cropRect.bottom)
                }
            }
    ) {
        val currentRenderScale = baseScale * externalScale * gestureZoom

        withTransform({
            translate(left = offsetX, top = offsetY)
            scale(currentRenderScale, currentRenderScale, pivot = Offset.Zero)
            rotate(
                degrees = rotationDegrees,
                pivot = Offset(effectiveWidth / 2f, effectiveHeight / 2f),
            )
        }) {
            val imgOffsetX = (effectiveWidth - bitmap.width) / 2f
            val imgOffsetY = (effectiveHeight - bitmap.height) / 2f
            drawImage(
                image = imageBitmap,
                topLeft = Offset(imgOffsetX, imgOffsetY),
            )
        }

        drawCropOverlay(cropRect)
    }
}

private fun calculateCropRect(
    canvasSize: Size,
    aspectRatioX: Float?,
    aspectRatioY: Float?,
): Rect {
    val padding = 16f
    val availableWidth = canvasSize.width - padding * 2
    val availableHeight = canvasSize.height - padding * 2

    val cropWidth: Float
    val cropHeight: Float

    if (aspectRatioX != null && aspectRatioY != null && aspectRatioY > 0f) {
        val ratio = aspectRatioX / aspectRatioY
        if (availableWidth / availableHeight > ratio) {
            cropHeight = availableHeight
            cropWidth = cropHeight * ratio
        } else {
            cropWidth = availableWidth
            cropHeight = cropWidth / ratio
        }
    } else {
        cropWidth = availableWidth
        cropHeight = availableHeight
    }

    val left = (canvasSize.width - cropWidth) / 2f
    val top = (canvasSize.height - cropHeight) / 2f
    return Rect(left, top, left + cropWidth, top + cropHeight)
}

private fun clampOffset(
    offset: Float,
    imageSize: Float,
    cropStart: Float,
    cropEnd: Float,
): Float {
    val cropSize = cropEnd - cropStart
    return if (imageSize <= cropSize) {
        cropStart + (cropSize - imageSize) / 2f
    } else {
        val maxOffset = cropStart
        val minOffset = cropEnd - imageSize
        offset.coerceIn(minOffset, maxOffset)
    }
}

private fun computeSourceCropRect(
    cropRect: Rect,
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    bitmapWidth: Int,
    bitmapHeight: Int,
    rotationDegrees: Float,
): RectF {
    val radians = Math.toRadians(rotationDegrees.toDouble())
    val cosA = abs(cos(radians)).toFloat()
    val sinA = abs(sin(radians)).toFloat()
    val effectiveWidth = bitmapWidth * cosA + bitmapHeight * sinA
    val effectiveHeight = bitmapWidth * sinA + bitmapHeight * cosA

    val srcLeft = (cropRect.left - offsetX) / scale
    val srcTop = (cropRect.top - offsetY) / scale
    val srcRight = (cropRect.right - offsetX) / scale
    val srcBottom = (cropRect.bottom - offsetY) / scale

    return RectF(
        srcLeft.coerceIn(0f, effectiveWidth),
        srcTop.coerceIn(0f, effectiveHeight),
        srcRight.coerceIn(0f, effectiveWidth),
        srcBottom.coerceIn(0f, effectiveHeight),
    )
}
