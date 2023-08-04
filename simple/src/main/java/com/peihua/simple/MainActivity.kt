package com.peihua.simple

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role.Companion.Image
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
import com.fz.imageloader.ImageLoader
import com.fz.imageloader.glide.ImageGlideFetcher
import com.fz.imageloader.widget.RatioImageView
import com.peihua.selector.result.PhotoCropVisualMediaRequest
import com.peihua.selector.result.PhotoCropVisualMediaRequestBuilder
import com.peihua.selector.result.PhotoVisualMediaRequestBuilder
import com.peihua.selector.result.SystemPhotoCropVisualMediaRequest
import com.peihua.selector.result.SystemPhotoCropVisualMediaRequestBuilder
import com.peihua.selector.result.contract.PhotoCropVisualMedia
import com.peihua.selector.result.contract.PhotoMultipleVisualMedia
import com.peihua.selector.result.contract.PhotoVisualMedia
import com.peihua.selector.result.contract.SytemPhotoCropVisualMedia
import com.peihua.selector.util.isAtLeastT
import com.peihua.simple.ui.theme.PictureSelectorTheme
import com.peihua.simple.ui.theme.Purple40
import id.zelory.compressor.createFile
import java.io.File

class MainActivity : ComponentActivity() {
    val launchMultipleImage = registerForActivityResult(PhotoMultipleVisualMedia(3)) {
        Log.d("MainActivity", "Uri=$it")
        if (it.isNonEmpty()) {
            val outputFile = "IMG_".createFile("jpg")
            val outputUri = Uri.fromFile(outputFile)
//            state.value = it
            selectedUris = it.toArrayList()
            stateUris.value = it
            launchCrop.launch(
                PhotoCropVisualMediaRequestBuilder(it.toArrayList(), outputUri)
                    .withAspectRatio(1f, 1f)
                    .build()
            )
        }
    }
    val launchImage = registerForActivityResult(PhotoVisualMedia()) {
        Log.d("MainActivity", "Uri=$it")
        if (it != null) {
            val outputFile = "IMG_".createFile("jpg")
            val outputUri = Uri.fromFile(outputFile)
//            state.value = it
            viewModel.state.postValue(it)
            launchCrop.launch(
                PhotoCropVisualMediaRequestBuilder(it, outputUri)
                    .withAspectRatio(1f, 1f)
                    .build()
            )
//            toSystemCropActivity(it)
        }
    }
    val state = mutableStateOf<Uri?>(null)
    var stateUris = mutableStateOf<List<Uri>?>(null)
    val launchCrop = registerForActivityResult(PhotoCropVisualMedia()) {
        val uris = it.data?.getParcelableArrayListExtraCompat(MediaStore.EXTRA_OUTPUT, Uri::class.java)
        if (uris.isNonEmpty()) {
            Log.d("MainActivity->Crop", "Uri=${uris}")
            stateUris.value = uris
            return@registerForActivityResult
        }
        val uri = it.data?.data
        Log.d("MainActivity->Crop", "Uri=${uri}")
        if (uri != null) {
//            state.value = (it.data?.data)
            viewModel.state.postValue(it.data?.data)
        }
    }
    val launchSystemCrop = registerForActivityResult(SytemPhotoCropVisualMedia()) {
        val uris = it.data?.getParcelableArrayListExtraCompat(MediaStore.EXTRA_OUTPUT, Uri::class.java)
        if (uris.isNonEmpty()) {
            Log.d("MainActivity->Crop", "Uri=${uris}")
            stateUris.value = uris
            return@registerForActivityResult
        }
        val uri = it.data?.data
        Log.d("MainActivity->Crop", "Uri=${uri}")
        if (uri != null) {
//            state.value = (it.data?.data)
            viewModel.state.postValue(it.data?.data)
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
        state.value = (Uri.parse("content://media/external/images/media/151"))
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
                    Greeting()
                    MyComposeScreen()
                    ListViewComposable(stateUris.value)
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
            launchSystemCrop.launch(SystemPhotoCropVisualMediaRequestBuilder(mImageUri, outputUri!!).build())
        }
    }

    @Composable
    fun MyComposeScreen() {
        val uri = state.value
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

    @Composable
    fun ListViewComposable(uris: List<Uri>?) {
        Column {
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

    var selectedUris: ArrayList<Uri> = arrayListOf()

    @Composable
    fun CustomImageViewWrapper() {
        Text(text = "单选相册", color = Color.White, fontSize = 20.sp, modifier = Modifier.clickable {
            launchImage.launch(
                PhotoVisualMediaRequestBuilder(PhotoVisualMedia.ImageAndVideo)
                    .setForceCustomUi(false)
                    .setShowGif(true)
                    .build()
            )
        })
        Text(text = "多选相册", color = Color.White, fontSize = 20.sp, modifier = Modifier.clickable {
            launchMultipleImage.launch(
                PhotoVisualMediaRequestBuilder(PhotoVisualMedia.ImageAndVideo)
                    .setForceCustomUi(false)
                    .setSelectedUris(selectedUris)
                    .setShowGif(false)
                    .build()
            )
        })
        Text(text = "裁剪图片", color = Color.White, fontSize = 20.sp, modifier = Modifier.clickable {
            val outputFile = "IMG_".createFile("jpg")
            val outputUri = Uri.fromFile(outputFile)
            //content://media/external/images/1000000126
            launchCrop.launch(
                PhotoCropVisualMediaRequestBuilder(
                    Uri.parse("content://media/external/images/media/1000000126"),
                    outputUri
                ).withAspectRatio(1f, 1f)
                    .build()
            )
        })
        Text(text = "系统裁剪图片", color = Color.White, fontSize = 20.sp, modifier = Modifier.clickable {
            //content://media/external/images/1000000126
            val uri = Uri.parse("content://media/picker/0/com.android.providers.media.photopicker/media/1000000126")
            toSystemCropActivity(uri)
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
                        text = it.toString()
                    }
                }
            },
        )
//        Text(text = state.value.toString())
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        RootView()
    }
}


class ViewModelUri : ViewModel() {
    val state = MutableLiveData<Uri?>(null)
}