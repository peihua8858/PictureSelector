package com.peihua.selector.crop;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.peihua.photopicker.BuildConfig;
import com.peihua.selector.crop.model.AspectRatio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

/**
 * Created by Oleksii Shliama (https://github.com/shliama).
 * <p/>
 * Builder class to ease Intent setup.
 */
public class UCrop {
    public static final int REQUEST_CROP = 69;
    public static final int RESULT_ERROR = 96;
    public static final int MIN_SIZE = 10;

    private static final String EXTRA_PREFIX = BuildConfig.LIBRARY_PACKAGE_NAME;
    public static final String EXTRA_CROP_TOTAL_DATA_SOURCE = EXTRA_PREFIX + ".CropTotalDataSource";
    public static final String EXTRA_CROP_INPUT_ORIGINAL = EXTRA_PREFIX + ".CropInputOriginal";

    public static final String EXTRA_INPUT_URI = EXTRA_PREFIX + ".InputUri";
    public static final String EXTRA_OUTPUT_URI = EXTRA_PREFIX + ".OutputUri";
    public static final String EXTRA_OUTPUT_CROP_ASPECT_RATIO = EXTRA_PREFIX + ".CropAspectRatio";
    public static final String EXTRA_OUTPUT_IMAGE_WIDTH = EXTRA_PREFIX + ".ImageWidth";
    public static final String EXTRA_OUTPUT_IMAGE_HEIGHT = EXTRA_PREFIX + ".ImageHeight";
    public static final String EXTRA_OUTPUT_OFFSET_X = EXTRA_PREFIX + ".OffsetX";
    public static final String EXTRA_OUTPUT_OFFSET_Y = EXTRA_PREFIX + ".OffsetY";
    public static final String EXTRA_ERROR = EXTRA_PREFIX + ".Error";

    public static final String EXTRA_ASPECT_RATIO_X = EXTRA_PREFIX + ".AspectRatioX";
    public static final String EXTRA_ASPECT_RATIO_Y = EXTRA_PREFIX + ".AspectRatioY";

    public static final String EXTRA_MAX_SIZE_X = EXTRA_PREFIX + ".MaxSizeX";
    public static final String EXTRA_MAX_SIZE_Y = EXTRA_PREFIX + ".MaxSizeY";

    private Intent mCropIntent;
    private Bundle mCropOptionsBundle;

    /**
     * This method creates new Intent builder and sets both source and destination image URIs.
     *
     * @param source      Uri for image to crop
     * @param destination Uri for saving the cropped image
     */
    public static UCrop of(@NonNull Uri source, @NonNull Uri destination) {
        return new UCrop(source, destination);
    }

    /**
     * This method creates new Intent builder and sets both source and destination image URIs.
     *
     * @param destination Uri for saving the cropped image
     * @param totalSource crop total data
     */
    public static UCrop of(@NonNull ArrayList<Uri> totalSource, @NonNull Uri destination) {
        return new UCrop(totalSource, destination);
    }

    /**
     * This method creates new Intent builder and sets both source and destination image URIs.
     *
     * @param source      Uri for image to crop
     * @param destination Uri for saving the cropped image
     */
    private UCrop(@NonNull Uri source, @NonNull Uri destination) {
        mCropIntent = new Intent();
        mCropOptionsBundle = new Bundle();
        mCropOptionsBundle.putParcelable(EXTRA_INPUT_URI, source);
        mCropOptionsBundle.putParcelable(EXTRA_OUTPUT_URI, destination);
    }


    /**
     * This method creates new Intent builder and sets both source and destination image URIs.
     *
     * @param destination Uri for saving the cropped image
     * @param totalSource crop total data
     */
    private UCrop(ArrayList<Uri> totalSource, @NonNull Uri destination) {
        mCropIntent = new Intent();
        mCropOptionsBundle = new Bundle();
        mCropOptionsBundle.putParcelable(EXTRA_OUTPUT_URI, destination);
        mCropOptionsBundle.putParcelableArrayList(EXTRA_CROP_TOTAL_DATA_SOURCE, totalSource);
    }

