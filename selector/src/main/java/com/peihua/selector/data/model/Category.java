package com.peihua.selector.data.model;

import static com.peihua.selector.util.CursorUtils.getCursorInt;
import static com.peihua.selector.util.CursorUtils.getCursorLong;
import static com.peihua.selector.util.CursorUtils.getCursorString;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.peihua.photopicker.R;
import com.peihua.selector.data.provider.ItemsProvider;
import com.peihua.selector.util.MimeUtils;
import com.peihua.selector.util.Utils;

import java.io.File;
import java.util.Locale;
import java.util.Objects;

public class Category {
    public static final Category DEFAULT = new Category();

    private final long mBucketId;
    private final String mId;
    private final String mDisplayName;
    private final boolean mIsLocal;
    private final Uri mCoverUri;
    private int mItemCount;
    private boolean mIsGif;
    private boolean mIsWebP;
    private String authority;

    private Category() {
        this(-1, null, null, null, 0, false);
    }

    @VisibleForTesting
    public Category(long bucketId, String id, String displayName, Uri coverUri, int itemCount,
                    boolean isLocal) {
        mBucketId = bucketId;
        mId = id;

        mDisplayName = displayName;
        mIsLocal = isLocal;
        mCoverUri = coverUri;
        mItemCount = itemCount;
    }

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "Category: {mId: %s, mDisplayName: %s, " +
                        "mCoverUri: %s, mItemCount: %d, mIsLocal: %b mBucketId:%s",
                mId, mDisplayName, mCoverUri, mItemCount, mIsLocal, mBucketId);
    }

    public String getId() {
        return mId;
    }

    public long getBucketId() {
        return mBucketId;
    }

    public String getDisplayName(Context context) {
        if (isDefault()) {
            return context.getString(R.string.picker_photos);
        }
        if (mIsLocal) {
            return getLocalizedDisplayName(context, mDisplayName);
        }
        return mDisplayName;
    }

    public String getAuthority() {
        return authority;
    }

    private String mMimeType;

    public String getDisplayName() {
        return mDisplayName;
    }

    public boolean isIsLocal() {
        return mIsLocal;
    }


    public boolean isLocal() {
        return mIsLocal;
    }

    public Uri getCoverUri() {
        return mCoverUri;
    }

    public int getItemCount() {
        return mItemCount;
    }

    public void setItemCount(int mItemCount) {
        this.mItemCount = mItemCount;
    }

    public boolean isDefault() {
        return TextUtils.isEmpty(mId);
    }

    public boolean isGif() {
        return mIsGif;
    }

    public boolean isAnimatedWebp() {
        return mIsWebP;
    }

    /**
     * Write the {@link Category} to the given {@code bundle}.
     */
    public void toBundle(@NonNull Bundle bundle) {
        bundle.putLong(MediaStore.MediaColumns.BUCKET_ID, mBucketId);
        bundle.putString(AlbumColumns.ID, mId);
        bundle.putString(AlbumColumns.DISPLAY_NAME, mDisplayName);
        // Re-using the 'media_cover_id' to store the media_cover_uri for lack of
        // a different constant
        bundle.putParcelable(AlbumColumns.MEDIA_COVER_ID, mCoverUri);
        bundle.putInt(AlbumColumns.MEDIA_COUNT, mItemCount);
        bundle.putBoolean(AlbumColumns.IS_LOCAL, mIsLocal);
    }

    /**
     * Create a {@link Category} from the {@code bundle}.
     */
    public static Category fromBundle(@NonNull Bundle bundle) {
        return new Category(bundle.getLong(MediaStore.MediaColumns.BUCKET_ID),
                bundle.getString(AlbumColumns.ID),
                bundle.getString(AlbumColumns.DISPLAY_NAME),
                bundle.getParcelable(AlbumColumns.MEDIA_COVER_ID),
                bundle.getInt(AlbumColumns.MEDIA_COUNT),
                bundle.getBoolean(AlbumColumns.IS_LOCAL));
    }

    private String realPath;

    /**
     * Create a {@link Category} from the {@code cursor}.
     */
    public static Category fromCursor(@NonNull Cursor cursor) {
        long bucketId = getCursorLong(cursor, MediaStore.MediaColumns.BUCKET_ID);
        String id = getCursorString(cursor, MediaStore.MediaColumns._ID);
        String mMimeType = getCursorString(cursor, MediaStore.MediaColumns.MIME_TYPE);
        String realPath = getCursorString(cursor, MediaStore.MediaColumns.DATA);
        String displayName = getCursorString(cursor, MediaStore.MediaColumns.BUCKET_DISPLAY_NAME);
        if (!TextUtils.isEmpty(realPath)) {
            displayName = (Utils.getFolderName(new File(realPath)));
        }
        final Uri coverUri = ItemsProvider.getItemsUri(id, mMimeType, realPath);
        Category category = new Category(bucketId, id, displayName,
                coverUri,
                getCursorInt(cursor, ItemsProvider.COLUMN_COUNT),
                true);
        category.realPath = realPath;
        category.mMimeType = mMimeType;
        category.parseMimeType();
        return category;
    }

    private void parseMimeType() {
        if (MimeUtils.isHasGif(mMimeType) || MimeUtils.isUrlHasGif(realPath)) {
            mIsGif = true;
        } else if (MimeUtils.isHasWebp(mMimeType) || MimeUtils.isUrlHasWebp(realPath)) {
            mIsWebP = true;
        }
    }

    private static String getLocalizedDisplayName(Context context, String albumId) {
        switch (albumId) {
            case AlbumColumns.ALBUM_ID_VIDEOS:
                return context.getString(R.string.picker_videos);
            case AlbumColumns.ALBUM_ID_CAMERA:
                return context.getString(R.string.picker_category_camera);
            case AlbumColumns.ALBUM_ID_SCREENSHOTS:
                return context.getString(R.string.picker_category_screenshots);
            case AlbumColumns.ALBUM_ID_DOWNLOADS:
                return context.getString(R.string.picker_category_downloads);
            case AlbumColumns.ALBUM_ID_FAVORITES:
                return context.getString(R.string.picker_category_favorites);
            default:
                return albumId;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        Category category = (Category) o;
        return mBucketId == category.mBucketId && mIsLocal == category.mIsLocal
                && mItemCount == category.mItemCount &&
                mIsGif == category.mIsGif && mIsWebP == category.mIsWebP
                && Objects.equals(mId, category.mId)
                && Objects.equals(mDisplayName, category.mDisplayName)
                && Objects.equals(mCoverUri, category.mCoverUri)
                && Objects.equals(getAuthority(), category.getAuthority())
                && Objects.equals(mMimeType, category.mMimeType)
                && Objects.equals(realPath, category.realPath);
    }

    @Override
    public int hashCode() {
        int result = Long.hashCode(mBucketId);
        result = 31 * result + Objects.hashCode(mId);
        result = 31 * result + Objects.hashCode(mDisplayName);
        result = 31 * result + Boolean.hashCode(mIsLocal);
        result = 31 * result + Objects.hashCode(mCoverUri);
        result = 31 * result + mItemCount;
        result = 31 * result + Boolean.hashCode(mIsGif);
        result = 31 * result + Boolean.hashCode(mIsWebP);
        result = 31 * result + Objects.hashCode(getAuthority());
        result = 31 * result + Objects.hashCode(mMimeType);
        result = 31 * result + Objects.hashCode(realPath);
        return result;
    }
}
