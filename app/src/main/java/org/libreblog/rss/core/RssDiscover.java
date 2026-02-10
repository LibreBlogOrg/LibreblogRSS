package org.libreblog.rss.core;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndImage;
import com.rometools.rome.feed.synd.SyndImageImpl;
import com.rometools.rome.feed.synd.SyndLink;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.libreblog.rss.utils.FeedCallback;
import org.libreblog.rss.utils.Utils;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class RssDiscover {
    public static final int DEFAULT_TIMEOUT = 6000;
    private static final Executor EXEC = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void discover(Context context, final String urlString, final FeedCallback cb) {
        EXEC.execute(() -> {
            try {
                boolean differentUrl = false;
                SyndFeed feed = getRSS(urlString);

                if (feed == null) {
                    feed = findRss(urlString);
                    if (feed == null) {
                        String canonicalUrl = findCanonicalLink(urlString);
                        if (canonicalUrl != null) feed = findRss(canonicalUrl);
                        if (feed == null) {
                            String host = new URI(urlString).getHost();
                            String user = urlString.replaceFirst("^https?://[^/]+/@([^/?#]+)/?.*$", "$1");
                            if (!user.isEmpty() && !user.equals(urlString)) {
                                feed = findRss("https://" + user + "." + host);
                            }
                        }
                    }

                    for (String path : Arrays.asList("feed", "rss", "atom", "feed.xml", "rss.xml", "atom.xml")) {
                        if (feed == null) {
                            feed = getRSS(urlString + (urlString.endsWith("/") ? path : "/" + path));
                        } else {
                            break;
                        }
                    }
                    differentUrl = true;
                }

                if (feed == null) {
                    MAIN.post(() -> {
                        if (cb != null) cb.onResult(null);
                    });
                    return;
                }

                useBestIcon(context, feed, differentUrl ? urlString : null, cb);
            } catch (Exception e) {
                Log.w("RssDiscover", "Cannot find RSS", e);
                MAIN.post(() -> {
                    if (cb != null) cb.onError(e);
                });
            }
        });
    }

    private static void useBestIcon(Context context, final SyndFeed feed, String originalUrl, final FeedCallback cb) {
        List<Img> imgs = new ArrayList<>();
        List<Img> originalUrlImgs = new ArrayList<>();
        List<Img> alternateImgs = new ArrayList<>();

        String feedImgStr = feed.getIcon() != null &&
                feed.getIcon().getUrl() != null &&
                !feed.getIcon().getUrl().isEmpty() ? feed.getIcon().getUrl() :
                feed.getImage() != null &&
                        feed.getImage().getUrl() != null &&
                        !feed.getImage().getUrl().isEmpty() ? feed.getImage().getUrl() : null;
        Img feedImg = new Img(feedImgStr, 1);

        String alternateLink = "";
        for (SyndLink link : feed.getLinks()) {
            if (!Objects.equals(link.getRel(), "self")) {
                alternateLink = link.getHref();
                break;
            }
        }

        if (alternateLink.isEmpty()) {
            alternateLink = findLinkInRSS(feed.getLink());
        }

        if (alternateLink != null) {
            alternateImgs = findIcon(alternateLink);
        }

        if (originalUrl != null && !originalUrl.equals(alternateLink)) {
            originalUrlImgs = findIcon(originalUrl);
        }

        imgs.add(feedImg);
        if (alternateImgs != null) imgs.addAll(alternateImgs);
        if (originalUrlImgs != null) imgs.addAll(originalUrlImgs);

        final int[] lastImgQuality = {0};
        final int[] runs = {0};
        for (Img img : imgs) {
            if (img == null || img.url == null) {
                MAIN.post(() -> {
                    runs[0]++;
                    if (runs[0] == imgs.size()) {
                        cb.onResult(feed);
                    }
                });
                continue;
            }

            if (img.url.toLowerCase().endsWith("svg")) {
                boolean noPreviousIcon = feed.getIcon() == null ||
                        feed.getIcon().getUrl() == null ||
                        feed.getIcon().getUrl().isEmpty();
                int imgQuality = img.priority == 1 ? 1 : 0;
                if (noPreviousIcon ||
                        imgQuality > lastImgQuality[0]) {
                    SyndImage syndImage = new SyndImageImpl();
                    syndImage.setUrl(img.url);
                    feed.setIcon(syndImage);
                    lastImgQuality[0] = img.priority == 1 ? 1 : 0;

                    MAIN.post(() -> {
                        runs[0]++;
                        if (runs[0] == imgs.size()) {
                            cb.onResult(feed);
                        }
                    });
                    continue;
                }
            }

            try {
                Glide.with(context)
                        .asBitmap()
                        .load(img.url)
                        .timeout(DEFAULT_TIMEOUT)
                        .override(Target.SIZE_ORIGINAL)
                        .listener(new RequestListener<>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                                        Target<Bitmap> target, boolean isFirstResource) {
                                MAIN.post(() -> {
                                    runs[0]++;
                                    if (runs[0] == imgs.size()) {
                                        cb.onResult(feed);
                                    }
                                });
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model,
                                                           Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                return false;
                            }
                        })
                        .into(new CustomTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                int w = resource.getWidth();
                                int h = resource.getHeight();

                                boolean noPreviousIcon = feed.getIcon() == null ||
                                        feed.getIcon().getUrl() == null ||
                                        feed.getIcon().getUrl().isEmpty();
                                int imgQuality = (w != 0 && w == h ? 2 : 0) + (img.priority == 1 ? 1 : 0);
                                if (noPreviousIcon ||
                                        imgQuality > lastImgQuality[0]) {
                                    lastImgQuality[0] = imgQuality;

                                    SyndImage syndImage = new SyndImageImpl();
                                    syndImage.setUrl(img.url);
                                    feed.setIcon(syndImage);
                                }

                                MAIN.post(() -> {
                                    runs[0]++;
                                    if (runs[0] == imgs.size()) {
                                        cb.onResult(feed);
                                    }
                                });
                            }

                            @Override
                            public void onLoadCleared(@Nullable Drawable placeholder) {
                            }
                        });

            } catch (Exception e) {
                MAIN.post(() -> {
                    runs[0]++;
                    if (runs[0] == imgs.size()) {
                        cb.onResult(feed);
                    }
                });
            }
        }
    }

    public static SyndFeed getRSS(String urlString) {
        SyndFeedInput input = new SyndFeedInput();
        try {
            SyndFeed feed = input.build(new XmlReader(getURLConnection(urlString)));
            feed.setLink(urlString);
            return feed;

        } catch (Exception e) {
            return null;
        }
    }

    public static SyndFeed findRss(String pageUrl) throws IOException {
        Document doc = getConnection(pageUrl, true).get();

        Elements links = doc.select("link[rel=alternate][type]");
        for (Element link : links) {
            String type = link.attr("type").toLowerCase();
            if (type.contains("rss") || type.contains("atom") || type.contains("xml")) {
                String href = link.absUrl("href");
                if (href.isEmpty()) href = link.attr("href");
                if (!href.isEmpty()) {
                    SyndFeed feed = getRSS(href);
                    if (feed == null) return null;

                    feed.setLink(href);
                    return feed;
                }
            }
        }

        return null;
    }

    public static String findCanonicalLink(String pageUrl) {
        Document doc;
        try {
            doc = getConnection(pageUrl, true).get();
        } catch (IOException e) {
            Log.w("RssDiscover", "Cannot find link", e);
            return null;
        }

        //return first link
        Elements links = doc.select("link[rel=canonical]");
        for (Element link : links) {
            String href = link.attr("href");
            if (href.startsWith("http") && !Objects.equals(href, pageUrl)) {
                return href;
            }
        }

        return null;
    }

    public static String findLinkInRSS(String pageUrl) {
        Document doc;
        try {
            doc = getConnection(pageUrl, true).get();
        } catch (IOException e) {
            Log.w("RssDiscover", "Cannot find link", e);
            return null;
        }

        //return first link
        Elements links = doc.select("link");
        for (Element link : links) {
            String value = link.nodeValue();
            value = value.trim();
            if (value.startsWith("http") && !Objects.equals(value, pageUrl)) {
                return value;
            }
        }

        return null;
    }

    public static URLConnection getURLConnection(String pageUrl) throws IOException {
        URL url = new URL(pageUrl);

        if (Utils.isOnionAddress(pageUrl)) {
            return url.openConnection(Utils.torProxy());
        }

        return url.openConnection();
    }

    public static Connection getConnection(String pageUrl, boolean head) {
        if (Utils.isOnionAddress(pageUrl)) {
            return Jsoup.connect(pageUrl)
                    .proxy(Utils.torProxy())
                    .method(head ? Connection.Method.HEAD : Connection.Method.GET)
                    .userAgent("Mozilla/5.0 (Android) feed-discovery")
                    .timeout(DEFAULT_TIMEOUT * 3)
                    .followRedirects(true);
        }

        return Jsoup.connect(pageUrl)
                .method(head ? Connection.Method.HEAD : Connection.Method.GET)
                .userAgent("Mozilla/5.0 (Android) feed-discovery")
                .timeout(DEFAULT_TIMEOUT)
                .followRedirects(true);
    }

    public static List<Img> findIcon(String pageUrl) {
        List<Img> imgs = new ArrayList<>();
        Document doc;
        try {
            doc = getConnection(pageUrl, true).get();
        } catch (IOException e) {
            return null;
        }

        Elements metas = doc.select("meta[property=og:image][content]");
        if (!metas.isEmpty()) imgs.add(new Img(metas.get(0).attr("content"), 1));

        Elements lds = doc.select("script[type=application/ld+json]");
        if (!lds.isEmpty()) {
            String jsonLd = lds.get(0).data();
            String img = Utils.getImageUrlFromJsonLd(jsonLd);
            if (img != null) imgs.add(new Img(img, 1));
        }

        String href = null;
        Elements links = doc.select("link[rel*=icon][href]");
        for (Element link : links) {
            href = link.attr("href");

            String sizes = link.attr("sizes");
            if (sizes.contains("x")) {
                String[] ws = sizes.split("x");
                String w = ws[0];
                boolean isNumber = NumberUtils.isCreatable(w);
                if (isNumber && Integer.parseInt(w) > 70) break;
            }
        }

        if (href != null && !href.startsWith("http")) href = pageUrl +
                (!pageUrl.endsWith("/") && !href.startsWith("/") ? "/" : "") + href;
        if (href != null) imgs.add(new Img(href, 2));

        return imgs;
    }

    public static class Img {
        public final String url;
        public final int priority;

        public Img(String url, int priority) {
            this.url = url;
            this.priority = priority;
        }
    }
}

