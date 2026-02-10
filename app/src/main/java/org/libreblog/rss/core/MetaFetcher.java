package org.libreblog.rss.core;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class MetaFetcher {
    private static final OkHttpClient client = new OkHttpClient();

    public static Map<String, String> getMetaTagsFromHead(String url) {
        if (url == null) return new LinkedHashMap<>();

        Request req = new Request.Builder().url(url).get().build();
        try (Response resp = client.newCall(req).execute()) {
            if (!resp.isSuccessful()) return new LinkedHashMap<>();

            String content = resp.body().string();
            return getMetasMap(content);
        } catch (IOException e) {
            return new LinkedHashMap<>();
        }
    }

    private static Map<String, String> getMetasMap(String content) {
        if (content == null || content.isEmpty()) return new LinkedHashMap<>();

        Document doc = Jsoup.parse(content);
        Elements metas = doc.select("head meta[name], head meta[property], head meta[content]");
        Map<String, String> metasMap = new LinkedHashMap<>();
        for (Element meta : metas) {
            String key = meta.hasAttr("name") ? meta.attr("name")
                    : meta.hasAttr("property") ? meta.attr("property") : null;
            String value = meta.hasAttr("content") ? meta.attr("content") : null;
            if (key != null && value != null) metasMap.put(key, value);
        }

        return metasMap;
    }
}