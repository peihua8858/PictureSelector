package com.peihua.selector.data.provider

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.DatabaseUtils
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.RemoteException
import android.os.Trace
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.fz.common.collections.isNonEmpty
import com.fz.common.text.ifNullOrEmpty
import com.fz.common.utils.dLog
import com.fz.common.utils.eLog
import com.fz.common.utils.vLog
import com.peihua.selector.data.model.Category
import com.peihua.selector.data.model.ConfigModel
import java.util.function.Supplier
import java.util.stream.Collectors

@RequiresApi(30)
internal class MediaProviderApi30Impl(context: Context) : MediaProviderApi29Impl(context) {
    companion object {
        const val DEFAULT_UID: Int = -1

        const val QUERY_ID_SELECTION: String = "android:query-id-selection"

        // This should be used to indicate if the ids passed in the query arguments should be checked
        // for permission and authority or not. This shall be used for pre-selection uris passed in
        // picker db query operations.
        const val QUERY_SHOULD_SCREEN_SELECTION_URIS: String = "android:query-should-screen-selection-uris"

        /**
         * {@hide}
         * [MediaStore.EXTRA_CALLING_PACKAGE_UID]
         */
        const val EXTRA_CALLING_PACKAGE_UID: String = "calling_package_uid"
    }

    override fun queryAllItems(
        category: Category,
        page: Int,
        config: ConfigModel,
        mimeTypes: Array<String>?,
        cancellationSignal: CancellationSignal?,
    ): Cursor? {
        try {
            return queryMedia(QUERY_URI, page, config, mimeTypes, category, cancellationSignal)
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }
    }

    private fun queryMedia(
        uri: Uri, page: Int, config: ConfigModel,
        mimeTypes: Array<String>?, category: Category,
        cancellationSignal: CancellationSignal?,
    ): Cursor? {
        return queryMedia(uri, page, config, mimeTypes, category, null, DEFAULT_UID, false, cancellationSignal)
    }

    @Throws(IllegalStateException::class)
    private fun queryMedia(
        uri: Uri, page: Int, config: ConfigModel,
        mimeTypes: Array<String>?, category: Category,
        preselectedUris: MutableList<Uri?>?, callingPackageUid: Int,
        shouldScreenSelectionUris: Boolean, cancellationSignal: CancellationSignal?,
    ): Cursor? {
        if (DEBUG) {
            dLog { "queryMedia() uri=" + uri + " cat=" + category + " mimeTypes=" + mimeTypes.contentToString() + " limit=" + config.pageSize }
        }
        Trace.beginSection("queryMedia")

        val extras = Bundle()
        var result: Cursor? = null
        try {
            context.contentResolver.acquireUnstableContentProviderClient(MediaStore.AUTHORITY).use { client ->
                if (client == null) {
                    eLog { "Unable to acquire unstable content provider for " + MediaStore.AUTHORITY }
                    return null
                }
                val selection = StringBuilder()
                val selectionArgs = ArrayList<String>()
                appendWhereSizeBytes(selection, selectionArgs, config)
                appendWhereDuration(selection, selectionArgs, mimeTypes, config)
                appendWhereMimeTypes(selection, selectionArgs, mimeTypes, config)
                appendWhereBucketId(selection, selectionArgs, category)
                extras.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection.toString())
                extras.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs.toTypedArray())
                extras.putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, config.sortOrder.ifNullOrEmpty { ORDER_BY })
                extras.putString(ContentResolver.QUERY_ARG_SQL_LIMIT, queryPageLimit(config.pageSize, page))
                extras.putStringArray(ContentResolver.QUERY_ARG_GROUP_COLUMNS, arrayOf(MediaStore.MediaColumns._ID))
                if (preselectedUris.isNonEmpty()) {
                    extras.putStringArrayList(
                        QUERY_ID_SELECTION, preselectedUris.stream()
                            .map { it.toString() }
                            .collect(Collectors.toCollection(Supplier { ArrayList() }))
                    )
                }
                extras.putInt(EXTRA_CALLING_PACKAGE_UID, callingPackageUid)
                extras.putBoolean(QUERY_SHOULD_SCREEN_SELECTION_URIS, shouldScreenSelectionUris)
                result = client.query(uri, PROJECTION, extras, cancellationSignal)
                return result
            }
        } catch (ignored: RemoteException) {
            // Do nothing, return null.
            eLog { "Failed to query merged media with extras: $extras  \n ${ignored.stackTraceToString()}" }
            return null
        } catch (ignored: PackageManager.NameNotFoundException) {
            eLog { "Failed to query merged media with extras: $extras  \n ${ignored.stackTraceToString()}" }
            return null
        } finally {
            Trace.endSection()
            if (DEBUG) {
                if (result == null) {
                    dLog { "queryMedia()'s result is null with extras: $extras" }
                } else {
                    dLog { "queryMedia() loaded " + result.count + " items with extras: $extras" }
                    if (DEBUG_DUMP_CURSORS) {
                        vLog { DatabaseUtils.dumpCursorToString(result) }
                    }
                }
            }
        }
    }
}