package com.peihua.simple

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import coil.compose.rememberAsyncImagePainter
import com.fz.common.collections.isNonEmpty
import com.fz.common.collections.toArrayList
import com.fz.common.file.cacheFile
import com.fz.common.file.copyToFile
import com.fz.common.file.createFileName
import com.fz.common.text.isNonEmpty
import com.fz.common.utils.getParcelableArrayListExtraCompat
import com.fz.common.utils.toString
import com.fz.imageloader.ImageLoader
import com.fz.imageloader.glide.ImageGlideFetcher
import com.fz.imageloader.widget.RatioImageView
import com.peihua.selector.result.PhotoCropVisualMediaRequestBuilder
import com.peihua.selector.result.PhotoVisualMediaRequestBuilder
import com.peihua.selector.result.SystemPhotoCropVisualMediaRequestBuilder
import com.peihua.selector.result.contract.PhotoCropVisualMedia
import com.peihua.selector.result.contract.PhotoMultipleVisualMedia
import com.peihua.selector.result.contract.PhotoVisualMedia
import com.peihua.selector.result.contract.SytemPhotoCropVisualMedia
import com.peihua.simple.ui.theme.PictureSelectorTheme
import com.peihua.simple.ui.theme.Purple40
import id.zelory.compressor.createFile
import java.io.File

class MainActivity : ComponentActivity() {
    val multiSelectPhotoRequest by lazy {
        PhotoVisualMediaRequestBuilder(PhotoVisualMedia.ImageAndVideo)
            .setForceCustomUi(false)
            .setMaxItemCount(10)
            .setMediaType(PhotoVisualMedia.MultipleMimeType("image/jpeg", "image/png"))
            .setShowGif(false)
    }
    val singleSelectPhotoRequest by lazy {
//        PhotoVisualMediaRequestBuilder(PhotoVisualMedia.SingleMimeType("image/gif"))
        PhotoVisualMediaRequestBuilder(PhotoVisualMedia.ImageOnly)
            .setForceCustomUi(true)
            .setShowGif(true)
    }
    val cropPhotoRequest by lazy {
        val outputFile = "IMG_".createFile("jpg")
        val outputUri = Uri.fromFile(outputFile)
        PhotoCropVisualMediaRequestBuilder(selectUrisState.value.toArrayList(), outputUri)
            .withAspectRatio(1f, 1f)
            .build()
    }


    val launchMultipleImage = registerForActivityResult(PhotoMultipleVisualMedia(3)) {
        Log.d("MainActivity", "Uri=$it")
        if (it.isNonEmpty()) {
            selectUrisState.value = it
            launchCrop.launch(cropPhotoRequest)
        }
    }
    val launchImage = registerForActivityResult(PhotoVisualMedia()) {
        Log.d("MainActivity", "Uri=$it")
        if (it != null) {
            selectUrisState.value = arrayListOf(it)
            viewModel.state.postValue(it)
            launchCrop.launch(cropPhotoRequest)
        }
    }
    val selectUrisState = mutableStateOf<List<Uri>>(arrayListOf())
    val cropUrisState = mutableStateOf<List<Uri>>(arrayListOf())
    val launchCrop = registerForActivityResult(PhotoCropVisualMedia()) {
        val uris =
            it.data?.getParcelableArrayListExtraCompat(MediaStore.EXTRA_OUTPUT, Uri::class.java)
        if (uris.isNonEmpty()) {
            Log.d("MainActivity->Crop", "Uri=${uris}")
            cropUrisState.value = uris
            return@registerForActivityResult
        }
    }
    val launchSystemCrop = registerForActivityResult(SytemPhotoCropVisualMedia()) {
        val uris =
            it.data?.getParcelableArrayListExtraCompat(MediaStore.EXTRA_OUTPUT, Uri::class.java)
        if (uris.isNonEmpty()) {
            Log.d("MainActivity->Crop", "Uri=${uris}")
            cropUrisState.value = uris
            return@registerForActivityResult
        }
    }
    val viewModel by viewModels<ViewModelUri>()
    fun String.createFile(extension: String): File {
        val fileCache = createFileName(extension)
        val parentPath = cacheFile("files")
        return File(parentPath, fileCache)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ImageLoader.getInstance().createProcessor(ImageGlideFetcher())
        setContent { RootView() }
    }

