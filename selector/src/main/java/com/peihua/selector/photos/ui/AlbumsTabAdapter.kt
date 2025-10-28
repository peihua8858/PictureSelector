package com.peihua.selector.photos.ui

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.peihua.selector.data.model.Category
import com.peihua.selector.photos.ui.PhotosTabAdapter.Companion.ITEM_TYPE_DATE_HEADER

/**
 * Adapts from model to something RecyclerView understands.
 */
class AlbumsTabAdapter(private val mOnClickListener: View.OnClickListener,
    private val mHasMimeTypeFilter: Boolean,
) : RecyclerView.Adapter<BaseViewHolder>() {
    private var mCategoryList: List<Category> = ArrayList()
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): BaseViewHolder {
        return AlbumGridHolder(
            viewGroup.context,
            viewGroup,
            mHasMimeTypeFilter
        )
    }

    override fun onBindViewHolder(itemHolder: BaseViewHolder, position: Int) {
        val category = getCategory(position)
        itemHolder.itemView.tag = category
        itemHolder.itemView.setOnClickListener(mOnClickListener)
        itemHolder.bind()
    }

    override fun getItemCount(): Int {
        return mCategoryList.size
    }

    override fun getItemViewType(position: Int): Int {
        return ITEM_TYPE_CATEGORY
    }

    fun getCategory(position: Int): Category {
        return mCategoryList[position]
    }

    fun updateCategoryList(categoryList: List<Category>) {
        mCategoryList = categoryList
        notifyDataSetChanged()
    }

    fun createSpanSizeLookup(
        layoutManager: GridLayoutManager,
    ): GridLayoutManager.SpanSizeLookup {
        return object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return 1
            }
        }
    }

    companion object {
        private const val ITEM_TYPE_CATEGORY = 1
        const val COLUMN_COUNT = 2
    }
}