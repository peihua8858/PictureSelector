package com.peihua.picker.core.data.provider

import android.content.Context
import android.provider.MediaStore
import com.peihua.selector.data.model.Category
import com.peihua.selector.data.model.ConfigModel
import com.peihua.selector.data.model.Item
import com.peihua.selector.util.MimeUtils
import com.peihua.selector.util.isAtLeastQ
import java.util.Locale

/**
 * @author：luck
 * @date：2021/11/11 12:53 下午
 * @describe：IBridgeMediaLoader
 */
abstract class IBridgeMediaLoader(
    val context: Context,
    var config: ConfigModel,
    var mimeTypes: Array<String>,
) {

    /**
     * query album list
     */
    abstract fun loadAllAlbum(query: (MutableList<Category>) -> Unit)

    /**
     * page query specified contents
     *
     * @param bucketId
     * @param page
     * @param pageSize
     */
    abstract fun loadPageMediaData(
        category: Category,
        page: Int,
        pageSize: Int,
        query: (MutableList<Item>, Boolean) -> Unit,
    )

    /**
     * query specified contents
     */
    abstract fun loadOnlyInAppDirAllMedia(query: (Category) -> Unit)

    /**
     * A filter declaring which rows to return,
     * formatted as an SQL WHERE clause (excluding the WHERE itself).
     * Passing null will return all rows for the given URI.
     */
    protected abstract fun selection(category: Category, groupBy: String = ""): String?
    protected abstract val selectionArgs: MutableList<String>?

    /**
     * How to order the rows, formatted as an SQL ORDER BY clause (excluding the ORDER BY itself).
     * Passing null will use the default sort order, which may be unordered.
     */
    protected abstract val sortOrder: String?

    /**
     * Get video (maximum or minimum time)
     *
     * @return
     */
    protected val durationCondition: String
        get() {
            val maxS = if (config.filterVideoMaxSecond == 0L) Long.MAX_VALUE else config.filterVideoMaxSecond
            return String.format(
                Locale.CHINA, "%d <%s $COLUMN_DURATION and $COLUMN_DURATION <= %d",
                Math.max(0L, config.filterVideoMinSecond), "=", maxS
            )
        }

    /**
     * Get media size (maxFileSize or miniFileSize)
     *
     * @return
     */
    protected val fileSizeCondition: String
        get() {
            val maxS = if (config.filterMaxFileSize == 0L) Long.MAX_VALUE else config.filterMaxFileSize
            return String.format(
                Locale.ENGLISH,
                "%d <%s " + MediaStore.MediaColumns.SIZE + " and " + MediaStore.MediaColumns.SIZE + " <= %d",
                0L.coerceAtLeast(config.filterMinFileSize),
                "=",
                maxS
            )
        }

    /**
     * getQueryMimeCondition
     *
     * @return
     */
    protected val queryMimeCondition: String
        get() {
            val filters = mimeTypes
            val filterSet = HashSet(filters.toList())
            val iterator: Iterator<String> = filterSet.iterator()
            val stringBuilder = StringBuilder()
            var index = -1
            while (iterator.hasNext()) {
                val value = iterator.next()
                index++
                stringBuilder.append(if (index == 0) " AND " else " OR ").append(MediaStore.MediaColumns.MIME_TYPE)
                    .append("='").append(value).append("'")
            }
            if (!config.isShowGif && !filterSet.contains(MimeUtils.ofGIF())) {
                stringBuilder.append(NOT_GIF)
            }
            if (!config.isShowWebp && !filterSet.contains(MimeUtils.ofWEBP())) {
                stringBuilder.append(NOT_WEBP)
            }
            if (!config.isShowBmp && !filterSet.contains(MimeUtils.ofBMP())
                && !filterSet.contains(MimeUtils.ofXmsBMP())
                && !filterSet.contains(MimeUtils.ofWapBMP())
            ) {
                stringBuilder.append(NOT_BMP).append(NOT_XMS_BMP).append(NOT_VND_WAP_BMP)
            }
            if (!config.isShowHeic && !filterSet.contains(MimeUtils.ofHeic())) {
                stringBuilder.append(NOT_HEIC)
            }
            return stringBuilder.toString()
        }

    /**
     * 查询方式
     */
    val isWithAllQuery: Boolean
        get() {
            return if (isAtLeastQ) {
                true
            } else {
                config.isPageSyncAsCount
            }
        }

    companion object {
        val TAG = IBridgeMediaLoader::class.java.simpleName

        @JvmStatic
        val QUERY_URI = MediaStore.Files.getContentUri("external")

        @JvmStatic
        val ORDER_BY = MediaStore.MediaColumns.DATE_MODIFIED + " DESC"
        const val NOT_GIF = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/gif') "
        const val NOT_WEBP: String = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/webp') "
        const val NOT_BMP: String = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/bmp') "
        const val NOT_XMS_BMP: String = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/x-ms-bmp') "
        const val NOT_VND_WAP_BMP: String = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/vnd.wap.wbmp') "
        const val NOT_HEIC: String = " AND (" + MediaStore.MediaColumns.MIME_TYPE + "!='image/heic') "
        const val GROUP_BY_BUCKET_Id = " GROUP BY (" + MediaStore.MediaColumns.BUCKET_ID
        const val COLUMN_COUNT = "count"
        const val COLUMN_BUCKET_ID = "bucket_id"
        const val COLUMN_DURATION = "duration"

        /**
         * A list of which columns to return. Passing null will return all columns, which is inefficient.
         */
        @JvmStatic
        protected val PROJECTION = arrayOf(
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
        protected val ALL_PROJECTION = arrayOf(
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
    }
}