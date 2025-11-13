package com.peihua.selector.data.model

object PictureMimeType {
    const val MIME_TYPE_IMAGE: String = "image/jpeg"
    const val MIME_TYPE_VIDEO: String = "video/mp4"
    const val MIME_TYPE_AUDIO: String = "audio/mpeg"
    const val MIME_TYPE_AUDIO_AMR: String = "audio/amr"
    const val MIME_TYPE_PREFIX_IMAGE: String = "image"
    const val MIME_TYPE_PREFIX_VIDEO: String = "video"
    const val MIME_TYPE_PREFIX_AUDIO: String = "audio"
    private const val MIME_TYPE_PNG: String = "image/png"
    const val MIME_TYPE_JPEG: String = "image/jpeg"
    private const val MIME_TYPE_JPG: String = "image/jpg"
    private const val MIME_TYPE_BMP: String = "image/bmp"
    private const val MIME_TYPE_XMS_BMP: String = "image/x-ms-bmp"
    private const val MIME_TYPE_WAP_BMP: String = "image/vnd.wap.wbmp"
    private const val MIME_TYPE_GIF: String = "image/gif"
    private const val MIME_TYPE_WEBP: String = "image/webp"
    private const val MIME_TYPE_HEIC: String = "image/heic"

    private const val MIME_TYPE_3GP: String = "video/3gp"
    private const val MIME_TYPE_MP4: String = "video/mp4"
    private const val MIME_TYPE_MPEG: String = "video/mpeg"
    private const val MIME_TYPE_AVI: String = "video/avi"

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

    fun ofHeic(): String {
        return MIME_TYPE_HEIC
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
}