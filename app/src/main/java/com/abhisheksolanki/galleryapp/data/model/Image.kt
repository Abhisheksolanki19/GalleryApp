package com.abhisheksolanki.galleryapp.data.model

import android.net.Uri
import androidx.compose.runtime.Stable

@Stable
data class Image(
    val id: Long,
    val name: String,
    val width: Int,
    val height: Int,
    val contentUri: Uri
)
