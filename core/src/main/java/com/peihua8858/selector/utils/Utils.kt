@file:JvmName("Utils")
@file:JvmMultifileClass

package com.peihua8858.selector.utils

import android.Manifest
import android.os.Build.VERSION.CODENAME
import android.os.Build.VERSION.SDK_INT
import android.util.Log
import com.peihua8858.tools.utils.dLog
import com.peihua8858.tools.utils.isAtLeastP
import com.peihua8858.tools.utils.isAtLeastT
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

 fun getPermissions(mimeTypes: Array<String>): Array<String> {
    val permissions = when {
        isAtLeastT && (mimeTypes.isEmpty() or MimeUtils.isAllMediaType(mimeTypes)) -> isUpsideDownCake(
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        )

        isAtLeastT && MimeUtils.isImageMimeType(mimeTypes) -> isUpsideDownCake(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
        isAtLeastT && MimeUtils.isImageMimeType(mimeTypes) -> isUpsideDownCake(arrayOf(Manifest.permission.READ_MEDIA_VIDEO))
        isAtLeastT && MimeUtils.isImageMimeType(mimeTypes) -> isUpsideDownCake(arrayOf(Manifest.permission.READ_MEDIA_AUDIO))
        isAtLeastP -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        else -> arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    }
    dLog { "requestPermissionsDsl>>>>>>>>>>  ${permissions.joinToString(",")}" }
    return permissions
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
    dLog { "deleteEndChar: index=$index, length=${length}, endChar=$endChar,endChar.length=${endChar.length}" }
    if (isNotEmpty() && index == length - endChar.length) {
        delete(index, length)
    }
    return this
}

inline val isUpsideDownCake: Boolean
    get() = SDK_INT >= 34

/** Checks if the device is running on a release version of Android UpsideDownCake or newer  */
inline val isAtLeastU: Boolean
    get() {
        return SDK_INT >= 34 || SDK_INT == 33 && isAtLeastPreReleaseCodename("UpsideDownCake")
    }

/** Checks if the device is running on a pre-release version of Android V or newer  */
inline val isAtLeastV: Boolean
    get() {
        return SDK_INT >= 34 && isAtLeastPreReleaseCodename("VanillaIceCream")
    }

fun isAtLeastPreReleaseCodename(codename: String): Boolean {
    // Special case "REL", which means the build is not a pre-release build.
    return if ("REL" == CODENAME) {
        false
    } else CODENAME >= codename

    // Otherwise lexically compare them. Return true if the build codename is equal to or
    // greater than the requested codename.
}