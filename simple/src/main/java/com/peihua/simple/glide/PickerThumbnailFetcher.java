package com.peihua.simple.glide;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.data.DataFetcher;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Custom {@link DataFetcher} to fetch a {@link ParcelFileDescriptor} for a thumbnail from a cloud
 * media provider.
 */
public class PickerThumbnailFetcher implements DataFetcher<ParcelFileDescriptor> {
    public static final String EXTRA_PREVIEW_THUMBNAIL = "android.provider.extra.PREVIEW_THUMBNAIL";
    public static final String EXTRA_MEDIASTORE_THUMB = "android.provider.extra.MEDIASTORE_THUMB";
    private final Context mContext;
    private final Uri mModel;
    private final int mWidth;
    private final int mHeight;
    private final boolean mIsThumbRequest;

    PickerThumbnailFetcher(Context context, Uri model, int width, int height,
            boolean isThumbRequest) {
        mContext = context;
        mModel = model;
        mWidth = width;
        mHeight = height;
        mIsThumbRequest = isThumbRequest;
    }

    @Override
    public void loadData(Priority priority, DataCallback<? super ParcelFileDescriptor> callback) {
        ContentResolver contentResolver = mContext.getContentResolver();
        final Bundle opts = new Bundle();
        opts.putParcelable(ContentResolver.EXTRA_SIZE, new Point(mWidth, mHeight));
        opts.putBoolean(EXTRA_PREVIEW_THUMBNAIL, true);

        if (mIsThumbRequest) {
            opts.putBoolean(EXTRA_MEDIASTORE_THUMB, true);
        }

        try (AssetFileDescriptor afd = contentResolver.openTypedAssetFileDescriptor(mModel,
                /* mimeType */ "image/*", opts, /* cancellationSignal */ null)) {
            if (afd == null) {
                final String err = "Failed to load data for " + mModel;
                callback.onLoadFailed(new FileNotFoundException(err));
                return;
            }
            callback.onDataReady(afd.getParcelFileDescriptor());
        } catch (IOException e) {
            callback.onLoadFailed(e);
        }
    }

    @Override
    public void cleanup() {
        // Intentionally empty only because we're not opening an InputStream or another I/O
        // resource.
    }

    @Override
    public void cancel() {
        // Intentionally empty.
    }

    @Override
    public Class<ParcelFileDescriptor> getDataClass() {
        return ParcelFileDescriptor.class;
    }

    @Override
    public DataSource getDataSource() {
        return DataSource.LOCAL;
    }
}
