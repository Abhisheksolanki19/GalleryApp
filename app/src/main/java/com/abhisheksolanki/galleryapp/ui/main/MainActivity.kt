package com.abhisheksolanki.galleryapp.ui.main

import android.Manifest
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Scale
import com.abhisheksolanki.galleryapp.R
import com.abhisheksolanki.galleryapp.data.model.Image
import com.abhisheksolanki.galleryapp.ui.theme.GalleryAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlin.math.abs

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var readPermissionGranted = false
    private lateinit var contentObserver: ContentObserver
    private lateinit var permissionsLauncher: ActivityResultLauncher<String>

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GalleryAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    val imagePagingDataState =
                        viewModel.imagePagingData.collectAsStateWithLifecycle().value
                    val imagePagingData = remember(imagePagingDataState) { imagePagingDataState }
                    val lazyPagingItems = imagePagingData?.collectAsLazyPagingItems()

                    if (readPermissionGranted) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black),
                        ) {
                            var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
                            if (lazyPagingItems?.loadState?.refresh is LoadState.Loading && selectedImageUri == null) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .align(Alignment.Center),
                                    color = Color.White
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    lazyPagingItems?.let {
                                        if (lazyPagingItems.itemCount != 0) {
                                            selectedImageUri?.let { SelectedImage(it) }
                                            ImageStrip(images = lazyPagingItems) { image ->
                                                selectedImageUri = image.contentUri
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier.fillMaxSize(),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "You don't have any images in your phone",
                                                    textAlign = TextAlign.Center,
                                                    style = TextStyle(
                                                        color = Color.White,
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.W800
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Can't read files without permission",
                                textAlign = TextAlign.Center,
                                style = TextStyle(
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.W800
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { updateOrRequestPermission() },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {
                                Text(
                                    text = "Grant Permission",
                                    style = TextStyle(
                                        color = Color.Black
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
        permissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
                readPermissionGranted = permission
                if (readPermissionGranted) {
                    viewModel.loadImagePagingData()
                }
            }
        updateOrRequestPermission()
        initContentObserver()
    }

    private fun initContentObserver() {
        contentObserver = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean) {
                if (readPermissionGranted) {
                    lifecycleScope.launch { viewModel.loadImagePagingData() }
                }
            }
        }
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            contentObserver
        )
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun updateOrRequestPermission() {
        val permission: String
        readPermissionGranted = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
            permission = Manifest.permission.READ_MEDIA_IMAGES
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            permission = Manifest.permission.READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        if (!readPermissionGranted) {
            permissionsLauncher.launch(permission)
        } else {
            viewModel.loadImagePagingData()
        }
    }
}

@Composable
fun ImageStrip(images: LazyPagingItems<Image>, selectedImage: (Image) -> Unit) {
    val lazyListState = rememberLazyListState()
    LazyRow(
        state = lazyListState,
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(
            count = images.itemCount,
            key = images.itemKey { it.id },
            contentType = images.itemContentType { "ImageItems" }
        ) { index ->
            images[index]?.let {
                ImageItem(lazyListState, index, it, selectedImage)
            }
        }
        item {
            if (images.loadState.append is LoadState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(32.dp),
                    color = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageItem(state: LazyListState, index: Int, image: Image, selectedImage: (Image) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
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
        onClick = {
            coroutineScope.launch {
                state.animateScrollToItem(abs(index - 3))
            }
        },
        shape = RoundedCornerShape(4.dp)
    ) {
        AsyncImage(
            model = image.contentUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.placeholder)
        )
    }
}

@Composable
fun SelectedImage(imageUri: Uri) {
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUri)
            .size(400, 400)
            .placeholder(R.drawable.placeholder)
            .scale(Scale.FILL)
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
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
        )
    }
}