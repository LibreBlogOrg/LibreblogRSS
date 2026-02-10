package org.libreblog.rss.ui;

import static org.libreblog.rss.core.SourceCrawler.TIME_TO_REFRESH;
import static org.libreblog.rss.ui.FragmentMain.HOME_BUTTON_ID;
import static org.libreblog.rss.ui.FragmentSettings.LIKED_ARTICLES_LIMIT;
import static org.libreblog.rss.ui.FragmentSettings.OPENED_ARTICLES_LIMIT;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.util.UnstableApi;

import org.libreblog.rss.R;
import org.libreblog.rss.core.ActivityPubHandler;
import org.libreblog.rss.core.ArticleSorter;
import org.libreblog.rss.core.DbHandler;
import org.libreblog.rss.core.MediaHandler;
import org.libreblog.rss.core.SourceCrawler;
import org.libreblog.rss.core.TermFrequency;
import org.libreblog.rss.utils.Settings;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        prepareApp();

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Context context = getApplicationContext();
        SharedPreferences settings = Settings.getSettings(context);
        long now = System.currentTimeMillis();

        if (settings.getLong("last_refresh", 0) + TIME_TO_REFRESH < now) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putLong("last_refresh", now - TIME_TO_REFRESH + 15000);
            editor.apply();

            SourceCrawler.OnRefreshListener onRefreshListener = null;
            try {
                ImageButton homeButton = requireViewById(HOME_BUTTON_ID);
                onRefreshListener = () -> FragmentMain.changeHomeIcon(homeButton);
            } catch (Exception ignore) {}

            new SourceCrawler(context, onRefreshListener).refresh(context);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void prepareApp() {
        Context context = getApplicationContext();
        ActivityPubHandler.setRepostedItem(getString(R.string.reposted_item));
        ArticleSorter.init(context);
        MediaHandler.init(context);
        TermFrequency.setStopwords(getResources().openRawResource(R.raw.stopwords));

        SharedPreferences settings = Settings.getSettings(context);
        int likedArticlesLimit = settings.getInt("liked_articles_limit", LIKED_ARTICLES_LIMIT);
        List<DbHandler.Article> likedArticles;
        int openedArticlesLimit = settings.getInt("opened_articles_limit", OPENED_ARTICLES_LIMIT);
        List<DbHandler.Article> openedArticles;

        try (DbHandler db = new DbHandler(context)) {
            likedArticles = db.getLikedArticles(likedArticlesLimit);
            for (DbHandler.Article article : likedArticles) {
                String sentence = (article.ctitle != null ? article.ctitle + " " : "")
                        + (article.cdescription != null ? article.cdescription : "");
                if (article.liked == 1) TermFrequency.addLikedTerms(sentence);
            }

            openedArticles = db.getOpenedArticles(openedArticlesLimit);
            for (DbHandler.Article article : openedArticles) {
                String sentence = (article.ctitle != null ? article.ctitle + " " : "")
                        + (article.cdescription != null ? article.cdescription : "");
                if (article.opened == 1) TermFrequency.addOpenedTerms(sentence);
            }

            db.deleteOldArticles(context);
        }
    }
}