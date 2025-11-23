package com.peihua8858.selector.picker.holder

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.peihua8858.selector.android.R
import com.peihua8858.selector.picker.model.Category
import com.peihua8858.selector.picker.widget.SquareImageView
import com.peihua8858.selector.utils.StringUtils
import java.text.NumberFormat
import java.util.Locale

/**
 * ViewHolder of a album item within a RecyclerView.
 */
class AlbumGridHolder(
    context: Context,
    parent: ViewGroup,
    hasMimeTypeFilter: Boolean,
) : BaseViewHolder(context, parent, R.layout.picker_item_album_grid) {
    private val mIconThumb: SquareImageView = itemView.findViewById(R.id.icon_thumbnail)
    private val mAlbumName: TextView = itemView.findViewById(R.id.album_name)
    private val mItemCount: TextView = itemView.findViewById(R.id.item_count)
    private val mHasMimeTypeFilter: Boolean

    init {
        mHasMimeTypeFilter = hasMimeTypeFilter
    }

    override fun bind() {
        val category = itemView.tag as Category
        mIconThumb.setImageUrl(category.coverUri, category.isGif)
        mAlbumName.text = category.getDisplayName(itemView.context)

        // Check whether there is a mime type filter or not. If yes, hide the item count. Otherwise,
        // show the item count and update the count.
        if (mHasMimeTypeFilter) {
            mItemCount.visibility = View.GONE
        } else {
            mItemCount.visibility = View.VISIBLE
            val itemCount = category.itemCount
            val message = StringUtils.getICUFormatString(itemView.resources, itemCount, com.peihua8858.selector.R.string.picker_album_item_count)
            val itemCountString = NumberFormat.getInstance(Locale.getDefault()).format(itemCount.toLong())
            mItemCount.text = TextUtils.expandTemplate(message, itemCountString);
        }
    }
}