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
import android.util.Log
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

@RequiresApi(29)
internal open class MediaProviderApi29Impl(context: Context) : MediaProviderApi26Impl(context) {

    override fun queryAllCategories(config: ConfigModel, mimeTypes: Array<String>?, cancellationSignal: CancellationSignal?): Cursor? {
        try {
            return queryAlbums(QUERY_URI, config, mimeTypes, cancellationSignal)
        } catch (e: Throwable) {
            e.printStackTrace()
            return null
        }
    }

    private fun queryAlbums(uri: Uri, config: ConfigModel, mimeTypes: Array<String>?, cancellationSignal: CancellationSignal?): Cursor? {
        if (DEBUG) {
            dLog { "queryAlbums() uri=" + uri + " mimeTypes=" + mimeTypes.contentToString() }
        }
        Trace.beginSection("queryAlbums")

        val extras = Bundle()
        var result: Cursor? = null
        try {
            return context.contentResolver.acquireUnstableContentProviderClient(MediaStore.AUTHORITY).use { client ->
                if (client == null) {
                    eLog { "Unable to acquire unstable content provider for " + MediaStore.AUTHORITY }
                    return null
                }
                val selection = StringBuilder()
                val selectionArgs = ArrayList<String>()
                appendWhereSizeBytes(selection, selectionArgs, config)
                appendWhereMimeTypes(selection, selectionArgs, mimeTypes, config)
                extras.putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection.toString())
                extras.putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs.toTypedArray())
                extras.putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, config.sortOrder.ifNullOrEmpty { ORDER_BY })
                result = client.query(uri, PROJECTION, extras, cancellationSignal)
                return result
            }
        } catch (ignored: RemoteException) {
            // Do nothing, return null.
            eLog { "Failed to query merged albums with extras: $extras \n ${ignored.stackTraceToString()}" }
            return null
        } catch (ignored: PackageManager.NameNotFoundException) {
            eLog { "Failed to query merged albums with extras: $extras \n ${ignored.stackTraceToString()}" }
            return null
        } finally {
            Trace.endSection()
            if (DEBUG) {
                if (result == null) {
                    dLog { "queryAlbums()'s result is null with extras: $extras" }
                } else {
                    dLog { "queryAlbums() loaded " + result.count + " items with extras: $extras" }
                    if (DEBUG_DUMP_CURSORS) {
                        vLog { DatabaseUtils.dumpCursorToString(result) }
                    }
                }
            }
        }
    }
}