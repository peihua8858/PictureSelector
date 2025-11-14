@file:JvmName("Utils")
@file:JvmMultifileClass

package com.peihua.selector.util

import android.Manifest
import android.util.Log
import androidx.fragment.app.Fragment
import com.fz.common.utils.dLog
import com.fz.common.utils.isAtLeastP
import com.peihua8858.permissions.core.MultiplePermissionCallbacks
import com.peihua8858.permisstions.fragment.requestPermissions
import java.io.Closeable
import java.io.File
import java.io.IOException

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
internal fun Fragment.requestPermissionsDsl(
    mimeTypes: Array<String>,
    requestBlock: MultiplePermissionCallbacks.() -> Unit,
) {
    val permissions = when {
        isAtLeastT && (mimeTypes.isEmpty() or MimeUtils.isAllMediaType(mimeTypes)) -> isUpsideDownCake(arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        ))

        isAtLeastT && MimeUtils.isImageMimeType(mimeTypes) -> isUpsideDownCake(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
        isAtLeastT && MimeUtils.isImageMimeType(mimeTypes) -> isUpsideDownCake(arrayOf(Manifest.permission.READ_MEDIA_VIDEO))
        isAtLeastT && MimeUtils.isImageMimeType(mimeTypes) -> isUpsideDownCake(arrayOf(Manifest.permission.READ_MEDIA_AUDIO))
        isAtLeastP -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        else -> arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    }
    dLog { "requestPermissionsDsl>>>>>>>>>>  ${permissions.joinToString(",")}" }
    requestPermissions(*permissions) { requestBlock() }
}

private fun isUpsideDownCake(array: Array<String>): Array<String> {
    return when {
        isUpsideDownCake -> arrayOf(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED, *array)
        else -> array
    }
}

/**
 * 删除最后一个指定字符
 * @author dingpeihua
 * @date 2022/1/12 15:00
 * @version 1.0
 */
fun StringBuilder.deleteEndChar(endChar: String): StringBuilder {
    val index = lastIndexOf(endChar)
    dLog { "deleteEndChar: index=$index, length=$length, endChar=$endChar,endChar.length=${endChar.length}" }
    if (isNotEmpty() && index == length - endChar.length) {
        delete(index, length)
    }
    return this
}