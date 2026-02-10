package org.libreblog.rss.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LevelListDrawable;
import android.text.Html;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.vdurmont.emoji.EmojiParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EmojiHandler {
    private static final Pattern SHORTCODE = Pattern.compile(":(?<name>[A-Za-z0-9_+-]+):");
    private static final String EMOJI_PREFIX = "emoji://";
    private static final String EMOJI_SEPARATOR = ";;;";
    private static final long SHORTCODE_EXPIRATION = 24 * 3600 * 1000;

    public static String replaceEmojiShortcodes(String text, String instanceUrl) {
        if (text == null) return "";

        String preParsed = EmojiParser.parseToUnicode(text);
        Matcher m = SHORTCODE.matcher(preParsed);

        if (instanceUrl == null) return preParsed;

        String[] parts = instanceUrl.split("/");
        if (!instanceUrl.startsWith("http") || parts.length < 3) {
            return preParsed;
        }

        StringBuilder sb = usePlaceholder(m, preParsed, parts);
        return sb.toString();
    }

    private static StringBuilder usePlaceholder(Matcher m, String preParsed, String[] parts) {
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (m.find()) {
            sb.append(preParsed, lastEnd, m.start());

            final String name = m.group("name");
            final String url = EMOJI_PREFIX + name + EMOJI_SEPARATOR + parts[0] + "//" + parts[2];
            final String replacement = "<img src=\"" + url + "\" />";

            sb.append(replacement);
            lastEnd = m.end();
        }
        sb.append(preParsed, lastEnd, preParsed.length());
        return sb;
    }

    public static Html.ImageGetter getImageGetter(Context context, TextView textView, int yOff) {
        return source -> {
            if (source == null) return null;

            String src = source.trim();
            if (!src.startsWith(EMOJI_PREFIX)) return null;

            final int h = Math.max(1, Math.round(textView.getTextSize() * 1.1f));
            final Resources res = context.getResources();

            final int yOffset = (int) (yOff * res.getDisplayMetrics().density + 0.5f);
            final LevelListDrawable placeholderContainer = new LevelListDrawable();
            ColorDrawable placeholder = new ColorDrawable(Color.TRANSPARENT);
            placeholder.setBounds(0, 0, h, h);
            placeholderContainer.addLevel(0, 0, placeholder);
            placeholderContainer.setBounds(0, 0, h, h);
            placeholderContainer.setLevel(0);

            Thread thread = new Thread(() -> findEmojiUrl(src, context)
                    .thenAccept(emojiUrl -> {
                        if (emojiUrl == null) return;

                        Glide.with(context.getApplicationContext())
                                .asBitmap()
                                .load(emojiUrl)
                                .into(new CustomTarget<Bitmap>() {
                                    @Override
                                    public void onResourceReady(@NonNull Bitmap resource,
                                                                @Nullable Transition<? super Bitmap> transition) {
                                        BitmapDrawable bitmapDrawable = new BitmapDrawable(res, resource);

                                        int bw = resource.getWidth();
                                        int bh = resource.getHeight();
                                        float scale = (float) h / bh;
                                        int width = Math.max(1, (int) (bw * scale));

                                        bitmapDrawable.setBounds(0, 0, width, h);
                                        Drawable inset = new InsetDrawable(bitmapDrawable, 0, -yOffset, 0, yOffset);
                                        inset.setBounds(0, -yOffset, width, h - yOffset);

                                        placeholderContainer.addLevel(1, 1, inset);
                                        placeholderContainer.setBounds(0, -yOffset, width, h - yOffset);
                                        placeholderContainer.setLevel(1);

                                        textView.post(() -> {
                                            CharSequence t = textView.getText();
                                            textView.setText(t);
                                            textView.requestLayout();
                                            textView.invalidate();
                                        });
                                    }

                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholderDrawable) {
                                    }

                                    @Override
                                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                                        // keep placeholder
                                    }
                                });
                    }));
            thread.start();

            return placeholderContainer;
        };
    }

    public static CompletableFuture<String> findEmojiUrl(String src, Context context) {
        if (src == null) return null;

        src = src.replace(EMOJI_PREFIX, "");
        String[] parts = src.split(EMOJI_SEPARATOR);
        String shortcode = parts[0];
        String instance = parts[1];
        String instanceDomain = instance.split("//")[1];

        Executor bg = Executors.newSingleThreadExecutor();
        SharedPreferences savedEmojis = context.getSharedPreferences("emoji." + instanceDomain, Context.MODE_PRIVATE);
        String emojiUrl = savedEmojis.getString(shortcode, "");
        if (!emojiUrl.isEmpty()) return CompletableFuture.supplyAsync(() -> emojiUrl, bg);
        if (savedEmojis.getLong("timestamp", 0) > System.currentTimeMillis() - SHORTCODE_EXPIRATION) {
            return CompletableFuture.supplyAsync(() -> null, bg);
        }

        final OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder()
                .url(instance + "/api/v1/custom_emojis")
                .get()
                .build();

        try (Response resp = client.newCall(req).execute()) {
            int code = resp.code();
            String body = resp.body().string();

            if (code < 200 || code >= 300) {
                updateSavedEmojis(savedEmojis, new JSONArray());
                return CompletableFuture.supplyAsync(() -> null, bg);
            }

            JSONTokener tokener = new JSONTokener(body.trim());
            Object jsonArr = tokener.nextValue();
            if (jsonArr instanceof JSONArray) {
                return CompletableFuture.supplyAsync(() -> {
                    updateSavedEmojis(savedEmojis, (JSONArray) jsonArr);
                    return savedEmojis.getString(shortcode, null);
                }, bg);
            } else {
                return CompletableFuture.supplyAsync(() -> null, bg);
            }
        } catch (Exception e) {
            return CompletableFuture.supplyAsync(() -> null, bg);
        }
    }

    private static void updateSavedEmojis(SharedPreferences savedEmojis, JSONArray jsonArr) {
        SharedPreferences.Editor editor = savedEmojis.edit();
        editor.putLong("timestamp", System.currentTimeMillis());

        for (int i = 0; i < jsonArr.length(); i++) {
            final JSONObject item = jsonArr.optJSONObject(i);
            if (item == null) continue;

            try {
                final String shortcode = item.getString("shortcode");
                final String url = item.getString("url");
                editor.putString(shortcode, url);
            } catch (JSONException ignored) {}
        }

        editor.apply();
    }
}
