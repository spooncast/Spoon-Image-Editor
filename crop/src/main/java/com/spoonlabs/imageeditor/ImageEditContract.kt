package com.spoonlabs.imageeditor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract

class ImageEditContract : ActivityResultContract<ImageEditConfig, ImageEditResult>() {

    override fun createIntent(context: Context, input: ImageEditConfig): Intent {
        return Intent(context, ImageEditActivity::class.java).apply {
            putExtra(ImageEditActivity.EXTRA_CONFIG, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): ImageEditResult {
        return when (resultCode) {
            Activity.RESULT_OK -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableExtra(ImageEditActivity.EXTRA_OUTPUT_URI, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent?.getParcelableExtra(ImageEditActivity.EXTRA_OUTPUT_URI)
                }
                if (uri != null) {
                    ImageEditResult.Success(uri)
                } else {
                    ImageEditResult.Error("No output URI")
                }
            }
            ImageEditActivity.RESULT_ERROR -> {
                val message = intent?.getStringExtra(ImageEditActivity.EXTRA_ERROR_MESSAGE)
                    ?: "Unknown Error"
                ImageEditResult.Error(message)
            }
            else -> ImageEditResult.Cancelled
        }
    }
}
