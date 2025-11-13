package com.peihua.selector.photos.ui

import android.content.Context
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.peihua.selector.data.model.Item
import com.peihua.selector.photos.ui.compose.ItemGrid

/**
 * ViewHolder of a photo item within a RecyclerView.
 */
class PhotoGridHolder(
    context: Context,
    parent: ViewGroup,
    private val canSelectMultiple: Boolean,
) : BaseViewHolder(context, ComposeView(context)) {
//    private val mIconThumb: RatioImageView = itemView.findViewById(R.id.icon_thumbnail)
//    private val mIconGif: ImageView = itemView.findViewById(R.id.icon_gif)
//    private val mIconMotionPhoto: ImageView = itemView.findViewById(R.id.icon_motion_photo)
//    private val mVideoBadgeContainer: View = itemView.findViewById(R.id.video_container)
//    private val mVideoDuration: TextView = mVideoBadgeContainer.findViewById(R.id.video_duration)
//    private val mOverlayGradient: View = itemView.findViewById(R.id.overlay_gradient)
//    private val mCanSelectMultiple: Boolean

//    init {
//
//        val iconCheck = itemView.findViewById<ImageView>(R.id.icon_check)
//        mCanSelectMultiple = canSelectMultiple
//        iconCheck.isVisible = mCanSelectMultiple
//    }

    override fun bind() {
        val itemView = itemView as ComposeView
        val item = itemView.tag as Item
        itemView.setContent {
            ItemGrid(item, itemView.isSelected, canSelectMultiple) {
            }
        }
//
//        if (item.isVideo) {
//            mIconThumb.setImageDrawable(item.videoThumbnail.toDrawable(mIconThumb.resources))
//        } else {
//            mIconThumb.setImageUrl(item.contentUri, item.isGif)
//        }
//        mIconGif.isVisible = item.isGifOrAnimatedWebp
//        mIconMotionPhoto.isVisible = item.isMotionPhoto
//        mVideoBadgeContainer.isVisible = item.isVideo
//        if (item.isVideo) {
//            mVideoDuration.text = item.durationText
//        }
//        mOverlayGradient.isVisible = showShowOverlayGradient(item)
    }

//    private fun showShowOverlayGradient(item: Item): Boolean {
//        return mCanSelectMultiple || item.isGifOrAnimatedWebp || item.isVideo ||
//                item.isMotionPhoto
//    }
}