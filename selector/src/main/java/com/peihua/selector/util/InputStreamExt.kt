package com.peihua.selector.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.fz.common.file.formatFileSize
import com.fz.common.utils.dLog
import com.fz.common.utils.eLog
import com.peihua.selector.crop.util.BitmapLoadUtils
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.resume
import kotlin.math.max

fun InputStream.decodeStreamToBitmap(screenWidth: Int, screenHeight: Int): Bitmap? {
    try {
        val mScreenWidth = screenWidth
        val mScreenHeight = screenHeight
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        BitmapFactory.decodeStream(this, null, o)
        dLog { "decodePathOptionsFile, o: $o" }
        val width_tmp = o.outWidth
        val height_tmp = o.outHeight
        var scale = 1
        if (width_tmp <= mScreenWidth && height_tmp <= mScreenHeight) {
            scale = 1
        } else {
            val widthFit: Double = width_tmp * 1.0 / mScreenWidth
            val heightFit: Double = height_tmp * 1.0 / mScreenHeight
            val fit = max(widthFit, heightFit)
            scale = (fit + 0.5).toInt()
        }
        dLog { "decodePathOptionsFile, scale: $scale,width_tmp:$width_tmp,height_tmp:$height_tmp" }
        var bitmap: Bitmap? = null
        if (scale == 1) {
            bitmap = BitmapFactory.decodeStream((this))
        } else {
            val o2 = BitmapFactory.Options()
            o2.inSampleSize = scale
            bitmap = BitmapFactory.decodeStream((this), null, o2)
        }
        if (bitmap != null) {
            eLog { "scale = " + scale + " bitmap.size = " + (bitmap.getRowBytes() * bitmap.getHeight()) }
        }
        dLog { "decodePathOptionsFile, bitmap: $bitmap" }
        return bitmap
    } catch (e: Throwable) {
        eLog { "fileNotFoundException, e: $e" }
    }
    return null
}

fun InputStream.adjustBitmapOrientation(decodeBitmap: Bitmap): Bitmap? {
    return try {
        val matrix = orientationMatrix
        dLog { "adjustBitmapOrientation, adjust degree " + matrix + "to 0." }
        BitmapLoadUtils.transformBitmap(decodeBitmap, matrix)
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}

val InputStream.orientationMatrix: Matrix
    get() {
        val matrix = Matrix()
        try {
            val exif = ExifInterface(this)
            val orientation =
                exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    matrix.setRotate(180f)
                    matrix.postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.setRotate(90f)
                    matrix.postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.setRotate(-90f)
                    matrix.postScale(-1f, 1f)
                }

                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return matrix
    }


/**
 * InputStream 写入 OutputStream,且不做关闭处理，由外部自行关闭
 */
suspend fun InputStream.writeToFileNoClose(
    ios: OutputStream,
    bufferSize: Int = 1024 * 8,
    callback: (progress: Long, speed: Long) -> Unit = { process, speed -> },
): Boolean {
    return try {
        suspendCancellableCoroutine { continuation ->
            val buffer = ByteArray(bufferSize)
            var length: Int
            var progress = 0L
            val totalLength = available().toLong()
            while ((this.read(buffer).also { length = it }) != -1 && continuation.isActive) {
                ios.write(buffer, 0, length)
                progress += length.toLong()
                callback(progress, length.toLong())
                dLog { "writeToFile, save file   progress:${progress.formatFileSize()},totalLength:${totalLength.formatFileSize()} length:${length}" }
            }
            callback(progress, length.toLong())
            ios.flush()
            dLog { "writeToFile, save file  to $ios successful" }
            continuation.resume(true)
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        dLog { "writeToFile, save file  to $ios failed,e:${e.message}" }
        false
    }
}

suspend fun InputStream?.writeToZip(
    zos: ZipOutputStream,
    bufferSize: Int = 4096,
    isCloseZip: Boolean = true,
    callback: (progress: Long, speed: Long) -> Unit = { process, speed -> },
): Boolean {
    if (this == null) {
        return false
    }
    return this.use { fins ->
        if (isCloseZip) {
            zos.use { zois ->
                fins.writeToFileNoClose(zois, bufferSize, callback)
            }
        } else {
            writeToFileNoClose(zos, bufferSize, callback)
        }
    }
}


suspend fun InputStream?.writeToFile(
    file: File?,
    bufferSize: Int = 4096,
    isCloseOs: Boolean = true,
    callback: (progress: Long, speed: Long) -> Unit = { process, isComplete -> },
): Boolean {
    val parentFile = file?.parentFile
    if (file == null || this == null || parentFile == null) {
        return false
    }
    if (file.exists()) {
        file.delete()
    }
    if (parentFile.exists().not()) {
        parentFile.mkdirs()
    }
    val os = FileOutputStream(file)
    return writeToFile(os, bufferSize, isCloseOs, callback)
}

suspend fun InputStream?.writeToFile(
    os: OutputStream?,
    bufferSize: Int = 4096,
    isCloseOs: Boolean = true,
    callback: (progress: Long, speed: Long) -> Unit = { process, speed -> },
): Boolean {
    if (os == null || this == null) {
        return false
    }
    return use {
        if (isCloseOs) {
            os.use {
                writeToFileNoClose(os, bufferSize, callback)
            }
        } else {
            writeToFileNoClose(os, bufferSize, callback)
        }
    }
}