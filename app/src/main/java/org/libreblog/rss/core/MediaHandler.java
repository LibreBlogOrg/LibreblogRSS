package org.libreblog.rss.core;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.database.StandaloneDatabaseProvider;
import androidx.media3.datasource.DataSink;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.Cache;
import androidx.media3.datasource.cache.CacheDataSink;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor;
import androidx.media3.datasource.cache.SimpleCache;

import java.io.File;

@UnstableApi
public class MediaHandler {
    public static final long MAX_CACHE_SIZE = 100L * 1024 * 1024; // 100MB
    public static DataSource.Factory cacheDataSourceFactory = null;

    @OptIn(markerClass = UnstableApi.class)
    public static void init(Context context) {
        if (context == null) return;
        if (cacheDataSourceFactory != null) return;

        DataSource.Factory upstreamFactory = new DefaultHttpDataSource.Factory();
        LeastRecentlyUsedCacheEvictor evictor = new LeastRecentlyUsedCacheEvictor(MAX_CACHE_SIZE);
        SimpleCache simpleCache = new SimpleCache(new File(context.getCacheDir(), "media"), evictor, new StandaloneDatabaseProvider(context));
        DataSink.Factory cacheSinkFactory = new MyCacheDataSinkFactory(simpleCache);
        cacheDataSourceFactory = new CacheDataSource.Factory()
                .setCache(simpleCache)
                .setUpstreamDataSourceFactory(upstreamFactory)
                .setCacheWriteDataSinkFactory(cacheSinkFactory);
    }

    public static final class MyCacheDataSinkFactory implements DataSink.Factory {
        private final Cache cache;

        public MyCacheDataSinkFactory(Cache cache) {
            this.cache = cache;
        }

        @NonNull
        @Override
        public DataSink createDataSink() {
            return new CacheDataSink(cache, CacheDataSink.DEFAULT_FRAGMENT_SIZE, CacheDataSink.DEFAULT_BUFFER_SIZE);
        }
    }
}
