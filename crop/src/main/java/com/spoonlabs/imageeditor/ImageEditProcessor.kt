package com.spoonlabs.imageeditor

import android.app.ActivityManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.util.Log
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import androidx.core.graphics.createBitmap

object ImageEditProcessor {

    private const val TAG = "ImageEditProcessor"
    private const val MAX_BITMAP_SIZE = 4096
    private const val JPEG_QUALITY = 90

    /**
     * 기기 가용 메모리에 따른 최대 비트맵 크기 계산.
     * 비트맵 처리 시 최대 2개가 동시에 메모리에 존재하므로,
     * 힙의 25%를 2개 비트맵이 나눠 쓸 수 있는 크기로 제한.
     */
    private fun getMaxBitmapSize(context: Context): Int {
        return try {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val heapMb = am?.memoryClass ?: 256
            val maxPixelsPerBitmap = (heapMb.toLong() * 1024 * 1024 / 4) / (4 * 2)
            val maxDimension = sqrt(maxPixelsPerBitmap.toDouble()).toInt()
            min(maxDimension, MAX_BITMAP_SIZE).coerceAtLeast(1024)
        } catch (_: Throwable) {
            2048
        }
    }

    fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val resolver = context.contentResolver
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

            val width = options.outWidth
            val height = options.outHeight
            if (width <= 0 || height <= 0) return null

