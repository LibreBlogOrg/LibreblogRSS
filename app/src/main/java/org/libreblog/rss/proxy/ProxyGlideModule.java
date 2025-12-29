package org.libreblog.rss.proxy;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.module.AppGlideModule;

import org.libreblog.rss.utils.Utils;

import java.io.InputStream;
import java.net.Proxy;

@GlideModule
public class ProxyGlideModule extends AppGlideModule {
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry) {
        registry.replace(GlideUrl.class, InputStream.class, new ProxyUrlLoaderFactory(Utils.torProxy(), Proxy.NO_PROXY));
    }
}

