package com.peihua.selector.util

import android.provider.MediaStore
import android.text.TextUtils
import com.fz.common.collections.isNonEmpty
import java.util.Locale

object MimeUtils {
    private const val MIME_TYPE_GIF = "image/gif"
    const val MIME_TYPE_PREFIX_IMAGE = "image"
    const val MIME_TYPE_PREFIX_VIDEO = "video"
    const val MIME_TYPE_PREFIX_AUDIO = "audio"
    private const val MIME_TYPE_PNG = "image/png"
    const val MIME_TYPE_JPEG = "image/jpeg"
    private const val MIME_TYPE_JPG = "image/jpg"
    private const val MIME_TYPE_BMP = "image/bmp"
    private const val MIME_TYPE_XMS_BMP = "image/x-ms-bmp"
    private const val MIME_TYPE_WAP_BMP = "image/vnd.wap.wbmp"
    private const val MIME_TYPE_WEBP = "image/webp"
    private const val MIME_TYPE_3GP = "video/3gp"
    private const val MIME_TYPE_MP4 = "video/mp4"
    private const val MIME_TYPE_MPEG = "video/mpeg"
    private const val MIME_TYPE_AVI = "video/avi"

    /**
     * isGif
     *
     * @param mimeType
     * @return
     */
    @JvmStatic
    fun isHasGif(mimeType: String?): Boolean {
        return mimeType != null && (mimeType == "image/gif" || mimeType == "image/GIF")
    }

    /**
     * isGif
     *
     * @param url
     * @return
     */
    @JvmStatic
    fun isUrlHasGif(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        return url.lowercase(Locale.getDefault()).endsWith(".gif")
    }

    /**
     * is has image
     *
     * @param url
     * @return
     */
    @JvmStatic
    fun isUrlHasImage(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        return (url.lowercase(Locale.getDefault()).endsWith(".jpg")
                || url.lowercase(Locale.getDefault()).endsWith(".jpeg")
                || url.lowercase(Locale.getDefault()).endsWith(".png")
                || url.lowercase(Locale.getDefault()).endsWith(".heic"))
    }

    /**
     * isWebp
     *
     * @param mimeType
     * @return
     */
    @JvmStatic
    fun isHasWebp(mimeType: String?): Boolean {
        return mimeType != null && mimeType.equals("image/webp", ignoreCase = true)
    }

    /**
     * isWebp
     *
     * @param url
     * @return
     */
    @JvmStatic
    fun isUrlHasWebp(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        return url.lowercase(Locale.getDefault()).endsWith(".webp")
    }

    /**
     * isVideo
     *
     * @param mimeType
     * @return
     */
    @JvmStatic
    fun isHasVideo(mimeType: String?): Boolean {
        return mimeType != null && mimeType.startsWith(MIME_TYPE_PREFIX_VIDEO)
    }

    /**
     * isVideo
     *
     * @param url
     * @return
     */
    @JvmStatic
    fun isUrlHasVideo(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        return url.lowercase(Locale.getDefault()).endsWith(".mp4")
    }

    /**
     * isAudio
     *
     * @param mimeType
     * @return
     */
    @JvmStatic
    fun isHasAudio(mimeType: String?): Boolean {
        return mimeType != null && mimeType.startsWith(MIME_TYPE_PREFIX_AUDIO)
    }

    /**
     * isAudio
     *
     * @param url
     * @return
     */
    @JvmStatic
    fun isUrlHasAudio(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        return url.lowercase(Locale.getDefault()).endsWith(".amr") || url.lowercase(Locale.getDefault())
            .endsWith(".mp3")
    }

    /**
     * isImage
     *
     * @param mimeType
     * @return
     */
    @JvmStatic
    fun isHasImage(mimeType: String?): Boolean {
        return mimeType != null && mimeType.startsWith(MIME_TYPE_PREFIX_IMAGE)
    }

    /**
     * isHasBmp
     *
     * @param mimeType
     * @return
     */
    @JvmStatic
    fun isHasBmp(mimeType: String): Boolean {
        return if (TextUtils.isEmpty(mimeType)) {
            false
        } else (mimeType.startsWith(ofBMP())
                || mimeType.startsWith(ofXmsBMP())
                || mimeType.startsWith(ofWapBMP()))
    }

    /**
     * Determine if it is JPG.
     *
     * @param mimeType is image file mimeType
     */
    @JvmStatic
    fun isJPEG(mimeType: String): Boolean {
        return if (TextUtils.isEmpty(mimeType)) {
            false
        } else mimeType.startsWith(MIME_TYPE_JPEG) || mimeType.startsWith(MIME_TYPE_JPG)
    }

