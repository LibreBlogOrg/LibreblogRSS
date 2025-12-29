package org.libreblog.rss.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {
    private static SharedPreferences settings = null;

    private static void initSettings(Context context) {
        if (context == null) return;

        settings = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    public static SharedPreferences getSettings(Context context) {
        if (context == null) return null;

        if (settings == null) initSettings(context);
        return settings;
    }
}
