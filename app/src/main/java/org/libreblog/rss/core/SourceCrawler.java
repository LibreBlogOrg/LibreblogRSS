package org.libreblog.rss.core;

import static com.github.pemistahl.lingua.api.Language.ARABIC;
import static com.github.pemistahl.lingua.api.Language.BENGALI;
import static com.github.pemistahl.lingua.api.Language.CHINESE;
import static com.github.pemistahl.lingua.api.Language.ENGLISH;
import static com.github.pemistahl.lingua.api.Language.FRENCH;
import static com.github.pemistahl.lingua.api.Language.GERMAN;
import static com.github.pemistahl.lingua.api.Language.HINDI;
import static com.github.pemistahl.lingua.api.Language.INDONESIAN;
import static com.github.pemistahl.lingua.api.Language.ITALIAN;
import static com.github.pemistahl.lingua.api.Language.JAPANESE;
import static com.github.pemistahl.lingua.api.Language.KOREAN;
import static com.github.pemistahl.lingua.api.Language.PORTUGUESE;
import static com.github.pemistahl.lingua.api.Language.RUSSIAN;
import static com.github.pemistahl.lingua.api.Language.SPANISH;
import static com.github.pemistahl.lingua.api.Language.URDU;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.github.pemistahl.lingua.api.Language;
import com.github.pemistahl.lingua.api.LanguageDetector;
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder;
import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;

import org.json.JSONObject;
import org.libreblog.rss.utils.Settings;

import java.util.List;
import java.util.Objects;

public class SourceCrawler {
    public static final int ONE_MINUTE = 60000;
    public static final int TIME_TO_REFRESH_SOURCE = 5 * ONE_MINUTE;
    public static final int TIME_TO_REFRESH_ACTIVITY_PUB_PROFILE = 30 * ONE_MINUTE;
    public static final int TIME_TO_REFRESH = 2 * ONE_MINUTE;
    private static final LanguageDetector languageDetector = LanguageDetectorBuilder.fromLanguages(ENGLISH, FRENCH,
            GERMAN, SPANISH, CHINESE, ARABIC, PORTUGUESE, RUSSIAN, JAPANESE, KOREAN, ITALIAN,
            URDU, BENGALI, HINDI, INDONESIAN).build();
    private final DbHandler db;
    private final OnRefreshListener onRefreshListener;

    public SourceCrawler(Context context, OnRefreshListener onRefreshListener) {
        this.db = new DbHandler(context);
        this.onRefreshListener = onRefreshListener;
    }

