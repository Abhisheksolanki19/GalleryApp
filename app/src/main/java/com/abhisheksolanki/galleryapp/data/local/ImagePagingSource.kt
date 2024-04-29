package com.abhisheksolanki.galleryapp.data.local

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.abhisheksolanki.galleryapp.data.model.Image
import com.abhisheksolanki.galleryapp.utils.common.sdk29AndUp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ImagePagingSource @Inject constructor(
    private var contentResolver: ContentResolver
) : PagingSource<Int, Image>() {

    companion object {
        private const val PAGE_SIZE = 10
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Image> {
        val nextPage = params.key ?: 0
        return try {
            val images = loadImages(contentResolver, nextPage)
            LoadResult.Page(
                data = images,
                prevKey = if (nextPage == 0) null else nextPage - 1,
                nextKey = if (images.isEmpty()) null else nextPage + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Image>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    private suspend fun loadImages(contentResolver: ContentResolver, page: Int): List<Image> {
        return withContext(Dispatchers.IO) {
            val collection = sdk29AndUp {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.WIDTH,
            )

            val images = mutableListOf<Image>()
            val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"
            val offset = page * 10

            var query: Cursor? = null

            try {
                // ContentResolver operation
                query = sdk29AndUp {
                    val bundle = Bundle().apply {
                        // sort
                        putString(
                            ContentResolver.QUERY_ARG_SORT_COLUMNS,
                            sortOrder
                        )
                        putInt(
                            ContentResolver.QUERY_ARG_SORT_DIRECTION,
                            ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                        )
                        // limit, offset
                        putInt(ContentResolver.QUERY_ARG_LIMIT, PAGE_SIZE)
                        putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
                    }
                    contentResolver.query(
                        collection,
                        projection,
                        bundle,
                        null
                    )
                } ?: contentResolver.query(
                    collection,
                    projection,
                    null,
                    null,
                    "$sortOrder LIMIT $PAGE_SIZE OFFSET $offset"
                )
            } catch (e: SecurityException) {
                // Handle permission-related error
                Log.d("ImagePagingSource", e.toString())
            } catch (e: Exception) {
                // Handle other exceptions
                Log.d("ImagePagingSource", e.toString())
            }

            query?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    images.add(Image(id, displayName, width, height, contentUri))
                }
                images.toList()
            } ?: emptyList()
        }
    }

}