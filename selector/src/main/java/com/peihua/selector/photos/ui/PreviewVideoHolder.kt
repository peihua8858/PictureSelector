package com.peihua.selector.photos.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.request.RequestOptions
import com.fz.imageloader.ImageOptions
import com.peihua.photopicker.R
import com.peihua.selector.data.model.Item

/**
 * ViewHolder of a video item within the [ViewPager2]
 */
class PreviewVideoHolder internal constructor(
    context: Context?,
    parent: ViewGroup?,
) : BaseViewHolder(
    context!!, parent!!, R.layout.picker_item_video_preview
) {
    val thumbnailView: ImageView = itemView.findViewById(R.id.preview_video_image)

    override fun bind() {
        // Video playback needs granular page state events and hence video playback is initiated by
        // ViewPagerWrapper and handled by PlaybackHandler#handleVideoPlayback.
        // Here, we set the ImageView with thumbnail from the video, to improve the
        // user experience while video player is not yet initialized or being prepared.
        val item = itemView.tag as Item
       com.fz.imageloader.ImageLoader.getInstance().loadImage(
            ImageOptions.Builder()
                .setBitmap(true)
                .setTargetView(thumbnailView)
                .setImageUrl(item.contentUri)
                .setOptions(RequestOptions().frame(1000))
                .build()
        )
//        mImageLoader.loadImageFromVideoForPreview(item, thumbnailView)
    }
}