    /**
     * Determine if it is JPG.
     *
     * @param mimeType is image file mimeType
     */
    @JvmStatic
    fun isJPG(mimeType: String): Boolean {
        return if (TextUtils.isEmpty(mimeType)) {
            false
        } else mimeType.startsWith(MIME_TYPE_JPG)
    }

    /**
     * is Network image
     *
     * @param path
     * @return
     */
    @JvmStatic
    fun isHasHttp(path: String): Boolean {
        return if (TextUtils.isEmpty(path)) {
            false
        } else path.startsWith("http",true) || path.startsWith("https",true)
    }

    /**
     * is content://
     *
     * @param url
     * @return
     */
    @JvmStatic
    fun isContent(url: String): Boolean {
        return if (TextUtils.isEmpty(url)) {
            false
        } else url.startsWith("content://",true)
    }

    @JvmStatic
    fun ofPNG(): String {
        return MIME_TYPE_PNG
    }

    @JvmStatic
    fun ofJPEG(): String {
        return MIME_TYPE_JPEG
    }

    @JvmStatic
    fun ofBMP(): String {
        return MIME_TYPE_BMP
    }

    @JvmStatic
    fun ofXmsBMP(): String {
        return MIME_TYPE_XMS_BMP
    }

    @JvmStatic
    fun ofWapBMP(): String {
        return MIME_TYPE_WAP_BMP
    }

    @JvmStatic
    fun ofGIF(): String {
        return MIME_TYPE_GIF
    }

    @JvmStatic
    fun ofWEBP(): String {
        return MIME_TYPE_WEBP
    }

    @JvmStatic
    fun of3GP(): String {
        return MIME_TYPE_3GP
    }

    @JvmStatic
    fun ofMP4(): String {
        return MIME_TYPE_MP4
    }

    @JvmStatic
    fun ofMPEG(): String {
        return MIME_TYPE_MPEG
    }

    @JvmStatic
    fun ofAVI(): String {
        return MIME_TYPE_AVI
    }

    @JvmStatic
    fun extractPrimaryType(mimeType: String): String {
        val slash = mimeType.indexOf('/')
        require(slash != -1)
        return mimeType.substring(0, slash)
    }

    @JvmStatic
    fun isAudioMimeType(mimeType: String?): Boolean {
        return mimeType?.startsWith( "audio/",true) ?: false
    }

    @JvmStatic
    fun isVideoMimeType(mimeType: String?): Boolean {
        return mimeType?.startsWith( "video/",true) ?: false
    }

    @JvmStatic
    fun isImageMimeType(mimeType: String?): Boolean {
        return mimeType?.startsWith("image/",true) ?: false
    }

    @JvmStatic
    fun isImageAndVideoMediaType(mimeType: String?): Boolean {
        val result = mimeType?.split(",")
        if (result.isNonEmpty()) {
            var findImage = false
            var findVideo = false
            for ((index, item) in result.withIndex()) {
                if (isImageMimeType(item)) {
                    findImage = true
                } else if (isVideoMimeType(item)) {
                    findVideo = true
                }
            }
            return findImage && findVideo
        }
        return false
    }

    @JvmStatic
    fun isImageOrVideoMediaType(mimeType: String?): Boolean {
        return isImageMimeType(mimeType) || isVideoMimeType(mimeType)
    }

    @JvmStatic
    fun isImageOrVideoMediaType(mediaType: Int): Boolean {
        return (MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE == mediaType
                || MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO == mediaType)
    }

    @JvmStatic
    fun isPlaylistMimeType(mimeType: String?): Boolean {
        return if (mimeType == null) false else when (mimeType.lowercase()) {
            "application/vnd.apple.mpegurl", "application/vnd.ms-wpl", "application/x-extension-smpl", "application/x-mpegurl", "application/xspf+xml", "audio/mpegurl", "audio/x-mpegurl", "audio/x-scpls" -> true
            else -> false
        }
    }

    @JvmStatic
    fun isSubtitleMimeType(mimeType: String?): Boolean {
        return if (mimeType == null) false else when (mimeType.lowercase()) {
            "application/lrc", "application/smil+xml", "application/ttml+xml", "application/x-extension-cap", "application/x-extension-srt", "application/x-extension-sub", "application/x-extension-vtt", "application/x-subrip", "text/vtt" -> true
            else -> false
        }
    }
}