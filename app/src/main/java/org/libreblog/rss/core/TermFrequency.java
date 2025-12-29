package org.libreblog.rss.core;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.libreblog.rss.utils.Utils;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class TermFrequency {
    private static final Map<String, Term> terms = new HashMap<>();
    private static JSONObject stopwords;

    public static void setStopwords(InputStream swInputStream) {
        String stopwordsStr = Utils.inputStreamToString(swInputStream);
        try {
            stopwords = new JSONObject(stopwordsStr);
        } catch (JSONException e) {
            Log.w("TermFrequency", "Cannot set stopwords", e);
        }
    }

    public static SetOfFrequencies frequencies(DbHandler.Article article) {
        if (article == null) return null;

        StringBuilder categories = new StringBuilder();
        if (article.categories != null) {
            try {
                JSONArray catArr = new JSONArray(article.categories);
                for (int i = 0; i < catArr.length(); i++) {
                    categories.append(catArr.get(i));
                }
            } catch (JSONException e) {
                Log.w("TermFrequency", "Cannot convert categories to JSON", e);
            }
        }

        String sentence = (article.ctitle != null ? article.ctitle : "") + " " +
                (article.cdescription != null ? article.cdescription : "") + " " +
                categories;
        SetOfFrequencies setOfFrequencies = new SetOfFrequencies();

        String[] words = sentence.split(" ");
        int likedSum = 0;
        int openedSum = 0;
        for (String word : words) {
            Term term = terms.get(word);
            if (term != null) {
                likedSum += term.liked;
                openedSum += term.opened;
            }
        }

        if (words.length > 0) {
            setOfFrequencies.liked += (double) likedSum / words.length;
            setOfFrequencies.opened += (double) openedSum / words.length;
        }

        return setOfFrequencies;
    }

    public static void addLikedTerms(String sentence) {
        if (sentence == null) return;

        String[] words = sentence.split(" ");
        for (String word : words) {
            Term term = terms.get(word);
            if (term != null) {
                term.liked += 1;
            } else {
                term = new Term(word);
                term.liked = 1;
                terms.put(word, term);
            }
        }
    }

    public static void addOpenedTerms(String sentence) {
        if (sentence == null) return;

        String[] words = sentence.split(" ");
        for (String word : words) {
            Term term = terms.get(word);
            if (term != null) {
                term.opened += 1;
            } else {
                term = new Term(word);
                term.opened = 1;
                terms.put(word, term);
            }
        }
    }

    public static String cleanSentence(String sentence, String language) {
        if (sentence == null) return null;

        sentence = sentence.replaceAll("\\p{Punct}", "");
        sentence = sentence.toLowerCase();

        String[] words = sentence.split(" ");
        StringBuilder cleaned = new StringBuilder();

        try {
            JSONArray sw = stopwords.getJSONArray(language);
            for (String word : words) {
                if (Utils.containsString(sw, word)) continue;
                cleaned.append(word).append(" ");
            }
        } catch (JSONException e) {
            Log.w("TermFrequency", "Cannot use stopwords", e);
        }

        if (cleaned.length() > 0) {
            cleaned = new StringBuilder(cleaned.toString().strip());
        }

        return cleaned.toString();
    }

    public static class Term {
        public final String word;
        public int liked = 0;
        public int opened = 0;

        public Term(String word) {
            this.word = word;
        }
    }

    public static class SetOfFrequencies {
        public double liked = 0;
        public double opened = 0;
    }
}