            val maxSize = getMaxBitmapSize(context)
            var inSampleSize = 1
            while (width / inSampleSize > maxSize || height / inSampleSize > maxSize) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
            // 디코딩 결과 검증
            if (bitmap == null || bitmap.width <= 0 || bitmap.height <= 0) {
                bitmap?.recycle()
                return null
            }
            bitmap
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load bitmap", e)
            null
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "OOM while loading bitmap", e)
            null
        }
    }

    fun cropAndSave(
        context: Context,
        sourceBitmap: Bitmap,
        cropRect: RectF,
        rotationDegrees: Float,
        brightness: Float = 0f,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        outputUri: Uri,
    ): Result<Uri> = try {
        // sourceBitmap 유효성 검사
        if (sourceBitmap.isRecycled) {
            Result.failure(IllegalStateException("Source bitmap is recycled"))
        } else if (sourceBitmap.width <= 0 || sourceBitmap.height <= 0) {
            Result.failure(IllegalStateException("Source bitmap has invalid dimensions"))
        } else {
            Result.success(
                cropAndSaveInternal(
                    context, sourceBitmap, cropRect, rotationDegrees,
                    brightness, flipHorizontal, flipVertical, outputUri,
                )
            )
        }
    } catch (e: Exception) {
        Log.w(TAG, "cropAndSave failed", e)
        Result.failure(e)
    } catch (e: OutOfMemoryError) {
        Log.w(TAG, "OOM in cropAndSave", e)
        Result.failure(RuntimeException("Out of memory while processing image", e))
    }

    private fun cropAndSaveInternal(
        context: Context,
        sourceBitmap: Bitmap,
        cropRect: RectF,
        rotationDegrees: Float,
        brightness: Float,
        flipHorizontal: Boolean,
        flipVertical: Boolean,
        outputUri: Uri,
    ): Uri {
        // NaN/Infinity 방어
        val safeRotation = if (rotationDegrees.isFinite()) rotationDegrees else 0f
        val safeBrightness = if (brightness.isFinite()) brightness.coerceIn(-1f, 1f) else 0f

        val radians = Math.toRadians(safeRotation.toDouble())
        val cosA = abs(cos(radians)).toFloat()
        val sinA = abs(sin(radians)).toFloat()

        val maxSize = getMaxBitmapSize(context)
        val rotatedW = (sourceBitmap.width * cosA + sourceBitmap.height * sinA)
            .coerceIn(1f, maxSize.toFloat()).roundToInt()
        val rotatedH = (sourceBitmap.width * sinA + sourceBitmap.height * cosA)
            .coerceIn(1f, maxSize.toFloat()).roundToInt()

        var rotatedBitmap: Bitmap? = null
        var resultBitmap: Bitmap? = null

        try {
            rotatedBitmap = if (safeRotation != 0f) {
                val bmp = Bitmap.createBitmap(rotatedW, rotatedH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                val matrix = Matrix().apply {
                    postTranslate(-sourceBitmap.width / 2f, -sourceBitmap.height / 2f)
                    postRotate(safeRotation)
                    postTranslate(rotatedW / 2f, rotatedH / 2f)
                }
                canvas.drawBitmap(
                    sourceBitmap, matrix,
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
                )
                bmp
            } else {
                sourceBitmap
            }

            // cropRect 값 안전하게 클램핑 — x+w, y+h가 비트맵 범위를 초과하지 않도록
            val safeLeft = if (cropRect.left.isFinite()) cropRect.left else 0f
            val safeTop = if (cropRect.top.isFinite()) cropRect.top else 0f
            val safeWidth = if (cropRect.width().isFinite()) cropRect.width() else rotatedBitmap.width.toFloat()
            val safeHeight = if (cropRect.height().isFinite()) cropRect.height() else rotatedBitmap.height.toFloat()

            val x = safeLeft.toInt().coerceIn(0, rotatedBitmap.width - 1)
            val y = safeTop.toInt().coerceIn(0, rotatedBitmap.height - 1)
            val w = safeWidth.toInt().coerceIn(1, rotatedBitmap.width - x)
            val h = safeHeight.toInt().coerceIn(1, rotatedBitmap.height - y)

            // Bitmap.createBitmap()은 crop이 전체 이미지와 일치하면 같은 reference를 반환할 수 있음.
            // sourceBitmap은 Compose UI에서 사용 중이므로 recycle하면 안 됨 — reference 추적으로 보호.
            var currentResult = Bitmap.createBitmap(rotatedBitmap, x, y, w, h)
            resultBitmap = currentResult

            if (flipHorizontal || flipVertical) {
                val flipMatrix = Matrix().apply {
                    postScale(
                        if (flipHorizontal) -1f else 1f,
                        if (flipVertical) -1f else 1f,
                        currentResult.width / 2f,
                        currentResult.height / 2f,
                    )
                }
                val flipped = Bitmap.createBitmap(
                    currentResult, 0, 0,
                    currentResult.width, currentResult.height,
                    flipMatrix, true,
                )
                if (flipped !== currentResult && currentResult !== sourceBitmap) {
                    currentResult.recycle()
                }
                currentResult = flipped
                resultBitmap = currentResult
            }

            if (safeBrightness != 0f) {
                val brightBitmap = createBitmap(currentResult.width, currentResult.height)
                val canvas = Canvas(brightBitmap)
                val scale = 1f + safeBrightness * 0.2f
                val offset = safeBrightness * 80f
                val cm = android.graphics.ColorMatrix(
                    floatArrayOf(
                        scale, 0f, 0f, 0f, offset,
                        0f, scale, 0f, 0f, offset,
                        0f, 0f, scale, 0f, offset,
                        0f, 0f, 0f, 1f, 0f,
                    ),
                )
                canvas.drawBitmap(
                    currentResult, 0f, 0f,
                    Paint().apply { colorFilter = ColorMatrixColorFilter(cm) },
                )
                if (currentResult !== sourceBitmap) {
                    currentResult.recycle()
                }
                currentResult = brightBitmap
                resultBitmap = currentResult
            }

            val stream = context.contentResolver.openOutputStream(outputUri)
                ?: throw IllegalArgumentException("Cannot open output URI: $outputUri")
            stream.use { os ->
                val success = currentResult.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, os)
                if (!success) {
                    throw IllegalStateException("Failed to compress bitmap to JPEG")
                }
            }

            return outputUri
        } finally {
            // sourceBitmap은 Compose UI에서 사용 중 — 절대 recycle하지 않음
            if (resultBitmap !== sourceBitmap) {
                resultBitmap?.recycle()
            }
            if (rotatedBitmap !== sourceBitmap && rotatedBitmap !== resultBitmap) {
                rotatedBitmap?.recycle()
            }
        }
    }
}
