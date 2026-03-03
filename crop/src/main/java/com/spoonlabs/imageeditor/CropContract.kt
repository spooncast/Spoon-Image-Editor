package com.spoonlabs.imageeditor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract

class CropContract : ActivityResultContract<CropConfig, CropResult>() {

    override fun createIntent(context: Context, input: CropConfig): Intent {
        return Intent(context, CropActivity::class.java).apply {
            putExtra(CropActivity.EXTRA_CROP_CONFIG, input)
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): CropResult {
        return when (resultCode) {
            Activity.RESULT_OK -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableExtra(CropActivity.EXTRA_OUTPUT_URI, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent?.getParcelableExtra(CropActivity.EXTRA_OUTPUT_URI)
                }
                if (uri != null) {
                    CropResult.Success(uri)
                } else {
                    CropResult.Error("No output URI")
                }
            }
            CropActivity.RESULT_ERROR -> {
                val message = intent?.getStringExtra(CropActivity.EXTRA_ERROR_MESSAGE)
                    ?: "Unknown Error"
                CropResult.Error(message)
            }
            else -> CropResult.Cancelled
        }
    }
}
