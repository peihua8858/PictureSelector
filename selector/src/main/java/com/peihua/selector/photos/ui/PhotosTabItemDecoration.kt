package com.peihua.selector.photos.ui

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.peihua.photopicker.R
import com.peihua.selector.util.isLayoutRtl

/**
 * The ItemDecoration that allows to add layout offsets to specific item views from the adapter's
 * data set for the [RecyclerView] on Photos tab.
 */
class PhotosTabItemDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val mSpacing: Int

    init {
        mSpacing = context.resources.getDimensionPixelSize(R.dimen.picker_photo_item_spacing)
    }

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val lp = view.layoutParams as GridLayoutManager.LayoutParams
        val layoutManager = parent.layoutManager as GridLayoutManager?
        val column = lp.spanIndex
        val spanCount = layoutManager!!.spanCount

        // The date header doesn't have spacing
        if (lp.spanSize == spanCount) {
            outRect[0, 0, 0] = 0
            return
        }
        val adapterPosition = parent.getChildAdapterPosition(view)
        if (adapterPosition > column) {
            val itemViewType = parent.adapter!!.getItemViewType(
                adapterPosition - column - 1
            )
            // if the above item is not a date header, add the top spacing
            if (itemViewType != PhotosTabAdapter.ITEM_TYPE_DATE_HEADER) {
                outRect.top = mSpacing
            }
        }

        // column * ((1f / spanCount) * spacing)
        val start = column * mSpacing / spanCount
        // spacing - (column + 1) * ((1f / spanCount) * spacing)
        val end = mSpacing - (column + 1) * mSpacing / spanCount
        if (parent.isLayoutRtl) {
            outRect.left = end
            outRect.right = start
        } else {
            outRect.left = start
            outRect.right = end
        }
    }
}