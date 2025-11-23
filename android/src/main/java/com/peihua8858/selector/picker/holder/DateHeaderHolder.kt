package com.peihua8858.selector.picker.holder

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.peihua8858.selector.android.R
import com.peihua8858.selector.picker.model.Item
import com.peihua8858.selector.utils.DateTimeUtils

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
            mTitle.setText(com.peihua8858.selector.R.string.picker_recent)
        } else {
            mTitle.text = DateTimeUtils.getDateHeaderString(dateTaken)
        }
    }
}