package com.abhisheksolanki.galleryapp.data.model

import android.net.Uri

data class Image(
    val id: Long,
    val name: String,
    val width: Int,
    val height: Int,
    val contentUri: Uri
)
