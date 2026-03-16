package com.spoonlabs.imageeditor

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImageEditActivity : ComponentActivity() {

    private var loadedBitmap by mutableStateOf<Bitmap?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            )
        } catch (e: Exception) {
            Log.w(TAG, "enableEdgeToEdge failed", e)
        }
        super.onCreate(savedInstanceState)

        try {
            WindowCompat.getInsetsController(window, window.decorView)?.apply {
                hide(WindowInsetsCompat.Type.statusBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } catch (e: Exception) {
            Log.w(TAG, "WindowInsetsController setup failed", e)
        }

        val config = try {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra<ImageEditConfig>(EXTRA_CONFIG)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse config", e)
            null
        }
        if (config == null) {
            finishWithError("No crop configuration provided")
            return
        }

        setContent {
            val bitmap = loadedBitmap
            if (bitmap != null && !bitmap.isRecycled) {
                ImageEditScreen(
                    bitmap = bitmap,
                    onConfirm = { cropRect, rotationDegrees, brightness, flipH, flipV ->
                        lifecycleScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                ImageEditProcessor.cropAndSave(
                                    context = this@ImageEditActivity,
                                    sourceBitmap = bitmap,
                                    cropRect = cropRect,
                                    rotationDegrees = rotationDegrees,
                                    brightness = brightness,
                                    flipHorizontal = flipH,
                                    flipVertical = flipV,
                                    outputUri = config.outputUri,
                                )
                            }
                            if (isFinishing || isDestroyed) return@launch
                            result.onSuccess { uri ->
                                setResult(
                                    RESULT_OK,
                                    Intent().putExtra(EXTRA_OUTPUT_URI, uri),
                                )
                                finish()
                            }.onFailure { e ->
                                finishWithError(e.localizedMessage ?: "Crop failed")
                            }
                        }
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                )
            }
        }

        if (loadedBitmap == null) {
            lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    ImageEditProcessor.loadBitmap(this@ImageEditActivity, config.sourceUri)
                }
                if (isFinishing || isDestroyed) {
                    bitmap?.recycle()
                    return@launch
                }
                if (bitmap == null) {
                    finishWithError("Failed to load image")
                } else {
                    loadedBitmap = bitmap
                }
            }
        }
    }

    private fun finishWithError(message: String) {
        setResult(
            RESULT_ERROR,
            Intent().putExtra(EXTRA_ERROR_MESSAGE, message),
        )
        finish()
    }

    companion object {
        private const val TAG = "ImageEditActivity"
        const val EXTRA_CONFIG = "image_edit_config"
        const val EXTRA_OUTPUT_URI = "image_edit_output_uri"
        const val EXTRA_ERROR_MESSAGE = "image_edit_error"
        const val RESULT_ERROR = RESULT_FIRST_USER
    }
}
