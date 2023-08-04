package com.peihua.selector.result

import android.graphics.Bitmap
import android.net.Uri
import com.peihua.selector.crop.UCrop
import com.peihua.selector.result.contract.PhotoVisualMedia

fun PhotoCropVisualMediaRequestBuilder(
    inputUri: Uri, outputUri: Uri,
) = PhotoCropVisualMediaRequest.Builder().setInputUri(inputUri)
    .setOutputUri(outputUri)

fun PhotoCropVisualMediaRequestBuilder(
    inputUris: ArrayList<Uri>,
    outputUri: Uri,
) = PhotoCropVisualMediaRequest.Builder()
    .setOutputUri(outputUri).setInputUris(inputUris)

fun PhotoCropVisualMediaRequest(
    inputUri: Uri, outputUri: Uri,
    mediaType: PhotoVisualMedia.VisualMediaType = PhotoVisualMedia.ImageOnly
) = PhotoCropVisualMediaRequest.Builder().setMediaType(mediaType).setInputUri(inputUri)
    .setOutputUri(outputUri).build()

class PhotoCropVisualMediaRequest internal constructor() {

    var mediaType: PhotoVisualMedia.VisualMediaType =
        PhotoVisualMedia.ImageOnly
        internal set
    var options: UCrop.Options<Builder> = Builder()
        internal set
    var inputUri: Uri = Uri.EMPTY
        internal set
    var outputUri: Uri = Uri.EMPTY
        internal set
    var outputFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
        internal set
    var inputUris: ArrayList<Uri> = arrayListOf()
        internal set
    /**
     * A builder for constructing [PickCropVisualMediaRequest] instances.
     */
    class Builder : UCrop.Options<Builder>() {
        private var mediaType: PhotoVisualMedia.VisualMediaType = PhotoVisualMedia.ImageOnly
        private var inputUri: Uri = Uri.EMPTY
        private var outputUri: Uri = Uri.EMPTY
        private var outputFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
        private var inputUris: ArrayList<Uri> = arrayListOf()

        /**
         * Set the media type for the [PickVisualMediaRequest].
         *
         * The type is the mime type to filter by, e.g. `PickVisualMedia.ImageOnly`,
         * `PickVisualMedia.ImageAndVideo`, `PickVisualMedia.SingleMimeType("image/gif")`
         *
         * @param mediaType type to go into the PickVisualMediaRequest
         * @return This builder.
         */
        internal fun setMediaType(mediaType: PhotoVisualMedia.VisualMediaType): Builder {
            this.mediaType = mediaType
            return this
        }

        fun setInputUris(inputUris: ArrayList<Uri>): Builder {
            this.inputUris = inputUris
            return this
        }

        fun addInputUri(inputUri: Uri): Builder {
            this.inputUris.add(inputUri)
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

        override fun self(): Builder {
            return this
        }

        /**
         * Build the PickVisualMediaRequest specified by this builder.
         *
         * @return the newly constructed PickVisualMediaRequest.
         */
        fun build(): PhotoCropVisualMediaRequest = PhotoCropVisualMediaRequest().apply {
            this.mediaType = this@Builder.mediaType
            this.inputUri = this@Builder.inputUri
            this.inputUris = this@Builder.inputUris
            this.outputUri = this@Builder.outputUri
            this.outputFormat = this@Builder.outputFormat
            this.options = this@Builder
        }
    }
}