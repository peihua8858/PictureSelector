package com.peihua.selector.result.contract

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.os.ext.SdkExtensions
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.*
import androidx.annotation.CallSuper
import com.fz.common.array.splicing
import com.peihua.selector.photos.PhotoPickerActivity
import com.peihua.selector.result.PhotoVisualMediaRequest
import kotlinx.parcelize.Parcelize

open class PhotoVisualMedia : ActivityResultContract<PhotoVisualMediaRequest, Uri?>() {
    companion object {
        /**
         * Check if the current device has support for the photo picker by checking the running
         * Android version or the SDK extension version.
         *
         * Note that this does not check for any Intent handled by
         * [ACTION_SYSTEM_FALLBACK_PICK_IMAGES].
         */
        @SuppressLint("ClassVerificationFailure", "NewApi")
        @Deprecated(
            message = "This method is deprecated in favor of isPhotoPickerAvailable(context) " +
                    "to support the picker provided by updatable system apps",
            replaceWith = ReplaceWith("isPhotoPickerAvailable(context)")
        )
        @JvmStatic
        fun isPhotoPickerAvailable(): Boolean {
            return isSystemPickerAvailable()
        }

        /**
         * In cases where the system framework provided [MediaStore.ACTION_PICK_IMAGES]
         * Photo Picker cannot be implemented, OEMs or system apps can provide a consistent
         * Photo Picker experience to those devices by creating an Activity that handles
         * this action. This app must also include [Intent.CATEGORY_DEFAULT] in the activity's
         * intent filter.
         *
         * Only system apps can implement this action - any non-system apps will be ignored
         * when searching for the activities that handle this Intent.
         *
         * Note: this should not be used directly, instead relying on the selection logic
         * done by [createIntent] to create the correct Intent for the current device.
         */
        @Suppress("ActionValue")
        /* Don't include SYSTEM_FALLBACK in the action */
        const val ACTION_SYSTEM_FALLBACK_PICK_IMAGES =
            "androidx.activity.result.contract.action.PICK_IMAGES"

        /**
         * Extra that will be sent by [PickMultipleVisualMedia] to an Activity that handles
         * [ACTION_SYSTEM_FALLBACK_PICK_IMAGES] that indicates that maximum number of photos
         * the user should select.
         *
         * If this extra is not present, only a single photo should be selectable.
         *
         * If this extra is present but equal to [Int.MAX_VALUE], then no limit should
         * be enforced.
         */
        @Suppress("ActionValue")
        /* Don't include SYSTEM_FALLBACK in the extra */
        const val EXTRA_SYSTEM_FALLBACK_PICK_IMAGES_MAX =
            "androidx.activity.result.contract.extra.PICK_IMAGES_MAX"

        internal const val GMS_ACTION_PICK_IMAGES =
            "com.google.android.gms.provider.action.PICK_IMAGES"
        internal const val GMS_EXTRA_PICK_IMAGES_MAX =
            "com.google.android.gms.provider.extra.PICK_IMAGES_MAX"
        const val EXTRA_PICK_IMAGES_MAX = MediaStore.EXTRA_PICK_IMAGES_MAX

        /**
         * 已选中图片列表
         */
        const val EXTRA_SELECTED_PICK_IMAGES = "SELECTED_PICK_IMAGES"

        /**
         * Check if the current device has support for the photo picker by checking the running
         * Android version, the SDK extension version or the picker provided by
         * a system app implementing [ACTION_SYSTEM_FALLBACK_PICK_IMAGES].
         */
        @SuppressLint("ClassVerificationFailure", "NewApi")
        @JvmStatic
        fun isPhotoPickerAvailable(context: Context): Boolean {
            return isSystemPickerAvailable() || isSystemFallbackPickerAvailable(context) ||
                    isGmsPickerAvailable(context)
        }

        /**
         * Check if the current device has support for the system framework provided photo
         * picker by checking the running Android version or the SDK extension version.
         *
         * Note that this does not check for any Intent handled by
         * [ACTION_SYSTEM_FALLBACK_PICK_IMAGES].
         */
        @SuppressLint("ClassVerificationFailure", "NewApi")
        @JvmStatic
        internal fun isSystemPickerAvailable(): Boolean {
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

        @JvmStatic
        internal fun isSystemFallbackPickerAvailable(context: Context): Boolean {
            return getSystemFallbackPicker(context) != null
        }

        @Suppress("DEPRECATION")
        @JvmStatic
        internal fun getSystemFallbackPicker(context: Context): ResolveInfo? {
            return context.packageManager.resolveActivity(
                Intent(ACTION_SYSTEM_FALLBACK_PICK_IMAGES),
                PackageManager.MATCH_DEFAULT_ONLY or PackageManager.MATCH_SYSTEM_ONLY
            )
        }

        @JvmStatic
        internal fun isGmsPickerAvailable(context: Context): Boolean {
            return getGmsPicker(context) != null
        }

        @Suppress("DEPRECATION")
        @JvmStatic
        internal fun getGmsPicker(context: Context): ResolveInfo? {
            return context.packageManager.resolveActivity(
                Intent(GMS_ACTION_PICK_IMAGES),
                PackageManager.MATCH_DEFAULT_ONLY or PackageManager.MATCH_SYSTEM_ONLY
            )
        }

        internal fun getVisualMimeType(input: VisualMediaType): String? {
            return when (input) {
                is ImageOnly -> "image/*"
                is VideoOnly -> "video/*"
                is AudioOnly -> "audio/*"
                is SingleMimeType -> input.mimeTypes[0]
                is MultipleMimeType -> "*/*"
                is ImageAndVideo -> "*/*"
            }
        }

        internal fun Intent.getClipDataUris(): List<Uri> {
            // Use a LinkedHashSet to maintain any ordering that may be
            // present in the ClipData
            val resultSet = LinkedHashSet<Uri>()
            data?.let { data ->
                resultSet.add(data)
            }
            val clipData = clipData
            if (clipData == null && resultSet.isEmpty()) {
                return emptyList()
            } else if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    if (uri != null) {
                        resultSet.add(uri)
                    }
                }
            }
            return ArrayList(resultSet)
        }

