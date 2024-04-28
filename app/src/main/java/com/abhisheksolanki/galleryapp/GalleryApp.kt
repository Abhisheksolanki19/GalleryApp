package com.abhisheksolanki.galleryapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GalleryApp: Application() {

    companion object{
        lateinit var INSTANCE: GalleryApp
    }

    override fun onCreate() {
        super.onCreate()
        INSTANCE = this
    }
}