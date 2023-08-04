package com.peihua.simple.glide;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

/**
 * Custom {@link ModelLoaderFactory} which provides a {@link ModelLoader} for loading thumbnails
 * from cloud media provider.
 */
public class PickerModelLoaderFactory implements ModelLoaderFactory<Uri, ParcelFileDescriptor> {

    private final Context mContext;

    public PickerModelLoaderFactory(Context context) {
        mContext = context;
    }

    @Override
    public ModelLoader<Uri, ParcelFileDescriptor> build(MultiModelLoaderFactory unused) {
        return new PickerModelLoader(mContext);
    }

    @Override
    public void teardown() {
        // Do nothing.
    }
}
