package com.peihua.selector.photos.ui

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.peihua.photopicker.R
import com.peihua.selector.util.isLayoutRtl

/**
 * The ItemDecoration that allows adding layout offsets to specific item views from the adapter's
 * data set for the [RecyclerView] on Albums tab.
 */
class AlbumsTabItemDecoration(context: Context) : RecyclerView.ItemDecoration() {
    private val mSpacing: Int
    private val mTopSpacing: Int

    init {
        mSpacing = context.resources.getDimensionPixelSize(R.dimen.picker_album_item_spacing)
        mTopSpacing = context.resources.getDimensionPixelSize(
            R.dimen.picker_album_item_top_spacing
        )
    }

    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val lp = view.layoutParams as GridLayoutManager.LayoutParams
        val layoutManager = parent.layoutManager as GridLayoutManager?
        val column = lp.spanIndex
        val spanCount = layoutManager!!.spanCount
        val adapterPosition = parent.getChildAdapterPosition(view)
        // the top gap of the album items on the first row is mSpacing
        if (adapterPosition < spanCount) {
            outRect.top = mSpacing
        } else {
            outRect.top = mTopSpacing
        }

        // spacing - column * ((1f / spanCount) * spacing)
        val start = mSpacing - column * mSpacing / spanCount
        // (column + 1) * ((1f / spanCount) * spacing)
        val end = (column + 1) * mSpacing / spanCount
        if (parent.isLayoutRtl) {
            outRect.left = end
            outRect.right = start
        } else {
            outRect.left = start
            outRect.right = end
        }
    }
}