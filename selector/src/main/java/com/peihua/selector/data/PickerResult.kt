package com.peihua.selector.data

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import com.fz.common.array.isNonEmpty
import com.fz.common.text.isNonEmpty
import com.peihua.selector.data.model.Item

/**
 * This class is responsible for returning result to the caller of the PhotoPicker.
 */
object PickerResult {
    /**
     * @return `Intent` which contains Uri that has been granted access on.
     */
    fun getPickerResponseIntent(
        canSelectMultiple: Boolean,
        selectedItems: List<Item>, mimeTypes: String
    ): Intent {
        return getPickerResponseIntent(
            canSelectMultiple, selectedItems,
            mimeTypes.split(",").toTypedArray()
        )
    }

    fun getPickerResponseIntent(
        canSelectMultiple: Boolean,
        selectedItems: List<Item>, mimeTypes: Array<String>
    ): Intent {
        // 1. Get Picker Uris corresponding to the selected items
        val selectedUris = getPickerUrisForItems(selectedItems)

        // 2. Grant read access to picker Uris and return
        val intent = Intent()
        val size = selectedUris.size
        if (size < 1) {
            // TODO (b/168783994): check if this is ever possible. If yes, handle properly,
            // if not, remove this if block.
            return intent
        }
        if (!canSelectMultiple) {
            intent.data = selectedUris[0]
        }
        // TODO (b/169737761): use correct mime types
        val tempMimeTypes = if (mimeTypes.isNonEmpty()) {
            mimeTypes
        } else {
            arrayOf("image/*", "video/*")
        }
        val clipData = ClipData(
            null /* label */, tempMimeTypes,
            ClipData.Item(selectedUris[0])
        )
        for (i in 1 until size) {
            clipData.addItem(ClipData.Item(selectedUris[i]))
        }
        intent.clipData = clipData
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        return intent
    }

    /**
     * Returns list of PhotoPicker Uris corresponding to each [Item]
     *
     * @param itemList list of Item for which we return uri list.
     */
    private fun getPickerUrisForItems(itemList: List<Item>): List<Uri> {
        val uris: MutableList<Uri> = ArrayList()
        for (item in itemList) {
            uris.add(item.contentUri)
        }
        return uris
    }
}