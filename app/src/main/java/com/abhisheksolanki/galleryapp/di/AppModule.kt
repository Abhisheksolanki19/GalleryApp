package com.abhisheksolanki.galleryapp.di

import android.content.ContentResolver
import android.content.Context
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.abhisheksolanki.galleryapp.data.local.ImagePagingSource
import com.abhisheksolanki.galleryapp.data.model.Image
import com.abhisheksolanki.galleryapp.ui.main.MainViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Singleton
    fun provideImagePager(imagePagingSource: ImagePagingSource): Pager<Int, Image> {
        return Pager(
            config = PagingConfig(pageSize = 10),
            pagingSourceFactory = {
                imagePagingSource
            }
        )
    }

    @Provides
    @Singleton
    fun provideMainViewModel(pager: Pager<Int, Image>): MainViewModel {
        return MainViewModel(pager)
    }
}