        internal fun createCustomIntent(context: Context, input: PhotoVisualMediaRequest): Intent {
            return Intent(context, PhotoPickerActivity::class.java).apply {
                Intent.EXTRA_CHOOSER_RESULT
                putExtra(EXTRA_SELECTED_PICK_IMAGES, input.selectedUris)
                type ="*/*"
                val mimeTypes = input.mediaType.mimeTypes
                if (mimeTypes.isEmpty()) {
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*"))
                } else {
                    putExtra(Intent.EXTRA_MIME_TYPES, input.mediaType.mimeTypes)
                }
                putExtra(Intent.EXTRA_INTENT, input.configModel)
            }
        }
    }

    /**
     * Represents filter input type accepted by the photo picker.
     */
    @Parcelize
    sealed class VisualMediaType(vararg val mimeTypes: String) : Parcelable

    /**
     * [VisualMediaType] object used to filter images only when using the photo picker.
     */
    @Parcelize
    object ImageOnly : VisualMediaType("image/*")

    /**
     * [VisualMediaType] object used to filter video only when using the photo picker.
     */
    @Parcelize
    object VideoOnly : VisualMediaType("video/*")

    /**
     * [VisualMediaType] object used to filter video only when using the photo picker.
     */
    @Parcelize
    object AudioOnly : VisualMediaType("video/*")

    /**
     * [VisualMediaType] object used to filter images and video when using the photo picker.
     */
    @Parcelize
    object ImageAndVideo : VisualMediaType("image/*", "video/*")

    /**
     * [VisualMediaType] class used to filter a single mime type only when using the photo
     * picker.
     */
    @Parcelize
    class SingleMimeType(val v: String) : VisualMediaType(v)

    /**
     * [VisualMediaType] class used to filter a single mime type only when using the photo
     * picker.
     */
    @Parcelize
    class MultipleMimeType(vararg val mimeType: String) : VisualMediaType(*mimeType)

    @CallSuper
    override fun createIntent(context: Context, input: PhotoVisualMediaRequest): Intent {
        // Check if Photo Picker is available on the device
        return if (input.isForceCustomUi || input.mediaType is MultipleMimeType) {
            createCustomIntent(context, input)
        } else if (isSystemPickerAvailable()) {
            Intent(MediaStore.ACTION_PICK_IMAGES).apply {
                setTypeAndNormalize(getVisualMimeType(input.mediaType))
                putExtra(Intent.EXTRA_MIME_TYPES, input.mediaType.mimeTypes)
            }
        } else if (isSystemFallbackPickerAvailable(context)) {
            val fallbackPicker = checkNotNull(getSystemFallbackPicker(context)).activityInfo
            Intent(ACTION_SYSTEM_FALLBACK_PICK_IMAGES).apply {
                setClassName(fallbackPicker.applicationInfo.packageName, fallbackPicker.name)
                setTypeAndNormalize(getVisualMimeType(input.mediaType))
                putExtra(Intent.EXTRA_MIME_TYPES, input.mediaType.mimeTypes)
            }
        } else if (isGmsPickerAvailable(context)) {
            val gmsPicker = checkNotNull(getGmsPicker(context)).activityInfo
            Intent(GMS_ACTION_PICK_IMAGES).apply {
                setClassName(gmsPicker.applicationInfo.packageName, gmsPicker.name)
                setTypeAndNormalize(getVisualMimeType(input.mediaType))
                putExtra(Intent.EXTRA_MIME_TYPES, input.mediaType.mimeTypes)
            }
        } else {
            createCustomIntent(context, input)
        }
    }

    @Suppress("InvalidNullabilityOverride")
    final override fun getSynchronousResult(
        context: Context,
        input: PhotoVisualMediaRequest,
    ): SynchronousResult<Uri?>? = null

    final override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return intent.takeIf { resultCode == Activity.RESULT_OK }?.run {
            // Check both the data URI and ClipData since the GMS picker
            // only returns results through getClipDataUris()
            data ?: getClipDataUris().firstOrNull()
        }
    }
}
