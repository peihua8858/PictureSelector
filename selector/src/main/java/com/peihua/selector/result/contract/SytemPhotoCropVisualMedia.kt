package com.peihua.selector.result.contract

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageAndVideo
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.SingleMimeType
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VideoOnly
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.VisualMediaType
import androidx.annotation.CallSuper
import com.peihua.selector.crop.UCrop
import com.peihua.selector.result.PhotoCropVisualMediaRequest
import com.peihua.selector.result.SystemPhotoCropVisualMediaRequest

/**
 * 系统裁剪Contract
 * @author dingpeihua
 * @date 2023/7/31 17:28
 * @version 1.0
 */
open class SytemPhotoCropVisualMedia : ActivityResultContract<SystemPhotoCropVisualMediaRequest, ActivityResult>() {
    companion object {
        const val ACTION_SYSTEM_CROP = "com.android.camera.action.CROP"
        internal fun getVisualMimeType(input: VisualMediaType): String? {
            return when (input) {
                is ImageOnly -> "image/*"
                is VideoOnly -> "video/*"
                is SingleMimeType -> input.mimeType
                is ImageAndVideo -> null
            }
        }

        @JvmStatic
        internal fun isSystemFallbackCorpAvailable(context: Context, input: SystemPhotoCropVisualMediaRequest): Boolean {
            return getSystemFallbackCrop(context, input.inputUri)!=null
        }

        @JvmStatic
        internal fun getSystemFallbackCrop(context: Context, uri: Uri): ResolveInfo? {
            val intent = Intent(ACTION_SYSTEM_CROP)
            intent.setDataAndType(uri, "image/*")
            val result = context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY or PackageManager.MATCH_SYSTEM_ONLY
            )
            return result
        }
    }


    @CallSuper
    override fun createIntent(context: Context, input: SystemPhotoCropVisualMediaRequest): Intent {
        // Check if Photo Picker is available on the device
        if (isSystemFallbackCorpAvailable(context, input = input)) {
            return Intent(ACTION_SYSTEM_CROP).apply {
                setDataAndType(input.inputUri, getVisualMimeType(input.mediaType))
                // 授权应用读取 Uri，这一步要有，不然裁剪程序会崩溃
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                //去黑边
                putExtra("scaleUpIfNeeded", input.scaleUpIfNeeded)
                //设置缩放
                putExtra("scale", input.scale)
                //显示View为可裁剪的
                putExtra("crop", input.crop)
                putExtra("circleCrop", input.circleCrop)
                //裁剪的宽高的比例为1:1
                putExtra("aspectX", input.aspectX)
                putExtra("aspectY", input.aspectY)
                //输出图片的宽高均为150
                putExtra("outputX", input.outputX)
                putExtra("outputY", input.outputY)
                // 设置图片的最终输出目录
                putExtra(MediaStore.EXTRA_OUTPUT, input.outputUri)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                putExtra("outputFormat", input.outputFormat.toString())
                putExtra("return-data", false)
            }
        } else if (input.isAutoCustomCorp) {
            val uCrop = UCrop.of(input.inputUri, input.outputUri)
            val inputMediaType = input.mediaType
            val mediaType = when (inputMediaType) {
                is ImageOnly -> PhotoVisualMedia.ImageOnly
                is VideoOnly -> PhotoVisualMedia.VideoOnly
                is ImageAndVideo -> PhotoVisualMedia.ImageAndVideo
                is SingleMimeType -> PhotoVisualMedia.SingleMimeType(inputMediaType.mimeType)
            }
            val options = PhotoCropVisualMediaRequest.Builder()
                .setMediaType(mediaType)
                .setInputUri(input.inputUri)
                .withAspectRatio(input.aspectX, input.aspectY)
                .withMaxResultSize(input.outputX.toInt(), input.outputY.toInt())
                .build().options
            uCrop.withOptions(options)
            return uCrop.getIntent(context)
        } else {
            throw IllegalStateException("the action $ACTION_SYSTEM_CROP not available")
        }
    }

    @Suppress("InvalidNullabilityOverride")
    final override fun getSynchronousResult(
        context: Context,
        input: SystemPhotoCropVisualMediaRequest,
    ): SynchronousResult<ActivityResult>? = null

    final override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult {
        return ActivityResult(resultCode, intent)
    }
}
