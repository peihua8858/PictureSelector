package com.peihua8858.selector.picker.holder

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.fz.imageloader.LoaderListener
import com.fz.imageloader.widget.RatioImageView
import com.peihua8858.selector.android.R
import com.peihua8858.selector.picker.model.Item
import com.peihua8858.tools.utils.dLog

/**
 * ViewHolder of a photo item within a RecyclerView.
 */
class PhotoGridHolder(
    context: Context,
    parent: ViewGroup,
    private val canSelectMultiple: Boolean,
) : BaseViewHolder(context, parent, R.layout.picker_item_photo_grid) {
    private val mIconThumb: RatioImageView = itemView.findViewById(R.id.icon_thumbnail)
    private val mIconGif: ImageView = itemView.findViewById(R.id.icon_gif)
    private val mIconMotionPhoto: ImageView = itemView.findViewById(R.id.icon_motion_photo)
    private val mVideoBadgeContainer: View = itemView.findViewById(R.id.video_container)
    private val mVideoDuration: TextView = mVideoBadgeContainer.findViewById(R.id.video_duration)
    private val mOverlayGradient: View = itemView.findViewById(R.id.overlay_gradient)
    private val iconVideo: ImageView = mVideoBadgeContainer.findViewById(R.id.icon_video)
    private val mCanSelectMultiple: Boolean

    init {

        val iconCheck = itemView.findViewById<ImageView>(R.id.icon_check)
        mCanSelectMultiple = canSelectMultiple
        iconCheck.isVisible = mCanSelectMultiple
    }

    override fun bind() {
        val item = itemView.tag as Item
        dLog { ">>>>>uri:${item.contentUri}" }
        mIconThumb.setListener(object : LoaderListener<Drawable> {
            override fun onError(e: Exception?): Boolean {
                dLog { ">>>>>>>>>${e?.stackTraceToString()}" }
                return false
            }

            override fun onSuccess(
                bitmap: Drawable?,
                width: Int,
                height: Int
            ): Boolean {
                dLog { ">>>>$bitmap,width:$width,height:$height" }
                return false
            }

        })
        if (item.isAudio) {
            mIconThumb.setImageResource(com.peihua8858.selector.R.drawable.picker_ic_audio_placeholder)
        } else {
            mIconThumb.setImageUrl(item.contentUri, item.isGif)
        }
        mIconGif.isVisible = item.isGifOrAnimatedWebp
        mIconMotionPhoto.isVisible = item.isMotionPhoto
        mVideoBadgeContainer.isVisible = item.isVideo || item.isAudio
        if (mVideoBadgeContainer.isVisible) {
            iconVideo.setImageResource(
                if (item.isVideo) com.peihua8858.selector.R.drawable.picker_ic_play_circle_filled
                else com.peihua8858.selector.R.drawable.picker_ic_audio
            )
            mVideoDuration.text = item.durationText
        }
        mOverlayGradient.isVisible = showShowOverlayGradient(item)
    }

    private fun showShowOverlayGradient(item: Item): Boolean {
        return mCanSelectMultiple || item.isGifOrAnimatedWebp || item.isVideo ||
                item.isMotionPhoto
    }
}