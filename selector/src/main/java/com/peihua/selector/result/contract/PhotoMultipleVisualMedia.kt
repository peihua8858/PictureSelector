package com.peihua.selector.result.contract

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.CallSuper
import com.fz.common.utils.dLog
import com.peihua.selector.data.Selection
import com.peihua.selector.result.PhotoVisualMediaRequest
import com.peihua.selector.result.contract.PhotoVisualMedia.Companion.ACTION_SYSTEM_FALLBACK_PICK_IMAGES
import com.peihua.selector.result.contract.PhotoVisualMedia.Companion.getClipDataUris

class PhotoMultipleVisualMedia(
    private val maxItems: Int = getMaxItems()
) : ActivityResultContract<PhotoVisualMediaRequest, List<@JvmSuppressWildcards Uri>>() {

    init {
        require(maxItems > 1) { "Max items must be higher than 1" }
    }

    @CallSuper
    @SuppressLint("NewApi", "ClassVerificationFailure")
    override fun createIntent(context: Context, input: PhotoVisualMediaRequest): Intent {
        var maxItems = input.maxItems
        if (maxItems <= 0) {
            maxItems = this.maxItems
        }
        require(maxItems > 0) { "Max items must be higher than 0" }
        dLog { "selectedUris>>>00000mSelection<><><><><>" }
        // Check to see if the photo picker is available
        return if (input.isForceCustomUi || input.mediaType is PhotoVisualMedia.MultipleMimeType) {
            dLog { "selectedUris>>>00000mSelection<><><><><>" }
            PhotoVisualMedia.createCustomIntent(context, input).apply {
                if (maxItems > 1) putExtra(PhotoVisualMedia.EXTRA_PICK_IMAGES_MAX, maxItems)
            }
        } else if (PhotoVisualMedia.isSystemPickerAvailable()) {
            dLog { "selectedUris>>>00000mSelection<><><><><>" }
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = PhotoVisualMedia.getVisualMimeType(input.mediaType)
                require(maxItems <= MediaStore.getPickImagesMaxLimit()) {
                    "Max items must be less or equals MediaStore.getPickImagesMaxLimit()"
                }
                if (maxItems > 1) putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, maxItems)
            }
        } else if (PhotoVisualMedia.isSystemFallbackPickerAvailable(context)) {
            dLog { "selectedUris>>>00000mSelection<><><><><>" }
            val fallbackPicker = checkNotNull(PhotoVisualMedia.getSystemFallbackPicker(context)).activityInfo
            Intent(ACTION_SYSTEM_FALLBACK_PICK_IMAGES).apply {
                setClassName(fallbackPicker.applicationInfo.packageName, fallbackPicker.name)
                type = PhotoVisualMedia.getVisualMimeType(input.mediaType)
                if (maxItems > 1) putExtra(PhotoVisualMedia.GMS_EXTRA_PICK_IMAGES_MAX, maxItems)
            }
        } else if (PhotoVisualMedia.isGmsPickerAvailable(context)) {
            dLog { "selectedUris>>>00000mSelection<><><><><>" }
            val gmsPicker = checkNotNull(PhotoVisualMedia.getGmsPicker(context)).activityInfo
            Intent(PhotoVisualMedia.GMS_ACTION_PICK_IMAGES).apply {
                setClassName(gmsPicker.applicationInfo.packageName, gmsPicker.name)
                if (maxItems > 1) putExtra(PhotoVisualMedia.GMS_EXTRA_PICK_IMAGES_MAX, maxItems)
            }
        } else {
            dLog { "selectedUris>>>00000mSelection<><><><><>" }
            PhotoVisualMedia.createCustomIntent(context, input).apply {
                if (maxItems > 1) putExtra(PhotoVisualMedia.EXTRA_PICK_IMAGES_MAX, maxItems)
            }
        }
    }

    @Suppress("InvalidNullabilityOverride")
    override fun getSynchronousResult(
        context: Context,
        input: PhotoVisualMediaRequest
    ): SynchronousResult<List<@JvmSuppressWildcards Uri>>? = null

    final override fun parseResult(resultCode: Int, intent: Intent?): List<Uri> {
        return intent.takeIf {
            resultCode == Activity.RESULT_OK
        }?.getClipDataUris() ?: emptyList()
    }

    internal companion object {
        /**
         * The system photo picker has a maximum limit of selectable items returned by
         * [MediaStore.getPickImagesMaxLimit()]
         * On devices supporting picker provided via [ACTION_SYSTEM_FALLBACK_PICK_IMAGES],
         * the limit may be ignored if it's higher than the allowed limit.
         * On devices not supporting the photo picker, the limit is ignored.
         *
         * @see MediaStore.EXTRA_PICK_IMAGES_MAX
         */
        @SuppressLint("NewApi", "ClassVerificationFailure")
        internal fun getMaxItems() = if (PhotoVisualMedia.isSystemPickerAvailable()) {
            MediaStore.getPickImagesMaxLimit()
        } else {
            Selection.PICK_IMAGES_MAX_LIMIT
        }
    }
}