package com.peihua.picker.core.data.model;

public class AlbumColumns {
    public static final String DISPLAY_NAME = "display_name";
    public static final String ID = "id";
    public static final String MEDIA_COUNT = "album_media_count";
    public static final String MEDIA_COVER_ID = "album_media_cover_id";
    public static final String IS_LOCAL = "is_local";
    /**
     * Includes local media present in any directory containing
     * {@link Environment#DIRECTORY_SCREENSHOTS} in relative path
     *
     * @hide
     */
    public static final String ALBUM_ID_SCREENSHOTS = "Screenshots";

    /**
     * Includes local images/videos that are present in the
     * {@link Environment#DIRECTORY_DCIM}/Camera directory.
     *
     * @hide
     */
    public static final String ALBUM_ID_CAMERA = "Camera";

    /**
     * Includes local and cloud videos only.
     *
     * @hide
     */
    public static final String ALBUM_ID_VIDEOS = "Videos";

    /**
     * Includes local images/videos that have {@link MediaStore.MediaColumns#IS_DOWNLOAD} set.
     *
     * @hide
     */
    public static final String ALBUM_ID_DOWNLOADS = "Downloads";

    /**
     * Includes local and cloud images/videos that have been favorited by the user.
     *
     * @hide
     */
    public static final String ALBUM_ID_FAVORITES = "Favorites";
}
