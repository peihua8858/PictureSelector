package com.peihua.simple.glide;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;

/**
 * Custom glide module to enable the loading of thumbnails from cloud media provider.
 */
@GlideModule
public class PickerGlideModule extends AppGlideModule {

    @Override
    public void registerComponents(Context context, Glide glide, Registry registry) {
        registry.prepend(Uri.class, ParcelFileDescriptor.class,
                new PickerModelLoaderFactory(context));
    }
}
