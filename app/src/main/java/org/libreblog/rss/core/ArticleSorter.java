package org.libreblog.rss.core;

import android.content.Context;
import android.content.SharedPreferences;

import org.libreblog.rss.utils.Settings;
import org.libreblog.rss.utils.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ArticleSorter {
    public static final int MIN_DESCRIPTION_LENGTH = 50;
    public static final int GOOD_DESCRIPTION_LENGTH = 150;
    public static final int TOO_MUCH_DESCRIPTION_LENGTH = 1250;
    public static final int NBR_STARS = 5;
    public static final float COEFF_RECENT = 0.35f;
    public static final float COEFF_COMPLETE = 0.15f;
    public static final float COEFF_SOURCE_SCORE = 0.15f;
    public static final float COEFF_REL_SOURCE_LIKABILITY = 0.05f;
    public static final float COEFF_REL_LIKED_FREQUENCY = 0.1f;
    public static final float COEFF_REL_OPENED_FREQUENCY = 0.2f;
    public static final float COEFF_SPAMMABILITY = 0.05f;
    public static final float COEFF_DISPLAYED = 0.05f;
    public static final float HAS_TITLE = 0.1f;
    public static final float HAS_DESCRIPTION = 0.1f;
    public static final float HAS_MIN_DESCRIPTION = 0.1f;
    public static final float HAS_GOOD_DESCRIPTION = 0.1f;
    public static final float HAS_IMAGE = 0.25f;
    public static final float HAS_AUDIO_OR_VIDEO = 0.2f;
    public static final float HAS_COMMENTS = 0.05f;
    public static final float HAS_AUTHOR = 0.05f;
    public static final float HAS_CATEGORIES = 0.15f;
    public static final float HAS_HTML = 0.1f;
    private static final Map<String, DbHandler.Source> sourcesMap = new HashMap<>();
    private static final List<Double> sourceLikabilityArray = new ArrayList<>();
    private static final List<Integer> sourceTotalArray = new ArrayList<>();
    private static final List<Double> likedArray = new ArrayList<>();
    private static final List<Double> openedArray = new ArrayList<>();
    private static final int SORT_ARRAY_LIMIT = 10_000;
    private static float coeffRecent;
    private static float coeffComplete;
    private static float coeffSourceScore;
    private static float coeffRelSourceLikability;
    private static float coeffRelLikedFrequency;
    private static float coeffRelOpenedFrequency;
    private static float coeffSpammability;
    private static float coeffDisplayed;
    private static float hasTitle;
    private static float hasDescription;
    private static float hasMinDescription;
    private static float hasGoodDescription;
    private static float hasImage;
    private static float hasAudioOrVideo;
    private static float hasComments;
    private static float hasAuthor;
    private static float hasCategories;
    private static float hasHTML;

    public static void init(Context context) {
        try (DbHandler db = new DbHandler(context)) {
            List<DbHandler.Source> sources = db.getSources();
            for (DbHandler.Source source : sources) {
                sourcesMap.put(source.id, source);
                if (source.articles > 0)
                    sourceLikabilityArray.add((double) (source.likes / source.articles));
                sourceTotalArray.add(source.articles);
            }
            sourceLikabilityArray.sort(Comparator.naturalOrder());

            for (DbHandler.Article article : db.getAllArticles(SORT_ARRAY_LIMIT)) {
                TermFrequency.SetOfFrequencies frequencies = TermFrequency.frequencies(article);
                likedArray.add(frequencies.liked);
                openedArray.add(frequencies.opened);
            }
        }
        likedArray.sort(Comparator.naturalOrder());
        openedArray.sort(Comparator.naturalOrder());
    }

    public static void updateCoefficients(Context context) {
        SharedPreferences settings = Settings.getSettings(context);
        coeffRecent = settings.getFloat("coeff_recent", COEFF_RECENT);
        coeffComplete = settings.getFloat("coeff_complete", COEFF_COMPLETE);
        coeffSourceScore = settings.getFloat("coeff_source_score", COEFF_SOURCE_SCORE);
        coeffRelSourceLikability = settings.getFloat("coeff_rel_source_likability", COEFF_REL_SOURCE_LIKABILITY);
        coeffRelLikedFrequency = settings.getFloat("coeff_rel_liked_frequency", COEFF_REL_LIKED_FREQUENCY);
        coeffRelOpenedFrequency = settings.getFloat("coeff_rel_opened_frequency", COEFF_REL_OPENED_FREQUENCY);
        coeffSpammability = settings.getFloat("coeff_spammability", COEFF_SPAMMABILITY);
        coeffDisplayed = settings.getFloat("coeff_displayed", COEFF_DISPLAYED);
        hasTitle = settings.getFloat("has_title", HAS_TITLE);
        hasDescription = settings.getFloat("has_description", HAS_DESCRIPTION);
        hasMinDescription = settings.getFloat("has_min_description", HAS_MIN_DESCRIPTION);
        hasGoodDescription = settings.getFloat("has_good_description", HAS_GOOD_DESCRIPTION);
        hasImage = settings.getFloat("has_image", HAS_IMAGE);
        hasAudioOrVideo = settings.getFloat("has_audio_or_video", HAS_AUDIO_OR_VIDEO);
        hasComments = settings.getFloat("has_comments", HAS_COMMENTS);
        hasAuthor = settings.getFloat("has_author", HAS_AUTHOR);
        hasCategories = settings.getFloat("has_categories", HAS_CATEGORIES);
        hasHTML = settings.getFloat("has_html", HAS_HTML);
    }

    public static void sort(Context context) {
        updateCoefficients(context);

        try (DbHandler db = new DbHandler(context)) {
            List<DbHandler.Article> articles = db.getAllArticles(0);
            for (DbHandler.Article article : articles) {
                double score = getScore(article);
                article.score = Math.round(score * 10_000f) / 10_000f;
            }

            db.setArticlesScore(articles);
        }
    }

    public static double getScore(DbHandler.Article article) {
        if (article == null) return 0;

        double score = 0;
        score += coeffRecent * isItRecent(article);
        score += coeffComplete * isItComplete(article, hasTitle, hasDescription,
                hasMinDescription, hasGoodDescription, hasImage, hasAudioOrVideo,
                hasComments, hasAuthor, hasCategories, hasHTML);
        score += coeffSourceScore * getSourceScore(article);
        score += coeffRelSourceLikability * getSourceRelativeLikability(article);

        TermFrequency.SetOfFrequencies frequencies = TermFrequency.frequencies(article);
        double rlf = getArticleRelativeLikedFrequency(frequencies);
        double rof = getArticleRelativeOpenedFrequency(frequencies);
        if (article.liked == 0) {
            score += coeffRelLikedFrequency * rlf;
        } else {
            score += coeffRelLikedFrequency * rlf * 0.5;
        }
        if (article.opened == 0) {
            score += coeffRelOpenedFrequency * rof;
        } else {
            score += coeffRelOpenedFrequency * rof * 0.5;
        }

        score -= coeffSpammability * getSourceRelativeSpammability(article);
        if (article.displayed == 1) {
            score -= coeffDisplayed;
        }
        return score;
    }

    private static double isItRecent(DbHandler.Article article) {
        if (article == null) return 0;
        if (article.published == 0) return 0;

        long now = System.currentTimeMillis();
        if (article.published > now) return 0;

        long diff = now - article.published;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (minutes < 60) {
            return 1;
        } else if (hours < 24) {
            return 0.75;
        } else if (days < 7) {
            return 0.5;
        } else if (days < 30) {
            return 0.25;
        }

        return 0;
    }

    private static double getArticleRelativeFrequency(double frequency, List<Double> array) {
        if (frequency == 0 || array == null) return 0;

        int percentile75 = (int) (array.size() * 0.75);
        int percentile50 = (int) (array.size() * 0.5);
        int percentile25 = (int) (array.size() * 0.25);

        if (frequency > array.get(percentile75)) return 1;
        if (frequency > array.get(percentile50)) return 0.75;
        if (frequency > array.get(percentile25)) return 0.5;
        if (frequency > 0) return 0.25;
        return 0;
    }

    private static double getArticleRelativeOpenedFrequency(TermFrequency.SetOfFrequencies frequencies) {
        if (frequencies == null) return 0;
        return getArticleRelativeFrequency(frequencies.opened, openedArray);
    }

    private static double getArticleRelativeLikedFrequency(TermFrequency.SetOfFrequencies frequencies) {
        if (frequencies == null) return 0;
        return getArticleRelativeFrequency(frequencies.liked, likedArray);
    }

    private static double getSourceRelativeLikability(DbHandler.Article article) {
        if (article == null) return 0;

        DbHandler.Source source = getSource(article);
        if (source == null) return 0;

        if (source.articles == 0) return 0.25;
        double likability = (double) source.likes / source.articles;
        int percentile75 = (int) (sourceLikabilityArray.size() * 0.75);
        int percentile50 = (int) (sourceLikabilityArray.size() * 0.5);
        int percentile25 = (int) (sourceLikabilityArray.size() * 0.25);

        if (likability >= sourceLikabilityArray.get(percentile75)) return 1;
        if (likability >= sourceLikabilityArray.get(percentile50)) return 0.75;
        if (likability >= sourceLikabilityArray.get(percentile25)) return 0.5;
        if (likability >= 0) return 0.25;
        return 0;
    }

    private static double getSourceRelativeSpammability(DbHandler.Article article) {
        if (article == null) return 0;

        DbHandler.Source source = getSource(article);
        if (source == null) return 0;

        int percentile90 = (int) (sourceTotalArray.size() * 0.9);
        int percentile75 = (int) (sourceTotalArray.size() * 0.75);
        int percentile50 = (int) (sourceTotalArray.size() * 0.5);
        int percentile25 = (int) (sourceTotalArray.size() * 0.25);

        if (source.articles >= sourceTotalArray.get(percentile90)) return 1;
        if (source.articles >= sourceTotalArray.get(percentile75)) return 0.75;
        if (source.articles >= sourceTotalArray.get(percentile50)) return 0.50;
        if (source.articles >= sourceTotalArray.get(percentile25)) return 0.25;
        return 0;
    }

    private static double isItComplete(DbHandler.Article article, float hasTitle, float hasDescription,
                                       float hasMinDescription, float hasGoodDescription, float hasImage,
                                       float hasAudioOrVideo, float hasComments, float hasAuthor,
                                       float hasCategories, float hasHTML) {
        double score = 0;

        if (article == null) return 0;
        if ((article.title == null || article.title.isEmpty()) &&
                (article.description == null || article.description.isEmpty())) return 0;
        if (article.published == 0) return 0;
        if (article.link == null || article.link.isEmpty()) return 0;

        if (article.title != null && !article.title.isEmpty()) score += hasTitle;
        if (article.description != null && !article.description.isEmpty()) score += hasDescription;

        if (article.description != null && article.description.length() < TOO_MUCH_DESCRIPTION_LENGTH) {
            if (article.description.length() > MIN_DESCRIPTION_LENGTH) score += hasMinDescription;
            if (article.description.length() > GOOD_DESCRIPTION_LENGTH) score += hasGoodDescription;
        }

        if (Utils.containsHtml(article.description)) score += hasHTML;

        if (article.image != null && !article.image.isEmpty()) {
            score += hasImage;
        } else if ((article.audio != null && !article.audio.isEmpty()) ||
                (article.video != null && !article.video.isEmpty())) {
            score += hasAudioOrVideo;
        }

        if (article.comments != null && !article.comments.isEmpty()) score += hasComments;
        if (article.authors != null && article.authors.length() > 2) score += hasAuthor;
        if (article.categories != null && article.categories.length() > 2) score += hasCategories;

        return score;
    }

    private static double getSourceScore(DbHandler.Article article) {
        if (article == null) return 0;

        DbHandler.Source source = getSource(article);
        if (source != null) return source.score / NBR_STARS;
        return 0;
    }

    private static DbHandler.Source getSource(DbHandler.Article article) {
        if (article == null) return null;

        String sourceId = article.source;
        return sourcesMap.get(sourceId);
    }
}
