package org.libreblog.rss.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import android.util.Patterns;
import android.webkit.URLUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final Pattern TAG_PATTERN = Pattern.compile(
            "(?i)<\\s*(?:b|strong|i|em|u|br|p|div|ul|ol|li|a|blockquote|sub|sup|font)\\b[^>]*>",
            Pattern.DOTALL
    );

    public static String getInitials(String fullName) {
        if (fullName == null || fullName.isEmpty()) return "X";

        StringBuilder initials = new StringBuilder();
        String[] words = fullName.split("\\s+");
        for (String word : words) {
            if (!word.isEmpty()) {
                initials.append(word.charAt(0));
            }
        }
        String initialsStr = initials.toString().toUpperCase();

        if (initialsStr.length() <= 2) return initialsStr;
        return initialsStr.substring(0, 2);
    }

    public static String generateMD5(String input) {
        if (input == null) return null;

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array to hexadecimal format
            StringBuilder hexString = new StringBuilder();
            for (byte b : messageDigest) {
                String hex = Integer.toHexString(0xFF & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (Exception e) {
            Log.w("Utils", "Cannot generate MD5 hashcode", e);
            return null;
        }
    }

    public static String inputStreamToString(InputStream inputStream) {
        if (inputStream == null) return null;

        StringBuilder result = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            Log.w("Utils", "Cannot convert InputStream to String", e);
        }
        return result.toString();
    }

    public static boolean containsString(JSONArray jsonArray, String str) {
        if (jsonArray == null) return false;

        for (int i = 0; i < jsonArray.length(); i++) {
            String stopword;
            try {
                stopword = jsonArray.getString(i);
            } catch (JSONException e) {
                Log.w("Utils", "Cannot check if JSONArray contains String", e);
                return false;
            }
            if (stopword.equals(str)) {
                return true;
            }
        }
        return false;
    }

    public static String extractImgTag(String input) {
        if (input == null) return null;

        Pattern tagPattern = Pattern.compile("<img\\b[^>]*>", Pattern.CASE_INSENSITIVE);
        Matcher tagMatcher = tagPattern.matcher(input);
        if (tagMatcher.find()) {
            return tagMatcher.group();
        }
        return null;
    }

    public static String extractSrcFromImgTag(String imgTag) {
        if (imgTag == null) return null;

        Pattern srcPattern = Pattern.compile("src\\s*=\\s*(['\"])(.*?)\\1", Pattern.CASE_INSENSITIVE);
        Matcher srcMatcher = srcPattern.matcher(imgTag);
        if (srcMatcher.find()) {
            return srcMatcher.group(2);
        }
        Pattern srcUnquoted = Pattern.compile("src\\s*=\\s*([^\\s\"'>]+)", Pattern.CASE_INSENSITIVE);
        Matcher uq = srcUnquoted.matcher(imgTag);
        if (uq.find()) return uq.group(1);
        return null;
    }

    public static List<String> extractFirstImgSrc(String input) {
        if (input == null) return null;

        List<String> oneUrl = new ArrayList<>();
        String tag = extractImgTag(input);
        String imgSrc = extractSrcFromImgTag(tag);
        if (imgSrc != null) {
            oneUrl.add(imgSrc);
            return oneUrl;
        }

        return null;
    }

    public static String toTimeAgo(long ts) {
        long now = System.currentTimeMillis();
        if (ts > now) ts = now;

        long diff = now - ts;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (seconds < 60) {
            return seconds <= 1 ? "just now" : seconds + "s";
        } else if (minutes < 60) {
            return minutes < 2 ? "1m" : minutes + "m";
        } else if (hours < 24) {
            return hours < 2 ? "1h" : hours + "h";
        } else if (days < 7) {
            return days < 2 ? "1d" : days + "d";
        } else if (days < 30) {
            long weeks = days / 7;
            return weeks < 2 ? "1w" : weeks + "w";
        } else if (days < 365) {
            long months = days / 30;
            return months < 2 ? "1mo" : months + "mo";
        } else {
            long years = days / 365;
            return years < 2 ? "1y" : years + "y";
        }
    }

    public static boolean probablyImageUrl(String candidateUrl) {
        if (candidateUrl == null || candidateUrl.isEmpty()) return false;
        if (!URLUtil.isValidUrl(candidateUrl)) return false;

        try {
            URL url = new URL(candidateUrl);
            String path = url.getPath().toLowerCase();
            return path.matches(".*\\.(png|jpe?g|gif|webp|bmp|svg|avif)$");
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static boolean probablyVideoUrl(String candidateUrl) {
        if (candidateUrl == null || candidateUrl.isEmpty()) return false;
        if (!URLUtil.isValidUrl(candidateUrl)) return false;

        try {
            URL url = new URL(candidateUrl);
            String path = url.getPath().toLowerCase();
            return path.matches(".*\\.(mp4|m4v|mkv|webm|mov|avi|mpg|mpeg|3gp|3g2|ts|flv|m2ts|ogg|ogv|mts|vob)$");
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static boolean probablyAudioUrl(String candidateUrl) {
        if (candidateUrl == null || candidateUrl.isEmpty()) return false;
        if (!URLUtil.isValidUrl(candidateUrl)) return false;

        try {
            URL url = new URL(candidateUrl);
            String path = url.getPath().toLowerCase();
            return path.matches(".*\\.(mp3|m4a|m4b|aac|flac|wav|ogg|oga|opus|wma|aiff?|pcm|alac|amr)$");
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static boolean probablyMediaUrl(String type, String candidateUrl) {
        if (Objects.equals(type, "audio")) return probablyAudioUrl(candidateUrl);
        if (Objects.equals(type, "video")) return probablyVideoUrl(candidateUrl);
        if (Objects.equals(type, "image")) return probablyImageUrl(candidateUrl);
        return false;
    }

    public static List<String> jsonArrayToList(JSONArray array) {
        if (array == null) return null;

        List<String> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            try {
                String s = array.isNull(i) ? null : array.getString(i);
                list.add(s);
            } catch (Exception ignore) {
            }
        }
        return list;
    }

    public static boolean isRemoteImageUrl(String s, int timeoutMs) {
        if (s == null) return false;

        try {
            URL url = new URL(s);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.connect();
            String type = conn.getContentType();
            int code = conn.getResponseCode();
            conn.disconnect();
            return code >= 200 && code < 400 && type != null && type.toLowerCase().startsWith("image/");
        } catch (Exception e) {
            return false;
        }
    }

    public static String cleanHtml(String html) {
        if (html == null) return null;

        Safelist safelist = Safelist.none()
                .addTags("b", "strong", "i", "em", "u", "br", "p", "div", "ul", "ol", "li", "a", "blockquote", "sub", "sup", "font");
        safelist.addAttributes("a", "href");
        safelist.addProtocols("a", "href", "http", "https", "mailto");
        return Jsoup.clean(html, safelist);
    }

    public static Proxy torProxy() {
        return new Proxy(Proxy.Type.SOCKS, new InetSocketAddress("127.0.0.1", 9050));
    }

    public static boolean isOnionAddress(String pageUrl) {
        if (pageUrl == null) return false;
        return pageUrl.split("/").length > 2 && pageUrl.split("/")[2].endsWith(".onion");
    }

    public static boolean isSvg(String imgUrl) {
        if (imgUrl == null) return false;
        return imgUrl.toLowerCase().contains(".svg");
    }

    public static boolean containsHtml(String input) {
        if (input == null || input.isEmpty()) return false;
        return TAG_PATTERN.matcher(input).find();
    }

    public static boolean isDarkTheme(Context context) {
        if (context == null) return false;

        int nightModeFlags = context.getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == Configuration.UI_MODE_NIGHT_YES;
    }

    public static String getImageUrlFromJsonLd(String jsonStr) {
        if (jsonStr == null || jsonStr.isEmpty()) return null;

        try {
            JSONObject obj = new JSONObject(jsonStr);
            JSONObject mainEntity = obj.getJSONObject("mainEntity");
            Object jsonValue = mainEntity.get("image");
            return extractUrlFromJsonValue(jsonValue);
        } catch (JSONException e) {
            return null;
        }
    }

    private static String extractUrlFromJsonValue(Object v) {
        if (v == null) return null;

        if (v instanceof String) return (String) v;

        if (v instanceof JSONObject) {
            JSONObject o = (JSONObject) v;
            if (o.has("url")) return o.optString("url", null);
            if (o.has("@id")) return o.optString("@id", null);
            if (o.has("contentUrl")) return o.optString("contentUrl", null);

            return null;
        }

        if (v instanceof JSONArray) {
            JSONArray arr = (JSONArray) v;
            if (arr.length() == 0) return null;
            return extractUrlFromJsonValue(arr.opt(0));
        }

        return null;
    }

    public static String convertIsoToRfc1123(String isoInstant) {
        Instant instant = Instant.from(DateTimeFormatter.ISO_INSTANT.parse(isoInstant));
        return DateTimeFormatter.RFC_1123_DATE_TIME.withLocale(Locale.ENGLISH)
                .format(ZonedDateTime.ofInstant(instant, ZoneId.of("GMT")));
    }

    public static String escapeForXml(String text) {
        if (text == null) return null;

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    public static String linkifyUrlsToHtml(String text) {
        if (text == null) return null;

        Pattern urlPattern = Patterns.WEB_URL;
        Matcher m = urlPattern.matcher(text);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (m.find()) {
            String url = m.group();
            int start = m.start();
            sb.append(text, lastEnd, start);

            // skip if already inside an <a>
            if (start > 0) {
                int lastOpen = text.lastIndexOf("<a", start);
                int lastClose = text.lastIndexOf("</a>", start);
                if (lastOpen > lastClose) {
                    sb.append(url);
                    lastEnd = m.end();
                    continue;
                }
            }

            String href = url.startsWith("www.") ? "http://" + url : url;
            String safeHref = href.replace("\"", "%22");
            String safeText = url.replace("<", "&lt;").replace(">", "&gt;");
            sb.append("<a href=\"").append(safeHref).append("\">").append(safeText).append("</a>");
            lastEnd = m.end();
        }

        sb.append(text, lastEnd, text.length());
        return sb.toString();
    }
}
