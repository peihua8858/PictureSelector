package com.peihua.selector.data.provider

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.provider.MediaStore
import android.util.Log
import com.fz.common.text.isNonEmpty
import com.fz.common.utils.isAtLeastN
import com.fz.common.utils.toLong
import com.peihua.selector.data.model.Category
import com.peihua.selector.data.model.ConfigModel
import com.peihua.selector.data.model.Item
import com.peihua.selector.util.DateTimeUtils
import com.peihua.selector.util.MimeUtils
import com.peihua.selector.util.isAtLeastO
import com.peihua.selector.util.isAtLeastQ
import com.peihua.selector.util.isAtLeastR
import com.peihua.selector.viewmodel.PickerViewModel
import java.io.File

/**
 * Provides image and video items from [MediaStore] collection to the Photo Picker.
 */
class ItemsProvider(context: Context, config: ConfigModel) : IBridgeMediaLoader(context, config,arrayOf("image/*", "image/*")) {

    @Throws(IllegalStateException::class)
    fun queryMediaByPage(
        category: Category,
        page: Int,
        pageSize: Int,
    ): Cursor? {
        val client = context.contentResolver
        val selectionArgs = selectionArgs
        if (category != Category.DEFAULT) {
            selectionArgs?.add(category.bucketId.toString())
        }
        val offset=(page - 1) * pageSize
        return if (isAtLeastR) {
            val queryArgs = createQueryArgsBundle(
                selection(category), selectionArgs?.toTypedArray(),
                pageSize,
                offset
            )
            client.query(QUERY_URI, PROJECTION, queryArgs, null)
        } else {
            client.query(
                QUERY_URI, PROJECTION, selection(category), selectionArgs?.toTypedArray(),
                "$sortOrder limit $pageSize offset $offset"
            )
        }
    }

    fun createQueryArgsBundle(
        selection: String?,
        selectionArgs: Array<String>?,
        limitCount: Int,
        offset: Int,
    ): Bundle {
        val queryArgs = Bundle()
        if (isAtLeastO) {
            queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            queryArgs.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            queryArgs.putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
            queryArgs.putStringArray(QUERY_ARG_MIME_TYPE, mimeTypes)
            if (isAtLeastR) {
                if (limitCount > 0 && offset >= 0) {
                    queryArgs.putString(ContentResolver.QUERY_ARG_SQL_LIMIT, "$limitCount offset $offset")
                }
            }
        }
        return queryArgs
    }

    fun queryAlbums(): Cursor? {
        val extras = Bundle()
        return try {
            val client = context.contentResolver
            if (client == null) {
                Log.e(TAG, "Unable to acquire unstable content provider for $QUERY_URI")
                return null
            }
            return if (isAtLeastQ) {
                val queryArgs = createQueryArgsBundle(
                    selection(Category.DEFAULT), selectionArgs?.toTypedArray(),
                    -1, -1
                )
                client.query(QUERY_URI, PROJECTION, queryArgs, null)
            } else {
                client.query(
                    QUERY_URI,
                    ALL_PROJECTION,
                    selection(Category.DEFAULT, GROUP_BY_BUCKET_Id),
                    selectionArgs?.toTypedArray(),
                    sortOrder
                )
            }
        } catch (ignored: RemoteException) {
            // Do nothing, return null.
            Log.w(TAG, "Failed to query merged albums with extras: $extras.", ignored)
            null
        } catch (ignored: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Failed to query merged albums with extras: $extras.", ignored)
            null
        }
    }

    companion object {
        private val TAG = ItemsProvider::class.java.simpleName

        @JvmStatic
        fun getItemsUri(id: String, mimeType: String?, realPath: String): Uri {
            if (isAtLeastN) {
                val contentUri: Uri = if (MimeUtils.isImageMimeType(mimeType)) {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                } else if (MimeUtils.isVideoMimeType(mimeType)) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else if (MimeUtils.isAudioMimeType(mimeType)) {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Files.getContentUri("external")
                }
                return ContentUris.withAppendedId(contentUri, id.toLong())
            }
            return Uri.fromFile(File(realPath))
        }
    }

