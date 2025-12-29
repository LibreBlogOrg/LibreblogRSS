package org.libreblog.rss.core;

import android.content.ContentValues;

import com.rometools.modules.mediarss.MediaEntryModule;
import com.rometools.modules.mediarss.types.MediaContent;
import com.rometools.modules.mediarss.types.MediaGroup;
import com.rometools.modules.mediarss.types.Thumbnail;
import com.rometools.rome.feed.module.Module;
import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndEnclosure;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndPerson;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.libreblog.rss.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RssHandler {
    public static final int MAX_ARTICLE_TITLE_LENGTH = 500;
    public static final int MAX_ARTICLE_DESCRIPTION_LENGTH = 10_000;
    public static final int MAX_CLEANED_DESCRIPTION_LENGTH = 500;

    public static ContentValues getArticleContentValues(SyndEntry entry, DbHandler.Source source, long timestamp, int position) {
        if (entry == null || entry.getLink() == null) return null;

        ContentValues values = new ContentValues();

        List<SyndPerson> authorsList = entry.getAuthors();
        List<String> authorsNamesList = authorsList.stream().map(SyndPerson::getName).collect(Collectors.toList());
        JSONArray authorsNamesArr = new JSONArray(authorsNamesList);

        List<SyndCategory> categoriesList = entry.getCategories();
        List<String> categoriesNamesList = categoriesList.stream().map(SyndCategory::getName).collect(Collectors.toList());
        JSONArray categoriesNamesArr = new JSONArray(categoriesNamesList);

        long published = 0;
        Date publishedDate = entry.getPublishedDate();
        if (publishedDate != null) published = publishedDate.getTime();

        String description = "";
        String cleanedDescription = "";
        String descriptionType = "text/plain";
        if (entry.getDescription() != null) {
            description = entry.getDescription().getValue();
            if (description.length() > MAX_ARTICLE_DESCRIPTION_LENGTH * 2) {
                description = description.substring(0, MAX_ARTICLE_DESCRIPTION_LENGTH * 2);
            }
            description = StringEscapeUtils.unescapeHtml4(description);
            description = description.trim();
            descriptionType = entry.getDescription().getType();

            cleanedDescription = description;
            if (Objects.equals(descriptionType, "text/html")) {
                if (cleanedDescription.length() > MAX_CLEANED_DESCRIPTION_LENGTH * 2) {
                    cleanedDescription = cleanedDescription.substring(0, MAX_CLEANED_DESCRIPTION_LENGTH * 2);
                }
                cleanedDescription = cleanedDescription.replaceAll("<[^>]*>", "");
            }

            if (cleanedDescription.length() > MAX_CLEANED_DESCRIPTION_LENGTH) {
                cleanedDescription = cleanedDescription.substring(0, MAX_CLEANED_DESCRIPTION_LENGTH);
            }

            cleanedDescription = cleanedDescription.trim();
            int lastWord = cleanedDescription.lastIndexOf(" ");
            cleanedDescription = cleanedDescription.substring(0, Math.max(lastWord, 0));
            cleanedDescription = TermFrequency.cleanSentence(cleanedDescription, source.language);

            if (description.length() > MAX_ARTICLE_DESCRIPTION_LENGTH) {
                description = description.substring(0, MAX_ARTICLE_DESCRIPTION_LENGTH).trim();
                description += "...";
            }
        }

        String title = entry.getTitle();
        String cleanedTitle = "";
        if (title != null && !title.isEmpty()) {
            if (title.length() > MAX_ARTICLE_TITLE_LENGTH * 2) {
                title = title.substring(0, MAX_ARTICLE_TITLE_LENGTH * 2);
            }
            title = StringEscapeUtils.unescapeHtml4(title);
            title = title.trim();

            if (title.length() > MAX_ARTICLE_TITLE_LENGTH) {
                title = title.substring(0, MAX_ARTICLE_TITLE_LENGTH).trim();
                cleanedTitle = title;
                int lastWord = cleanedTitle.lastIndexOf(" ");
                cleanedTitle = cleanedTitle.substring(0, Math.max(lastWord, 0));

                title += "...";
            } else {
                cleanedTitle = title;
            }
            cleanedTitle = TermFrequency.cleanSentence(cleanedTitle, source.language);
        }

        String id = Utils.generateMD5(source.id + "-" + entry.getLink());
        List<String> audio = extractFromEnclosure(entry, "audio");
        List<String> video = extractFromEnclosure(entry, "video");
        List<String> image = extractFromEnclosure(entry, "image");
        if (audio == null) audio = extractMedia(entry, "audio");
        if (video == null) video = extractMedia(entry, "video");
        if (image == null) image = extractMedia(entry, "image");
        if (image == null && video == null && audio == null && Objects.equals(descriptionType, "text/html")) {
            image = Utils.extractFirstImgSrc(entry.getDescription().getValue());
        }
        JSONArray audioArr = null;
        if (audio != null) audioArr = new JSONArray(audio);
        JSONArray imageArr = null;
        if (image != null) imageArr = new JSONArray(image);
        JSONArray videoArr = null;
        if (video != null) videoArr = new JSONArray(video);

        DbHandler.Article article = new DbHandler.Article();
        article.id = id;
        values.put(DbHandler.ARTICLES_ID_COL, article.id);
        article.source = source.id;
        values.put(DbHandler.ARTICLES_SOURCE_COL, article.source);
        article.title = title;
        values.put(DbHandler.ARTICLES_TITLE_COL, article.title);
        article.ctitle = cleanedTitle;
        values.put(DbHandler.ARTICLES_CLEANED_TITLE_COL, article.ctitle);
        article.link = entry.getLink();
        values.put(DbHandler.ARTICLES_LINK_COL, article.link);
        article.authors = authorsNamesArr.toString();
        values.put(DbHandler.ARTICLES_AUTHORS_COL, article.authors);
        article.categories = categoriesNamesArr.toString();
        values.put(DbHandler.ARTICLES_CATEGORIES_COL, article.categories);
        article.comments = entry.getComments();
        values.put(DbHandler.ARTICLES_COMMENTS_COL, article.comments);
        article.published = published;
        values.put(DbHandler.ARTICLES_PUBLISHED_COL, article.published);
        article.timestamp = timestamp;
        values.put(DbHandler.ARTICLES_TIMESTAMP_COL, article.timestamp);
        article.position = position;
        values.put(DbHandler.ARTICLES_POSITION_COL, position);
        if (imageArr != null) {
            article.image = imageArr.toString();
            values.put(DbHandler.ARTICLES_IMAGE_COL, article.image);
        }
        if (videoArr != null) {
            article.video = videoArr.toString();
            values.put(DbHandler.ARTICLES_VIDEO_COL, article.video);
        }
        if (audioArr != null) {
            article.audio = audioArr.toString();
            values.put(DbHandler.ARTICLES_AUDIO_COL, article.audio);
        }
        article.description = description;
        values.put(DbHandler.ARTICLES_DESCRIPTION_COL, article.description);
        article.cdescription = cleanedDescription;
        values.put(DbHandler.ARTICLES_CLEANED_DESCRIPTION_COL, article.cdescription);
        values.put(DbHandler.ARTICLES_LIKED_COL, 0);
        values.put(DbHandler.ARTICLES_OPENED_COL, 0);
        values.put(DbHandler.ARTICLES_DISPLAYED_COL, 0);
        values.put(DbHandler.ARTICLES_HIDDEN_COL, 0);
        article.score = ArticlesSorter.getScore(article);
        values.put(DbHandler.ARTICLES_SCORE_COL, article.score);
        return values;
    }

    public static List<String> extractFromEnclosure(SyndEntry entry, String type) {
        if (entry == null) return null;

        List<String> mediaUrls = new ArrayList<>();
        List<SyndEnclosure> enclosures = entry.getEnclosures();
        for (SyndEnclosure enclosure : enclosures) {
            if (enclosure.getType() != null &&
                    enclosure.getType().toLowerCase().startsWith(type)) {
                String url = enclosure.getUrl();
                if (Utils.probablyMediaUrl(type, url)) mediaUrls.add(url);
            }
        }

        if (!mediaUrls.isEmpty()) return mediaUrls;
        return null;
    }

    public static boolean checkMediaType(MediaContent mc, String type) {
        if (mc == null) return false;

        return (mc.getType() != null && mc.getType().toLowerCase().startsWith(type))
                || (mc.getMedium() != null && mc.getMedium().equalsIgnoreCase(type));
    }

    public static List<String> extractMedia(SyndEntry entry, String type) {
        if (entry == null || type == null) return null;

        List<String> mediaUrls = new ArrayList<>();
        List<String> thumbUrls = new ArrayList<>();


        Module m = entry.getModule(MediaEntryModule.URI);
        if (m instanceof MediaEntryModule) {
            MediaEntryModule mem = (MediaEntryModule) m;

            MediaContent[] contents = mem.getMediaContents();
            if (contents != null) {
                for (MediaContent mc : contents) {
                    if (mc == null) continue;
                    if (mc.getReference() != null && checkMediaType(mc, type)) {
                        String mediaUrl = mc.getReference().toString();
                        if (Utils.probablyMediaUrl(type, mediaUrl)) mediaUrls.add(mediaUrl);
                    }
                }
            }

            if (!mediaUrls.isEmpty()) return mediaUrls;

            MediaGroup[] groups = mem.getMediaGroups();
            if (groups != null) {
                for (MediaGroup g : groups) {
                    if (g == null) continue;
                    MediaContent[] gContents = g.getContents();
                    if (gContents != null) {
                        for (MediaContent mc : gContents) {
                            if (mc == null) continue;
                            if (mc.getReference() != null && checkMediaType(mc, type)) {
                                String mediaUrl = mc.getReference().toString();
                                if (Utils.probablyMediaUrl(type, mediaUrl)) mediaUrls.add(mediaUrl);
                            }
                        }
                    }

                    if (Objects.equals(type, "image")) {
                        Thumbnail[] thumbs = g.getMetadata() != null ? g.getMetadata().getThumbnail() : null;
                        if (thumbs != null) {
                            for (Thumbnail t : thumbs) {
                                if (t != null && t.getUrl() != null) {
                                    thumbUrls.add(t.getUrl().toString());
                                }
                            }
                        }
                    }
                }
            }

            if (!mediaUrls.isEmpty()) return mediaUrls;
            if (!thumbUrls.isEmpty()) return thumbUrls;

            if (Objects.equals(type, "image")) {
                Thumbnail[] thumbs = mem.getMetadata() != null ? mem.getMetadata().getThumbnail() : null;
                if (thumbs != null) {
                    for (Thumbnail t : thumbs) {
                        if (t != null && t.getUrl() != null) {
                            thumbUrls.add(t.getUrl().toString());
                        }
                    }
                }
            }

            if (!thumbUrls.isEmpty()) return thumbUrls;
        }

        return null;
    }

}
