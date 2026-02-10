package org.libreblog.rss.utils;

import com.rometools.rome.feed.synd.SyndFeed;

public interface FeedCallback {
    void onResult(SyndFeed feed);

    void onError(Exception e);
}
