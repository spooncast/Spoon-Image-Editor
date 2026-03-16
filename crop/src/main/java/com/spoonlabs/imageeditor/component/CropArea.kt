package com.spoonlabs.imageeditor.component

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Stable
private data class TransformState(
    val gestureZoom: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
)

private const val MAX_GESTURE_ZOOM = 6f
private const val RECT_EPSILON = 0.5f

private fun Rect.approximatelyEquals(other: Rect): Boolean =
    abs(left - other.left) < RECT_EPSILON &&
        abs(top - other.top) < RECT_EPSILON &&
        abs(right - other.right) < RECT_EPSILON &&
        abs(bottom - other.bottom) < RECT_EPSILON

private fun computeEffectiveSize(
    bitmapWidth: Float,
    bitmapHeight: Float,
    rotationDegrees: Float,
): Pair<Float, Float> {
    if (bitmapWidth <= 0f || bitmapHeight <= 0f) return Pair(1f, 1f)
    val safeRotation = if (rotationDegrees.isFinite()) rotationDegrees else 0f
    val radians = Math.toRadians(safeRotation.toDouble())
    val cosA = abs(cos(radians)).toFloat()
    val sinA = abs(sin(radians)).toFloat()
    val w = bitmapWidth * cosA + bitmapHeight * sinA
    val h = bitmapWidth * sinA + bitmapHeight * cosA
    return Pair(
        if (w.isFinite() && w > 0f) w else 1f,
        if (h.isFinite() && h > 0f) h else 1f,
    )
}

