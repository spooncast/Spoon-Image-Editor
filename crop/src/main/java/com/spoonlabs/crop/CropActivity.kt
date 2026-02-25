package com.spoonlabs.crop

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CropActivity : ComponentActivity() {

    private var sourceBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val config = intent.getParcelableExtra<CropConfig>(EXTRA_CROP_CONFIG)
        if (config == null) {
            finishWithError("No crop configuration provided")
            return
        }

        val bitmap = CropImageProcessor.loadBitmap(this, config.sourceUri)
        if (bitmap == null) {
            finishWithError("Failed to load image")
            return
        }
        sourceBitmap = bitmap

        setContent {
            CropScreen(
                bitmap = bitmap,
                aspectRatioX = config.aspectRatioX,
                aspectRatioY = config.aspectRatioY,
                onConfirm = { cropRect, rotationDegrees ->
                    lifecycleScope.launch {
                        val result = withContext(Dispatchers.IO) {
                            CropImageProcessor.cropAndSave(
                                sourceBitmap = bitmap,
                                cropRect = cropRect,
                                rotationDegrees = rotationDegrees,
                                outputUri = config.outputUri,
                            )
                        }
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

    override fun onDestroy() {
        super.onDestroy()
        sourceBitmap?.recycle()
        sourceBitmap = null
    }

    private fun finishWithError(message: String) {
        setResult(
            RESULT_ERROR,
            Intent().putExtra(EXTRA_ERROR_MESSAGE, message),
        )
        finish()
    }

    companion object {
        const val EXTRA_CROP_CONFIG = "crop_config"
        const val EXTRA_OUTPUT_URI = "crop_output_uri"
        const val EXTRA_ERROR_MESSAGE = "crop_error"
        const val RESULT_ERROR = 96
        const val REQUEST_CROP = 69
    }
}
