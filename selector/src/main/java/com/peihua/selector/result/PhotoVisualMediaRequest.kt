package com.peihua.selector.result

import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import com.fz.common.utils.dLog
import com.peihua.selector.data.model.ConfigModel
import com.peihua.selector.result.contract.PhotoVisualMedia

fun PhotoVisualMediaRequestBuilder(
    mediaType: PhotoVisualMedia.VisualMediaType = PhotoVisualMedia.ImageOnly
) = PhotoVisualMediaRequest.Builder().setMediaType(mediaType)

fun PhotoVisualMediaRequest(
    mediaType: PhotoVisualMedia.VisualMediaType = PhotoVisualMedia.ImageOnly
) = PhotoVisualMediaRequest.Builder().setMediaType(mediaType).build()

/**
 * A request for a
 * [androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia] or
 * [androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia] Activity Contract.
 */
class PhotoVisualMediaRequest internal constructor() {

    var mediaType: PhotoVisualMedia.VisualMediaType = PhotoVisualMedia.ImageAndVideo
        internal set
    var configModel: ConfigModel = ConfigModel.default()
        internal set
    var isForceCustomUi: Boolean = false
        internal set
    var selectedUris: ArrayList<Uri> = arrayListOf()
        internal set
    var maxItems: Int = -1
        internal set

    /**
     * A builder for constructing [PickVisualMediaRequest] instances.
     */
    class Builder {

        private var mediaType: PhotoVisualMedia.VisualMediaType = PhotoVisualMedia.ImageAndVideo
        private var configModel: ConfigModel = ConfigModel.default()
        private var isForceCustomUi: Boolean = false
        private var selectedUris: ArrayList<Uri> = arrayListOf()
        private var maxItems: Int = -1

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

        fun setSelectedUris(selectedUris: ArrayList<Uri>): Builder {
            this.selectedUris = selectedUris
            dLog { "setSelectedUris: ${selectedUris.joinToString(",")}" }
            return this
        }

        /**
         * 是否强制使用自定义UI
         */
        fun setForceCustomUi(isForceCustomUi: Boolean): Builder {
            this.isForceCustomUi = isForceCustomUi
            return this
        }

        fun setShowGif(isShowGif: Boolean): Builder {
            this.configModel.isShowGif = isShowGif
            return this
        }

        fun setShowWebp(isShowWebp: Boolean): Builder {
            this.configModel.isShowWebp = isShowWebp
            return this
        }

        fun setShowBmp(isShowBmp: Boolean): Builder {
            this.configModel.isShowBmp = isShowBmp
            return this
        }

        fun setPageSyncAsCount(isPageSyncAsCount: Boolean): Builder {
            this.configModel.isPageSyncAsCount = isPageSyncAsCount
            return this
        }

        fun setSortOrder(sortOrder: String): Builder {
            this.configModel.sortOrder = sortOrder
            return this
        }

        fun setPageSize(pageSize: Int): Builder {
            this.configModel.pageSize = pageSize
            return this
        }

        fun setFilterVideoMinSecond(filterVideoMinSecond: Long): Builder {
            this.configModel.filterVideoMinSecond = filterVideoMinSecond
            return this
        }

        fun setFilterVideoMaxSecond(filterVideoMaxSecond: Long): Builder {
            this.configModel.filterVideoMaxSecond = filterVideoMaxSecond
            return this
        }

        fun setFilterMinFileSize(filterMinFileSize: Long): Builder {
            this.configModel.filterMinFileSize = filterMinFileSize
            return this
        }

        fun setFilterMaxFileSize(filterMaxFileSize: Long): Builder {
            this.configModel.filterMaxFileSize = filterMaxFileSize
            return this
        }

        fun setMaxItemCount(maxCount: Int): Builder {
            this.maxItems = maxCount
            return this
        }

        /**
         * Build the PickVisualMediaRequest specified by this builder.
         *
         * @return the newly constructed PickVisualMediaRequest.
         */
        fun build(): PhotoVisualMediaRequest = PhotoVisualMediaRequest().apply {
            this.mediaType = this@Builder.mediaType
            this.selectedUris = this@Builder.selectedUris
            this.configModel = this@Builder.configModel
            this.isForceCustomUi = this@Builder.isForceCustomUi
            this.maxItems = this@Builder.maxItems
        }
    }
}
