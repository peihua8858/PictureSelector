package com.peihua.selector.photos.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.fz.common.utils.getDimensionPixelOffset
import com.peihua.photopicker.R
import com.peihua.selector.data.model.Category

/**
 * ViewHolder of a album item within a RecyclerView.
 */
class AlbumGridHolder(
    context: Context, screenWidth: Int, parent: ViewGroup, hasMimeTypeFilter: Boolean
) : BaseViewHolder(context, parent, R.layout.item_album_grid) {
    private val mIconThumb: SquareImageView = itemView.findViewById(R.id.icon_thumbnail)
    private val mAlbumName: TextView = itemView.findViewById(R.id.album_name)
    private val mItemCount: TextView = itemView.findViewById(R.id.item_count)
    private val mHasMimeTypeFilter: Boolean
    private val spacing = context.getDimensionPixelOffset(R.dimen.picker_album_item_spacing)
    private val mWidth = (screenWidth - spacing) / 2

    init {
        mHasMimeTypeFilter = hasMimeTypeFilter
    }

    override fun bind() {
        val category = itemView.tag as Category
        mIconThumb.layoutParams.apply {
            width = mWidth
            height = mWidth
        }
        mIconThumb.setImageUrl(category.coverUri, category.isGif, mWidth, mWidth)
        mAlbumName.text = category.getDisplayName(itemView.context)

        // Check whether there is a mime type filter or not. If yes, hide the item count. Otherwise,
        // show the item count and update the count.
        if (mHasMimeTypeFilter) {
            mItemCount.visibility = View.GONE
        } else {
            mItemCount.visibility = View.VISIBLE
            val itemCount = category.itemCount
            val message= itemView.resources.getString(if(itemCount==1) R.string.picker_album_item_count_1 else R.string.picker_album_item_count,itemCount)
            mItemCount.text = message
        }
    }
}