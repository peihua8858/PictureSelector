package com.peihua.selector.photos.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

/**
 * ViewHolder of a item within a [RecyclerView.Adapter].
 */
abstract class BaseViewHolder(context: Context, view: View) : RecyclerView.ViewHolder(view) {
    constructor(context: Context, parent: ViewGroup, layout: Int) : this(
        context,
        inflateLayout<View>(context, parent, layout)
    )

    abstract fun bind()

    companion object {
        private fun <V : View?> inflateLayout(
            context: Context,
            parent: ViewGroup, layout: Int
        ): V {
            val inflater = LayoutInflater.from(context)
            return inflater.inflate(layout, parent, false) as V
        }
    }
}