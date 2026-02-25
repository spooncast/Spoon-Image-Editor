package com.spoonlabs.crop

import android.net.Uri

sealed interface CropResult {
    data class Success(val outputUri: Uri) : CropResult
    data class Error(val message: String) : CropResult
    data object Cancelled : CropResult
}
