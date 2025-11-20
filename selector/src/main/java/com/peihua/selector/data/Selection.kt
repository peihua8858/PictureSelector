package com.peihua.selector.data

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fz.common.utils.dLog
import com.fz.common.utils.getParcelableArrayListCompat
import com.fz.common.utils.isAtLeastT
import com.peihua.selector.data.model.Item
import com.peihua.selector.result.contract.PhotoVisualMedia
import java.util.Collections

/**
 * A class that tracks Selection
 */
class Selection {
    // The list of selected items.
    private val mSelectedItems: MutableMap<Uri, Item> = HashMap()
    private val mSelectedItemSize = MutableLiveData<Int>()

    // The list of selected items for preview. This needs to be saved separately so that if activity
    // gets killed, we will still have deselected items for preview.
    private var mSelectedItemsForPreview: MutableList<Item> = ArrayList()
    private var mSelectMultiple = false

    /**
     * Return maximum limit of items that can be selected
     */
    var maxSelectionLimit = 1
        private set

    /**
     * @return returns whether more items can be selected or not. `true` if the number of
     * selected items is lower than or equal to `mMaxLimit`, `false` otherwise.
     */
    // This is set to false when max selection limit is reached.
    var isSelectionAllowed = true
        private set
    val selectedItems: List<Item>
        /**
         * @return [.mSelectedItems] - A [List] of selected [Item]
         */
        get() = Collections.unmodifiableList(ArrayList(mSelectedItems.values))
    val selectedItemCount: LiveData<Int>
        /**
         * @return [LiveData] of count of selected items in [.mSelectedItems]
         */
        get() {
            if (mSelectedItemSize.value == null) {
                mSelectedItemSize.value = mSelectedItems.size
            }
            return mSelectedItemSize
        }

    /**
     * Add the selected `item` into [.mSelectedItems].
     */
    fun addSelectedItem(item: Item) {
        mSelectedItems[item.contentUri] = item
        mSelectedItemSize.postValue(mSelectedItems.size)
        updateSelectionAllowed()
    }

    /**
     * Clears [.mSelectedItems] and sets the selected item as given `item`
     */
    fun setSelectedItem(item: Item) {
        mSelectedItems.clear()
        mSelectedItems[item.contentUri] = item
        mSelectedItemSize.postValue(mSelectedItems.size)
        updateSelectionAllowed()
    }

    /**
     * Remove the `item` from the selected item list [.mSelectedItems].
     *
     * @param item the item to be removed from the selected item list
     */
    fun removeSelectedItem(item: Item) {
        mSelectedItems.remove(item.contentUri)
        mSelectedItemSize.postValue(mSelectedItems.size)
        updateSelectionAllowed()
    }

    /**
     * Clear all selected items
     */
    fun clearSelectedItems() {
        mSelectedItems.clear()
        mSelectedItemSize.postValue(mSelectedItems.size)
        updateSelectionAllowed()
    }

    /**
     * @return `true` if give `item` is present in selected items
     * [.mSelectedItems], `false` otherwise
     */
    fun isItemSelected(item: Item): Boolean {
        if (mSelectedItems.containsKey(item.contentUri)) {
            val selItem = mSelectedItems[item.contentUri]
            if (selItem == null || selItem == Item.EMPTY) {
                mSelectedItems[item.contentUri] = item
            }
            dLog { "selectedUris>>isItemSelected: ${selItem == null || selItem == Item.EMPTY}" }
            return true
        }
        return false
    }

    private fun updateSelectionAllowed() {
        val size = mSelectedItems.size
        if (size >= maxSelectionLimit) {
            if (isSelectionAllowed) {
                isSelectionAllowed = false
            }
        } else {
            // size < mMaxSelectionLimit
            if (!isSelectionAllowed) {
                isSelectionAllowed = true
            }
        }
    }

    /**
     * Prepares current selected items for previewing all selected items in multi-select preview.
     * The method also sorts the selected items by [Item.compareTo] method which sorts based
     * on dateTaken values.
     */
    fun prepareSelectedItemsForPreviewAll() {
        mSelectedItemsForPreview = ArrayList(mSelectedItems.values)
        mSelectedItemsForPreview.sortWith { item, item2 ->
            item.compareTo(
                item2
            )
        }
    }

    /**
     * Sets the given `item` as the item for previewing. This method will be used while
     * previewing on long press.
     */
    fun prepareItemForPreviewOnLongPress(item: Item) {
        mSelectedItemsForPreview = mutableListOf(item)
    }

    val selectedItemsForPreview: List<Item>
        /**
         * @return [.mSelectedItemsForPreview] - selected items for preview.
         */
        get() = Collections.unmodifiableList(mSelectedItemsForPreview)

    /**
     * Parse values from `intent` and set corresponding fields
     */
    fun parseSelectionValuesFromIntent(intent: Intent) {
        val bundle = intent.extras
        dLog { "selectedUris: ${bundle}" }
        bundle?.apply {
            val selectedUris =
                getParcelableArrayListCompat(PhotoVisualMedia.EXTRA_SELECTED_PICK_IMAGES, Uri::class.java)
            selectedUris.forEach {
                mSelectedItems[it] = Item.EMPTY
            }
            dLog { "selectedUris: ${selectedUris.joinToString(",")}" }
            updateSelectionAllowed()
            val isExtraPickImagesMaxSet = containsKey(PhotoVisualMedia.EXTRA_PICK_IMAGES_MAX)
            // Check EXTRA_PICK_IMAGES_MAX value only if the flag is set.
            if (isExtraPickImagesMaxSet) {
                val extraMax = getInt(PhotoVisualMedia.EXTRA_PICK_IMAGES_MAX,  /* defaultValue */ -1)
                // Multi selection max limit should always be greater than 1 and less than or equal
                // to PICK_IMAGES_MAX_LIMIT.
                val pickImageMaxLimit: Int = if (isAtLeastT) {
                    MediaStore.getPickImagesMaxLimit()
                } else {
                    PICK_IMAGES_MAX_LIMIT
                }
                require(!(extraMax <= 1 || extraMax > pickImageMaxLimit)) { "Invalid EXTRA_PICK_IMAGES_MAX value" }
                mSelectMultiple = true
                maxSelectionLimit = extraMax
            }
        }
    }

    /**
     * Return whether supports multiple select [.mSelectMultiple] or not
     */
    fun canSelectMultiple(): Boolean {
        return mSelectMultiple
    }

    companion object {
        const val PICK_IMAGES_MAX_LIMIT = 100
    }
}