    @Composable
    fun RootView() {
        PictureSelectorTheme {
            // A surface container using the 'background' color from the theme
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Purple40)
                    .padding(24.dp)
            ) {
                Column {
                    CustomImageViewWrapper()
//                    Greeting()
//                    MyComposeScreen()
                    ListViewComposable(selectUrisState.value)
                    CropListView(cropUrisState.value)
                }
            }
        }
    }

    fun Context.getRealPathFromURI(uri: Uri?): File? {
        if (uri == null) return null
        val column = arrayOf(MediaStore.Images.Media.DATA)
        val sel: String
        val cursor = try {
            val wholeID = DocumentsContract.getDocumentId(uri)
            val id = wholeID.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
            // where id is equal to
            sel = MediaStore.Images.Media._ID + "=?"
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                column, sel, arrayOf(id), null
            )
        } catch (e: Throwable) {
            contentResolver.query(
                uri, column, null,
                null, null
            )
        }
        return cursor?.use {
            val columnIndex = cursor.getColumnIndex(column[0])
            cursor.moveToFirst()
            val filePath = cursor.getString(columnIndex)
            if (filePath.isNonEmpty()) {
                return File(filePath)
            }
            null
        }
    }

    private fun toSystemCropActivity(uri: Uri?) {
        if (uri == null) return
        val fis = contentResolver.openInputStream(uri)
        if (fis != null) {
            val file = createFile(this)
            if (!file.exists()) {
                file.parentFile?.mkdirs()
            }
            file.copyToFile(fis)
//            val file = getRealPathFromURI(uri) ?: return
            val mImageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                /*7.0以上要通过FileProvider将File转化为Uri*/
                FileProvider.getUriForFile(this, "com.peihua.simple.PickerProvider", file)
            } else {
                /*7.0以下则直接使用Uri的fromFile方法将File转化为Uri*/
                Uri.fromFile(file)
            }
            val outputUri = contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                ContentValues()
            )
            launchSystemCrop.launch(
                SystemPhotoCropVisualMediaRequestBuilder(mImageUri, outputUri!!)
                    .setCircleCrop(true)
                    .setAspectY(1f)
                    .setAspectX(1f)
                    .setAutoCustomCorp(true)
                    .setOutputX(200f)
                    .setOutputY(200f)
                    .build()
            )
        }
    }

    @Composable
    fun MyComposeScreen() {
        Column {
            AndroidView(
                modifier = Modifier.size(100.dp),
                factory = { context ->
                    // Creates view
                    RatioImageView(context).apply {
                        viewModel.state.observe(this@MainActivity) {
                            setImageUrl(it)
                        }
                    }
                },
            )
//            Image(
//                painter = rememberAsyncImagePainter(uri),
//                contentDescription = null,
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .aspectRatio(1f)
//            )
        }

    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun ListViewComposable(uris: List<Uri>?) {
        Column {
            Text(text = "选择图片列表", color = Color.White, fontSize = 20.sp)
            FlowRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uris?.forEach {
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun CropListView(uris: List<Uri>?) {
        Column {
            Text(text = "裁切图片列表", color = Color.White, fontSize = 20.sp)
            FlowRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uris?.forEach {
                    Image(
                        painter = rememberAsyncImagePainter(it),
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .aspectRatio(1f)
                    )
                }
            }
        }
    }

    @Composable
    fun CustomImageViewWrapper() {
        Text(
            text = "单选相册",
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.clickable {
                launchImage.launch(singleSelectPhotoRequest.build())
            })
        Text(
            text = "多选相册",
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.clickable {
                launchMultipleImage.launch(
                    multiSelectPhotoRequest
                        .setSelectedUris(selectUrisState.value.toArrayList())
                        .build()
                )
            })
        Text(
            text = "裁剪图片",
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.clickable {
                val outputFile = "IMG_".createFile("jpg")
                val outputUri = Uri.fromFile(outputFile)
                //content://media/external/images/1000000126
                if (selectUrisState.value.isEmpty()) {
                    showToast("请选择一致图片")
                    return@clickable
                }
                if (selectUrisState.value.isNonEmpty()) {
                    launchCrop.launch(cropPhotoRequest)
                }
            })
        Text(
            text = "系统裁剪图片",
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.clickable {
                //content://media/external/images/1000000126
                val imageUri = selectUrisState.value.firstOrNull()
                if (imageUri == null) {
                    showToast("请选择一致图片")
                    return@clickable
                }
                toSystemCropActivity(imageUri)
            })
    }

    @Composable
    fun Greeting() {
        AndroidView(
            factory = { context ->
                TextView(context).apply {
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 20f
                    viewModel.state.observe(this@MainActivity) {
                        text = it.toString("")
                    }
                }
            },
        )
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        RootView()
    }

    fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }
}


class ViewModelUri : ViewModel() {
    val state = MutableLiveData<Uri?>(null)
}