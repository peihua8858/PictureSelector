@file:JvmName("Utils")
@file:JvmMultifileClass
package com.peihua.selector.util
import android.Manifest
import android.util.Log
import androidx.fragment.app.Fragment
import com.fz.common.permissions.PermissionCallbacksDSL
import com.fz.common.permissions.requestPermissionsDsl
import com.peihua.selector.data.model.ConfigModel
import java.io.Closeable
import java.io.File
import java.io.IOException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

fun File.getFolderName(): String {
    return parentFile.name
}

fun Closeable?.closeSilently() {
    if (this == null) return
    try {
        this.close()
    } catch (t: IOException) {
        Log.w("closeSilently", "close fail ", t)
    }
}

/**
 *
 * Android 9开始没有写入外部存储器的权限
 * Android 13 权限细分为读取图片权限[Manifest.permission.READ_MEDIA_IMAGES]、
 * 读取视频权限[Manifest.permission.READ_MEDIA_VIDEO]、读取音频权限[Manifest.permission.READ_MEDIA_AUDIO]等，
 * 故需要根据每天类型选择请求不同的权限
 */
internal fun Fragment.requestPermissionsDsl(mimeTypes: Array<String>, requestBlock: PermissionCallbacksDSL.() -> Unit) {
    val permissions = mutableListOf<String>()
    if (isAtLeastT) {
        if (MimeUtils.isImageMimeType(mimeTypes)) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        }
        if (MimeUtils.isVideoMimeType(mimeTypes)) {
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
        }
        if (MimeUtils.isAudioMimeType(mimeTypes)) {
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        }
    } else if (isAtLeastPie) {
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
    } else {
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }
    requestPermissionsDsl(*permissions.toTypedArray()) { requestBlock() }
}