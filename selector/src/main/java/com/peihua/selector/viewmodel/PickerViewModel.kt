package com.peihua.selector.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fz.common.model.ViewModelState
import com.fz.common.model.request
import com.fz.common.text.isNonEmpty
import com.fz.common.utils.getParcelableExtraCompat
import com.peihua.selector.data.MuteStatus
import com.peihua.selector.data.Selection
import com.peihua.selector.data.model.Category
import com.peihua.selector.data.model.ConfigModel
import com.peihua.selector.data.model.Item
import com.peihua.selector.data.provider.ItemsProvider
import com.peihua.selector.util.DateTimeUtils
import com.peihua.selector.util.MimeUtils
import com.peihua.selector.util.isAtLeastT

/**
 * PickerViewModel to store and handle data for PhotoPickerActivity.
 */
class PickerViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * @return `mSelection` that manages the selection
     */
    val selection: Selection

    /**
     * @return `mMuteStatus` that tracks the volume mute status of the video preview
     */
    val muteStatus: MuteStatus

    // data set to reduce memories.
    // The list of Items with all photos and videos
    private val mItemList: MutableLiveData<ViewModelState<MutableList<Item>>> = MutableLiveData()

    // The list of Items with all photos and videos in category
    private val mCategoryItemList: MutableLiveData<ViewModelState<MutableList<Item>>> =
        MutableLiveData()

    // The list of categories.
    private val mCategoryList: MutableLiveData<ViewModelState<MutableList<Category>>> =
        MutableLiveData()
    private var mItemsProvider: ItemsProvider
    var configModel = ConfigModel.default()
        private set(value) {
            field = value
            mItemsProvider.config = value
            if (MimeUtils.isImageOrVideoMediaType(value.mimeType)) {
                mMimeTypeFilter = value.mimeType
            }
        }
    private var mMimeTypeFilter: String? = null

    /**
     * @return BottomSheet state
     */
    /**
     * Set BottomSheet state
     */
    var bottomSheetState = 0
    private var mCurrentCategory: Category? = null

    init {
        val context = application.applicationContext
        mItemsProvider = ItemsProvider(context, configModel)
        selection = Selection()
        muteStatus = MuteStatus()
    }

    @VisibleForTesting
    fun setItemsProvider(itemsProvider: ItemsProvider) {
        mItemsProvider = itemsProvider
    }

    val categoryItems: LiveData<ViewModelState<MutableList<Item>>>
        get() {
            return mCategoryItemList
        }
    val items: LiveData<ViewModelState<MutableList<Item>>>
        get() {
            return mItemList
        }
    val categories: LiveData<ViewModelState<MutableList<Category>>>
        get() {
            return mCategoryList
        }

    fun requestMediasAsync(
        page: Int,
        category: Category = Category.DEFAULT,
        isLoadMore: Boolean = false,
    ) {
        request(if (category == Category.DEFAULT) mItemList else mCategoryItemList) {
            val items: MutableList<Item> = ArrayList()
            mItemsProvider.queryMediaByPage(category, page, configModel.pageSize).use { cursor ->
                if (cursor == null || cursor.count == 0) {
                    Log.d(
                        TAG,
                        "Didn't receive any items for $category, either cursor is null or cursor count is zero"
                    )
                    return@use
                }
                // We only add the RECENT header on the PhotosTabFragment with CATEGORY_DEFAULT. In this
                // case, we call this method {loadItems} with null category. When the category is not
                // empty, we don't show the RECENT header.
                val showRecent = category.isDefault && !isLoadMore
                var recentSize = 0
                var currentDateTaken: Long = 0
                if (showRecent) {
                    // add Recent date header
                    items.add(Item.createDateItem(0))
                }
                while (cursor.moveToNext()) {
                    // here again.
                    val item = Item.fromCursor(cursor)
                    val dateTaken = item.dateTaken
                    // the minimum count of items in recent is not reached
                    if (showRecent && recentSize < RECENT_MINIMUM_COUNT) {
                        recentSize++
                        currentDateTaken = dateTaken
                    }

                    // The date taken of these two images are not on the
                    // same day, add the new date header.
                    if (!DateTimeUtils.isSameDate(currentDateTaken, dateTaken)) {
                        items.add(Item.createDateItem(dateTaken))
                        currentDateTaken = dateTaken
                    }
                    items.add(item)
                }
            }
            items
        }
    }

    fun requestCategories() {
        request(mCategoryList) {
            val categoryList: MutableList<Category> = ArrayList()
            mItemsProvider.queryAlbums().use { cursor ->
                if (cursor == null || cursor.count == 0) {
                    Log.d(
                        TAG,
                        "Didn't receive any categories, either cursor is null or cursor count is zero"
                    )
                    return@use
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val countMap = HashMap<Long, Category>()
                    while (cursor.moveToNext()) {
                        val category = Category.fromCursor(cursor)
                        category.apply {
                            val bucketId = category.bucketId
                            var item = countMap[bucketId]
                            if (item == null) {
                                item = Category(bucketId, id, displayName, coverUri, 1, isIsLocal)
                                countMap[bucketId] = item
                            } else {
                                item.itemCount += 1
                            }
                        }
                    }
                    categoryList.addAll(countMap.values)
                } else {
                    while (cursor.moveToNext()) {
                        val category = Category.fromCursor(cursor)
                        categoryList.add(category)
                    }
                }
                Log.d(TAG, "Loaded " + categoryList.size + " categories")
            }
            categoryList
        }
    }


    /**
     * Return whether the [.mMimeTypeFilter] is `null` or not
     */
    fun hasMimeTypeFilter(): Boolean {
        return !mMimeTypeFilter.isNonEmpty()
    }

    /**
     * Parse values from `intent` and set corresponding fields
     */
    @Throws(IllegalArgumentException::class)
    fun parseValuesFromIntent(intent: Intent) {
        val model: ConfigModel? =
            intent.getParcelableExtraCompat(Intent.EXTRA_INTENT, ConfigModel::class.java)
        configModel = model ?: ConfigModel.default()
        selection.parseSelectionValuesFromIntent(intent)
    }

    companion object {
        const val TAG = "PhotoPicker"
        const val RECENT_MINIMUM_COUNT = 12
        private const val INSTANCE_ID_MAX = 1 shl 15
    }
}