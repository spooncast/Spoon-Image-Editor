package com.spoonlabs.crop.component

import android.graphics.Bitmap
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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
    modifier: Modifier = Modifier,
    onCropStateChanged: ((getCropRect: () -> RectF) -> Unit)? = null,
    onImageTap: (() -> Unit)? = null,
) {
    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    var gestureZoom by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var lastRotation by remember { mutableFloatStateOf(rotationDegrees) }
    var lastAspectKey by remember { mutableStateOf("${aspectRatioX}_${aspectRatioY}") }
    var initialized by remember { mutableStateOf(false) }

    // 회전된 이미지의 바운딩 박스 크기
    val radians = Math.toRadians(rotationDegrees.toDouble())
    val cosA = abs(cos(radians)).toFloat()
    val sinA = abs(sin(radians)).toFloat()
    val effectiveWidth = bitmap.width * cosA + bitmap.height * sinA
    val effectiveHeight = bitmap.width * sinA + bitmap.height * cosA

    // cropRect: 크롭 오버레이 및 이미지 스케일/위치 기준 (topInset + bottomInset 포함)
    val cropRect = remember(canvasSize, aspectRatioX, aspectRatioY, topInset, bottomInset) {
        if (canvasSize == IntSize.Zero) return@remember Rect.Zero
        calculateCropRect(
            canvasSize.toSize(),
            aspectRatioX,
            aspectRatioY,
            bitmap.width.toFloat(),
            bitmap.height.toFloat(),
            topInset,
            bottomInset,
        )
    }

    // baseScale: 회전된 이미지가 cropRect를 완전히 덮도록 하는 스케일
    val baseScale = remember(cropRect, effectiveWidth, effectiveHeight) {
        if (cropRect == Rect.Zero || effectiveWidth <= 0f || effectiveHeight <= 0f) 1f
        else max(
            cropRect.width / effectiveWidth,
            cropRect.height / effectiveHeight,
        )
    }

    val renderScale = baseScale * gestureZoom

    // 이미지를 cropRect 중앙에 맞추는 헬퍼
    fun centerImage() {
        val scale = baseScale * gestureZoom
        offsetX = cropRect.center.x - effectiveWidth * scale / 2f
        offsetY = cropRect.center.y - effectiveHeight * scale / 2f
    }

    // 초기 센터링: topInset 측정 완료 후 한 번 실행
    if (!initialized && cropRect != Rect.Zero && topInset > 0f) {
        initialized = true
        centerImage()
    }

    // cropRect 변경 시 (패널 열기/닫기, topInset 변경 등) 리센터
    var lastCropRect by remember { mutableStateOf(Rect.Zero) }
    if (initialized && cropRect != Rect.Zero && cropRect != lastCropRect) {
        lastCropRect = cropRect
        gestureZoom = 1f
        centerImage()
    }

    // 회전 변경 시 재센터
    if (lastRotation != rotationDegrees) {
        lastRotation = rotationDegrees
        gestureZoom = 1f
        centerImage()
    }

    // 비율 변경 시
    val currentAspectKey = "${aspectRatioX}_${aspectRatioY}"
    if (lastAspectKey != currentAspectKey && canvasSize != IntSize.Zero) {
        lastAspectKey = currentAspectKey
        gestureZoom = 1f
        centerImage()
    }

    // Brightness color filter
    val brightnessFilter = remember(brightness) {
        if (brightness == 0f) null
        else ColorFilter.colorMatrix(ColorMatrix().apply {
            val v = brightness * 255f
            set(0, 4, v)
            set(1, 4, v)
            set(2, 4, v)
        })
    }

    // cropRect → 원본 좌표 변환 제공
    SideEffect {
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
            }
            .pointerInput(onImageTap) {
                detectTapGestures { onImageTap?.invoke() }
            }
            .pointerInput(baseScale, cropRect) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val oldScale = baseScale * gestureZoom
                    val newGestureZoom = (gestureZoom * zoom).coerceIn(1f, 10f)
                    val newScale = baseScale * newGestureZoom
                    gestureZoom = newGestureZoom

                    // centroid 기준 확대
                    val newOffsetX = centroid.x - (centroid.x - offsetX) * (newScale / oldScale) + pan.x
                    val newOffsetY = centroid.y - (centroid.y - offsetY) * (newScale / oldScale) + pan.y

                    val imgW = effectiveWidth * newScale
                    val imgH = effectiveHeight * newScale
                    offsetX = clampOffset(newOffsetX, imgW, cropRect.left, cropRect.right)
                    offsetY = clampOffset(newOffsetY, imgH, cropRect.top, cropRect.bottom)
                }
            }
    ) {
        val currentRenderScale = baseScale * gestureZoom

        withTransform({
            translate(left = offsetX, top = offsetY)
            scale(currentRenderScale, currentRenderScale, pivot = Offset.Zero)
            // Flip
            if (flipHorizontal || flipVertical) {
                scale(
                    scaleX = if (flipHorizontal) -1f else 1f,
                    scaleY = if (flipVertical) -1f else 1f,
                    pivot = Offset(effectiveWidth / 2f, effectiveHeight / 2f),
                )
            }
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
                colorFilter = brightnessFilter,
            )
        }

        drawCropOverlay(cropRect)
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
    val paddingH = 16f  // 좌우
    val paddingTop = 8f + topInset  // 상단 (투명 툴바 높이 반영)
    val paddingBottom = 48f + bottomInset // 하단 기본 여백 + 상세 패널 높이
    val availableWidth = canvasSize.width - paddingH * 2
    val availableHeight = canvasSize.height - paddingTop - paddingBottom

    // 원본 이미지가 캔버스 안에 fit되는 스케일
    val fitScale = min(
        availableWidth / imageWidth,
        availableHeight / imageHeight,
    )
    val fittedW = imageWidth * fitScale
    val fittedH = imageHeight * fitScale

    val cropWidth: Float
    val cropHeight: Float

    if (aspectRatioX != null && aspectRatioY != null && aspectRatioY > 0f) {
        val ratio = aspectRatioX / aspectRatioY
        if (fittedW / fittedH > ratio) {
            cropHeight = fittedH
            cropWidth = cropHeight * ratio
        } else {
            cropWidth = fittedW
            cropHeight = cropWidth / ratio
        }
    } else {
        cropWidth = fittedW
        cropHeight = fittedH
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