    /**
     * Set an aspect ratio for crop bounds.
     * User won't see the menu with other ratios options.
     *
     * @param x aspect ratio X
     * @param y aspect ratio Y
     */
    public UCrop withAspectRatio(float x, float y) {
        mCropOptionsBundle.putFloat(EXTRA_ASPECT_RATIO_X, x);
        mCropOptionsBundle.putFloat(EXTRA_ASPECT_RATIO_Y, y);
        return this;
    }

    /**
     * Set an aspect ratio for crop bounds that is evaluated from source image width and height.
     * User won't see the menu with other ratios options.
     */
    public UCrop useSourceImageAspectRatio() {
        mCropOptionsBundle.putFloat(EXTRA_ASPECT_RATIO_X, 0);
        mCropOptionsBundle.putFloat(EXTRA_ASPECT_RATIO_Y, 0);
        return this;
    }

    /**
     * Set maximum size for result cropped image. Maximum size cannot be less then {@value MIN_SIZE}
     *
     * @param width  max cropped image width
     * @param height max cropped image height
     */
    public UCrop withMaxResultSize(@IntRange(from = MIN_SIZE) int width, @IntRange(from = MIN_SIZE) int height) {
        if (width < MIN_SIZE) {
            width = MIN_SIZE;
        }

        if (height < MIN_SIZE) {
            height = MIN_SIZE;
        }

        mCropOptionsBundle.putInt(EXTRA_MAX_SIZE_X, width);
        mCropOptionsBundle.putInt(EXTRA_MAX_SIZE_Y, height);
        return this;
    }

    public UCrop withOptions(@NonNull Options options) {
        mCropOptionsBundle.putAll(options.getOptionBundle());
        return this;
    }

    /**
     * Get Intent to start {@link UCropActivity}
     *
     * @return Intent for {@link UCropActivity}
     */
    public Intent getIntent(@NonNull Context context) {
        ArrayList<String> dataSource = mCropOptionsBundle.getStringArrayList(EXTRA_CROP_TOTAL_DATA_SOURCE);
        if (dataSource != null && dataSource.size() > 1) {
            mCropIntent.setClass(context, UCropMultipleActivity.class);
        } else {
            mCropIntent.setClass(context, UCropActivity.class);
        }
        mCropIntent.putExtras(mCropOptionsBundle);
        return mCropIntent;
    }

    /**
     * Get Fragment {@link UCropFragment}
     *
     * @return Fragment of {@link UCropFragment}
     */
    public UCropFragment getFragment() {
        return UCropFragment.newInstance(mCropOptionsBundle);
    }

    public UCropFragment getFragment(Bundle bundle) {
        mCropOptionsBundle = bundle;
        return getFragment();
    }

    /**
     * Retrieve cropped image Uri from the result Intent
     *
     * @param intent crop result intent
     */
    @Nullable
    public static Uri getOutput(@NonNull Intent intent) {
        return intent.getParcelableExtra(UCrop.EXTRA_OUTPUT_URI);
    }

    /**
     * Retrieve the width of the cropped image
     *
     * @param intent crop result intent
     */
    public static int getOutputImageWidth(@NonNull Intent intent) {
        return intent.getIntExtra(EXTRA_OUTPUT_IMAGE_WIDTH, -1);
    }

    /**
     * Retrieve the height of the cropped image
     *
     * @param intent crop result intent
     */
    public static int getOutputImageHeight(@NonNull Intent intent) {
        return intent.getIntExtra(EXTRA_OUTPUT_IMAGE_HEIGHT, -1);
    }

    /**
     * Retrieve cropped image aspect ratio from the result Intent
     *
     * @param intent crop result intent
     * @return aspect ratio as a floating point value (x:y) - so it will be 1 for 1:1 or 4/3 for 4:3
     */
    public static float getOutputCropAspectRatio(@NonNull Intent intent) {
        return intent.getFloatExtra(EXTRA_OUTPUT_CROP_ASPECT_RATIO, 0f);
    }

