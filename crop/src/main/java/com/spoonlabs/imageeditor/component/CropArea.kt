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
    val radians = Math.toRadians(rotationDegrees.toDouble())
    val cosA = abs(cos(radians)).toFloat()
    val sinA = abs(sin(radians)).toFloat()
    return Pair(
        bitmapWidth * cosA + bitmapHeight * sinA,
        bitmapWidth * sinA + bitmapHeight * cosA,
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
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
    val bmpW = bitmap.width.toFloat()
    val bmpH = bitmap.height.toFloat()
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
            topInset,
            bottomInset,
        )
    }

    val baseScale = remember(cropRect, fitW, fitH) {
        if (cropRect == Rect.Zero || fitW <= 0f || fitH <= 0f) 1f
        else max(
            cropRect.width / fitW,
            cropRect.height / fitH,
        )
    }

    val renderScale = baseScale * transform.gestureZoom

    fun centerImage(zoom: Float = transform.gestureZoom) {
        val scale = baseScale * zoom
        val imgW = effectiveWidth * scale
        val imgH = effectiveHeight * scale
        transform = transform.copy(
            gestureZoom = zoom,
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
        if (brightness == 0f) null
        else {
            val scale = 1f + brightness * 0.2f
            val offset = brightness * 80f
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
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height,
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

                    val (curEffW, curEffH) = computeEffectiveSize(bmpW, bmpH, rotationDegrees)

                    val oldScale = curBaseScale * currentTransform.gestureZoom
                    val newGestureZoom = (currentTransform.gestureZoom * zoom).coerceIn(1f, MAX_GESTURE_ZOOM)
                    val newScale = curBaseScale * newGestureZoom

                    val newOffsetX = centroid.x - (centroid.x - currentTransform.offsetX) * (newScale / oldScale) + pan.x
                    val newOffsetY = centroid.y - (centroid.y - currentTransform.offsetY) * (newScale / oldScale) + pan.y

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
        val currentRenderScale = baseScale * transform.gestureZoom

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
            val imgOffsetX = (effectiveWidth - bitmap.width) / 2f
            val imgOffsetY = (effectiveHeight - bitmap.height) / 2f
            drawImage(
                image = imageBitmap,
                topLeft = Offset(imgOffsetX, imgOffsetY),
                colorFilter = brightnessFilter,
            )
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
    val paddingH = 16f
    val paddingTop = 16f + topInset
    val paddingBottom = 16f + bottomInset
    val availableWidth = canvasSize.width - paddingH * 2
    val availableHeight = canvasSize.height - paddingTop - paddingBottom

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
        val fitScale = min(
            availableWidth / imageWidth,
            availableHeight / imageHeight,
        )
        cropWidth = imageWidth * fitScale
        cropHeight = imageHeight * fitScale
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
    flipHorizontal: Boolean = false,
    flipVertical: Boolean = false,
): RectF {
    val bmpW = bitmapWidth.toFloat()
    val bmpH = bitmapHeight.toFloat()
    val radians = Math.toRadians(rotationDegrees.toDouble())
    val cosA = abs(cos(radians)).toFloat()
    val sinA = abs(sin(radians)).toFloat()
    val effectiveWidth = bmpW * cosA + bmpH * sinA
    val effectiveHeight = bmpW * sinA + bmpH * cosA

    // Screen crop rect → effective (rotated+flipped) space
    var effLeft = (cropRect.left - offsetX) / scale
    var effTop = (cropRect.top - offsetY) / scale
    var effRight = (cropRect.right - offsetX) / scale
    var effBottom = (cropRect.bottom - offsetY) / scale

    // Undo flip to get rotated-only coordinates (cropAndSave applies flip after crop)
    if (flipHorizontal) {
        val tmpLeft = effectiveWidth - effRight
        val tmpRight = effectiveWidth - effLeft
        effLeft = tmpLeft
        effRight = tmpRight
    }
    if (flipVertical) {
        val tmpTop = effectiveHeight - effBottom
        val tmpBottom = effectiveHeight - effTop
        effTop = tmpTop
        effBottom = tmpBottom
    }

    return RectF(
        effLeft.coerceIn(0f, effectiveWidth),
        effTop.coerceIn(0f, effectiveHeight),
        effRight.coerceIn(0f, effectiveWidth),
        effBottom.coerceIn(0f, effectiveHeight),
    )
}
