package org.libreblog.rss.proxy;

import androidx.annotation.NonNull;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.InputStream;
import java.net.Proxy;

public class ProxyUrlLoaderFactory implements ModelLoaderFactory<GlideUrl, InputStream> {
    private final Proxy torProxy;
    private final Proxy defaultProxy;

    public ProxyUrlLoaderFactory(Proxy torProxy, Proxy defaultProxy) {
        this.torProxy = torProxy;
        this.defaultProxy = defaultProxy;
    }

    @NonNull
    @Override
    public ModelLoader<GlideUrl, InputStream> build(@NonNull MultiModelLoaderFactory multiFactory) {
        return new ProxyUrlLoader(torProxy, defaultProxy);
    }

    @Override
    public void teardown() {
    }
}
