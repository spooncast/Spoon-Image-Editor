package com.spoonlabs.imageeditor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

object CropImageProcessor {

    private const val MAX_BITMAP_SIZE = 4096
    private const val JPEG_QUALITY = 90

    fun loadBitmap(context: Context, uri: Uri): Bitmap? {
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
        return resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        }
    }

    fun cropAndSave(
        sourceBitmap: Bitmap,
        cropRect: RectF,
        rotationDegrees: Float,
        brightness: Float = 0f,
        flipHorizontal: Boolean = false,
        flipVertical: Boolean = false,
        outputUri: Uri,
    ): Result<Uri> = runCatching {
        val radians = Math.toRadians(rotationDegrees.toDouble())
        val cosA = abs(cos(radians)).toFloat()
        val sinA = abs(sin(radians)).toFloat()

        val rotatedW = (sourceBitmap.width * cosA + sourceBitmap.height * sinA).roundToInt()
        val rotatedH = (sourceBitmap.width * sinA + sourceBitmap.height * cosA).roundToInt()

        // Create rotated bitmap
        val rotatedBitmap = if (rotationDegrees != 0f) {
            val result = Bitmap.createBitmap(rotatedW, rotatedH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(result)
            val matrix = Matrix().apply {
                postTranslate(-sourceBitmap.width / 2f, -sourceBitmap.height / 2f)
                postRotate(rotationDegrees)
                postTranslate(rotatedW / 2f, rotatedH / 2f)
            }
            canvas.drawBitmap(sourceBitmap, matrix, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            result
        } else {
            sourceBitmap
        }

        val x = cropRect.left.toInt().coerceIn(0, rotatedBitmap.width - 1)
        val y = cropRect.top.toInt().coerceIn(0, rotatedBitmap.height - 1)
        val w = cropRect.width().toInt().coerceIn(1, rotatedBitmap.width - x)
        val h = cropRect.height().toInt().coerceIn(1, rotatedBitmap.height - y)

        var resultBitmap = Bitmap.createBitmap(rotatedBitmap, x, y, w, h)

        // Apply flip
        if (flipHorizontal || flipVertical) {
            val flipMatrix = Matrix().apply {
                postScale(
                    if (flipHorizontal) -1f else 1f,
                    if (flipVertical) -1f else 1f,
                    resultBitmap.width / 2f,
                    resultBitmap.height / 2f,
                )
            }
            val flipped = Bitmap.createBitmap(
                resultBitmap, 0, 0,
                resultBitmap.width, resultBitmap.height,
                flipMatrix, true,
            )
            if (flipped !== resultBitmap) resultBitmap.recycle()
            resultBitmap = flipped
        }

        // Apply brightness
        if (brightness != 0f) {
            val brightBitmap = Bitmap.createBitmap(
                resultBitmap.width, resultBitmap.height, Bitmap.Config.ARGB_8888,
            )
            val canvas = Canvas(brightBitmap)
            val v = brightness * 255f
            val cm = android.graphics.ColorMatrix(floatArrayOf(
                1f, 0f, 0f, 0f, v,
                0f, 1f, 0f, 0f, v,
                0f, 0f, 1f, 0f, v,
                0f, 0f, 0f, 1f, 0f,
            ))
            val paint = Paint().apply {
                colorFilter = ColorMatrixColorFilter(cm)
            }
            canvas.drawBitmap(resultBitmap, 0f, 0f, paint)
            resultBitmap.recycle()
            resultBitmap = brightBitmap
        }

        val outputPath = outputUri.path ?: throw IllegalArgumentException("Invalid output URI")
        FileOutputStream(outputPath).use { fos ->
            resultBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos)
        }

        resultBitmap.recycle()
        if (rotatedBitmap !== sourceBitmap) rotatedBitmap.recycle()

        outputUri
    }
}
