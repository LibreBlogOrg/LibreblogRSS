package org.libreblog.rss.core;

import static org.libreblog.rss.ui.FragmentSettings.DELETE_AFTER_X_DAYS;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.rometools.rome.feed.synd.SyndEntry;

import org.libreblog.rss.utils.Settings;
import org.libreblog.rss.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DbHandler extends SQLiteOpenHelper {
    public static final String DB_NAME = "reader_db";
    public static final int DB_VERSION = 39;

    //Articles
    public static final String ARTICLES_TABLE_NAME = "articles";
    public static final String ARTICLES_ID_COL = "id";
    public static final String ARTICLES_SOURCE_COL = "source";
    public static final String ARTICLES_TITLE_COL = "title";
    public static final String ARTICLES_CLEANED_TITLE_COL = "ctitle";
    public static final String ARTICLES_LINK_COL = "link";
    public static final String ARTICLES_AUTHORS_COL = "authors";
    public static final String ARTICLES_CATEGORIES_COL = "categories";
    public static final String ARTICLES_COMMENTS_COL = "comments";
    public static final String ARTICLES_PUBLISHED_COL = "published";
    public static final String ARTICLES_TIMESTAMP_COL = "timestamp";
    public static final String ARTICLES_POSITION_COL = "position";
    public static final String ARTICLES_IMAGE_COL = "image";
    public static final String ARTICLES_VIDEO_COL = "video";
    public static final String ARTICLES_AUDIO_COL = "audio";
    public static final String ARTICLES_DESCRIPTION_COL = "description";
    public static final String ARTICLES_CLEANED_DESCRIPTION_COL = "cdescription";
    public static final String ARTICLES_LIKED_COL = "liked";
    public static final String ARTICLES_OPENED_COL = "opened";
    public static final String ARTICLES_DISPLAYED_COL = "displayed";
    public static final String ARTICLES_HIDDEN_COL = "hidden";
    public static final String ARTICLES_SCORE_COL = "score";
    public static final String[] ARTICLES_ALL_COLUMNS = {
            ARTICLES_ID_COL,
            ARTICLES_SOURCE_COL,
            ARTICLES_TITLE_COL,
            ARTICLES_CLEANED_TITLE_COL,
            ARTICLES_LINK_COL,
            ARTICLES_AUTHORS_COL,
            ARTICLES_CATEGORIES_COL,
            ARTICLES_COMMENTS_COL,
            ARTICLES_PUBLISHED_COL,
            ARTICLES_TIMESTAMP_COL,
            ARTICLES_POSITION_COL,
            ARTICLES_IMAGE_COL,
            ARTICLES_AUDIO_COL,
            ARTICLES_VIDEO_COL,
            ARTICLES_DESCRIPTION_COL,
            ARTICLES_CLEANED_DESCRIPTION_COL,
            ARTICLES_LIKED_COL,
            ARTICLES_OPENED_COL,
            ARTICLES_DISPLAYED_COL,
            ARTICLES_HIDDEN_COL,
            ARTICLES_SCORE_COL
    };

    //Sources
    public static final String SOURCE_TYPE_RSS = "rss";
    public static final String SOURCE_TYPE_ACTIVITY_PUB = "activity_pub";
    public static final String SOURCES_TABLE_NAME = "sources";
    public static final String SOURCES_ID_COL = "id";
    public static final String SOURCES_NAME_COL = "name";
    public static final String SOURCES_TITLE_COL = "title";
    public static final String SOURCES_LINK_COL = "link";
    public static final String SOURCES_DESCRIPTION_COL = "description";
    public static final String SOURCES_IMAGE_COL = "image";
    public static final String SOURCES_TYPE_COL = "type";
    public static final String SOURCES_LANGUAGE_COL = "language";
    public static final String SOURCES_PREFERRED_IMAGE_COL = "preferred_image";
    public static final String SOURCES_SCORE_COL = "score";
    public static final String SOURCES_ARTICLES_COL = "articles";
    public static final String SOURCES_LIKES_COL = "likes";
    public static final String SOURCES_REFRESHED_COL = "refreshed";
    public static final String SOURCES_TTL_COL = "ttl";
    public static final String[] SOURCES_ALL_COLUMNS = {
            SOURCES_ID_COL,
            SOURCES_NAME_COL,
            SOURCES_TITLE_COL,
            SOURCES_LINK_COL,
            SOURCES_DESCRIPTION_COL,
            SOURCES_IMAGE_COL,
            SOURCES_TYPE_COL,
            SOURCES_LANGUAGE_COL,
            SOURCES_PREFERRED_IMAGE_COL,
            SOURCES_SCORE_COL,
            SOURCES_REFRESHED_COL,
            SOURCES_TTL_COL,
            SOURCES_LIKES_COL,
            SOURCES_ARTICLES_COL
    };

    public DbHandler(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    private static Source fromCursorToSource(Cursor cursor) {
        if (cursor == null) return null;

        Source source = new Source();
        source.id = cursor.getString(
                cursor.getColumnIndexOrThrow(SOURCES_ID_COL));
        source.name = cursor.getString(
                cursor.getColumnIndexOrThrow(SOURCES_NAME_COL));
        source.title = cursor.getString(
                cursor.getColumnIndexOrThrow(SOURCES_TITLE_COL));
        source.link = cursor.getString(
                cursor.getColumnIndexOrThrow(SOURCES_LINK_COL));
        source.description = cursor.getString(
                cursor.getColumnIndexOrThrow(SOURCES_DESCRIPTION_COL));
        source.image = cursor.getString(
                cursor.getColumnIndexOrThrow(SOURCES_IMAGE_COL));
        source.type = cursor.getString(
                cursor.getColumnIndexOrThrow(SOURCES_TYPE_COL));
        source.language = cursor.getString(
                cursor.getColumnIndexOrThrow(SOURCES_LANGUAGE_COL));
        source.preferredImage = cursor.getString(
                cursor.getColumnIndexOrThrow(SOURCES_PREFERRED_IMAGE_COL));
        source.score = cursor.getDouble(
                cursor.getColumnIndexOrThrow(SOURCES_SCORE_COL));
        source.refreshed = cursor.getLong(
                cursor.getColumnIndexOrThrow(SOURCES_REFRESHED_COL));
        source.ttl = cursor.getInt(
                cursor.getColumnIndexOrThrow(SOURCES_TTL_COL));
        source.likes = cursor.getInt(
                cursor.getColumnIndexOrThrow(SOURCES_LIKES_COL));
        source.articles = cursor.getInt(
                cursor.getColumnIndexOrThrow(SOURCES_ARTICLES_COL));

        return source;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        if (db == null) return;

        String query1 = "CREATE TABLE " + ARTICLES_TABLE_NAME + " ("
                + ARTICLES_ID_COL + " TEXT PRIMARY KEY, "
                + ARTICLES_SOURCE_COL + " TEXT,"
                + ARTICLES_TITLE_COL + " TEXT,"
                + ARTICLES_CLEANED_TITLE_COL + " TEXT,"
                + ARTICLES_LINK_COL + " TEXT,"
                + ARTICLES_AUTHORS_COL + " TEXT,"
                + ARTICLES_CATEGORIES_COL + " TEXT,"
                + ARTICLES_COMMENTS_COL + " TEXT,"
                + ARTICLES_PUBLISHED_COL + " INTEGER,"
                + ARTICLES_TIMESTAMP_COL + " INTEGER,"
                + ARTICLES_POSITION_COL + " INTEGER,"
                + ARTICLES_IMAGE_COL + " TEXT,"
                + ARTICLES_VIDEO_COL + " TEXT,"
                + ARTICLES_AUDIO_COL + " TEXT,"
                + ARTICLES_LIKED_COL + " INTEGER,"
                + ARTICLES_OPENED_COL + " INTEGER,"
                + ARTICLES_DISPLAYED_COL + " INTEGER,"
                + ARTICLES_HIDDEN_COL + " INTEGER,"
                + ARTICLES_SCORE_COL + " REAL,"
                + ARTICLES_DESCRIPTION_COL + " TEXT,"
                + ARTICLES_CLEANED_DESCRIPTION_COL + " TEXT)";

        String query2 = "CREATE TABLE " + SOURCES_TABLE_NAME + " ("
                + SOURCES_ID_COL + " TEXT PRIMARY KEY, "
                + SOURCES_NAME_COL + " TEXT,"
                + SOURCES_TITLE_COL + " TEXT,"
                + SOURCES_LINK_COL + " TEXT,"
                + SOURCES_DESCRIPTION_COL + " TEXT,"
                + SOURCES_IMAGE_COL + " TEXT,"
                + SOURCES_TYPE_COL + " TEXT,"
                + SOURCES_LANGUAGE_COL + " TEXT,"
                + SOURCES_PREFERRED_IMAGE_COL + " TEXT,"
                + SOURCES_REFRESHED_COL + " INTEGER,"
                + SOURCES_TTL_COL + " INTEGER,"
                + SOURCES_LIKES_COL + " INTEGER,"
                + SOURCES_ARTICLES_COL + " INTEGER,"
                + SOURCES_SCORE_COL + " REAL)";

        db.execSQL(query1);
        db.execSQL(query2);
    }

    public boolean putArticle(SyndEntry article, Source source, long timestamp, int position) {
        if (article == null) return false;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = RssArticleHandler.getArticleContentValues(article, source, timestamp, position);
        if (values == null) return false;

        long res = db.insertWithOnConflict(
                ARTICLES_TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE);

        if (res != -1) {
            incSourceArticles(source.id);

            String title = (String) values.get(DbHandler.ARTICLES_TITLE_COL);
            final String description = (String) values.get(DbHandler.ARTICLES_DESCRIPTION_COL);
            if (Objects.equals(source.type, DbHandler.SOURCE_TYPE_ACTIVITY_PUB) &&
                    title != null && title.startsWith(ActivityPubHandler.getRepostedItem()) &&
                    (description == null || description.isEmpty())) {
                new Thread(() -> {
                    String id = (String) values.get(DbHandler.ARTICLES_ID_COL);
                    String link = (String) values.get(DbHandler.ARTICLES_LINK_COL);

                    Map<String, String> metaTags = MetaFetcher.getMetaTagsFromHead(link);
                    String metaDescription = metaTags.getOrDefault("og:description",
                            metaTags.getOrDefault("description", null));
                    if (metaDescription != null) {
                        metaDescription = metaDescription.replaceAll("\n", "<br>");
                        metaDescription = Utils.linkifyUrlsToHtml(metaDescription);
                        updateArticleDescription(id, metaDescription);
                    }

                    String metaImage = metaTags.getOrDefault("og:image",null);
                    if (metaImage != null) {
                        updateArticleImage(id, "[\"" + metaImage + "\"]");
                    }
                }).start();
            }
        }

        return (res != -1);
    }

    public void updateSource(String id, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();

        String selection = SOURCES_ID_COL + " = ?";
        String[] selectionArgs = {id};

        db.update(SOURCES_TABLE_NAME, values, selection, selectionArgs);
    }

    public void setSourceImage(String id, String imageUrl) {
        ContentValues values = new ContentValues();
        values.put(SOURCES_IMAGE_COL, imageUrl);
        updateSource(id, values);
    }

    public void setSourceTitle(String id, String title) {
        ContentValues values = new ContentValues();
        values.put(SOURCES_TITLE_COL, title);
        updateSource(id, values);
    }

    public void setSourceLink(String id, String link) {
        ContentValues values = new ContentValues();
        values.put(SOURCES_LINK_COL, link);
        updateSource(id, values);
    }

    public void setSourceDescription(String id, String description) {
        ContentValues values = new ContentValues();
        values.put(SOURCES_DESCRIPTION_COL, description);
        updateSource(id, values);
    }

    public void setSourceLanguage(String id, String lang) {
        ContentValues values = new ContentValues();
        values.put(SOURCES_LANGUAGE_COL, lang);
        updateSource(id, values);
    }

    public void setSourcePreferredImage(String id, String img) {
        ContentValues values = new ContentValues();
        values.put(SOURCES_PREFERRED_IMAGE_COL, img);
        updateSource(id, values);
    }

    public void updateSourceRefreshed(String id) {
        ContentValues values = new ContentValues();
        values.put(DbHandler.SOURCES_REFRESHED_COL, System.currentTimeMillis());
        updateSource(id, values);
    }

    public void setSourceTtl(String id, int ttl) {
        ContentValues values = new ContentValues();
        values.put(DbHandler.SOURCES_TTL_COL, ttl);
        updateSource(id, values);
    }

    public void setSourceScore(String id, double score) {
        ContentValues values = new ContentValues();
        values.put(DbHandler.SOURCES_SCORE_COL, score);
        updateSource(id, values);
    }

    public void incSourceArticles(String sourceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + SOURCES_TABLE_NAME + " SET " +
                SOURCES_ARTICLES_COL + " = " + SOURCES_ARTICLES_COL +
                " + 1 WHERE " + SOURCES_ID_COL + " = '" + sourceId + "'");
    }

    public void incSourceLikes(String sourceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + SOURCES_TABLE_NAME + " SET " +
                SOURCES_LIKES_COL + " = " + SOURCES_LIKES_COL +
                " + 1 WHERE " + SOURCES_ID_COL + " = '" + sourceId + "'");
    }

    public void decSourceLikes(String sourceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("UPDATE " + SOURCES_TABLE_NAME + " SET " +
                SOURCES_LIKES_COL + " = " + SOURCES_LIKES_COL +
                " - 1 WHERE " + SOURCES_ID_COL + " = '" + sourceId + "'");
    }

    public void deleteSource(String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        String[] whereArgs = {id};
        db.delete(SOURCES_TABLE_NAME, SOURCES_ID_COL + " = ?", whereArgs);
        db.delete(ARTICLES_TABLE_NAME, ARTICLES_SOURCE_COL + " = ?", whereArgs);
    }

    public void putSource(Source source) {
        if (source == null) return;

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(SOURCES_ID_COL, source.id);
        values.put(SOURCES_NAME_COL, source.name);
        values.put(SOURCES_TITLE_COL, source.title);
        values.put(SOURCES_LINK_COL, source.link);
        values.put(SOURCES_DESCRIPTION_COL, source.description);
        values.put(SOURCES_IMAGE_COL, source.image);
        values.put(SOURCES_TYPE_COL, source.type);
        values.put(SOURCES_LANGUAGE_COL, source.language);
        values.put(SOURCES_PREFERRED_IMAGE_COL, source.preferredImage);
        values.put(SOURCES_SCORE_COL, source.score);
        values.put(SOURCES_REFRESHED_COL, source.refreshed);
        values.put(SOURCES_TTL_COL, source.ttl);
        values.put(SOURCES_LIKES_COL, source.likes);
        values.put(SOURCES_ARTICLES_COL, source.articles);


        db.insertWithOnConflict(
                SOURCES_TABLE_NAME,
                null,
                values,
                SQLiteDatabase.CONFLICT_IGNORE);
    }

    public List<Source> getSources() {
        SQLiteDatabase db = this.getReadableDatabase();
        String sortOrder = SOURCES_SCORE_COL + " DESC, "
                + SOURCES_LIKES_COL + " / ("
                + SOURCES_ARTICLES_COL + " + 1)";
        Cursor cursor = db.query(
                SOURCES_TABLE_NAME,
                SOURCES_ALL_COLUMNS,
                null,
                null,
                null,
                null,
                sortOrder
        );

        List<Source> sources = new ArrayList<>();
        while (cursor.moveToNext()) {
            sources.add(fromCursorToSource(cursor));
        }
        cursor.close();

        return sources;
    }

    public Source getSource(String sourceId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = SOURCES_ID_COL + " = ?";
        String[] selectionArgs = {sourceId};
        Cursor cursor = db.query(
                SOURCES_TABLE_NAME,
                SOURCES_ALL_COLUMNS,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        cursor.moveToFirst();
        return fromCursorToSource(cursor);
    }

    public List<Article> getArticles(int offset, int howMany, boolean hiddenToo) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sortOrder = ARTICLES_SCORE_COL + " DESC, "
                + ARTICLES_PUBLISHED_COL + " DESC";
        String selection = null;
        if (!hiddenToo) {
            selection = ARTICLES_HIDDEN_COL + " = 0";
        }
        Cursor cursor = db.query(
                ARTICLES_TABLE_NAME,
                ARTICLES_ALL_COLUMNS,
                selection,
                null,
                null,
                null,
                sortOrder,
                "" + (offset + howMany)
        );

        int count = cursor.getCount();
        if (offset >= count) return new ArrayList<>();
        if (offset + howMany > count) howMany = count - offset;

        cursor.moveToPosition(offset - 1);
        List<Article> articles = cursorToArticles(cursor, offset + howMany);
        cursor.close();

        return articles;
    }

    public List<Article> getAllArticles(int queryLimit) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor;
        if (queryLimit == 0) {
            cursor = db.query(
                    ARTICLES_TABLE_NAME,
                    ARTICLES_ALL_COLUMNS,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        } else {
            cursor = db.query(
                    ARTICLES_TABLE_NAME,
                    ARTICLES_ALL_COLUMNS,
                    null,
                    null,
                    null,
                    null,
                    ARTICLES_TIMESTAMP_COL + " DESC",
                    "" + queryLimit
            );
        }

        cursor.moveToPosition(-1);
        List<Article> articles = cursorToArticles(cursor, 0);
        cursor.close();

        return articles;
    }


    public List<Article> getAllArticlesLiked(int offset, int howMany, boolean hiddenToo, String searchText) {
        SQLiteDatabase db = this.getReadableDatabase();
        String sortOrder = ARTICLES_PUBLISHED_COL + " DESC";

        String selection = ARTICLES_LIKED_COL + " = 1";
        if (!hiddenToo) {
            selection += " AND " + ARTICLES_HIDDEN_COL + " = 0";
        }
        if (searchText != null && !searchText.isEmpty()) {
            searchText = searchText.toLowerCase();
            selection += " AND (LOWER(" + ARTICLES_TITLE_COL + ") LIKE ? OR LOWER("
                    + ARTICLES_DESCRIPTION_COL + ") LIKE ?)";
        }
        Cursor cursor = db.query(
                ARTICLES_TABLE_NAME,
                ARTICLES_ALL_COLUMNS,
                selection,
                (searchText != null && !searchText.isEmpty()) ? new String[]{"%" + searchText + "%", "%" + searchText + "%"} : null,
                null,
                null,
                sortOrder,
                "" + (offset + howMany)
        );

        int count = cursor.getCount();
        if (offset >= count) return new ArrayList<>();
        if (offset + howMany > count) howMany = count - offset;

        cursor.moveToPosition(offset - 1);
        List<Article> articles = cursorToArticles(cursor, offset + howMany);
        cursor.close();

        return articles;
    }

    public List<Article> getArticlesFromSource(String sourceId, int offset, int howMany, boolean hiddenToo) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = ARTICLES_SOURCE_COL + " = ?";
        String[] selectionArgs = {sourceId};
        if (!hiddenToo) {
            selection += " AND " + ARTICLES_HIDDEN_COL + " = 0";
        } else {
            selection += " AND " + ARTICLES_HIDDEN_COL + " = 1";
        }

        String sortOrder = ARTICLES_SCORE_COL + " DESC, "
                + ARTICLES_PUBLISHED_COL + " DESC, "
                + ARTICLES_TIMESTAMP_COL + " DESC, "
                + ARTICLES_POSITION_COL + " DESC";
        Cursor cursor = db.query(
                ARTICLES_TABLE_NAME,
                ARTICLES_ALL_COLUMNS,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder,
                "" + (offset + howMany)
        );

        int count = cursor.getCount();
        if (offset >= count) return new ArrayList<>();
        if (offset + howMany > count) howMany = count - offset;

        cursor.moveToPosition(offset - 1);
        List<Article> articles = cursorToArticles(cursor, offset + howMany);
        cursor.close();

        return articles;
    }

    public List<Article> getLikedArticles(int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = ARTICLES_LIKED_COL + " = 1";
        String sortOrder = ARTICLES_PUBLISHED_COL + " DESC";
        Cursor cursor = db.query(
                ARTICLES_TABLE_NAME,
                ARTICLES_ALL_COLUMNS,
                selection,
                null,
                null,
                null,
                sortOrder,
                "" + limit
        );

        List<Article> articles = cursorToArticles(cursor, limit);
        cursor.close();

        return articles;
    }

    public List<Article> getOpenedArticles(int limit) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selection = ARTICLES_OPENED_COL + " = 1";
        String sortOrder = ARTICLES_PUBLISHED_COL + " DESC";
        Cursor cursor = db.query(
                ARTICLES_TABLE_NAME,
                ARTICLES_ALL_COLUMNS,
                selection,
                null,
                null,
                null,
                sortOrder,
                "" + limit
        );

        List<Article> articles = cursorToArticles(cursor, limit);
        cursor.close();

        return articles;
    }

    private List<Article> cursorToArticles(Cursor cursor, int limit) {
        List<Article> articles = new ArrayList<>();
        if (cursor == null) return articles;

        while (cursor.moveToNext() && (limit == 0 || cursor.getPosition() < limit)) {
            Article article = new Article();

            article.id = cursor.getString(
                    cursor.getColumnIndexOrThrow(ARTICLES_ID_COL));
            article.source = cursor.getString(
                    cursor.getColumnIndexOrThrow(ARTICLES_SOURCE_COL));
            article.title = cursor.getString(
                    cursor.getColumnIndexOrThrow(ARTICLES_TITLE_COL));
            article.ctitle = cursor.getString(
                    cursor.getColumnIndexOrThrow(ARTICLES_CLEANED_TITLE_COL));
            article.link = cursor.getString(
                    cursor.getColumnIndexOrThrow(ARTICLES_LINK_COL));
            article.authors = cursor.getString(
                    cursor.getColumnIndexOrThrow(ARTICLES_AUTHORS_COL));
            article.categories = cursor.getString(
                    cursor.getColumnIndexOrThrow(ARTICLES_CATEGORIES_COL));
            article.comments = cursor.getString(
                    cursor.getColumnIndexOrThrow(ARTICLES_COMMENTS_COL));
            article.published = cursor.getLong(
                    cursor.getColumnIndexOrThrow(ARTICLES_PUBLISHED_COL));
            article.timestamp = cursor.getLong(
                    cursor.getColumnIndexOrThrow(ARTICLES_TIMESTAMP_COL));
            article.position = cursor.getInt(
                    cursor.getColumnIndexOrThrow(ARTICLES_POSITION_COL));
            article.image = cursor.getString(
                    cursor.getColumnIndexOrThrow(ARTICLES_IMAGE_COL));
            article.audio = cursor.getString(
                    cursor.getColumnIndexOrThrow(ARTICLES_AUDIO_COL));
            article.video = cursor.getString(
                    cursor.getColumnIndexOrThrow(ARTICLES_VIDEO_COL));
            article.description = cursor.getString(
                    cursor.getColumnIndexOrThrow(ARTICLES_DESCRIPTION_COL));
            article.cdescription = cursor.getString(
                    cursor.getColumnIndexOrThrow(ARTICLES_CLEANED_DESCRIPTION_COL));
            article.liked = cursor.getInt(
                    cursor.getColumnIndexOrThrow(ARTICLES_LIKED_COL));
            article.opened = cursor.getInt(
                    cursor.getColumnIndexOrThrow(ARTICLES_OPENED_COL));
            article.displayed = cursor.getInt(
                    cursor.getColumnIndexOrThrow(ARTICLES_DISPLAYED_COL));
            article.hidden = cursor.getInt(
                    cursor.getColumnIndexOrThrow(ARTICLES_HIDDEN_COL));
            article.score = cursor.getDouble(
                    cursor.getColumnIndexOrThrow(ARTICLES_SCORE_COL));

            articles.add(article);
        }

        return articles;
    }

    public void deleteOldArticles(Context context) {
        SharedPreferences settings = Settings.getSettings(context);
        int days = settings.getInt("delete_after_x_days", DELETE_AFTER_X_DAYS);

        SQLiteDatabase db = this.getWritableDatabase();

        long now = System.currentTimeMillis();
        long when = now - days * 86400000L;
        String where = DbHandler.ARTICLES_TIMESTAMP_COL + " < " + when;

        db.delete(ARTICLES_TABLE_NAME, where, null);
    }

    public void updateArticle(String id, ContentValues values) {
        SQLiteDatabase db = this.getWritableDatabase();

        String selection = DbHandler.ARTICLES_ID_COL + " = ?";
        String[] selectionArgs = {id};

        db.update(ARTICLES_TABLE_NAME, values, selection, selectionArgs);
    }

    public void likeArticle(String id, String sourceId) {
        ContentValues values = new ContentValues();
        values.put(DbHandler.ARTICLES_LIKED_COL, 1);
        updateArticle(id, values);
        incSourceLikes(sourceId);
    }

    public void dislikeArticle(String id, String sourceId) {
        ContentValues values = new ContentValues();
        values.put(DbHandler.ARTICLES_LIKED_COL, 0);
        updateArticle(id, values);
        decSourceLikes(sourceId);
    }

    public void displayArticle(String id) {
        ContentValues values = new ContentValues();
        values.put(DbHandler.ARTICLES_DISPLAYED_COL, 1);
        updateArticle(id, values);
    }

    public void hideArticle(String id) {
        ContentValues values = new ContentValues();
        values.put(DbHandler.ARTICLES_HIDDEN_COL, 1);
        updateArticle(id, values);
    }

    public void showArticle(String id) {
        ContentValues values = new ContentValues();
        values.put(DbHandler.ARTICLES_HIDDEN_COL, 0);
        updateArticle(id, values);
    }


    public void openArticle(String id) {
        ContentValues values = new ContentValues();
        values.put(DbHandler.ARTICLES_OPENED_COL, 1);
        updateArticle(id, values);
    }

    public void updateArticleDescription(String id, String description) {
        ContentValues values = new ContentValues();
        values.put(DbHandler.ARTICLES_DESCRIPTION_COL, description);
        updateArticle(id, values);
    }

    public void updateArticleImage(String id, String image) {
        ContentValues values = new ContentValues();
        values.put(DbHandler.ARTICLES_IMAGE_COL, image);
        updateArticle(id, values);
    }

    public void setArticlesScore(List<DbHandler.Article> articles) {
        if (articles == null) return;
        SQLiteDatabase db = this.getWritableDatabase();

        for (DbHandler.Article article : articles) {
            ContentValues values = new ContentValues();
            values.put(DbHandler.ARTICLES_SCORE_COL, article.score);
            String selection = DbHandler.ARTICLES_ID_COL + " = ?";
            String[] selectionArgs = {article.id};

            db.update(ARTICLES_TABLE_NAME, values, selection, selectionArgs);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (db == null) return;

        db.execSQL("DROP TABLE IF EXISTS " + ARTICLES_TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + SOURCES_TABLE_NAME);
        onCreate(db);
    }

    public static class Article {
        public String id;
        public String source;
        public String title;
        public String ctitle;
        public String link;
        public String authors;
        public String categories;
        public String comments;
        public long published = 0;
        public long timestamp = 0;
        public long position = 0;
        public String image;
        public String audio;
        public String video;
        public String description;
        public String cdescription;
        public int liked = 0;
        public int opened = 0;
        public int displayed = 0;
        public int hidden = 0;
        public double score = 0;
    }

    public static class Source {
        public String id;
        public String name;
        public String title;
        public String link;
        public String description;
        public String language = "en";
        public String image;
        public String preferredImage;
        public String type;
        public double score = 2.5;
        public int articles = 0;
        public int likes = 0;
        public long refreshed = 0;
        public int ttl = 0;
    }
}