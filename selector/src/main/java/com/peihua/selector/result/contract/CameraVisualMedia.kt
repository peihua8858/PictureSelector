package com.peihua.selector.result.contract

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.*
import androidx.annotation.CallSuper
import androidx.core.content.FileProvider
import com.peihua.selector.result.TakeCameraVisualMediaRequest

open class CameraVisualMedia : ActivityResultContract<TakeCameraVisualMediaRequest, ActivityResult>() {
    companion object {
        /**
         * Check if the current device has support for the photo picker by checking the running
         * Android version or the SDK extension version
         */
        @SuppressLint("ClassVerificationFailure", "NewApi")
        @JvmStatic
        fun isPhotoPickerAvailable(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                true
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // getExtension is seen as part of Android Tiramisu only while the SdkExtensions
                // have been added on Android R
                SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2
            } else {
                false
            }
        }

        internal fun getVisualMimeType(input: VisualMediaType): String? {
            return when (input) {
                is ImageOnly -> "image/*"
                is VideoOnly -> "video/*"
                is SingleMimeType -> input.mimeType
                is ImageAndVideo -> null
            }
        }
    }


    @CallSuper
    override fun createIntent(context: Context, input: TakeCameraVisualMediaRequest): Intent {
        // Check if Photo Picker is available on the device
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            val mImageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                /*7.0以上要通过FileProvider将File转化为Uri*/
                FileProvider.getUriForFile(context, "AppConfigs.AUTHORITY", input.outputFile)
            } else {
                /*7.0以下则直接使用Uri的fromFile方法将File转化为Uri*/
                Uri.fromFile(input.outputFile)
            }
            putExtra(MediaStore.EXTRA_OUTPUT, mImageUri) //将用于输出的文件Uri传递给相机
        }
    }

    @Suppress("InvalidNullabilityOverride")
    final override fun getSynchronousResult(
        context: Context,
        input: TakeCameraVisualMediaRequest
    ): SynchronousResult<ActivityResult>? = null

    final override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult {
        return ActivityResult(resultCode,intent)
    }
}
