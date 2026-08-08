package com.peihua.selector.result

import android.graphics.Bitmap
import android.net.Uri
import com.peihua.selector.result.contract.PhotoVisualMedia

fun TakeCameraVisualMediaRequest(outputFile: Uri,
    mediaType: PhotoVisualMedia.VisualMediaType = PhotoVisualMedia.ImageOnly
) = TakeCameraVisualMediaRequest.Builder().setMediaType(mediaType)
    .setOutputFile(outputFile).build()

class TakeCameraVisualMediaRequest internal constructor() {

    var mediaType: PhotoVisualMedia.VisualMediaType =
        PhotoVisualMedia.ImageAndVideo
        internal set
    var outputFile: Uri = Uri.EMPTY
        internal set

    /**
     * A builder for constructing [PickCropVisualMediaRequest] instances.
     */
    class Builder {
        private var mediaType: PhotoVisualMedia.VisualMediaType = PhotoVisualMedia.ImageAndVideo
        private var outputFile: Uri = Uri.EMPTY
        private var outputFormat: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG

        /**
         * Set the media type for the [PickVisualMediaRequest].
         *
         * The type is the mime type to filter by, e.g. `PickVisualMedia.ImageOnly`,
         * `PickVisualMedia.ImageAndVideo`, `PickVisualMedia.SingleMimeType("image/gif")`
         *
         * @param mediaType type to go into the PickVisualMediaRequest
         * @return This builder.
         */
        fun setMediaType(mediaType: PhotoVisualMedia.VisualMediaType): Builder {
            this.mediaType = mediaType
            return this
        }


        fun setOutputFormat(outputFormat: Bitmap.CompressFormat): Builder {
            this.outputFormat = outputFormat
            return this
        }
        fun setOutputFile(outputFile: Uri): Builder {
            this.outputFile = outputFile
            return this
        }
        /**
         * Build the PickVisualMediaRequest specified by this builder.
         *
         * @return the newly constructed PickVisualMediaRequest.
         */
        fun build(): TakeCameraVisualMediaRequest = TakeCameraVisualMediaRequest().apply {
            this.mediaType = this@Builder.mediaType
            this.outputFile = this@Builder.outputFile
        }
    }
}