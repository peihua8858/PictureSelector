package com.peihua.selector.util;


import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Utility class for various layout modes that PhotoPicker supports.
 */
public class LayoutModeUtils {

    public static final Mode MODE_PHOTOS_TAB = Mode.of(Mode.MODE_PHOTOS_TAB);
    public static final Mode MODE_ALBUMS_TAB = Mode.of(Mode.MODE_ALBUMS_TAB);
    public static final Mode MODE_ALBUM_PHOTOS_TAB = Mode.of(Mode.MODE_ALBUM_PHOTOS_TAB);
    public static final Mode MODE_PREVIEW = Mode.of(Mode.MODE_PREVIEW);


    public static class Mode {
        public boolean isPhotosTabOrAlbumsTab;
        public boolean isPreview;
        @IntDef(value = {
                MODE_PHOTOS_TAB,
                MODE_ALBUMS_TAB,
                MODE_ALBUM_PHOTOS_TAB,
                MODE_PREVIEW,
        })
        @Retention(RetentionPolicy.SOURCE)
        public @interface ModeType {}

        public static final int MODE_PHOTOS_TAB = 1;
        public static final int MODE_ALBUMS_TAB = 2;
        public static final int MODE_ALBUM_PHOTOS_TAB = 3;
        public static final int MODE_PREVIEW = 4;

        public static Mode of(@ModeType int modeType) {
            Mode mode = new Mode();
            switch(modeType) {
                case MODE_PHOTOS_TAB:
                case MODE_ALBUMS_TAB:
                    mode.isPhotosTabOrAlbumsTab = true;
                    mode.isPreview = false;
                    break;
                case MODE_ALBUM_PHOTOS_TAB:
                    mode.isPhotosTabOrAlbumsTab = false;
                    mode.isPreview = false;
                    break;
                case MODE_PREVIEW:
                    mode.isPhotosTabOrAlbumsTab = false;
                    mode.isPreview = true;
                    break;
                default:
            }
            return mode;
        }
    }
}
