package org.libreblog.rss.ui;

import static org.libreblog.rss.core.ArticlesSorter.COEFF_COMPLETE;
import static org.libreblog.rss.core.ArticlesSorter.COEFF_DISPLAYED;
import static org.libreblog.rss.core.ArticlesSorter.COEFF_RECENT;
import static org.libreblog.rss.core.ArticlesSorter.COEFF_REL_LIKED_FREQUENCY;
import static org.libreblog.rss.core.ArticlesSorter.COEFF_REL_OPENED_FREQUENCY;
import static org.libreblog.rss.core.ArticlesSorter.COEFF_REL_SOURCE_LIKABILITY;
import static org.libreblog.rss.core.ArticlesSorter.COEFF_SOURCE_SCORE;
import static org.libreblog.rss.core.ArticlesSorter.COEFF_SPAMMABILITY;
import static org.libreblog.rss.core.ArticlesSorter.HAS_AUDIO_OR_VIDEO;
import static org.libreblog.rss.core.ArticlesSorter.HAS_AUTHOR;
import static org.libreblog.rss.core.ArticlesSorter.HAS_CATEGORIES;
import static org.libreblog.rss.core.ArticlesSorter.HAS_COMMENTS;
import static org.libreblog.rss.core.ArticlesSorter.HAS_DESCRIPTION;
import static org.libreblog.rss.core.ArticlesSorter.HAS_GOOD_DESCRIPTION;
import static org.libreblog.rss.core.ArticlesSorter.HAS_HTML;
import static org.libreblog.rss.core.ArticlesSorter.HAS_IMAGE;
import static org.libreblog.rss.core.ArticlesSorter.HAS_MIN_DESCRIPTION;
import static org.libreblog.rss.core.ArticlesSorter.HAS_TITLE;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;

import org.libreblog.rss.R;
import org.libreblog.rss.utils.Settings;
import org.libreblog.rss.utils.Utils;

public class FragmentSettings extends Fragment {
    public static final int ARTICLES_BY_PAGE_REFRESH = 10;
    public static final int DELETE_AFTER_X_DAYS = 30;
    public static final int LIKED_ARTICLES_LIMIT = 50;
    public static final int OPENED_ARTICLES_LIMIT = 150;

