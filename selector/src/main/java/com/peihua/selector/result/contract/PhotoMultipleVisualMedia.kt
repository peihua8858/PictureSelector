package com.peihua.selector.result.contract

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.Companion.EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_MAX
import androidx.annotation.CallSuper
import com.peihua.selector.data.Selection
import com.peihua.selector.result.PhotoVisualMediaRequest
import com.peihua.selector.result.contract.PhotoVisualMedia.Companion.ACTION_SYSTEM_FALLBACK_PICK_IMAGES
import com.peihua.selector.result.contract.PhotoVisualMedia.Companion.getVisualMimeType
import com.peihua.selector.result.contract.PhotoVisualMedia.Companion.isGmsPickerAvailable
import com.peihua.selector.result.contract.PhotoVisualMedia.Companion.isSystemFallbackPickerAvailable
import com.peihua.selector.result.contract.PhotoVisualMedia.Companion.isSystemPickerAvailable
import com.peihua.selector.util.getClipDataUris

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

        return if (isSystemPickerAvailable()) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                type = getVisualMimeType(input.mediaType)
                require(maxItems <= MediaStore.getPickImagesMaxLimit()) {
                    "Max items must be less or equals MediaStore.getPickImagesMaxLimit()"
                }

                putExtra(MediaStore.EXTRA_PICK_IMAGES_MAX, maxItems)
            }
        } else if (isSystemFallbackPickerAvailable(context)) {
            val fallbackPicker = checkNotNull(PhotoVisualMedia.getSystemFallbackPicker(context)).activityInfo
            Intent(PickVisualMedia.ACTION_SYSTEM_FALLBACK_PICK_IMAGES).apply {
                setClassName(fallbackPicker.applicationInfo.packageName, fallbackPicker.name)
                type = getVisualMimeType(input.mediaType)
                putExtra(EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_MAX, maxItems)
            }
        } else if (isGmsPickerAvailable(context)) {
            val gmsPicker = checkNotNull(PhotoVisualMedia.getGmsPicker(context)).activityInfo
            Intent(PhotoVisualMedia.GMS_ACTION_PICK_IMAGES).apply {
                setClassName(gmsPicker.applicationInfo.packageName, gmsPicker.name)
                putExtra(PhotoVisualMedia.GMS_EXTRA_PICK_IMAGES_MAX, maxItems)
            }
        } else {
            // For older devices running KitKat and higher and devices running Android 12
            // and 13 without the SDK extension that includes the Photo Picker, rely on the
            // ACTION_OPEN_DOCUMENT intent
            Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = getVisualMimeType(input.mediaType)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)

                if (type == null) {
                    // ACTION_OPEN_DOCUMENT requires to set this parameter when launching the
                    // intent with multiple mime types
                    type = "*/*"
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                }
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
        internal fun getMaxItems() = if (isSystemPickerAvailable()) {
            MediaStore.getPickImagesMaxLimit()
        } else {
            Selection.PICK_IMAGES_MAX_LIMIT
        }
    }
}