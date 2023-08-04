package com.peihua.selector.photos.ui

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.peihua.photopicker.R
import com.peihua.selector.data.Selection
import com.peihua.selector.data.model.Item

/**
 * Adapts from model to something RecyclerView understands.
 */
class PhotosTabAdapter(
    private val mSelection: Selection,private val screenWidth:Int,
    private val mOnClickListener: View.OnClickListener,
    private val mOnLongClickListener: View.OnLongClickListener
) : RecyclerView.Adapter<BaseViewHolder>() {
    private var mItemList: List<Item> = ArrayList()
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): BaseViewHolder {
        return if (viewType == ITEM_TYPE_DATE_HEADER) {
            DateHeaderHolder(viewGroup.context, viewGroup)
        } else PhotoGridHolder(
            viewGroup.context,screenWidth, viewGroup,
            mSelection.canSelectMultiple()
        )
    }

    override fun onBindViewHolder(itemHolder: BaseViewHolder, position: Int) {
        val item = getItem(position)
        itemHolder.itemView.tag = item
        if (itemHolder.itemViewType == ITEM_TYPE_PHOTO) {
            itemHolder.itemView.setOnClickListener(mOnClickListener)
            itemHolder.itemView.setOnLongClickListener(mOnLongClickListener)
            val context = itemHolder.itemView.context
            itemHolder.itemView.contentDescription = item.getContentDescription(context)
            if (mSelection.canSelectMultiple()) {
                val isSelected = mSelection.isItemSelected(item)
                itemHolder.itemView.isSelected = isSelected

                // There is an issue b/223695510 about not selected in Accessibility mode. It only
                // says selected state, but it doesn't say not selected state. Add the not selected
                // only to avoid that it says selected twice.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    itemHolder.itemView.stateDescription =
                        if (isSelected) null else context.getString(R.string.not_selected)
                }
            }
        }
        itemHolder.bind()
    }

    override fun getItemCount(): Int {
        return mItemList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isDate) {
            ITEM_TYPE_DATE_HEADER
        } else ITEM_TYPE_PHOTO
    }

    fun getItem(position: Int): Item {
        return mItemList[position]
    }
    fun updateItemList(itemList: List<Item>) {
        mItemList = itemList
        notifyDataSetChanged()
    }

    fun createSpanSizeLookup(
        layoutManager: GridLayoutManager
    ): GridLayoutManager.SpanSizeLookup {
        return object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val itemViewType = getItemViewType(position)
                // For the item view type is ITEM_TYPE_DATE_HEADER, it is full
                // span, return the span count of the layoutManager.
                return if (itemViewType == ITEM_TYPE_DATE_HEADER) {
                    layoutManager.spanCount
                } else {
                    1
                }
            }
        }
    }

    companion object {
        const val ITEM_TYPE_DATE_HEADER = 0
        private const val ITEM_TYPE_PHOTO = 1
        const val COLUMN_COUNT = 3
    }
}