package com.peihua.selector.crop

import android.graphics.ColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import androidx.recyclerview.widget.RecyclerView
import com.fz.imageloader.ImageLoader
import com.peihua.photopicker.R

/**
 * @author：luck
 * @date：2016-12-31 22:22
 * @describe：UCropGalleryAdapter
 */
class UCropGalleryAdapter(private val list: List<String>?) : RecyclerView.Adapter<UCropGalleryAdapter.ViewHolder>() {
    var currentSelectPosition = 0
    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.picker_crop_gallery_adapter_item,
            parent, false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val path = list!![position]
        ImageLoader.getInstance().loadImage(holder.mIvPhoto, path)
        val colorFilter: ColorFilter?
        if (currentSelectPosition == position) {
            holder.mViewCurrentSelect.visibility = View.VISIBLE
            colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                ContextCompat.getColor(holder.itemView.context, R.color.picker_color_80),
                BlendModeCompat.SRC_ATOP
            )
        } else {
            colorFilter = BlendModeColorFilterCompat.createBlendModeColorFilterCompat(
                ContextCompat.getColor(holder.itemView.context, R.color.picker_color_20),
                BlendModeCompat.SRC_ATOP
            )
            holder.mViewCurrentSelect.visibility = View.GONE
        }
        holder.mIvPhoto.colorFilter = colorFilter
        holder.itemView.setOnClickListener { v ->
            if (listener != null) {
                listener?.invoke(holder.absoluteAdapterPosition, v)
            }
        }
    }

    override fun getItemCount(): Int {
        return list?.size ?: 0
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var mIvPhoto: ImageView
        var mViewCurrentSelect: View

        init {
            mIvPhoto = view.findViewById(R.id.iv_photo)
            mViewCurrentSelect = view.findViewById(R.id.view_current_select)
        }
    }

    private var listener:  ((Int, View) -> Unit)? = null
    fun setOnItemClickListener(listener: (Int, View) -> Unit) {
        this.listener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int, view: View?)
    }
}