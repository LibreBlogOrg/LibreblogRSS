package org.libreblog.rss.proxy;

import static org.libreblog.rss.core.RssDiscover.DEFAULT_TIMEOUT;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.signature.ObjectKey;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

public class ProxyUrlLoader implements ModelLoader<GlideUrl, InputStream> {
    private final Proxy torProxy;
    private final Proxy defaultProxy;

    public ProxyUrlLoader(Proxy torProxy, Proxy defaultProxy) {
        this.torProxy = torProxy;
        this.defaultProxy = defaultProxy;
    }

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(@NonNull GlideUrl model, int width, int height, @NonNull Options options) {
        return new LoadData<>(new ObjectKey(model), new ProxyDataFetcher(model.toStringUrl(), torProxy, defaultProxy));
    }

    @Override
    public boolean handles(@NonNull GlideUrl model) {
        return true;
    }

    private static class ProxyDataFetcher implements DataFetcher<InputStream> {
        private final String url;
        private final Proxy torProxy;
        private final Proxy defaultProxy;
        private HttpURLConnection conn;
        private InputStream stream;

        ProxyDataFetcher(String url, Proxy torProxy, Proxy defaultProxy) {
            this.url = url;
            this.torProxy = torProxy;
            this.defaultProxy = defaultProxy;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super InputStream> callback) {
            try {
                URL u = new URL(url);
                if (u.getHost() != null && u.getHost().endsWith(".onion")) {
                    conn = (HttpURLConnection) u.openConnection(torProxy);
                } else {
                    conn = (HttpURLConnection) u.openConnection(defaultProxy);
                }
                conn.setConnectTimeout(DEFAULT_TIMEOUT * 3);
                conn.setReadTimeout(DEFAULT_TIMEOUT * 3);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestProperty("User-Agent", "Feed reader (Android)");
                int code = conn.getResponseCode();
                if (code >= 400) {
                    stream = conn.getErrorStream();
                } else {
                    stream = conn.getInputStream();
                }
                callback.onDataReady(stream);
            } catch (IOException e) {
                Log.w("ProxyUrlLoader", "Cannot load data", e);
                callback.onLoadFailed(e);
            }
        }

        @Override
        public void cleanup() {
            try {
                if (stream != null) stream.close();
            } catch (IOException ignored) {
            }
            if (conn != null) conn.disconnect();
        }

        @Override
        public void cancel() {
        }

        @NonNull
        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource() {
            return DataSource.REMOTE;
        }
    }
}