@Composable
fun CropArea(
    bitmap: Bitmap,
    rotationDegrees: Float,
    aspectRatioX: Float?,
    aspectRatioY: Float?,
    brightness: Float = 0f,
    flipHorizontal: Boolean = false,
    flipVertical: Boolean = false,
    topInset: Float = 0f,
    bottomInset: Float = 0f,
    showCropOverlay: Boolean = true,
    fitImageWidth: Float = 0f,
    fitImageHeight: Float = 0f,
    modifier: Modifier = Modifier,
    onCropStateChanged: ((getCropRect: () -> RectF) -> Unit)? = null,
    onImageTap: (() -> Unit)? = null,
) {
    if (bitmap.isRecycled) return
    val bmpW: Float
    val bmpH: Float
    try {
        bmpW = bitmap.width.toFloat()
        bmpH = bitmap.height.toFloat()
    } catch (_: Throwable) {
        return // bitmap이 접근 중 recycle된 경우
    }
    if (bmpW <= 0f || bmpH <= 0f) return

    val imageBitmap = remember(bitmap) {
        try {
            bitmap.asImageBitmap()
        } catch (_: Throwable) {
            null
        }
    } ?: return

    val fitW = if (fitImageWidth > 0f) fitImageWidth else bmpW
    val fitH = if (fitImageHeight > 0f) fitImageHeight else bmpH

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var transform by remember { mutableStateOf(TransformState()) }

    var lastRotation by remember { mutableFloatStateOf(rotationDegrees) }
    var lastAspectKey by remember { mutableStateOf("${aspectRatioX}_${aspectRatioY}") }
    var initialized by remember { mutableStateOf(false) }
    var lastCropRect by remember { mutableStateOf(Rect.Zero) }

    val (effectiveWidth, effectiveHeight) = remember(rotationDegrees, bmpW, bmpH) {
        computeEffectiveSize(bmpW, bmpH, rotationDegrees)
    }

    val cropRect = remember(canvasSize, aspectRatioX, aspectRatioY, fitW, fitH, topInset, bottomInset) {
        if (canvasSize == IntSize.Zero) return@remember Rect.Zero
        calculateCropRect(
            canvasSize.toSize(),
            aspectRatioX,
            aspectRatioY,
            fitW,
            fitH,
            topInset.coerceAtLeast(0f),
            bottomInset.coerceAtLeast(0f),
        )
    }

    val baseScale = remember(cropRect, fitW, fitH) {
        if (cropRect == Rect.Zero || fitW <= 0f || fitH <= 0f) 1f
        else max(
            cropRect.width / fitW,
            cropRect.height / fitH,
        ).coerceIn(0.001f, 100f)
    }

    val renderScale = (baseScale * transform.gestureZoom).let {
        if (it.isFinite() && it > 0f) it else 1f
    }

    fun centerImage(zoom: Float = transform.gestureZoom) {
        val safeZoom = if (zoom.isFinite() && zoom > 0f) zoom else 1f
        val scale = baseScale * safeZoom
        val imgW = effectiveWidth * scale
        val imgH = effectiveHeight * scale
        transform = transform.copy(
            gestureZoom = safeZoom,
            offsetX = cropRect.center.x - imgW / 2f,
            offsetY = cropRect.center.y - imgH / 2f,
        )
    }

    if (!initialized && cropRect != Rect.Zero) {
        initialized = true
        centerImage()
    }

    if (initialized && cropRect != Rect.Zero && !cropRect.approximatelyEquals(lastCropRect)) {
        lastCropRect = cropRect
        val scale = baseScale * transform.gestureZoom
        val imgW = effectiveWidth * scale
        val imgH = effectiveHeight * scale
        transform = transform.copy(
            offsetX = clampOffset(transform.offsetX, imgW, cropRect.left, cropRect.right),
            offsetY = clampOffset(transform.offsetY, imgH, cropRect.top, cropRect.bottom),
        )
    }

    if (lastRotation != rotationDegrees) {
        lastRotation = rotationDegrees
        centerImage(zoom = transform.gestureZoom)
    }

    val currentAspectKey = "${aspectRatioX}_${aspectRatioY}"
    if (lastAspectKey != currentAspectKey && canvasSize != IntSize.Zero) {
        lastAspectKey = currentAspectKey
        centerImage(zoom = 1f)
    }

    val brightnessFilter = remember(brightness) {
        val safeBrightness = if (brightness.isFinite()) brightness else 0f
        if (safeBrightness == 0f) null
        else {
            val clampedBrightness = safeBrightness.coerceIn(-1f, 1f)
            val scale = 1f + clampedBrightness * 0.2f
            val offset = clampedBrightness * 80f
            val matrix = ColorMatrix().apply {
                set(0, 0, scale)
                set(1, 1, scale)
                set(2, 2, scale)
                set(0, 4, offset)
                set(1, 4, offset)
                set(2, 4, offset)
            }
            ColorFilter.colorMatrix(matrix)
        }
    }

    LaunchedEffect(
        cropRect,
        transform.offsetX,
        transform.offsetY,
        renderScale,
        rotationDegrees,
        flipHorizontal,
        flipVertical,
    ) {
        onCropStateChanged?.invoke {
            computeSourceCropRect(
                cropRect = cropRect,
                offsetX = transform.offsetX,
                offsetY = transform.offsetY,
                scale = renderScale,
                bitmapWidth = if (!bitmap.isRecycled) bitmap.width else bmpW.toInt(),
                bitmapHeight = if (!bitmap.isRecycled) bitmap.height else bmpH.toInt(),
                rotationDegrees = rotationDegrees,
                flipHorizontal = flipHorizontal,
                flipVertical = flipVertical,
            )
        }
    }

    Canvas(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { size -> canvasSize = size }
            .pointerInput(onImageTap) {
                detectTapGestures { onImageTap?.invoke() }
            }
            .pointerInput(cropRect, rotationDegrees) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    if (cropRect == Rect.Zero) return@detectTransformGestures

                    val currentTransform = transform

                    val curBaseScale = if (fitW <= 0f || fitH <= 0f) 1f
                    else max(cropRect.width / fitW, cropRect.height / fitH)
                        .let { if (it.isFinite() && it > 0f) it else 1f }

                    val (curEffW, curEffH) = computeEffectiveSize(bmpW, bmpH, rotationDegrees)

                    val oldScale = curBaseScale * currentTransform.gestureZoom
                    if (!oldScale.isFinite() || oldScale <= 0f) return@detectTransformGestures
                    val newGestureZoom = (currentTransform.gestureZoom * zoom).coerceIn(1f, MAX_GESTURE_ZOOM)
                    val newScale = curBaseScale * newGestureZoom

                    val ratio = newScale / oldScale
                    if (!ratio.isFinite()) return@detectTransformGestures

                    val newOffsetX = centroid.x - (centroid.x - currentTransform.offsetX) * ratio + pan.x
                    val newOffsetY = centroid.y - (centroid.y - currentTransform.offsetY) * ratio + pan.y

                    val imgW = curEffW * newScale
                    val imgH = curEffH * newScale

                    transform = TransformState(
                        gestureZoom = newGestureZoom,
                        offsetX = clampOffset(newOffsetX, imgW, cropRect.left, cropRect.right),
                        offsetY = clampOffset(newOffsetY, imgH, cropRect.top, cropRect.bottom),
                    )
                }
            }
    ) {
        val currentRenderScale = (baseScale * transform.gestureZoom).let {
            if (it.isFinite() && it > 0f) it else 1f
        }

        withTransform({
            translate(left = transform.offsetX, top = transform.offsetY)
            scale(currentRenderScale, currentRenderScale, pivot = Offset.Zero)
            rotate(
                degrees = rotationDegrees,
                pivot = Offset(effectiveWidth / 2f, effectiveHeight / 2f),
            )
            if (flipHorizontal || flipVertical) {
                scale(
                    scaleX = if (flipHorizontal) -1f else 1f,
                    scaleY = if (flipVertical) -1f else 1f,
                    pivot = Offset(effectiveWidth / 2f, effectiveHeight / 2f),
                )
            }
        }) {
            if (!bitmap.isRecycled) {
                try {
                    val imgOffsetX = (effectiveWidth - bmpW) / 2f
                    val imgOffsetY = (effectiveHeight - bmpH) / 2f
                    drawImage(
                        image = imageBitmap,
                        topLeft = Offset(imgOffsetX, imgOffsetY),
                        colorFilter = brightnessFilter,
                    )
                } catch (_: Throwable) {
                    // bitmap이 isRecycled 체크와 draw 사이에 recycle될 수 있음 (race condition)
                }
            }
        }

        if (showCropOverlay) {
            drawCropOverlay(cropRect)
        }
    }
}