    public FragmentSettings() {
        super(R.layout.fragment_settings);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SharedPreferences settings = Settings.getSettings(view.getContext());
        float coeffRecent = settings.getFloat("coeff_recent", COEFF_RECENT);
        float coeffComplete = settings.getFloat("coeff_complete", COEFF_COMPLETE);
        float coeffSourceScore = settings.getFloat("coeff_source_score", COEFF_SOURCE_SCORE);
        float coeffRelSourceLikability = settings.getFloat("coeff_rel_source_likability", COEFF_REL_SOURCE_LIKABILITY);
        float coeffRelLikedFrequency = settings.getFloat("coeff_rel_liked_frequency", COEFF_REL_LIKED_FREQUENCY);
        float coeffRelOpenedFrequency = settings.getFloat("coeff_rel_opened_frequency", COEFF_REL_OPENED_FREQUENCY);
        float coeffSpammability = settings.getFloat("coeff_spammability", COEFF_SPAMMABILITY);
        float coeffDisplayed = settings.getFloat("coeff_displayed", COEFF_DISPLAYED);
        float hasTitle = settings.getFloat("has_title", HAS_TITLE);
        float hasDescription = settings.getFloat("has_description", HAS_DESCRIPTION);
        float hasMinDescription = settings.getFloat("has_min_description", HAS_MIN_DESCRIPTION);
        float hasGoodDescription = settings.getFloat("has_good_description", HAS_GOOD_DESCRIPTION);
        float hasHTML = settings.getFloat("has_html", HAS_HTML);
        float hasImage = settings.getFloat("has_image", HAS_IMAGE);
        float hasAudioOrVideo = settings.getFloat("has_audio_or_video", HAS_AUDIO_OR_VIDEO);
        float hasComments = settings.getFloat("has_comments", HAS_COMMENTS);
        float hasAuthor = settings.getFloat("has_author", HAS_AUTHOR);
        float hasCategories = settings.getFloat("has_categories", HAS_CATEGORIES);
        int articlesByPageRefresh = settings.getInt("articles_by_page_refresh", ARTICLES_BY_PAGE_REFRESH);
        int deleteAfterXDays = settings.getInt("delete_after_x_days", DELETE_AFTER_X_DAYS);
        int likedArticlesLimit = settings.getInt("liked_articles_limit", LIKED_ARTICLES_LIMIT);
        int openedArticlesLimit = settings.getInt("opened_articles_limit", OPENED_ARTICLES_LIMIT);

        EditText recent = requireActivity().findViewById(R.id.edit_text_recent);
        recent.setText(Float.toString(coeffRecent));
        EditText complete = requireActivity().findViewById(R.id.edit_text_complete);
        complete.setText(Float.toString(coeffComplete));
        EditText sourceScore = requireActivity().findViewById(R.id.edit_text_source_score);
        sourceScore.setText(Float.toString(coeffSourceScore));
        EditText sourceLikability = requireActivity().findViewById(R.id.edit_text_source_likability);
        sourceLikability.setText(Float.toString(coeffRelSourceLikability));
        EditText liked = requireActivity().findViewById(R.id.edit_text_liked);
        liked.setText(Float.toString(coeffRelLikedFrequency));
        EditText opened = requireActivity().findViewById(R.id.edit_text_opened);
        opened.setText(Float.toString(coeffRelOpenedFrequency));
        EditText spammability = requireActivity().findViewById(R.id.edit_text_spammability);
        spammability.setText(Float.toString(coeffSpammability));
        EditText displayed = requireActivity().findViewById(R.id.edit_text_displayed);
        displayed.setText(Float.toString(coeffDisplayed));
        EditText hTitle = requireActivity().findViewById(R.id.edit_text_complete_title);
        hTitle.setText(Float.toString(hasTitle));
        EditText hDescription = requireActivity().findViewById(R.id.edit_text_complete_description);
        hDescription.setText(Float.toString(hasDescription));
        EditText hMinDescription = requireActivity().findViewById(R.id.edit_text_complete_description_min);
        hMinDescription.setText(Float.toString(hasMinDescription));
        EditText hGoodDescription = requireActivity().findViewById(R.id.edit_text_complete_description_good);
        hGoodDescription.setText(Float.toString(hasGoodDescription));
        EditText hImage = requireActivity().findViewById(R.id.edit_text_complete_image);
        hImage.setText(Float.toString(hasImage));
        EditText hHTML = requireActivity().findViewById(R.id.edit_text_complete_html);
        hHTML.setText(Float.toString(hasHTML));
        EditText hAudioOrVideo = requireActivity().findViewById(R.id.edit_text_complete_audio_or_video);
        hAudioOrVideo.setText(Float.toString(hasAudioOrVideo));
        EditText hComments = requireActivity().findViewById(R.id.edit_text_complete_comments);
        hComments.setText(Float.toString(hasComments));
        EditText hAuthor = requireActivity().findViewById(R.id.edit_text_complete_authors);
        hAuthor.setText(Float.toString(hasAuthor));
        EditText hCategories = requireActivity().findViewById(R.id.edit_text_complete_categories);
        hCategories.setText(Float.toString(hasCategories));
        EditText aBPR = requireActivity().findViewById(R.id.edit_text_articles_page);
        aBPR.setText(Integer.toString(articlesByPageRefresh));
        EditText dAXD = requireActivity().findViewById(R.id.edit_text_delete_after_x_days);
        dAXD.setText(Integer.toString(deleteAfterXDays));
        EditText likedLimit = requireActivity().findViewById(R.id.edit_text_liked_articles_limit);
        likedLimit.setText(Integer.toString(likedArticlesLimit));
        EditText openedLimit = requireActivity().findViewById(R.id.edit_text_opened_articles_limit);
        openedLimit.setText(Integer.toString(openedArticlesLimit));

        AppCompatImageButton closeButton = requireActivity().findViewById(R.id.close_settings_button);
        if (Utils.isDarkTheme(view.getContext()))
            closeButton.setBackgroundColor(Color.rgb(120, 120, 240));
        closeButton.setOnClickListener(v -> exit());

        Button saveButton = requireActivity().findViewById(R.id.save_settings_button);
        saveButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = settings.edit();

            if (!areCoefficientsValid(recent, complete, sourceScore, sourceLikability, liked, opened,
                    spammability, displayed, hTitle, hDescription, hMinDescription, hGoodDescription,
                    hImage, hHTML, hAudioOrVideo, hComments, hAuthor, hCategories)) {
                Toast.makeText(v.getContext(), R.string.coefficients_must_be_bigger, Toast.LENGTH_SHORT).show();
                return;
            }
            if (!recent.getText().toString().isEmpty())
                editor.putFloat("coeff_recent", Float.parseFloat(recent.getText().toString()));
            if (!complete.getText().toString().isEmpty())
                editor.putFloat("coeff_complete", Float.parseFloat(complete.getText().toString()));
            if (!sourceScore.getText().toString().isEmpty())
                editor.putFloat("coeff_source_score", Float.parseFloat(sourceScore.getText().toString()));
            if (!sourceLikability.getText().toString().isEmpty())
                editor.putFloat("coeff_rel_source_likability", Float.parseFloat(sourceLikability.getText().toString()));
            if (!liked.getText().toString().isEmpty())
                editor.putFloat("coeff_rel_liked_frequency", Float.parseFloat(liked.getText().toString()));
            if (!opened.getText().toString().isEmpty())
                editor.putFloat("coeff_rel_opened_frequency", Float.parseFloat(opened.getText().toString()));
            if (!spammability.getText().toString().isEmpty())
                editor.putFloat("coeff_spammability", Float.parseFloat(spammability.getText().toString()));
            if (!displayed.getText().toString().isEmpty())
                editor.putFloat("coeff_displayed", Float.parseFloat(displayed.getText().toString()));
            if (!hTitle.getText().toString().isEmpty())
                editor.putFloat("has_title", Float.parseFloat(hTitle.getText().toString()));
            if (!hDescription.getText().toString().isEmpty())
                editor.putFloat("has_description", Float.parseFloat(hDescription.getText().toString()));
            if (!hMinDescription.getText().toString().isEmpty())
                editor.putFloat("has_min_description", Float.parseFloat(hMinDescription.getText().toString()));
            if (!hGoodDescription.getText().toString().isEmpty())
                editor.putFloat("has_good_description", Float.parseFloat(hGoodDescription.getText().toString()));
            if (!hImage.getText().toString().isEmpty())
                editor.putFloat("has_image", Float.parseFloat(hImage.getText().toString()));
            if (!hHTML.getText().toString().isEmpty())
                editor.putFloat("has_html", Float.parseFloat(hHTML.getText().toString()));
            if (!hAudioOrVideo.getText().toString().isEmpty())
                editor.putFloat("has_audio_or_video", Float.parseFloat(hAudioOrVideo.getText().toString()));
            if (!hComments.getText().toString().isEmpty())
                editor.putFloat("has_comments", Float.parseFloat(hComments.getText().toString()));
            if (!hAuthor.getText().toString().isEmpty())
                editor.putFloat("has_author", Float.parseFloat(hAuthor.getText().toString()));
            if (!hCategories.getText().toString().isEmpty())
                editor.putFloat("has_categories", Float.parseFloat(hCategories.getText().toString()));
            if (!aBPR.getText().toString().isEmpty()) {
                int aBPRValue = Integer.parseInt(aBPR.getText().toString());
                if (aBPRValue == 0) aBPRValue = 1;
                editor.putInt("articles_by_page_refresh", Math.abs(aBPRValue));
            }
            if (!dAXD.getText().toString().isEmpty()) {
                int dAXDValue = Integer.parseInt(dAXD.getText().toString());
                if (dAXDValue == 0) dAXDValue = 1;
                editor.putInt("delete_after_x_days", Math.abs(dAXDValue));
            }
            if (!likedLimit.getText().toString().isEmpty()) {
                int likedLimitValue = Integer.parseInt(likedLimit.getText().toString());
                if (likedLimitValue == 0) likedLimitValue = 1;
                editor.putInt("liked_articles_limit", Math.abs(likedLimitValue));
            }
            if (!openedLimit.getText().toString().isEmpty()) {
                int openedLimitValue = Integer.parseInt(openedLimit.getText().toString());
                if (openedLimitValue == 0) openedLimitValue = 1;
                editor.putInt("opened_articles_limit", Math.abs(openedLimitValue));
            }

            editor.apply();
            exit();
        });

        Button resetButton = requireActivity().findViewById(R.id.reset_settings_button);
        resetButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = settings.edit();

            editor.putFloat("coeff_recent", COEFF_RECENT);
            editor.putFloat("coeff_complete", COEFF_COMPLETE);
            editor.putFloat("coeff_source_score", COEFF_SOURCE_SCORE);
            editor.putFloat("coeff_rel_source_likability", COEFF_REL_SOURCE_LIKABILITY);
            editor.putFloat("coeff_rel_liked_frequency", COEFF_REL_LIKED_FREQUENCY);
            editor.putFloat("coeff_rel_opened_frequency", COEFF_REL_OPENED_FREQUENCY);
            editor.putFloat("coeff_spammability", COEFF_SPAMMABILITY);
            editor.putFloat("coeff_displayed", COEFF_DISPLAYED);
            editor.putFloat("has_title", HAS_TITLE);
            editor.putFloat("has_description", HAS_DESCRIPTION);
            editor.putFloat("has_min_description", HAS_MIN_DESCRIPTION);
            editor.putFloat("has_good_description", HAS_GOOD_DESCRIPTION);
            editor.putFloat("has_image", HAS_IMAGE);
            editor.putFloat("has_html", HAS_HTML);
            editor.putFloat("has_audio_or_video", HAS_AUDIO_OR_VIDEO);
            editor.putFloat("has_comments", HAS_COMMENTS);
            editor.putFloat("has_author", HAS_AUTHOR);
            editor.putFloat("has_categories", HAS_CATEGORIES);
            editor.putInt("articles_by_page_refresh", ARTICLES_BY_PAGE_REFRESH);
            editor.putInt("delete_after_x_days", DELETE_AFTER_X_DAYS);
            editor.putInt("liked_articles_limit", LIKED_ARTICLES_LIMIT);
            editor.putInt("opened_articles_limit", OPENED_ARTICLES_LIMIT);

            editor.apply();
            exit();
        });

    }

    private boolean areCoefficientsValid(EditText... editTextArr) {
        if (editTextArr == null) return true;

        for (EditText editText : editTextArr) {
            if (!editText.getText().toString().isEmpty()) {
                float nbr = Float.parseFloat(editText.getText().toString());
                if (nbr < 0 || nbr > 1) return false;
            }
        }

        return true;
    }

    private void exit() {
        requireActivity().getSupportFragmentManager().popBackStack();
    }
}
