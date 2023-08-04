package com.peihua.selector.photos.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.peihua.photopicker.R
import com.peihua.selector.data.model.Item
import com.peihua.selector.util.DateTimeUtils

/**
 * ViewHolder of a date header within a RecyclerView.
 */
class DateHeaderHolder(context: Context, parent: ViewGroup) :
    BaseViewHolder(context, parent, R.layout.picker_item_date_header) {
    private val mTitle: TextView = itemView.findViewById(R.id.date_header_title)

    override fun bind() {
        val item = itemView.tag as Item
        val dateTaken = item.dateTaken
        if (dateTaken == 0L) {
            mTitle.setText(R.string.picker_recent)
        } else {
            mTitle.text = DateTimeUtils.getDateHeaderString(dateTaken)
        }
    }
}