private fun calculateCropRect(
    canvasSize: Size,
    aspectRatioX: Float?,
    aspectRatioY: Float?,
    imageWidth: Float,
    imageHeight: Float,
    topInset: Float = 0f,
    bottomInset: Float = 0f,
): Rect {
    if (canvasSize.width <= 0f || canvasSize.height <= 0f) return Rect.Zero

    val paddingH = 16f
    val paddingTop = 16f + topInset.coerceAtLeast(0f)
    val paddingBottom = 16f + bottomInset.coerceAtLeast(0f)
    val availableWidth = (canvasSize.width - paddingH * 2).coerceAtLeast(1f)
    val availableHeight = (canvasSize.height - paddingTop - paddingBottom).coerceAtLeast(1f)

    val cropWidth: Float
    val cropHeight: Float

    if (aspectRatioX != null && aspectRatioY != null && aspectRatioY > 0f && aspectRatioX > 0f) {
        val ratio = aspectRatioX / aspectRatioY
        if (!ratio.isFinite() || ratio <= 0f) {
            cropWidth = availableWidth
            cropHeight = availableHeight
        } else if (availableWidth / availableHeight > ratio) {
            cropHeight = availableHeight
            cropWidth = cropHeight * ratio
        } else {
            cropWidth = availableWidth
            cropHeight = cropWidth / ratio
        }
    } else {
        val safeImageWidth = if (imageWidth > 0f) imageWidth else 1f
        val safeImageHeight = if (imageHeight > 0f) imageHeight else 1f
        val fitScale = min(
            availableWidth / safeImageWidth,
            availableHeight / safeImageHeight,
        ).let { if (it.isFinite() && it > 0f) it else 1f }
        cropWidth = safeImageWidth * fitScale
        cropHeight = safeImageHeight * fitScale
    }

    val left = (canvasSize.width - cropWidth) / 2f
    val top = paddingTop + (availableHeight - cropHeight) / 2f
    return Rect(left, top, left + cropWidth, top + cropHeight)
}

private fun clampOffset(
    offset: Float,
    imageSize: Float,
    cropStart: Float,
    cropEnd: Float,
): Float {
    if (!offset.isFinite()) return cropStart
    val cropSize = cropEnd - cropStart
    if (!imageSize.isFinite() || imageSize <= 0f) return cropStart
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
    flipHorizontal: Boolean = false,
    flipVertical: Boolean = false,
): RectF {
    val bmpW = bitmapWidth.toFloat()
    val bmpH = bitmapHeight.toFloat()
    if (bmpW <= 0f || bmpH <= 0f) return RectF(0f, 0f, bmpW.coerceAtLeast(1f), bmpH.coerceAtLeast(1f))
    if (!scale.isFinite() || scale <= 0f) return RectF(0f, 0f, bmpW, bmpH)

    val radians = Math.toRadians(rotationDegrees.toDouble())
    val cosA = abs(cos(radians)).toFloat()
    val sinA = abs(sin(radians)).toFloat()
    val effectiveWidth = (bmpW * cosA + bmpH * sinA).let { if (it.isFinite() && it > 0f) it else 1f }
    val effectiveHeight = (bmpW * sinA + bmpH * cosA).let { if (it.isFinite() && it > 0f) it else 1f }

    val effLeft = (cropRect.left - offsetX) / scale
    val effTop = (cropRect.top - offsetY) / scale
    val effRight = (cropRect.right - offsetX) / scale
    val effBottom = (cropRect.bottom - offsetY) / scale

    if (!flipHorizontal && !flipVertical) {
        return RectF(
            effLeft.coerceIn(0f, effectiveWidth),
            effTop.coerceIn(0f, effectiveHeight),
            effRight.coerceIn(0f, effectiveWidth),
            effBottom.coerceIn(0f, effectiveHeight),
        )
    }

    val cx = effectiveWidth / 2f
    val cy = effectiveHeight / 2f
    val cosR = cos(radians).toFloat()
    val sinR = sin(radians).toFloat()

    fun transformPoint(x: Float, y: Float): Pair<Float, Float> {
        var px = x - cx
        var py = y - cy
        var tx = px * cosR - py * sinR
        var ty = px * sinR + py * cosR
        if (flipHorizontal) tx = -tx
        if (flipVertical) ty = -ty
        px = tx * cosR + ty * sinR
        py = -tx * sinR + ty * cosR
        return Pair(px + cx, py + cy)
    }

    val (x1, y1) = transformPoint(effLeft, effTop)
    val (x2, y2) = transformPoint(effRight, effTop)
    val (x3, y3) = transformPoint(effLeft, effBottom)
    val (x4, y4) = transformPoint(effRight, effBottom)

    return RectF(
        minOf(x1, x2, x3, x4).coerceIn(0f, effectiveWidth),
        minOf(y1, y2, y3, y4).coerceIn(0f, effectiveHeight),
        maxOf(x1, x2, x3, x4).coerceIn(0f, effectiveWidth),
        maxOf(y1, y2, y3, y4).coerceIn(0f, effectiveHeight),
    )
}