    override fun getAlbumFirstCover(bucketId: Long): String? {
        return null
    }

    override fun loadAllAlbum(query: (MutableList<Category>) -> Unit) {
        val categoryList: MutableList<Category> = ArrayList()
        queryAlbums().use { cursor ->
            if (cursor == null || cursor.count == 0) {
                Log.d(
                    TAG, "Didn't receive any categories, either cursor is null or"
                            + " cursor count is zero"
                )
                query.invoke(categoryList)
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
            query.invoke(categoryList)
        }
    }

    override fun loadPageMediaData(
        category: Category,
        page: Int,
        pageSize: Int,
        query: (MutableList<Item>, Boolean) -> Unit,
    ) {
        val items: MutableList<Item> = ArrayList()
        queryMediaByPage(category, page, pageSize).use { cursor ->
            if (cursor == null || cursor.count == 0) {
                Log.d(
                    PickerViewModel.TAG,
                    "Didn't receive any items for $category, either cursor is null or cursor count is zero"
                )
                query.invoke(items, false)
                return@use
            }

            // We only add the RECENT header on the PhotosTabFragment with CATEGORY_DEFAULT. In this
            // case, we call this method {loadItems} with null category. When the category is not
            // empty, we don't show the RECENT header.
            val showRecent = category.isDefault
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
                if (showRecent && recentSize < PickerViewModel.RECENT_MINIMUM_COUNT) {
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
        Log.d(PickerViewModel.TAG, "Loaded " + items.size + " items in " + category)
        query.invoke(items, true)
    }

    override fun loadOnlyInAppDirAllMedia(query: (Category) -> Unit) {
    }

    override fun selection(category: Category, groupBy: String): String? {
        val durationCondition: String = durationCondition
        val fileSizeCondition: String = fileSizeCondition
        val queryMimeCondition: String = queryMimeCondition
        if (MimeUtils.isImageAndVideoMediaType(config.mimeType)) {
            return getSelectionArgsForAllMediaCondition(
                durationCondition,
                fileSizeCondition,
                queryMimeCondition,
                category,
                groupBy
            )
        } else if (MimeUtils.isImageMimeType(config.mimeType)) {
            return getSelectionArgsForImageMediaCondition(fileSizeCondition, queryMimeCondition, category, groupBy)
        } else if (MimeUtils.isVideoMimeType(config.mimeType)) {
            return getSelectionArgsForVideoMediaCondition(durationCondition, queryMimeCondition, category, groupBy)
        } else if (MimeUtils.isAudioMimeType(config.mimeType)) {
            return getSelectionArgsForAudioMediaCondition(durationCondition, queryMimeCondition, category, groupBy)
        }
        return null
    }

    override val selectionArgs: MutableList<String>?
        get() {
            if (MimeUtils.isImageAndVideoMediaType(config.mimeType)) {
                return arrayListOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
                )
            } else if (MimeUtils.isImageMimeType(config.mimeType)) {
                return arrayListOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
            } else if (MimeUtils.isVideoMimeType(config.mimeType)) {
                return arrayListOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
            } else if (MimeUtils.isAudioMimeType(config.mimeType)) {
                return arrayListOf(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO.toString())
            }
            return null
        }
    override val sortOrder: String
        get() = config.sortOrder.ifEmpty { ORDER_BY }

    private fun getSelectionArgsForAllMediaCondition(
        timeCondition: String,
        sizeCondition: String,
        queryMimeTypeOptions: String,
        category: Category,
        groupBy: String,
    ): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("(").append(MediaStore.Files.FileColumns.MEDIA_TYPE).append("=?")
            .append(queryMimeTypeOptions).append(" OR ")
            .append(MediaStore.Files.FileColumns.MEDIA_TYPE).append("=? AND ").append(timeCondition).append(") AND ")
            .append(sizeCondition)

        if (category != Category.DEFAULT) {
            stringBuilder.append(" AND ").append(COLUMN_BUCKET_ID).append("=? ")
        }
        return if (isWithAllQuery) {
            stringBuilder.toString()
        } else {
            if (groupBy.isNonEmpty()) {
                stringBuilder.append(")").append(groupBy).toString()
            } else {
                stringBuilder.toString()
            }
        }
    }

    /**
     * Query conditions in image modes
     *
     * @param fileSizeCondition
     * @param queryMimeTypeOptions
     * @return
     */
    private fun getSelectionArgsForImageMediaCondition(
        fileSizeCondition: String,
        queryMimeTypeOptions: String,
        category: Category,
        groupBy: String,
    ): String {
        val stringBuilder = java.lang.StringBuilder()
        return if (isWithAllQuery) {
            stringBuilder.append(MediaStore.Files.FileColumns.MEDIA_TYPE).append("=?")
            if (category != Category.DEFAULT) {
                stringBuilder.append(" AND ").append(COLUMN_BUCKET_ID).append("=? ")
            }
            stringBuilder.append(queryMimeTypeOptions).append(" AND ").append(fileSizeCondition).toString()
        } else {
            stringBuilder.append("(").append(MediaStore.Files.FileColumns.MEDIA_TYPE).append("=?")
                .append(queryMimeTypeOptions).append(") AND ").append(fileSizeCondition)
            if (category != Category.DEFAULT) {
                stringBuilder.append(" AND ").append(COLUMN_BUCKET_ID).append("=? ").toString()
            }
            if (groupBy.isNonEmpty()) {
                stringBuilder.append(")").append(groupBy).toString()
            } else {
                stringBuilder.toString()
            }
        }

    }

    /**
     * Video mode conditions
     *
     * @param durationCondition
     * @param queryMimeCondition
     * @return
     */
    private fun getSelectionArgsForVideoMediaCondition(
        durationCondition: String,
        queryMimeCondition: String,
        category: Category,
        groupBy: String,
    ): String {
        val stringBuilder = java.lang.StringBuilder()
        return if (isWithAllQuery) {
            stringBuilder.append(MediaStore.Files.FileColumns.MEDIA_TYPE).append("=?").append(queryMimeCondition)
                .append(" AND ")
            if (category != Category.DEFAULT) {
                stringBuilder.append(COLUMN_BUCKET_ID).append("=? AND ")
            }
            stringBuilder.append(durationCondition).toString()
        } else {
            stringBuilder.append("(").append(MediaStore.Files.FileColumns.MEDIA_TYPE).append("=?")
                .append(queryMimeCondition).append(") AND ").append(durationCondition)
            if (category != Category.DEFAULT) {
                stringBuilder.append(" AND ").append(COLUMN_BUCKET_ID).append("=? ").toString()
            }
            if (groupBy.isNonEmpty()) {
                stringBuilder.append(")").append(groupBy).toString()
            } else {
                stringBuilder.toString()
            }
        }
    }

    /**
     * Audio mode conditions
     *
     * @param durationCondition
     * @param queryMimeCondition
     * @return
     */
    private fun getSelectionArgsForAudioMediaCondition(
        durationCondition: String,
        queryMimeCondition: String,
        category: Category,
        groupBy: String,
    ): String {
        val stringBuilder = java.lang.StringBuilder()
        return if (isWithAllQuery) {
            stringBuilder.append(MediaStore.Files.FileColumns.MEDIA_TYPE).append("=?").append(queryMimeCondition)
                .append(" AND ").append(durationCondition)
            if (category != Category.DEFAULT) {
                stringBuilder.append(" AND ").append(COLUMN_BUCKET_ID).append("=? ").toString()
            } else {
                stringBuilder.toString()
            }
        } else {
            stringBuilder.append("(").append(MediaStore.Files.FileColumns.MEDIA_TYPE).append("=?")
                .append(queryMimeCondition).append(") AND ").append(durationCondition)
            if (category != Category.DEFAULT) {
                stringBuilder.append(" AND ").append(COLUMN_BUCKET_ID).append("=? ")
            }
            if (groupBy.isNonEmpty()) {
                stringBuilder.append(")").append(groupBy).toString()
            } else {
                stringBuilder.toString()
            }
        }
    }
}