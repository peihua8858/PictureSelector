package com.peihua.selector.photos.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.peihua.selector.data.MuteStatus
import com.peihua.selector.data.model.Item

/**
 * Adapter for Preview RecyclerView to preview all images and videos.
 */
internal class PreviewAdapter(context: Context?, muteStatus: MuteStatus?) : RecyclerView.Adapter<BaseViewHolder>() {
    private var mItemList: List<Item> = ArrayList()
    private val mPlaybackHandler: PlaybackHandler

    init {
        mPlaybackHandler = PlaybackHandler(context, muteStatus)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): BaseViewHolder {
        return if (viewType == ITEM_TYPE_IMAGE) {
            PreviewImageHolder(viewGroup.context, viewGroup)
        } else {
            PreviewVideoHolder(viewGroup.context, viewGroup)
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.contentDescription = item.getContentDescription(holder.itemView.context)
        holder.itemView.tag = item
        holder.bind()
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        super.onViewAttachedToWindow(holder)
        val item = holder.itemView.tag as Item
        if (item.isVideo) {
            // TODO(b/222506900): Refactor thumbnail show / hide logic to be handled from a single
            // place. Currently, we show the thumbnail here and hide it when playback starts in
            // PlaybackHandler/RemotePreviewHandler.
            val videoHolder = holder as PreviewVideoHolder
            mPlaybackHandler.onViewAttachedToWindow(holder.itemView)
        }
    }

    override fun getItemCount(): Int {
        return mItemList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (mItemList[position].isVideo) {
            ITEM_TYPE_VIDEO
        } else ITEM_TYPE_IMAGE
        // Everything other than video mimeType are previewed using PreviewImageHolder. This also
        // includes GIF which uses Glide to load image.
    }

    fun onHandlePageSelected(itemView: View?) {
        mPlaybackHandler.handleVideoPlayback(itemView)
    }

    fun onStop() {
        mPlaybackHandler.releaseResources()
    }

    fun onDestroy() {}
    fun getItem(position: Int): Item {
        return mItemList[position]
    }

    fun updateItemList(itemList: List<Item>) {
        mItemList = itemList
        notifyDataSetChanged()
    }

    companion object {
        private const val ITEM_TYPE_IMAGE = 1
        private const val ITEM_TYPE_VIDEO = 2
    }
}