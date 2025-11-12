package com.peihua.selector.data.provider

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.CancellationSignal
import android.provider.MediaStore
import com.fz.common.utils.isAtLeastO
import com.fz.common.utils.isAtLeastQ
import com.fz.common.utils.isAtLeastR
import com.peihua.selector.data.model.Category
import com.peihua.selector.data.model.ConfigModel
import com.peihua.selector.util.MimeUtils

abstract class IMediaProvider(protected val context: Context) {
    abstract fun queryAllCategories(
        config: ConfigModel,
        mimeTypes: Array<String>?,
        cancellationSignal: CancellationSignal?,
    ): Cursor?

    /**
     * query album list
     */
    abstract fun queryAllItems(
        category: Category,
        page: Int,
        config: ConfigModel,
        mimeTypes: Array<String>?,
        cancellationSignal: CancellationSignal?,
    ): Cursor?

    companion object {
        const val DEBUG: Boolean = true
        const val DEBUG_DUMP_CURSORS: Boolean = false
        val QUERY_URI: Uri = MediaStore.Files.getContentUri("external")
        const val ORDER_BY = MediaStore.MediaColumns.DATE_MODIFIED + " DESC"
        const val NOT_GIF = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/gif') "
        const val NOT_WEBP: String = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/webp') "
        const val NOT_BMP: String = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/bmp') "
        const val NOT_XMS_BMP: String = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/x-ms-bmp') "
        const val NOT_VND_WAP_BMP: String = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/vnd.wap.wbmp') "
        const val NOT_HEIC: String = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/heic') "
        const val GROUP_BY_BUCKET_Id = " GROUP BY (" + MediaStore.MediaColumns.BUCKET_ID
        const val WHERE_MIME_TYPE: String = MediaStore.MediaColumns.MIME_TYPE + " LIKE ? "
        const val WHERE_MAX_SIZE_BYTES: String = MediaStore.MediaColumns.SIZE + " <= ? "
        const val WHERE_MIN_SIZE_BYTES: String = MediaStore.MediaColumns.SIZE + " >= ? "
        const val WHERE_MAX_COLUMN_DURATION = MediaStore.MediaColumns.DURATION + " <= ? "
        const val WHERE_MIN_COLUMN_DURATION = MediaStore.MediaColumns.DURATION + " >= ? "
        const val COLUMN_COUNT = "count"

        /**
         * A list of which columns to return. Passing null will return all columns, which is inefficient.
         */
        @JvmStatic
        val PROJECTION = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.ORIENTATION,
        )

        /**
         * A list of which columns to return. Passing null will return all columns, which is inefficient.
         */
        @JvmStatic
        val ALL_PROJECTION = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DURATION,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.ORIENTATION,
            "COUNT(*) AS $COLUMN_COUNT"
        )

        fun create(context: Context): IMediaProvider {
            return when {
                isAtLeastR -> MediaProviderApi30Impl(context)
                isAtLeastQ -> MediaProviderApi29Impl(context)
                isAtLeastO -> MediaProviderApi26Impl(context)
                else -> MediaProviderApi24Impl(context)
            }
        }
    }

    protected fun appendWhereMimeTypes(selection: StringBuilder, selectArgs: MutableList<String>, mimeTypes: Array<String>?, config: ConfigModel) {
        val whereMimeTypes = ArrayList<String>()
        selection.append(if (selection.isNotEmpty()) " AND (" else " (")
        mimeTypes?.forEach {
            whereMimeTypes.add(WHERE_MIME_TYPE)
            selectArgs.add(it.replace('*', '%'))
        }
        if (whereMimeTypes.isNotEmpty()) {
            selection.append(whereMimeTypes.joinToString(" OR "))
        }
        val filterSet = HashSet(mimeTypes?.toList() ?: listOf())
        if (!config.isShowGif && !filterSet.contains(MimeUtils.ofGIF())) {
            selection.append(NOT_GIF)
        }
        if (!config.isShowWebp && !filterSet.contains(MimeUtils.ofWEBP())) {
            selection.append(NOT_WEBP)
        }
        if (!config.isShowBmp && !filterSet.contains(MimeUtils.ofBMP())
            && !filterSet.contains(MimeUtils.ofXmsBMP())
            && !filterSet.contains(MimeUtils.ofWapBMP())
        ) {
            selection.append(NOT_BMP)
                .append(NOT_XMS_BMP)
                .append(NOT_VND_WAP_BMP)
        }
        if (!config.isShowHeic && !filterSet.contains(MimeUtils.ofHeic())) {
            selection.append(NOT_HEIC)
        }
        selection.append(")")
    }

    protected fun appendWhereSizeBytes(selection: StringBuilder, selectArgs: MutableList<String>, config: ConfigModel) {
        val maxS = if (config.filterMaxFileSize == 0L) Long.MAX_VALUE else config.filterMaxFileSize
        selection.append(if (selection.isNotEmpty()) " AND " else " ")
        selection.append(WHERE_MIN_SIZE_BYTES)
        selection.append(" AND ")
        selection.append(WHERE_MAX_SIZE_BYTES)
        selectArgs.add(config.filterMinFileSize.toString())
        selectArgs.add(maxS.toString())
    }

    protected fun appendWhereDuration(selection: StringBuilder, selectArgs: MutableList<String>, mimeTypes: Array<String>?, config: ConfigModel) {
        if (MimeUtils.isVideoMimeType(mimeTypes)) {
            val maxS = if (config.filterVideoMaxSecond == 0L) Long.MAX_VALUE else config.filterVideoMaxSecond
            selection.append(if (selection.isNotEmpty()) " AND " else " ")
            selection.append(WHERE_MAX_COLUMN_DURATION)
            selection.append(" AND ")
            selection.append(WHERE_MIN_COLUMN_DURATION)
            selectArgs.add(config.filterVideoMinSecond.toString())
            selectArgs.add(maxS.toString())
        }
    }

    val queryPageLimit: (Int, Int) -> String = { pageSize, page ->
        val offset = (page - 1) * pageSize
        if (isAtLeastR) "$pageSize offset $offset" else "limit $pageSize offset $offset"
    }

    fun appendWhereBucketId(selection: StringBuilder, selectArgs: MutableList<String>, category: Category) {
        if (category != Category.DEFAULT) {
            selection.append(if (selection.isNotEmpty()) " AND " else " ")
            selection.append(MediaStore.MediaColumns.BUCKET_ID)
            selection.append(" = ? ")
            selectArgs.add(category.bucketId.toString())
        }
    }
}