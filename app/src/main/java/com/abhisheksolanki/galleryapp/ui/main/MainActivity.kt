package com.abhisheksolanki.galleryapp.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Size
import com.abhisheksolanki.galleryapp.R
import com.abhisheksolanki.galleryapp.data.model.Image
import com.abhisheksolanki.galleryapp.ui.theme.GalleryAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var readPermissionGranted = false
    private lateinit var contentObserver: ContentObserver
    private lateinit var permissionsLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateOrRequestPermission()
        setContent {
            GalleryAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val images = viewModel.images.collectAsStateWithLifecycle().value
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        var selectedImageUri by remember { mutableStateOf<Image?>(null) }
                        selectedImageUri?.let { SelectedImage(it) }
                        Text(
                            modifier = Modifier.padding(8.dp),
                            text = "${selectedImageUri?.contentUri}"
                        )
                        images?.let {
                            ImageStrip(images = it) { image -> selectedImageUri = image }
                        }
                    }
                }
            }
        }

        permissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
                readPermissionGranted = permission
            }
        initContentObserver()
    }

    private fun initContentObserver() {
        contentObserver = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                if (readPermissionGranted) {
                    lifecycleScope.launch { viewModel.loadImages(contentResolver) }
                }
            }
        }
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )
    }

    private fun updateOrRequestPermission() {
        val readPermissionGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (!readPermissionGranted) {
            permissionsLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (readPermissionGranted) {
            lifecycleScope.launch {
                viewModel.loadImages(contentResolver)
            }
        }
    }
}

@Composable
fun ImageStrip(images: List<Image>, selectedImage: (Image) -> Unit) {
    val lazyListState = rememberLazyListState()
    LazyRow(
        state = lazyListState,
        verticalAlignment = Alignment.Bottom,
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        itemsIndexed(
            items = images,
            key = { index, image -> image.id }
        ) { index, image ->
            ImageItem(lazyListState, index, image, selectedImage)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageItem(state: LazyListState, index: Int, image: Image, selectedImage: (Image) -> Unit) {
    val isMiddleElement by remember {
        derivedStateOf {
            val layoutInfo = state.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            val itemInfo = visibleItemsInfo.firstOrNull { it.index == index }
            itemInfo?.let {
                val delta = it.size / 2
                val center = state.layoutInfo.viewportEndOffset / 2
                val childCenter = it.offset + it.size / 2
                val target = childCenter - center
                if (target in -delta..delta) return@derivedStateOf true
            }
            false
        }
    }
    if (isMiddleElement) {
        selectedImage(image)
    }
    val scale = animateFloatAsState(if (isMiddleElement) 1.2f else 1f, label = "")
    Card(
        modifier = Modifier
            .height(72.dp)
            .width(56.dp)
            .scale(scale = scale.value)
            .padding(4.dp),
        onClick = { }
    ) {
        AsyncImage(
            model = image.contentUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.placeholder)
        )
    }
}

@Composable
fun SelectedImage(imageUri: Image) {
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUri.contentUri)
            .placeholder(R.drawable.placeholder)
            .size(Size.ORIGINAL)
            .build()
    )
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp)
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
    }
}