    public static void updateLastRefresh(Context context) {
        SharedPreferences settings = Settings.getSettings(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("last_refresh", System.currentTimeMillis());
        editor.apply();
    }

    private static String formatLanguage(String lang) {
        if (lang == null) return "en";

        lang = lang.toLowerCase();
        lang = lang.substring(0, 2);

        List<String> langs = List.of("af", "ar", "hy", "eu", "bn", "br", "bg", "ca",
                "zh", "hr", "cs", "da", "nl", "en", "eo", "et", "fi", "fr", "gl", "de", "el",
                "gu", "ha", "he", "hi", "hu", "id", "ga", "it", "ja", "ko", "ku", "la", "lt",
                "lv", "ms", "mr", "no", "fa", "pl", "pt", "ro", "ru", "sk", "sl", "so", "st",
                "es", "sw", "sv", "th", "tl", "tr", "uk", "ur", "vi", "yo", "zu");
        if (langs.contains(lang)) {
            return lang;
        }
        return "en";
    }

    private SyndFeed buildFeed(DbHandler.Source source) throws Exception {
        if (Objects.equals(source.type, DbHandler.SOURCE_TYPE_RSS)) {
            SyndFeedInput input = new SyndFeedInput();
            return input.build(new XmlReader(RssDiscover.getURLConnection(source.id)));
        } else if (Objects.equals(source.type, DbHandler.SOURCE_TYPE_ACTIVITY_PUB)) {
            JSONObject outbox;
            if (source.refreshed + TIME_TO_REFRESH_ACTIVITY_PUB_PROFILE < System.currentTimeMillis()) {
                outbox = ActivityPubHandler.findOutbox(source.id, source,0);
            } else {
                outbox = ActivityPubHandler.findOutbox(source.link, source,0);
            }
            return ActivityPubHandler.convertOutboxJsonToSyndFeed(source, outbox);
        }

        throw new RuntimeException("Unrecognized source type");
    }

    public void refresh(Context context) {
        List<DbHandler.Source> sources = db.getSources();

        int[] count = {0};
        int pos = sources.size() - 1;
        while (pos >= 0) {
            DbHandler.Source source = sources.get(pos--);
            if (source.ttl > 0) {
                if (source.refreshed + (long) source.ttl * ONE_MINUTE > System.currentTimeMillis()) {
                    count[0]++;
                    continue;
                }
            } else if (source.refreshed + TIME_TO_REFRESH_SOURCE > System.currentTimeMillis()) {
                count[0]++;
                continue;
            }

            Thread thread = new Thread(() -> {
                try {
                    SyndFeed feed = buildFeed(source);
                    updateSource(feed, source);
                    long ts = System.currentTimeMillis();

                    boolean ok;
                    int i = 0;
                    for (SyndEntry entry : feed.getEntries()) {
                        ok = db.putArticle(entry, source, ts, i);
                        if (ok) i++;
                    }
                } catch (Exception e) {
                    Log.w("SourceCrawler", "Cannot update source", e);
                }

                count[0]++;
                if (count[0] == sources.size()) {
                    ArticleSorter.sort(context);
                    updateLastRefresh(context);
                    if (onRefreshListener != null) onRefreshListener.onRefresh();
                }
            });
            thread.start();
        }
    }

    public void refreshSource(String sourceId) {
        DbHandler.Source source = db.getSource(sourceId);

        Thread thread = new Thread(() -> {
            try {
                SyndFeed feed = buildFeed(source);
                updateSource(feed, source);
                long ts = System.currentTimeMillis();

                boolean ok;
                int i = 0;
                for (SyndEntry entry : feed.getEntries()) {
                    ok = db.putArticle(entry, source, ts, i);
                    if (ok) i++;
                }
            } catch (Exception e) {
                Log.w("SourceCrawler", "Cannot refresh source", e);
            }
        });
        thread.start();
    }

    private void updateSource(SyndFeed feed, DbHandler.Source source) {
        if (feed == null || source == null) return;

        StringBuilder detectLanguageOf = new StringBuilder();

        if (feed.getIcon() != null) {
            db.setSourceImage(source.id, feed.getIcon().getUrl());
        } else if (feed.getImage() != null) {
            db.setSourceImage(source.id, feed.getImage().getUrl());
        }

        String title = feed.getTitle();
        if (title != null && !title.trim().isEmpty()) {
            db.setSourceTitle(source.id, title.trim());
            detectLanguageOf = new StringBuilder(title);
        }

        String link = feed.getLink();
        if (link != null && !link.isEmpty()) {
            db.setSourceLink(source.id, link);
        }

        String description = feed.getDescription();
        if (description != null && !description.trim().isEmpty()) {
            db.setSourceDescription(source.id, description.trim());
            detectLanguageOf.append(" ").append(description);
        }

        int count = 0;
        for (SyndEntry entry : feed.getEntries()) {
            String t = entry.getTitle();
            if (t != null && !t.isEmpty()) detectLanguageOf.append(" ").append(t);
            if (count++ > 10) break;
        }

        String language = feed.getLanguage();
        if (language != null && !language.isEmpty()) {
            db.setSourceLanguage(source.id, formatLanguage(feed.getLanguage()));
        } else if (source.language == null || source.language.isEmpty()) {
            Language detectedLanguage = languageDetector.detectLanguageOf(detectLanguageOf.toString());
            db.setSourceLanguage(source.id, formatLanguage(detectedLanguage.getIsoCode639_1().toString()));
        }

        WireFeed wire = feed.createWireFeed();
        if (wire instanceof Channel) {
            Channel channel = (Channel) wire;
            int ttl = channel.getTtl();
            if (ttl > 0 && ttl <= 60) {
                db.setSourceTtl(source.id, ttl);
            } else if (ttl > 60) {
                db.setSourceTtl(source.id, 60);
            }
        }

        db.updateSourceRefreshed(source.id);
    }

    public interface OnRefreshListener {
        void onRefresh();
    }
}