    /**
     * Retrieve the x of the cropped offset x
     *
     * @param intent crop result intent
     */
    public static int getOutputImageOffsetX(@NonNull Intent intent) {
        return intent.getIntExtra(EXTRA_OUTPUT_OFFSET_X, 0);
    }

    /**
     * Retrieve the y of the cropped offset y
     *
     * @param intent crop result intent
     */
    public static int getOutputImageOffsetY(@NonNull Intent intent) {
        return intent.getIntExtra(EXTRA_OUTPUT_OFFSET_Y, 0);
    }

    /**
     * Method retrieves error from the result intent.
     *
     * @param result crop result Intent
     * @return Throwable that could happen while image processing
     */
    @Nullable
    public static Throwable getError(@NonNull Intent result) {
        return (Throwable) result.getSerializableExtra(EXTRA_ERROR);
    }

    /**
     * Class that helps to setup advanced configs that are not commonly used.
     * Use it with method {@link #withOptions(Options)}
     */
    public static abstract class Options<T extends Options> {

        public static final String EXTRA_COMPRESSION_FORMAT_NAME = EXTRA_PREFIX + ".CompressionFormatName";
        public static final String EXTRA_COMPRESSION_QUALITY = EXTRA_PREFIX + ".CompressionQuality";

        public static final String EXTRA_CROP_OUTPUT_DIR = EXTRA_PREFIX + ".CropOutputDir";

        public static final String EXTRA_CROP_OUTPUT_FILE_NAME = EXTRA_PREFIX + ".CropOutputFileName";

        public static final String EXTRA_CROP_FORBID_GIF_WEBP = EXTRA_PREFIX + ".ForbidCropGifWebp";

        public static final String EXTRA_CROP_FORBID_SKIP = EXTRA_PREFIX + ".ForbidSkipCrop";

        public static final String EXTRA_DRAG_IMAGES = EXTRA_PREFIX + ".isDragImages";

        public static final String EXTRA_CROP_CUSTOM_LOADER_BITMAP = EXTRA_PREFIX + ".CustomLoaderCropBitmap";

        public static final String EXTRA_CROP_DRAG_CENTER = EXTRA_PREFIX + ".DragSmoothToCenter";

        public static final String EXTRA_ALLOWED_GESTURES = EXTRA_PREFIX + ".AllowedGestures";

        public static final String EXTRA_MAX_BITMAP_SIZE = EXTRA_PREFIX + ".MaxBitmapSize";
        public static final String EXTRA_MAX_SCALE_MULTIPLIER = EXTRA_PREFIX + ".MaxScaleMultiplier";
        public static final String EXTRA_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION = EXTRA_PREFIX + ".ImageToCropBoundsAnimDuration";

        public static final String EXTRA_DIMMED_LAYER_COLOR = EXTRA_PREFIX + ".DimmedLayerColor";
        public static final String EXTRA_CIRCLE_STROKE_COLOR = EXTRA_PREFIX + ".CircleStrokeColor";
        public static final String EXTRA_CIRCLE_DIMMED_LAYER = EXTRA_PREFIX + ".CircleDimmedLayer";

        public static final String EXTRA_SHOW_CROP_FRAME = EXTRA_PREFIX + ".ShowCropFrame";
        public static final String EXTRA_CROP_FRAME_COLOR = EXTRA_PREFIX + ".CropFrameColor";
        public static final String EXTRA_CROP_FRAME_STROKE_WIDTH = EXTRA_PREFIX + ".CropFrameStrokeWidth";

        public static final String EXTRA_SHOW_CROP_GRID = EXTRA_PREFIX + ".ShowCropGrid";

