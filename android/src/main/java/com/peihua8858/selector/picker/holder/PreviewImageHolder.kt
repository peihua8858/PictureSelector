package com.peihua8858.selector.picker.holder

import android.content.Context
import android.view.ViewGroup
import com.fz.imageloader.widget.RatioImageView
import com.peihua8858.selector.android.R
import com.peihua8858.selector.picker.model.Item

/**
 * ViewHolder of an image item within the [ViewPager2]
 */
class PreviewImageHolder(
    context: Context, parent: ViewGroup,
) : BaseViewHolder(context, parent, R.layout.picker_item_image_preview) {
    private val mImageView: RatioImageView = itemView.findViewById(R.id.preview_imageView)

    override fun bind() {
        val item = itemView.tag as Item
        mImageView.setImageUrl(item.contentUri,item.isGif)
    }
}