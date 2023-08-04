package com.peihua.selector.result

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.*
fun SystemPhotoCropVisualMediaRequestBuilder(
    inputUri: Uri, outputUri: Uri,
    mediaType: VisualMediaType = ImageOnly
) = SystemPhotoCropVisualMediaRequest.Builder().setMediaType(mediaType).setInputUri(inputUri)
    .setOutputUri(outputUri)
fun SystemPhotoCropVisualMediaRequest(
    inputUri: Uri, outputUri: Uri,
    mediaType: VisualMediaType = ImageOnly
) = SystemPhotoCropVisualMediaRequest.Builder().setMediaType(mediaType).setInputUri(inputUri)
    .setOutputUri(outputUri).build()

class SystemPhotoCropVisualMediaRequest internal constructor() {

    var mediaType: VisualMediaType =
        ImageAndVideo
        internal set
    var scaleUpIfNeeded: Boolean = true
        internal set
    var scale: Boolean = true
        internal set
    var crop: Boolean = true
        internal set
    var aspectX: Float = 1F
        internal set
    var aspectY: Float = 1F
        internal set
    var outputX: Float = 150F
        internal set
    var outputY: Float = 150F
        internal set
    var inputUri: Uri = Uri.parse("")
        internal set
    var outputUri: Uri = Uri.parse("")
        internal set
    var outputFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
        internal set
    var circleCrop: Boolean = false
        internal set

    /**
     * A builder for constructing [PickCropVisualMediaRequest] instances.
     */
    class Builder {
        private var mediaType: VisualMediaType = ImageAndVideo
        private var scaleUpIfNeeded: Boolean = true
        private var scale: Boolean = true
        private var crop: Boolean = true
        private var aspectX: Float = 1F
        private var aspectY: Float = 1F
        private var outputX: Float = 150F
        private var outputY: Float = 150F
        private var inputUri: Uri = Uri.parse("")
        private var outputUri: Uri = Uri.parse("")
        private var outputFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
        private var circleCrop: Boolean = false

        /**
         * Set the media type for the [PickVisualMediaRequest].
         *
         * The type is the mime type to filter by, e.g. `PickVisualMedia.ImageOnly`,
         * `PickVisualMedia.ImageAndVideo`, `PickVisualMedia.SingleMimeType("image/gif")`
         *
         * @param mediaType type to go into the PickVisualMediaRequest
         * @return This builder.
         */
        fun setMediaType(mediaType: VisualMediaType): Builder {
            this.mediaType = mediaType
            return this
        }

        fun setScaleUpIfNeeded(scaleUpIfNeeded: Boolean): Builder {
            this.scaleUpIfNeeded = scaleUpIfNeeded
            return this
        }

        fun setScale(scale: Boolean): Builder {
            this.scale = scale
            return this
        }

        fun setCrop(crop: Boolean): Builder {
            this.crop = crop
            return this
        }
        fun setCircleCrop(circleCrop: Boolean): Builder {
            this.circleCrop = circleCrop
            return this
        }
        fun setAspectX(aspectX: Float): Builder {
            this.aspectX = aspectX
            return this
        }

        fun setAspectY(aspectY: Float): Builder {
            this.aspectY = aspectY
            return this
        }

        fun setOutputX(outputX: Float): Builder {
            this.outputX = outputX
            return this
        }

        fun setOutputY(outputY: Float): Builder {
            this.outputY = outputY
            return this
        }

        fun setInputUri(inputUri: Uri): Builder {
            this.inputUri = inputUri
            return this
        }

        fun setOutputUri(outputUri: Uri): Builder {
            this.outputUri = outputUri
            return this
        }

        fun setOutputFormat(outputFormat: Bitmap.CompressFormat): Builder {
            this.outputFormat = outputFormat
            return this
        }

        /**
         * Build the PickVisualMediaRequest specified by this builder.
         *
         * @return the newly constructed PickVisualMediaRequest.
         */
        fun build(): SystemPhotoCropVisualMediaRequest = SystemPhotoCropVisualMediaRequest().apply {
            this.mediaType = this@Builder.mediaType
            this.scaleUpIfNeeded = this@Builder.scaleUpIfNeeded
            this.scale = this@Builder.scale
            this.crop = this@Builder.crop
            this.aspectX = this@Builder.aspectX
            this.aspectY = this@Builder.aspectY
            this.outputX = this@Builder.outputX
            this.outputY = this@Builder.outputY
            this.inputUri = this@Builder.inputUri
            this.outputUri = this@Builder.outputUri
            this.outputFormat = this@Builder.outputFormat
            this.circleCrop = this@Builder.circleCrop
        }
    }
}