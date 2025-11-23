package com.peihua8858.selector.picker.result.contract

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper
import com.peihua8858.selector.crop.UCrop
import com.peihua8858.selector.picker.result.PhotoCropVisualMediaRequest

/**
 * 图片裁剪
 * @author dingpeihua
 * @date 2023/8/3 10:41
 * @version 1.0
 */
open class PhotoCropVisualMedia : ActivityResultContract<PhotoCropVisualMediaRequest, ActivityResult>() {

    @CallSuper
    override fun createIntent(context: Context, input: PhotoCropVisualMediaRequest): Intent {
        val inputUris = input.inputUris
        val uCrop = if (inputUris.size > 1) {
            UCrop.of(inputUris, input.outputUri)
        } else {
            if (input.inputUri == Uri.EMPTY && inputUris.size == 1) {
                UCrop.of(inputUris[0], input.outputUri)
            } else {
                UCrop.of(input.inputUri, input.outputUri)
            }
        }
        uCrop.withOptions(input.options)
        return uCrop.getIntent(context)
    }

    @Suppress("InvalidNullabilityOverride")
    final override fun getSynchronousResult(
        context: Context,
        input: PhotoCropVisualMediaRequest
    ): SynchronousResult<ActivityResult>? = null

    final override fun parseResult(resultCode: Int, intent: Intent?): ActivityResult {
        return ActivityResult(resultCode, intent)
    }

    companion object {
        const val EXTRA_OUTPUT_WHOLE_DATA = "EXTRA_OUTPUT_WHOLE_DATA"
        internal const val ACTION_CROP_IMAGES: String = "android.provider.selector.action.CROP_IMAGES"
        internal const val ACTION_MULTIPLE_CROP_IMAGES = "android.provider.selector.action.MULTIPLE_CROP_IMAGES"
    }
}
