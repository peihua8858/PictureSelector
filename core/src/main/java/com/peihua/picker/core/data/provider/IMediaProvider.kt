package com.peihua.picker.core.data.provider

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.CancellationSignal
import android.provider.MediaStore
import com.fz.common.utils.dLog
import com.fz.common.utils.isAtLeastO
import com.fz.common.utils.isAtLeastQ
import com.fz.common.utils.isAtLeastR
import com.peihua.selector.data.model.Category
import com.peihua.selector.data.model.ConfigModel
import com.peihua.selector.data.model.PictureMimeType
import com.peihua.selector.util.MimeUtils
import com.peihua.selector.util.deleteEndChar

abstract class IMediaProvider(protected val context: Context) {
    /**
     * query album list
     */
    abstract fun queryAllCategories(
        config: ConfigModel,
        mimeTypes: Array<String>,
        cancellationSignal: CancellationSignal?,
    ): Cursor?

    /**
     * query album list
     */
    abstract fun queryAllItems(
        category: Category,
        page: Int,
        config: ConfigModel,
        mimeTypes: Array<String>,
        cancellationSignal: CancellationSignal?,
    ): Cursor?

    companion object {
        const val DEBUG: Boolean = true
        const val DEBUG_DUMP_CURSORS: Boolean = false
        val QUERY_URI: Uri = MediaStore.Files.getContentUri("external")
        const val ORDER_BY = MediaStore.MediaColumns.DATE_ADDED + " DESC";
        const val NOT_GIF = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/gif') "
        const val NOT_WEBP: String = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/webp') "
        const val NOT_BMP: String = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/bmp') "
        const val NOT_XMS_BMP: String = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/x-ms-bmp') "
        const val NOT_VND_WAP_BMP: String = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/vnd.wap.wbmp') "
        const val NOT_HEIC: String = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/heic') "
        const val GROUP_BY_BUCKET_Id = " GROUP BY (" + MediaStore.MediaColumns.BUCKET_ID
        const val WHERE_MIME_TYPE_LIKE: String = MediaStore.MediaColumns.MIME_TYPE + " LIKE  "
        const val WHERE_MEDIA_TYPE: String = MediaStore.Files.FileColumns.MEDIA_TYPE + " = ? "
        const val WHERE_MAX_SIZE_BYTES: String = MediaStore.MediaColumns.SIZE + " <=  "
        const val WHERE_MIN_SIZE_BYTES: String = MediaStore.MediaColumns.SIZE + " >=  "
        const val WHERE_MAX_COLUMN_DURATION = MediaStore.MediaColumns.DURATION + " <=  "
        const val WHERE_MIN_COLUMN_DURATION = MediaStore.MediaColumns.DURATION + " >=  "
        const val WHERE_BUCKET_ID = MediaStore.MediaColumns.BUCKET_ID + " = ? "
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
            MediaStore.MediaColumns.DATE_TAKEN,
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
            MediaStore.MediaColumns.DATE_TAKEN,
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

    val queryPageLimit: (Int, Int) -> String = { pageSize, page ->
        val offset = (page - 1) * pageSize
        if (isAtLeastR) "$pageSize offset $offset" else "limit $pageSize offset $offset"
    }

    open fun createPageSelectionAndArgs(category: Category, mimeTypes: Array<String>, config: ConfigModel): Pair<String, Array<String>> {
        val selection = StringBuilder()
        val selectionArgs = ArrayList<String>()
        val timeCondition = getTimeCondition(config)
        val fileSizeCondition = getSizeCondition(config)
        var count = 0
        selection.append("(")
        if (MimeUtils.isImageMimeType(mimeTypes)) {
            selection.append(WHERE_MEDIA_TYPE).append(getImageMimeTypesCondition(mimeTypes, config))
                .append(" OR ")
            selectionArgs.add(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
            count++
        }
        if (MimeUtils.isVideoMimeType(mimeTypes)) {
            selection.append("(").append(WHERE_MEDIA_TYPE).append(getVideoMimeTypeCondition(mimeTypes)).append(" AND ")
                .append(timeCondition).append(")").append(" OR ")
            selectionArgs.add(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
            count++
        }
        if (MimeUtils.isAudioMimeType(mimeTypes)) {
            selection.append("(").append(WHERE_MEDIA_TYPE).append(getAudioMimeTypeCondition(mimeTypes)).append(" AND ")
                .append(timeCondition).append(")")
            selectionArgs.add(MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO.toString())
            count++
        }
        dLog { "createPageSelectionAndArgs: selection=$selection, selectionArgs=$selectionArgs" }
        selection.deleteEndChar("OR ")
        if (count >= 2) {
            selection.append(")")
        } else {
            selection.deleteCharAt(0)
        }
        selection.append(" AND ")
        selection.append(fileSizeCondition)
        if (category != Category.DEFAULT) {
            selection.append(if (selection.isNotEmpty()) " AND " else " ")
            selection.append(WHERE_BUCKET_ID)
            selectionArgs.add(category.bucketId.toString())
        }
        dLog { "createPageSelectionAndArgs: selection=$selection, selectionArgs=$selectionArgs" }
        return Pair(selection.toString(), selectionArgs.toTypedArray())
    }

    fun getSizeCondition(config: ConfigModel): String {
        val selection = StringBuilder()
        val maxS = if (config.filterMaxFileSize == 0L) Long.MAX_VALUE else config.filterMaxFileSize
        selection.append(if (selection.isNotEmpty()) " AND " else " ")
        selection.append(WHERE_MIN_SIZE_BYTES).append(config.filterMinFileSize)
        selection.append(" AND ")
        selection.append(WHERE_MAX_SIZE_BYTES).append(maxS)
        return selection.toString()
    }

    fun getTimeCondition(config: ConfigModel): String {
        val selection = StringBuilder()
        val maxS = if (config.filterVideoMaxSecond == 0L) Long.MAX_VALUE else config.filterVideoMaxSecond
        selection.append(if (selection.isNotEmpty()) " AND " else " ")
        selection.append(WHERE_MIN_COLUMN_DURATION).append(config.filterVideoMinSecond)
        selection.append(" AND ")
        selection.append(WHERE_MAX_COLUMN_DURATION).append(maxS)
        return selection.toString()
    }

    /**
     * 图片类型条件
     * @param mimeTypes
     * @param config
     */
    fun getImageMimeTypesCondition(mimeTypes: Array<String>, config: ConfigModel): String {
        val selections = StringBuilder()
        for ((index, mimeType) in mimeTypes.withIndex()) {
            if (!MimeUtils.isImageMimeType(mimeType)) {
                continue
            }
            selections.append(if (index == 0) " AND " else " OR ").append(WHERE_MIME_TYPE_LIKE)
                .append("'").append(mimeType.replace('*', '%')).append("'")
        }
        selections.deleteEndChar("OR ")
        if (!config.isShowGif && !mimeTypes.contains(PictureMimeType.ofGIF())) {
            selections.append(NOT_GIF)
        }
        if (!config.isShowWebp && !mimeTypes.contains(PictureMimeType.ofWEBP())) {
            selections.append(NOT_WEBP)
        }
        if (!config.isShowBmp && !mimeTypes.contains(PictureMimeType.ofBMP()) && !mimeTypes.contains(
                PictureMimeType.ofXmsBMP()
            ) && !mimeTypes.contains(PictureMimeType.ofWapBMP())
        ) {
            selections.append(NOT_BMP).append(NOT_XMS_BMP).append(NOT_VND_WAP_BMP)
        }
        if (!config.isShowHeic && !mimeTypes.contains(PictureMimeType.ofHeic())) {
            selections.append(NOT_HEIC)
        }
        return selections.toString()
    }

    protected fun getVideoMimeTypeCondition(mimeTypes: Array<String>): String {
        val selections = java.lang.StringBuilder()
        for ((index, mimeType) in mimeTypes.withIndex()) {
            if (!MimeUtils.isVideoMimeType(mimeType)) {
                continue
            }
            selections.append(if (index == 0) " AND " else " OR ")
                .append(WHERE_MIME_TYPE_LIKE).append("'").append(mimeType)
                .append("'")
        }
        selections.deleteEndChar("OR ")
        return selections.toString()
    }

    protected fun getAudioMimeTypeCondition(mimeTypes: Array<String>): String {
        val selections = java.lang.StringBuilder()
        for ((index, mimeType) in mimeTypes.withIndex()) {
            if (!MimeUtils.isAudioMimeType(mimeType)) {
                continue
            }
            selections.append(if (index == 0) " AND " else " OR ")
                .append(WHERE_MIME_TYPE_LIKE).append("'").append(mimeType)
                .append("'")
        }
        selections.deleteEndChar("OR ")
        return selections.toString()
    }
}