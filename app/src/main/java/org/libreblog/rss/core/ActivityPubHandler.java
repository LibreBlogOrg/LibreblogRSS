package org.libreblog.rss.core;

import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.libreblog.rss.utils.Utils;
import org.xml.sax.InputSource;

import java.io.Reader;
import java.io.StringReader;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ActivityPubHandler {
    private static String repostedItem = "Shared a post from ";

    public static void setRepostedItem(String rItem) {
        repostedItem = rItem;
    }

    public static String getRepostedItem() {
        return repostedItem;
    }

    public static JSONObject fetch(String url) throws Exception {
        if (url == null) return null;

        final OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder()
                .url(url)
                .header("Accept", "application/activity+json")
                .get()
                .build();

        try (Response resp = client.newCall(req).execute()) {
            int code = resp.code();
            String body = resp.body().string();

            if (code < 200 || code >= 300) {
                throw new RuntimeException("HTTP " + code + ": " + body);
            }

            String ct = resp.header("Content-Type", "");
            if (ct == null || !ct.startsWith("application/activity+json")) {
                throw new RuntimeException("Unexpected Content-Type: " + ct);
            }

            JSONTokener tokener = new JSONTokener(body.trim());
            Object json = tokener.nextValue();
            if (json instanceof JSONObject) {
                return (JSONObject) json;
            } else {
                throw new RuntimeException("Response is not a valid JSON");
            }
        }
    }

    public static JSONObject findOutbox(String url, DbHandler.Source source, int level) throws Exception {
        if (level > 2)
            throw new RuntimeException("Could not find the outbox - recursion depth exceeded");
        if (url == null)
            throw new RuntimeException("There is no URL");

        JSONObject json = fetch(url);
        if (!json.has("type")) throw new RuntimeException("Could not find the outbox - no type");
        if (json.getString("type").startsWith("OrderedCollection")) {
            if (json.has("first") && level < 2) {
                fillSourceAttributes(source, json);
                return findOutbox(json.getString("first"), source, level + 1);
            }

            if (source != null) {
                source.type = DbHandler.SOURCE_TYPE_ACTIVITY_PUB;
                source.link = url;
            }
            return json;
        }
        if (json.has("outbox")) {
            fillSourceAttributes(source, json);
            return findOutbox(json.getString("outbox"), source, level + 1);
        }

        throw new RuntimeException("Could not find the outbox - invalid activity json");
    }

    private static void fillSourceAttributes(DbHandler.Source source, JSONObject json) throws JSONException {
        if (source == null || json == null) return;

        if (json.has("name")) {
            source.title = json.getString("name");
        }

        if (json.has("summary")) {
            source.description = json.getString("summary");
        }

        if (json.has("icon")) {
            JSONObject iconObj = json.getJSONObject("icon");
            if (iconObj.has("url")) source.image = iconObj.getString("url");
        } else if (json.has("image")) {
            JSONObject imageObj = json.getJSONObject("image");
            if (imageObj.has("url")) source.image = imageObj.getString("url");
        }
    }

    private static String getOrderedCollectionItems(JSONObject json) throws JSONException {
        if (json == null || !json.has("orderedItems"))
            throw new RuntimeException("Outbox does not have items");

        StringBuilder result = new StringBuilder();
        JSONArray items = json.getJSONArray("orderedItems");
        for (int i = 0; i < items.length(); i++) {
            final JSONObject item = items.optJSONObject(i);
            if (item == null) continue;

            final String itemPub = Utils.convertIsoToRfc1123(item.getString("published"));
            JSONObject itemObj = null;
            String itemObjStr = "";
            String itemObjId;
            try {
                itemObj = item.getJSONObject("object");
                itemObjId = itemObj.getString("id");
            } catch (JSONException e) {
                try {
                    itemObjStr = item.getString("object");
                    itemObjId = itemObjStr;
                } catch (JSONException e2) {
                    continue;
                }
            }

            result.append("    <item>\n      <link>").append(itemObjId).append("</link>\n").
                    append("      <pubDate>").append(itemPub).append("</pubDate>\n");

            if (item.getString("type").equals("Announce")) {
                result.append(generateTitleAndDescription(itemObjStr, itemObj));
            } else if (itemObj != null) {
                result.append("      <description><![CDATA[").
                        append(itemObj.getString("content")).
                        append("]]></description>\n");

                if (itemObj.has("attachment")) {
                    JSONArray attachments = itemObj.getJSONArray("attachment");
                    if (attachments.length() >= 1) {
                        result.append(generateMediaTag(attachments.get(0)));
                    }
                }
                if (itemObj.has("tag")) {
                    JSONArray tags = itemObj.getJSONArray("tag");
                    if (tags.length() >= 1) {
                        result.append(generateCategoryTags(tags));
                    }
                }
            }

            result.append("    </item>\n");
        }

        return result.toString();
    }

    private static String generateTitleAndDescription(String itemObjStr, JSONObject itemObj) throws JSONException {
        if (itemObj == null && itemObjStr == null) return "";

        StringBuilder result = new StringBuilder();
        if (itemObj == null) {
            result.append("      <title>").append(repostedItem).append(prettifyRepostId(itemObjStr)).append("</title>");
        } else if (itemObj.has("id") && itemObj.has("content")) {
            final String id = itemObj.getString("id");
            final String content = itemObj.getString("content");
            result.append("      <title>").append(repostedItem).append(prettifyRepostId(id)).append("</title>")
                    .append("      <description><![CDATA[").
                    append(prettifyRepostId(id)).append("\n").
                    append(content).append("]]></description>\n");
        }

        return result.toString();
    }

    private static String prettifyRepostId(String id) {
        if (id == null) return "";

        String[] parts = id.split("/");

        if (parts.length < 3) return id;
        return parts[2];
    }

    private static String generateMediaTag(Object attachmentObj) throws JSONException {
        if (!(attachmentObj instanceof JSONObject)) return "";

        JSONObject attachment = (JSONObject) attachmentObj;
        if (!attachment.has("url") || !attachment.has("mediaType")) return "";

        return "      <media:content url=\"" + attachment.getString("url") +
                "\" type=\"" + attachment.getString("mediaType") +
                "\" >\n      </media:content>\n";
    }

    private static String generateCategoryTags(JSONArray tagsArray) throws JSONException {
        if (tagsArray == null) return "";

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < tagsArray.length(); i++) {
            final Object tag = tagsArray.get(i);
            if (tag instanceof JSONObject) {
                final JSONObject tagObj = (JSONObject) tag;
                if (tagObj.has("name")) {
                    result.append("      <category>").
                            append(tagObj.getString("name")).
                            append("</category>\n");
                }
            }
        }

        return result.toString();
    }

    public static SyndFeed convertOutboxJsonToSyndFeed(DbHandler.Source source, JSONObject json) throws JSONException, FeedException {
        if (source == null || json == null) return null;

        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<rss version=\"2.0\" xmlns:media=\"http://search.yahoo.com/mrss/\" >\n" +
                "  <channel>\n" +
                "    <title>" + Utils.escapeForXml(source.title) + "</title>\n" +
                "    <link>" + source.link + "</link>\n" +
                "    <description><![CDATA[" + source.description + "]]></description>\n" +
                getOrderedCollectionItems(json) +
                "  </channel>\n" +
                "</rss>\n";
        Reader r = new StringReader(xml);

        SyndFeedInput syndFeedInput = new SyndFeedInput();
        return syndFeedInput.build(new InputSource(r));
    }
}

