package org.libreblog.rss.ui;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import static org.libreblog.rss.core.DbHandler.SOURCES_NAME_COL;
import static org.libreblog.rss.core.DbHandler.SOURCES_PREFERRED_IMAGE_COL;
import static org.libreblog.rss.core.RssDiscover.DEFAULT_TIMEOUT;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.rometools.rome.feed.synd.SyndFeed;

import org.json.JSONObject;
import org.libreblog.rss.R;
import org.libreblog.rss.core.ActivityPubHandler;
import org.libreblog.rss.core.DbHandler;
import org.libreblog.rss.core.RssDiscover;
import org.libreblog.rss.core.SourceCrawler;
import org.libreblog.rss.utils.FeedCallback;
import org.libreblog.rss.utils.Settings;
import org.libreblog.rss.utils.Utils;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class FragmentAddSource extends Fragment {
    private DbHandler db;
    private EditText id, name, preferredImage;

    public FragmentAddSource() {
        super(R.layout.fragment_add_source);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        LinearLayout settingsBtLayout = requireActivity().findViewById(R.id.settings_button_layout);
        LinearLayout moreOpBtLayout = requireActivity().findViewById(R.id.more_options_button_layout);
        db = new DbHandler(getContext());
        id = requireActivity().findViewById(R.id.edit_id);
        name = requireActivity().findViewById(R.id.edit_name);
        preferredImage = requireActivity().findViewById(R.id.edit_preferred_image);

        Button save = requireActivity().findViewById(R.id.add_button);
        Button cancel = requireActivity().findViewById(R.id.cancel_button);
        cancel.setOnClickListener(v -> exit());
        AppCompatImageButton closeButton = requireActivity().findViewById(R.id.close_add_source_button);
        if (Utils.isDarkTheme(view.getContext()))
            closeButton.setBackgroundColor(Color.rgb(120, 120, 240));
        closeButton.setOnClickListener(v -> exit());
        ProgressBar pBar = requireActivity().findViewById(R.id.progress_bar);
        pBar.setVisibility(INVISIBLE);

        if (args != null) {
            save.setOnClickListener(v -> saveEditedSource());

            TextView tvTitle = requireActivity().findViewById(R.id.source_title_label);
            tvTitle.setText(R.string.edit_source);
            save.setText(R.string.save);

            settingsBtLayout.setVisibility(INVISIBLE);
            moreOpBtLayout.setVisibility(INVISIBLE);

            if (args.containsKey("id")) {
                id.setText(args.getString("id"));
                id.setKeyListener(null);
                id.setCursorVisible(false);
                id.setTextIsSelectable(true);
                id.setTextColor(Color.rgb(106, 106,106));
            }

            if (args.containsKey("name")) {
                name.setText(args.getString("name"));
            }

            if (args.containsKey("preferred_image")) {
                preferredImage.setText(args.getString("preferred_image"));
            }
        } else {
            final boolean[] loading = {false};
            save.setOnClickListener(v -> saveNewSource(loading, pBar, save));

            settingsBtLayout.setOnClickListener(v -> openSettings());
            moreOpBtLayout.setOnClickListener(v -> openMoreOptions());

            if (Utils.isDarkTheme(view.getContext())) {
                settingsBtLayout.setBackgroundColor(Color.rgb(120, 120, 240));
                moreOpBtLayout.setBackgroundColor(Color.rgb(120, 120, 240));
                TextView settingsBtText = requireActivity().findViewById(R.id.settings_button_text);
                settingsBtText.setTextColor(Color.rgb(50, 50, 50));
                TextView moreOpBtText = requireActivity().findViewById(R.id.more_options_button_text);
                moreOpBtText.setTextColor(Color.rgb(50, 50, 50));
                AppCompatImageButton settingsBt = requireActivity().findViewById(R.id.settings_button);
                settingsBt.setColorFilter(Color.rgb(50, 50, 50));
                AppCompatImageButton moreOpBt = requireActivity().findViewById(R.id.more_options_button);
                moreOpBt.setColorFilter(Color.rgb(50, 50, 50));
            }
        }
    }

    private void saveEditedSource() {
        if (name.getText().toString().isEmpty()) {
            Toast.makeText(getContext(), R.string.choose_a_name, Toast.LENGTH_SHORT).show();
            return;
        }

        String preferredImageUrl = preferredImage.getText().toString();
        if (!preferredImageUrl.isEmpty() && !URLUtil.isValidUrl(preferredImageUrl)) {
            Toast.makeText(getContext(), R.string.invalid_image_url, Toast.LENGTH_SHORT).show();
            return;
        }

        ContentValues values = new ContentValues();
        values.put(SOURCES_NAME_COL, name.getText().toString());
        values.put(SOURCES_PREFERRED_IMAGE_COL, preferredImageUrl);

        db.updateSource(id.getText().toString(), values);
        exit();
    }

    private void saveNewSource(boolean[] loading, ProgressBar pBar, Button save) {
        if (loading[0]) return;

        DbHandler.Source source = new DbHandler.Source();
        source.id = id.getText().toString().trim();
        source.name = name.getText().toString().trim();
        source.preferredImage = preferredImage.getText().toString().trim();

        if (source.name.isEmpty()) {
            Toast.makeText(getContext(), R.string.choose_a_name, Toast.LENGTH_SHORT).show();
            return;
        }

        if (source.id.isEmpty()) {
            Toast.makeText(getContext(), R.string.enter_a_url, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!source.id.startsWith("http")) source.id = "https://" + source.id;

        if (!URLUtil.isValidUrl(source.id)) {
            Toast.makeText(getContext(), R.string.invalid_url, Toast.LENGTH_SHORT).show();
            return;
        }

        List<DbHandler.Source> sources = db.getSources();
        for (DbHandler.Source s : sources) {
            if (Objects.equals(s.id, source.id)) {
                Toast.makeText(getContext(), R.string.source_already_added, Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (source.preferredImage != null && !source.preferredImage.isEmpty()
                && !URLUtil.isValidUrl(source.preferredImage)) {
            Toast.makeText(getContext(), R.string.invalid_image_url, Toast.LENGTH_SHORT).show();
            return;
        }

        FeedCallback feedCb = getNewSourceCallback(source, loading, pBar, save);
        save.setBackgroundColor(Color.LTGRAY);
        pBar.setVisibility(VISIBLE);
        loading[0] = true;

        if (source.preferredImage != null && !source.preferredImage.isEmpty()) {
            new Thread(() -> {
                boolean ok = Utils.isRemoteImageUrl(source.preferredImage, DEFAULT_TIMEOUT);
                if (!ok) source.preferredImage = null;
                discoverAPubOrRss(getContext(), source, feedCb);
            }).start();
        } else {
            new Thread(() -> discoverAPubOrRss(getContext(), source, feedCb)).start();
        }

        makeLastRefreshZero();
    }

    private void discoverAPubOrRss(Context context, DbHandler.Source source, FeedCallback feedCb) {
        try {
            JSONObject outbox = ActivityPubHandler.findOutbox(source.id, source,0);
            SyndFeed feed = ActivityPubHandler.convertOutboxJsonToSyndFeed(source, outbox);
            feedCb.onResult(feed);
        } catch (Exception e) {
            source.type = DbHandler.SOURCE_TYPE_RSS;
            RssDiscover.discover(context, source.id, feedCb);
        }
    }

    private FeedCallback getNewSourceCallback(DbHandler.Source source, boolean[] loading,
                                              ProgressBar pBar, Button save) {
        return new FeedCallback() {
            @Override
            public void onResult(SyndFeed feed) {
                if (feed != null && feed.getLink() != null) {
                    if (Objects.equals(source.type, DbHandler.SOURCE_TYPE_RSS)) {
                        source.id = feed.getLink();
                        String description = feed.getDescription();
                        if (description != null) source.description = description.trim();
                        String title = feed.getTitle();
                        if (title != null) source.title = title.trim();
                        if (feed.getIcon() != null) {
                            source.image = feed.getIcon().getUrl();
                        } else if (feed.getImage() != null) {
                            source.image = feed.getImage().getUrl();
                        }
                    }

                    boolean isNew = true;
                    List<DbHandler.Source> sources = db.getSources();
                    for (DbHandler.Source s : sources) {
                        if (Objects.equals(s.id, source.id)) {
                            isNew = false;
                            break;
                        }
                    }

                    if (isNew) {
                        db.putSource(source);
                        new SourceCrawler(getContext(), null).refreshSource(source.id);
                    }

                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            loading[0] = false;
                            pBar.setVisibility(INVISIBLE);
                            exit();
                        }
                    }, 2000);
                } else {
                    save.setBackgroundColor(Color.rgb(120, 120, 240));
                    pBar.setVisibility(INVISIBLE);
                    loading[0] = false;
                    Toast.makeText(getContext(), R.string.rss_not_found, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(Exception e) {
                Log.w("FragmentAddSource", "Cannot discover RSS", e);
                Toast.makeText(getContext(), R.string.error, Toast.LENGTH_SHORT).show();
                pBar.setVisibility(INVISIBLE);
                save.setBackgroundColor(Color.rgb(120, 120, 240));
                loading[0] = false;
            }
        };
    }

    private void makeLastRefreshZero() {
        SharedPreferences settings = Settings.getSettings(getContext());
        assert settings != null;
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("last_refresh", 0);
        editor.apply();
    }

    private void exit() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }

    private void openSettings() {
        openPage(FragmentSettings.class, "Settings");
    }

    private void openMoreOptions() {
        openPage(FragmentMoreOptions.class, "More options");
    }

    private void openPage(Class<? extends androidx.fragment.app.Fragment> c, String name) {
        FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, c, null)
                .setReorderingAllowed(true)
                .addToBackStack(name)
                .commit();
    }
}