        public static final String EXTRA_CROP_GRID_ROW_COUNT = EXTRA_PREFIX + ".CropGridRowCount";
        public static final String EXTRA_CROP_GRID_COLUMN_COUNT = EXTRA_PREFIX + ".CropGridColumnCount";
        public static final String EXTRA_CROP_GRID_COLOR = EXTRA_PREFIX + ".CropGridColor";
        public static final String EXTRA_CROP_GRID_STROKE_WIDTH = EXTRA_PREFIX + ".CropGridStrokeWidth";
        public static final String EXTRA_CIRCLE_STROKE_WIDTH_LAYER = EXTRA_PREFIX + ".CircleStrokeWidth";
        public static final String EXTRA_GALLERY_BAR_BACKGROUND = EXTRA_PREFIX + ".GalleryBarBackground";

        public static final String EXTRA_UCROP_COLOR_CONTROLS_WIDGET_ACTIVE = EXTRA_PREFIX + ".UcropColorControlsWidgetActive";

        //        public static final String EXTRA_UCROP_WIDGET_COLOR_TOOLBAR = EXTRA_PREFIX + ".UcropToolbarWidgetColor";
        public static final String EXTRA_UCROP_TITLE_TEXT_TOOLBAR = EXTRA_PREFIX + ".UcropToolbarTitleText";
//        public static final String EXTRA_UCROP_TITLE_TEXT_SIZE_TOOLBAR = EXTRA_PREFIX + ".UcropToolbarTitleTextSize";
//        public static final String EXTRA_UCROP_WIDGET_CANCEL_DRAWABLE = EXTRA_PREFIX + ".UcropToolbarCancelDrawable";
//        public static final String EXTRA_UCROP_WIDGET_CROP_DRAWABLE = EXTRA_PREFIX + ".UcropToolbarCropDrawable";

//        public static final String EXTRA_UCROP_LOGO_COLOR = EXTRA_PREFIX + ".UcropLogoColor";

        public static final String EXTRA_HIDE_BOTTOM_CONTROLS = EXTRA_PREFIX + ".HideBottomControls";
        public static final String EXTRA_FREE_STYLE_CROP = EXTRA_PREFIX + ".FreeStyleCrop";

        public static final String EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT = EXTRA_PREFIX + ".AspectRatioSelectedByDefault";
        public static final String EXTRA_ASPECT_RATIO_OPTIONS = EXTRA_PREFIX + ".AspectRatioOptions";
        public static final String EXTRA_SKIP_CROP_MIME_TYPE = EXTRA_PREFIX + ".SkipCropMimeType";

        public static final String EXTRA_MULTIPLE_ASPECT_RATIO = EXTRA_PREFIX + ".MultipleAspectRatio";

        public static final String EXTRA_UCROP_ROOT_VIEW_BACKGROUND_COLOR = EXTRA_PREFIX + ".UcropRootViewBackgroundColor";


        private final Bundle mOptionBundle;

        public Options() {
            mOptionBundle = new Bundle();
        }

        @NonNull
        public Bundle getOptionBundle() {
            return mOptionBundle;
        }

        /**
         * Set one of {@link Bitmap.CompressFormat} that will be used to save resulting Bitmap.
         */
        public T setCompressionFormat(@NonNull Bitmap.CompressFormat format) {
            mOptionBundle.putString(EXTRA_COMPRESSION_FORMAT_NAME, format.name());
            return self();
        }

        /**
         * Set one of {@link context.getExternalFilesDir()} The path that will be used to save
         * when clipping multiple drawings
         * Valid when multiple pictures are cropped
         */
        public T setCropOutputPathDir(@NonNull String dir) {
            mOptionBundle.putString(EXTRA_CROP_OUTPUT_DIR, dir);
            return self();
        }

        /**
         * File name after clipping output
         * Valid when multiple pictures are cropped
         * <p>
         * When multiple pictures are cropped, the front will automatically keep up with the timestamp
         * </p>
         */
        public T setCropOutputFileName(@NonNull String fileName) {
            mOptionBundle.putString(EXTRA_CROP_OUTPUT_FILE_NAME, fileName);
            return self();
        }

