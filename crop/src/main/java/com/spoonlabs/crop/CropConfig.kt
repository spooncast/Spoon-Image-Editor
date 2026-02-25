package com.spoonlabs.crop

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class CropConfig(
    val sourceUri: Uri,
    val outputUri: Uri,
    val aspectRatioX: Float? = null,
    val aspectRatioY: Float? = null,
) : Parcelable
