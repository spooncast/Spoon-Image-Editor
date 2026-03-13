package com.spoonlabs.imageeditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.core.graphics.createBitmap

object ImageEditProcessor {

    private const val MAX_BITMAP_SIZE = 4096
    private const val JPEG_QUALITY = 90

    fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            val resolver = context.contentResolver
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

            val width = options.outWidth
            val height = options.outHeight
            if (width <= 0 || height <= 0) return null

            var inSampleSize = 1
            while (width / inSampleSize > MAX_BITMAP_SIZE || height / inSampleSize > MAX_BITMAP_SIZE) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
        } catch (_: Exception) {
            null
        } catch (_: OutOfMemoryError) {
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
        Result.success(cropAndSaveInternal(
            context, sourceBitmap, cropRect, rotationDegrees,
            brightness, flipHorizontal, flipVertical, outputUri,
        ))
    } catch (e: Exception) {
        Result.failure(e)
    } catch (e: OutOfMemoryError) {
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
        val radians = Math.toRadians(rotationDegrees.toDouble())
        val cosA = abs(cos(radians)).toFloat()
        val sinA = abs(sin(radians)).toFloat()

        val rotatedW = (sourceBitmap.width * cosA + sourceBitmap.height * sinA)
            .coerceIn(1f, MAX_BITMAP_SIZE.toFloat()).roundToInt()
        val rotatedH = (sourceBitmap.width * sinA + sourceBitmap.height * cosA)
            .coerceIn(1f, MAX_BITMAP_SIZE.toFloat()).roundToInt()

        var rotatedBitmap: Bitmap? = null
        var resultBitmap: Bitmap? = null

        try {
            rotatedBitmap = if (rotationDegrees != 0f) {
                val bmp = Bitmap.createBitmap(rotatedW, rotatedH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                val matrix = Matrix().apply {
                    postTranslate(-sourceBitmap.width / 2f, -sourceBitmap.height / 2f)
                    postRotate(rotationDegrees)
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

            val x = cropRect.left.toInt().coerceIn(0, rotatedBitmap.width - 1)
            val y = cropRect.top.toInt().coerceIn(0, rotatedBitmap.height - 1)
            val w = cropRect.width().toInt().coerceIn(1, rotatedBitmap.width - x)
            val h = cropRect.height().toInt().coerceIn(1, rotatedBitmap.height - y)

            // Bitmap.createBitmap()은 crop이 전체 이미지와 일치하면 같은 reference를 반환할 수 있음.
            // sourceBitmap은 Compose UI에서 사용 중이므로 반드시 별도 bitmap으로 분리해야 함.
            var currentResult = Bitmap.createBitmap(rotatedBitmap, x, y, w, h)
            if (currentResult === sourceBitmap) {
                currentResult = sourceBitmap.copy(sourceBitmap.config ?: Bitmap.Config.ARGB_8888, true)
            }
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
                if (flipped !== currentResult) {
                    currentResult.recycle()
                }
                currentResult = flipped
                resultBitmap = currentResult
            }

            if (brightness != 0f) {
                val clampedBrightness = brightness.coerceIn(-1f, 1f)
                val brightBitmap = createBitmap(currentResult.width, currentResult.height)
                val canvas = Canvas(brightBitmap)
                val scale = 1f + clampedBrightness * 0.2f
                val offset = clampedBrightness * 80f
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
                currentResult.recycle()
                currentResult = brightBitmap
                resultBitmap = currentResult
            }

            context.contentResolver.openOutputStream(outputUri)?.use { os ->
                currentResult.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, os)
            } ?: throw IllegalArgumentException("Cannot open output URI: $outputUri")

            outputUri
        } finally {
            resultBitmap?.recycle()
            if (rotatedBitmap !== sourceBitmap && rotatedBitmap !== resultBitmap) {
                rotatedBitmap?.recycle()
            }
        }
    }
}
