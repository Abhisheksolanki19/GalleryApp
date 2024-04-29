package com.abhisheksolanki.galleryapp.ui.main

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingData
import com.abhisheksolanki.galleryapp.data.model.Image
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val pager: Pager<Int, Image>
) : ViewModel() {

    private val _imagePagingData = MutableStateFlow<Flow<PagingData<Image>>?>(null)
    val imagePagingData = _imagePagingData.asStateFlow()

    fun loadImagePagingData() {
        _imagePagingData.value = pager.flow
    }

}