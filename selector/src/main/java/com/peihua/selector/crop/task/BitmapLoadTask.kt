package com.peihua.selector.crop.task

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import com.fz.common.coroutine.ModernAsyncTask
import com.peihua.selector.crop.OkHttpClientStore
import com.peihua.selector.crop.callback.BitmapLoadCallback
import com.peihua.selector.crop.model.ExifInfo
import com.peihua.selector.crop.task.BitmapLoadTask.BitmapWorkerResult
import com.peihua.selector.crop.util.BitmapLoadUtils
import com.peihua.selector.util.adjustBitmapOrientation
import com.peihua.selector.util.orientationMatrix
import okhttp3.Request
import okhttp3.Response
import okio.BufferedSource
import okio.Sink
import okio.sink
import java.io.IOException
import java.lang.ref.WeakReference

/**
 * Creates and returns a Bitmap for a given Uri(String url).
 * inSampleSize is calculated based on requiredWidth property. However can be adjusted if OOM occurs.
 * If any EXIF config is found - bitmap is transformed properly.
 */
class BitmapLoadTask(
    context: Context,
    private var inputUri: Uri,
    private val outputUri: Uri?,
    private val requiredWidth: Int,
    private val requiredHeight: Int,
    private val loadCallback: BitmapLoadCallback,
) : ModernAsyncTask<Void, Void, BitmapWorkerResult>() {
    private val mContext: WeakReference<Context> = WeakReference<Context>(context)

    class BitmapWorkerResult {
        var mBitmapResult: Bitmap? = null
        var mExifInfo: ExifInfo? = null
        var mBitmapWorkerException: Exception? = null

        constructor(bitmapResult: Bitmap, exifInfo: ExifInfo) {
            mBitmapResult = bitmapResult
            mExifInfo = exifInfo
        }

        constructor(bitmapWorkerException: Exception) {
            mBitmapWorkerException = bitmapWorkerException
        }
    }

    @SuppressLint("Recycle")
    override suspend fun doInBackground(vararg params: Void): BitmapWorkerResult {
        val context = mContext.get()
        if (context == null) {
            return BitmapWorkerResult(NullPointerException("context is null"))
        }

        try {
            processInputUri()
        } catch (e: NullPointerException) {
            return BitmapWorkerResult(e)
        } catch (e: IOException) {
            return BitmapWorkerResult(e)
        }

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        try {
            val stream = context.contentResolver.openInputStream(inputUri)
            BitmapFactory.decodeStream(stream, null, options)
            options.inSampleSize = BitmapLoadUtils.computeSize(options.outWidth, options.outHeight)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        options.inJustDecodeBounds = false

        var decodeSampledBitmap: Bitmap? = null

        var decodeAttemptSuccess = false
        while (!decodeAttemptSuccess) {
            try {
                val stream = context.contentResolver.openInputStream(inputUri)
                try {
                    decodeSampledBitmap = BitmapFactory.decodeStream(stream, null, options)
                    if (options.outWidth == -1 || options.outHeight == -1) {
                        return BitmapWorkerResult(IllegalArgumentException("Bounds for bitmap could not be retrieved from the Uri: [$inputUri]"))
                    }
                } finally {
                    BitmapLoadUtils.close(stream)
                }
                if (BitmapLoadUtils.checkSize(decodeSampledBitmap, options)) continue
                decodeAttemptSuccess = true
            } catch (error: OutOfMemoryError) {
                Log.e(TAG, "doInBackground: BitmapFactory.decodeFileDescriptor: ", error)
                options.inSampleSize *= 2
            } catch (e: IOException) {
                Log.e(TAG, "doInBackground: ImageDecoder.createSource: ", e)
                return BitmapWorkerResult(
                    IllegalArgumentException(
                        "Bitmap could not be decoded from the Uri: [$inputUri]",
                        e
                    )
                )
            }
        }

        if (decodeSampledBitmap == null) {
            return BitmapWorkerResult(IllegalArgumentException("Bitmap could not be decoded from the Uri: [$inputUri]"))
        }
        val exifOrientation = BitmapLoadUtils.getExifOrientation(context, inputUri)
        val exifDegrees = BitmapLoadUtils.exifToDegrees(exifOrientation)
        val exifTranslation = BitmapLoadUtils.exifToTranslation(exifOrientation)

        val exifInfo = ExifInfo(exifOrientation, exifDegrees, exifTranslation)
        try {
            context.contentResolver.openInputStream(inputUri)
                .use {
                    val bitmap = it.adjustBitmapOrientation(decodeSampledBitmap)
                    if (bitmap != null) {
                        return BitmapWorkerResult(bitmap, exifInfo)
                    }
                }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return BitmapWorkerResult(decodeSampledBitmap, exifInfo)
    }

    @Throws(NullPointerException::class, IOException::class)
    private fun processInputUri() {
        val inputUriScheme = inputUri.scheme
        Log.d(TAG, "Uri scheme: $inputUriScheme")
        if ("http" == inputUriScheme || "https" == inputUriScheme) {
            try {
                downloadFile(inputUri, outputUri)
            } catch (e: NullPointerException) {
                Log.e(TAG, "Downloading failed", e)
                throw e
            } catch (e: IOException) {
                Log.e(TAG, "Downloading failed", e)
                throw e
            }
        } else if ("file" != inputUriScheme && "content" != inputUriScheme) {
            Log.e(TAG, "Invalid Uri scheme $inputUriScheme")
            throw IllegalArgumentException("Invalid Uri scheme$inputUriScheme")
        }
    }

    @Throws(NullPointerException::class, IOException::class)
    private fun downloadFile(inputUri: Uri, outputUri: Uri?) {
        Log.d(TAG, "downloadFile")

        if (outputUri == null) {
            throw NullPointerException("Output Uri is null - cannot download image")
        }

        val context = mContext.get()
        if (context == null) {
            throw NullPointerException("Context is null")
        }

        val client = OkHttpClientStore.INSTANCE.getClient()

        var source: BufferedSource? = null
        var sink: Sink? = null
        var response: Response? = null
        try {
            val request = Request.Builder()
                .url(inputUri.toString())
                .build()
            response = client.newCall(request).execute()
            source = response.body?.source()

            val outputStream = context.contentResolver.openOutputStream(outputUri)
            if (outputStream != null) {
                sink = outputStream.sink()
                source?.readAll(sink)
            } else {
                throw NullPointerException("OutputStream for given output Uri is null")
            }
        } finally {
            BitmapLoadUtils.close(source)
            BitmapLoadUtils.close(sink)
            if (response != null) {
                BitmapLoadUtils.close(response.body)
            }
            client.dispatcher.cancelAll()

            // swap uris, because input image was downloaded to the output destination
            // (cropped image will override it later)
            this.inputUri = outputUri
        }
    }

    override fun onPostExecute(result: BitmapWorkerResult?) {
        if (result == null) {
            return
        }
        if (result.mBitmapWorkerException == null) {
            loadCallback.onBitmapLoaded(
                result.mBitmapResult!!,
                result.mExifInfo!!,
                inputUri,
                outputUri
            )
        } else {
            loadCallback.onFailure(result.mBitmapWorkerException!!)
        }
    }

    companion object {
        private const val TAG = "BitmapWorkerTask"
    }
}