        /**
         * @param isForbidSkipCrop - It is forbidden to skip when cutting multiple drawings
         */
        public T isForbidSkipMultipleCrop(boolean isForbidSkipCrop) {
            mOptionBundle.putBoolean(EXTRA_CROP_FORBID_SKIP, isForbidSkipCrop);
            return self();
        }

        /**
         * Get the bitmap of the uCrop resource using the custom loader
         *
         * @param isUseBitmap
         */
        public T isUseCustomLoaderBitmap(boolean isUseBitmap) {
            mOptionBundle.putBoolean(EXTRA_CROP_CUSTOM_LOADER_BITMAP, isUseBitmap);
            return self();
        }

        /**
         * isDragCenter
         *
         * @param isDragCenter Crop and drag automatically center
         */
        public T isCropDragSmoothToCenter(boolean isDragCenter) {
            mOptionBundle.putBoolean(EXTRA_CROP_DRAG_CENTER, isDragCenter);
            return self();
        }

        /**
         * @param isForbidCropGifWebp - Do you need to support clipping dynamic graphs gif or webp
         */
        public T isForbidCropGifWebp(boolean isForbidCropGifWebp) {
            mOptionBundle.putBoolean(EXTRA_CROP_FORBID_GIF_WEBP, isForbidCropGifWebp);
            return self();
        }

        /**
         * Set compression quality [0-100] that will be used to save resulting Bitmap.
         */
        public T setCompressionQuality(@IntRange(from = 0) int compressQuality) {
            mOptionBundle.putInt(EXTRA_COMPRESSION_QUALITY, compressQuality);
            return self();
        }

        /**
         * Choose what set of gestures will be enabled on each tab - if any.
         */
        public T setAllowedGestures(@UCropActivity.GestureTypes int tabScale,
                                    @UCropActivity.GestureTypes int tabRotate,
                                    @UCropActivity.GestureTypes int tabAspectRatio) {
            mOptionBundle.putIntArray(EXTRA_ALLOWED_GESTURES, new int[]{tabScale, tabRotate, tabAspectRatio});
            return self();
        }

        /**
         * This method sets multiplier that is used to calculate max image scale from min image scale.
         *
         * @param maxScaleMultiplier - (minScale * maxScaleMultiplier) = maxScale
         */
        public T setMaxScaleMultiplier(@FloatRange(from = 1.0, fromInclusive = false) float maxScaleMultiplier) {
            mOptionBundle.putFloat(EXTRA_MAX_SCALE_MULTIPLIER, maxScaleMultiplier);
            return self();
        }

        /**
         * This method sets animation duration for image to wrap the crop bounds
         *
         * @param durationMillis - duration in milliseconds
         */
        public T setImageToCropBoundsAnimDuration(@IntRange(from = MIN_SIZE) int durationMillis) {
            mOptionBundle.putInt(EXTRA_IMAGE_TO_CROP_BOUNDS_ANIM_DURATION, durationMillis);
            return self();
        }

        /**
         * Setter for max size for both width and height of bitmap that will be decoded from an input Uri and used in the view.
         *
         * @param maxBitmapSize - size in pixels
         */
        public T setMaxBitmapSize(@IntRange(from = MIN_SIZE) int maxBitmapSize) {
            mOptionBundle.putInt(EXTRA_MAX_BITMAP_SIZE, maxBitmapSize);
            return self();
        }

        /**
         * @param color - desired color of dimmed area around the crop bounds
         */
        public T setDimmedLayerColor(@ColorInt int color) {
            mOptionBundle.putInt(EXTRA_DIMMED_LAYER_COLOR, color);
            return self();
        }

        /**
         * @param color - desired color of dimmed stroke area around the crop bounds
         */
        public T setCircleStrokeColor(@ColorInt int color) {
            mOptionBundle.putInt(EXTRA_CIRCLE_STROKE_COLOR, color);
            return self();
        }

        /**
         * @param isCircle - set it to true if you want dimmed layer to have an circle inside
         */
        public T setCircleDimmedLayer(boolean isCircle) {
            mOptionBundle.putBoolean(EXTRA_CIRCLE_DIMMED_LAYER, isCircle);
            return self();
        }

