package com.peihua.selector.photos.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.fz.common.utils.getDimensionPixelOffset
import com.fz.imageloader.widget.RatioImageView
import com.peihua.photopicker.R
import com.peihua.selector.data.model.Item

/**
 * ViewHolder of a photo item within a RecyclerView.
 */
class PhotoGridHolder(
    context: Context, screenWidth:Int, parent: ViewGroup, canSelectMultiple: Boolean
) : BaseViewHolder(context, parent, R.layout.picker_item_photo_grid) {
    private val mIconThumb: RatioImageView = itemView.findViewById(R.id.icon_thumbnail)
    private val mIconGif: ImageView = itemView.findViewById(R.id.icon_gif)
    private val mIconMotionPhoto: ImageView = itemView.findViewById(R.id.icon_motion_photo)
    private val mVideoBadgeContainer: View = itemView.findViewById(R.id.video_container)
    private val mVideoDuration: TextView = mVideoBadgeContainer.findViewById(R.id.video_duration)
    private val mOverlayGradient: View = itemView.findViewById(R.id.overlay_gradient)
    private val mCanSelectMultiple: Boolean
    private val margin = context.getDimensionPixelOffset(R.dimen.picker_photo_item_spacing)
    private val mWidth = (screenWidth - margin * 2) / 3

    init {

        val iconCheck = itemView.findViewById<ImageView>(R.id.icon_check)
        mCanSelectMultiple = canSelectMultiple
        if (mCanSelectMultiple) {
            iconCheck.visibility = View.VISIBLE
        } else {
            iconCheck.visibility = View.GONE
        }
    }

    override fun bind() {
        val item = itemView.tag as Item
        mIconThumb.layoutParams.apply {
            width = mWidth
            height = mWidth
        }
        mIconThumb.setImageUrl(item.contentUri,item.isGif, mWidth, mWidth)
        mIconGif.visibility = if (item.isGifOrAnimatedWebp) View.VISIBLE else View.GONE
        mIconMotionPhoto.visibility = if (item.isMotionPhoto) View.VISIBLE else View.GONE
        if (item.isVideo) {
            mVideoBadgeContainer.visibility = View.VISIBLE
            mVideoDuration.text = item.durationText
        } else {
            mVideoBadgeContainer.visibility = View.GONE
        }
        mOverlayGradient.isVisible = showShowOverlayGradient(item)
    }

    private fun showShowOverlayGradient(item: Item): Boolean {
        return mCanSelectMultiple || item.isGifOrAnimatedWebp || item.isVideo ||
                item.isMotionPhoto
    }
}