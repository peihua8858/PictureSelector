package com.peihua.simple.glide;

import android.content.Context;
import android.content.UriMatcher;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.load.Option;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

/**
 * Custom {@link ModelLoader} to load thumbnails from cloud media provider.
 */
public final class PickerModelLoader implements ModelLoader<Uri, ParcelFileDescriptor> {
    private final Context mContext;
    public static final Option<Boolean> THUMBNAIL_REQUEST =
            Option.memory(PickerThumbnailFetcher.EXTRA_MEDIASTORE_THUMB, false);
    PickerModelLoader(Context context) {
        mContext = context;
    }

    @Override
    public LoadData<ParcelFileDescriptor> buildLoadData(Uri model, int width, int height,
            Options options) {
        final boolean isThumbRequest = Boolean.TRUE.equals(options.get(THUMBNAIL_REQUEST));
        return new LoadData<>(new ObjectKey(model),
                new PickerThumbnailFetcher(mContext, model, width, height, isThumbRequest));
    }

    @Override
    public boolean handles(Uri model) {
        final int pickerId = 1;
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
//        matcher.addURI(model.getAuthority(),
//                CloudMediaProviderContract.URI_PATH_MEDIA + "/*", pickerId);

        // Matches picker URIs of the form content://<authority>/media
        return matcher.match(model) == pickerId;
    }
}