        /**
         * @param show - set to true if you want to see a crop frame rectangle on top of an image
         */
        public T setShowCropFrame(boolean show) {
            mOptionBundle.putBoolean(EXTRA_SHOW_CROP_FRAME, show);
            return self();
        }

        /**
         * @param color - desired color of crop frame
         */
        public T setCropFrameColor(@ColorInt int color) {
            mOptionBundle.putInt(EXTRA_CROP_FRAME_COLOR, color);
            return self();
        }

        /**
         * @param width - desired width of crop frame line in pixels
         */
        public T setCropFrameStrokeWidth(@IntRange(from = 0) int width) {
            mOptionBundle.putInt(EXTRA_CROP_FRAME_STROKE_WIDTH, width);
            return self();
        }

        /**
         * @param show - set to true if you want to see a crop grid/guidelines on top of an image
         */
        public T setShowCropGrid(boolean show) {
            mOptionBundle.putBoolean(EXTRA_SHOW_CROP_GRID, show);
            return self();
        }

        /**
         * @param count - crop grid rows count.
         */
        public T setCropGridRowCount(@IntRange(from = 0) int count) {
            mOptionBundle.putInt(EXTRA_CROP_GRID_ROW_COUNT, count);
            return self();
        }

        /**
         * @param count - crop grid columns count.
         */
        public T setCropGridColumnCount(@IntRange(from = 0) int count) {
            mOptionBundle.putInt(EXTRA_CROP_GRID_COLUMN_COUNT, count);
            return self();
        }

        /**
         * @param color - desired color of crop grid/guidelines
         */
        public T setCropGridColor(@ColorInt int color) {
            mOptionBundle.putInt(EXTRA_CROP_GRID_COLOR, color);
            return self();
        }

        /**
         * @param width - desired width of crop grid lines in pixels
         */
        public T setCropGridStrokeWidth(@IntRange(from = 0) int width) {
            mOptionBundle.putInt(EXTRA_CROP_GRID_STROKE_WIDTH, width);
            return self();
        }

        /**
         * @param width Set the circular clipping border
         */
        public T setCircleStrokeWidth(@IntRange(from = 0) int width) {
            mOptionBundle.putInt(EXTRA_CIRCLE_STROKE_WIDTH_LAYER, width);
            return self();
        }

        /**
         * @param color - desired resolved color of the gallery bar background
         */
        public T setCropGalleryBarBackgroundResources(@ColorInt int color) {
            mOptionBundle.putInt(EXTRA_GALLERY_BAR_BACKGROUND, color);
            return self();
        }


        /**
         * Can I drag and drop images when crop
         *
         * @param isDragImages
         */
        public T isDragCropImages(boolean isDragImages) {
            mOptionBundle.putBoolean(EXTRA_DRAG_IMAGES, isDragImages);
            return self();
        }

        /**
         * @param color - desired resolved color of the active and selected widget and progress wheel middle line (default is white)
         */
        public T setActiveControlsWidgetColor(@ColorInt int color) {
            mOptionBundle.putInt(EXTRA_UCROP_COLOR_CONTROLS_WIDGET_ACTIVE, color);
            return self();
        }

        /**
         * @param text - desired text for Toolbar title
         */
        public T setToolbarTitle(@Nullable String text) {
            mOptionBundle.putString(EXTRA_UCROP_TITLE_TEXT_TOOLBAR, text);
            return self();
        }

        /**
         * @param hide - set to true to hide the bottom controls (shown by default)
         */
        public T setHideBottomControls(boolean hide) {
            mOptionBundle.putBoolean(EXTRA_HIDE_BOTTOM_CONTROLS, hide);
            return self();
        }

        /**
         * @param enabled - set to true to let user resize crop bounds (disabled by default)
         */
        public T setFreeStyleCropEnabled(boolean enabled) {
            mOptionBundle.putBoolean(EXTRA_FREE_STYLE_CROP, enabled);
            return self();
        }

