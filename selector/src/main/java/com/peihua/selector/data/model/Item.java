package com.peihua.selector.data.model;

import static com.peihua.selector.util.CursorUtils.getCursorInt;
import static com.peihua.selector.util.CursorUtils.getCursorLong;
import static com.peihua.selector.util.CursorUtils.getCursorString;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.peihua.photopicker.R;
import com.peihua.selector.data.provider.ItemsProvider;
import com.peihua.selector.util.DateTimeUtils;
import com.peihua.selector.util.MimeUtils;

/**
 * Base class representing one single entity/item in the PhotoPicker.
 */
public class Item {
    private String mId;
    private long mDateTaken;
    private long mGenerationModified;
    private long mDuration;
    private String mMimeType;
    private Uri mUri;
    private boolean mIsImage;
    private boolean mIsGif;
    private boolean mIsWebP;
    private boolean mIsMotion;
    private boolean mIsVideo;
    private int mSpecialFormat;
    private String realPath;
    private boolean mIsDate;

    private Item() {
    }

    public Item(@NonNull Cursor cursor) {
        updateFromCursor(cursor);
    }

    @VisibleForTesting
    public Item(String id, String mimeType, long dateTaken, long generationModified, long duration,
                Uri uri, int specialFormat) {
        mId = id;
        mMimeType = mimeType;
        mDateTaken = dateTaken;
        mGenerationModified = generationModified;
        mDuration = duration;
        mUri = uri;
        mSpecialFormat = specialFormat;
        parseMimeType();
    }

    public String getId() {
        return mId;
    }

    public boolean isImage() {
        return mIsImage;
    }

    public boolean isVideo() {
        return mIsVideo;
    }

    public boolean isGifOrAnimatedWebp() {
        return isGif() || isAnimatedWebp();
    }

    public boolean isGif() {
        return mIsGif;
    }

    public boolean isAnimatedWebp() {
        return mIsWebP;
    }

    public boolean isMotionPhoto() {
        return mIsMotion;
    }

    public boolean isDate() {
        return mIsDate;
    }

    public Uri getContentUri() {
        return mUri;
    }

    public long getDuration() {
        return mDuration;
    }

    public String getMimeType() {
        return mMimeType;
    }

    public long getDateTaken() {
        return mDateTaken;
    }

    public long getGenerationModified() {
        return mGenerationModified;
    }

    @VisibleForTesting
    public int getSpecialFormat() {
        return mSpecialFormat;
    }

    public static Item fromCursor(Cursor cursor) {
        assert (cursor != null);
        final Item item = new Item(cursor);
        return item;
    }

    /**
     * Return the date item. If dateTaken is 0, it is a recent item.
     *
     * @param dateTaken the time of date taken. The unit is in milliseconds
     *                  since January 1, 1970 00:00:00.0 UTC.
     * @return the item with date type
     */
    public static Item createDateItem(long dateTaken) {
        final Item item = new Item();
        item.mIsDate = true;
        item.mDateTaken = dateTaken;
        return item;
    }

    public static final String AUTHORITY = "authority";

    /**
     * Update the item based on the cursor
     *
     * @param cursor the cursor to update the data
     */
    public void updateFromCursor(@NonNull Cursor cursor) {
        //  MediaStore.MediaColumns._ID,
        //            MediaStore.MediaColumns.DATA,
        //            MediaStore.MediaColumns.MIME_TYPE,
        //            MediaStore.MediaColumns.WIDTH,
        //            MediaStore.MediaColumns.HEIGHT,
        //            MediaStore.MediaColumns.DURATION,
        //            MediaStore.MediaColumns.SIZE,
        //            MediaStore.MediaColumns.DISPLAY_NAME,
        //            MediaStore.MediaColumns.BUCKET_ID,
        //            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
        //            MediaStore.MediaColumns.DATE_ADDED,
        //            MediaStore.MediaColumns.ORIENTATION,


        mId = getCursorString(cursor, MediaStore.MediaColumns._ID);
        mMimeType = getCursorString(cursor, MediaStore.MediaColumns.MIME_TYPE);
        mDateTaken = getCursorLong(cursor, MediaStore.MediaColumns.DATE_MODIFIED);
        mGenerationModified = getCursorLong(cursor, MediaStore.MediaColumns.DATE_ADDED);
        mDuration = getCursorLong(cursor, MediaStore.MediaColumns.DURATION);
        mSpecialFormat = getCursorInt(cursor, MediaStore.MediaColumns.MIME_TYPE);
        realPath = getCursorString(cursor, MediaStore.MediaColumns.DATA);
        mUri = ItemsProvider.getItemsUri(mId, mMimeType, realPath);
        parseMimeType();
    }

    public String getContentDescription(@NonNull Context context) {
        if (isVideo()) {
            return context.getString(R.string.picker_video_item_content_desc,
                    DateTimeUtils.getDateTimeStringForContentDesc(getDateTaken()),
                    getDurationText());
        }

        final String itemType;
        if (isGif() || isAnimatedWebp()) {
            itemType = context.getString(R.string.picker_gif);
        } else if (isMotionPhoto()) {
            itemType = context.getString(R.string.picker_motion_photo);
        } else {
            itemType = context.getString(R.string.picker_photo);
        }

        return context.getString(R.string.picker_item_content_desc, itemType,
                DateTimeUtils.getDateTimeStringForContentDesc(getDateTaken()));
    }

    public String getDurationText() {
        if (mDuration == -1) {
            return "";
        }
        return DateUtils.formatElapsedTime(mDuration / 1000);
    }

    private void parseMimeType() {
        if (MimeUtils.isHasGif(mMimeType)||MimeUtils.isUrlHasGif(realPath)) {
            mIsGif = true;
        } else if (MimeUtils.isHasWebp(mMimeType)||MimeUtils.isUrlHasWebp(realPath)) {
            mIsWebP = true;
        } else if (MimeUtils.isImageMimeType(mMimeType)||MimeUtils.isUrlHasImage(realPath)) {
            mIsImage = true;
        } else if (MimeUtils.isVideoMimeType(mMimeType)||MimeUtils.isUrlHasVideo(realPath)) {
            mIsVideo = true;
        }
    }

    /**
     * Compares this item with given {@code anotherItem} by comparing
     * {@link Item#getDateTaken()} value. When {@link Item#getDateTaken()} is
     * same, Items are compared based on {@link Item#getId}.
     */
    public int compareTo(Item anotherItem) {
        if (mDateTaken > anotherItem.getDateTaken()) {
            return 1;
        } else if (mDateTaken < anotherItem.getDateTaken()) {
            return -1;
        } else {
            return mId.compareTo(anotherItem.getId());
        }
    }
}
