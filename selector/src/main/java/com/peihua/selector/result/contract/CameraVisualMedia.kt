package com.peihua.selector.result.contract

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper
import com.peihua.selector.result.TakeCameraVisualMediaRequest

open class CameraVisualMedia : ActivityResultContract<TakeCameraVisualMediaRequest, ActivityResult>() {
    @CallSuper
    override fun createIntent(context: Context, input: TakeCameraVisualMediaRequest): Intent {
        // Check if Photo Picker is available on the device
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, input.outputFile) //将用于输出的文件Uri传递给相机
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
