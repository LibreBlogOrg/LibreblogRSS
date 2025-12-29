package org.libreblog.rss.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.Toast;

import org.libreblog.rss.R;

public class ExtraOptions {
    public static void openAppRating(View view) {
        if (view == null) return;

        Context context = view.getContext();
        final String packageName = context.getPackageName();

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName));
            intent.setPackage("com.android.vending");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    public static void openAboutPage(View view) {
        if (view == null) return;

        Context context = view.getContext();
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://libreblog.org"));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_app_available_to_open_the_link, Toast.LENGTH_SHORT).show();
        }
    }
}