        /**
         * Pass an ordered list of desired aspect ratios that should be available for a user.
         *
         * @param selectedByDefault - index of aspect ratio option that is selected by default (starts with 0).
         * @param aspectRatio       - list of aspect ratio options that are available to user
         */
        public T setAspectRatioOptions(int selectedByDefault, AspectRatio... aspectRatio) {
            if (selectedByDefault >= aspectRatio.length) {
                throw new IllegalArgumentException(String.format(Locale.US,
                        "Index [selectedByDefault = %d] (0-based) cannot be higher or equal than aspect ratio options count [count = %d].",
                        selectedByDefault, aspectRatio.length));
            }
            mOptionBundle.putInt(EXTRA_ASPECT_RATIO_SELECTED_BY_DEFAULT, selectedByDefault);
            mOptionBundle.putParcelableArrayList(EXTRA_ASPECT_RATIO_OPTIONS, new ArrayList<Parcelable>(Arrays.asList(aspectRatio)));
            return self();
        }

        /**
         * Skip crop mimeType
         *
         * @param mimeTypes Use example {@link { image/gift or image/webp ... }}
         * @return
         */
        public T setSkipCropMimeType(String... mimeTypes) {
            if (mimeTypes != null && mimeTypes.length > 0) {
                mOptionBundle.putStringArrayList(EXTRA_SKIP_CROP_MIME_TYPE, new ArrayList<>(Arrays.asList(mimeTypes)));
            }
            return self();
        }

        /**
         * @param color - desired background color that should be applied to the root view
         */
        public T setRootViewBackgroundColor(@ColorInt int color) {
            mOptionBundle.putInt(EXTRA_UCROP_ROOT_VIEW_BACKGROUND_COLOR, color);
            return self();
        }

        /**
         * Set an aspect ratio for crop bounds.
         * User won't see the menu with other ratios options.
         *
         * @param x aspect ratio X
         * @param y aspect ratio Y
         */
        public T withAspectRatio(float x, float y) {
            mOptionBundle.putFloat(EXTRA_ASPECT_RATIO_X, x);
            mOptionBundle.putFloat(EXTRA_ASPECT_RATIO_Y, y);
            return self();
        }

        /**
         * The corresponding crop scale of each graph in multi graph crop
         *
         * @param aspectRatio - The corresponding crop scale of each graph in multi graph crop
         */
        public T setMultipleCropAspectRatio(AspectRatio... aspectRatio) {
            float aspectRatioX = mOptionBundle.getFloat(EXTRA_ASPECT_RATIO_X, 0);
            float aspectRatioY = mOptionBundle.getFloat(EXTRA_ASPECT_RATIO_Y, 0);
            if (aspectRatio.length > 0 && aspectRatioX <= 0 && aspectRatioY <= 0) {
                withAspectRatio(aspectRatio[0].getAspectRatioX(), aspectRatio[0].getAspectRatioY());
            }
            mOptionBundle.putParcelableArrayList(EXTRA_MULTIPLE_ASPECT_RATIO, new ArrayList<Parcelable>(Arrays.asList(aspectRatio)));
            return self();
        }


        /**
         * Set an aspect ratio for crop bounds that is evaluated from source image width and height.
         * User won't see the menu with other ratios options.
         */
        public T useSourceImageAspectRatio() {
            mOptionBundle.putFloat(EXTRA_ASPECT_RATIO_X, 0);
            mOptionBundle.putFloat(EXTRA_ASPECT_RATIO_Y, 0);
            return self();
        }

        /**
         * Set maximum size for result cropped image.
         *
         * @param width  max cropped image width
         * @param height max cropped image height
         */
        public T withMaxResultSize(@IntRange(from = MIN_SIZE) int width, @IntRange(from = MIN_SIZE) int height) {
            mOptionBundle.putInt(EXTRA_MAX_SIZE_X, width);
            mOptionBundle.putInt(EXTRA_MAX_SIZE_Y, height);
            return self();
        }

        public abstract T self();

    }

}
