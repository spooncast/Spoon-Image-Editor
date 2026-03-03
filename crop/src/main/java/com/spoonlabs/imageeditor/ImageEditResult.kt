package com.spoonlabs.imageeditor

import android.net.Uri

sealed interface ImageEditResult {
    data class Success(val outputUri: Uri) : ImageEditResult
    data class Error(val message: String) : ImageEditResult
    data object Cancelled : ImageEditResult
}
