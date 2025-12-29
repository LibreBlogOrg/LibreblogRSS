package org.libreblog.rss.core;

import static androidx.media3.ui.PlayerView.IMAGE_DISPLAY_MODE_FIT;
import static org.libreblog.rss.ui.FragmentSettings.ARTICLES_BY_PAGE_REFRESH;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.ui.PlayerView;

import org.json.JSONArray;
import org.libreblog.rss.R;
import org.libreblog.rss.proxy.GlideApp;
import org.libreblog.rss.ui.FragmentMain;
import org.libreblog.rss.ui.FullscreenVideoActivity;
import org.libreblog.rss.utils.Settings;
import org.libreblog.rss.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedMaker {
    public static final int DOG_VIEW_ID = 23456;
    public static final int ARTICLES_INITIAL_LINE_COUNT = 4;
    public static final int ARTICLES_INITIAL_LINE_COUNT_2 = 2;
    public static final Map<String, DbHandler.Source> sourcesMap = new HashMap<>();
    private static final List<String> shownArticles = new ArrayList<>();
    private static DbHandler db;

    public static void init(Context context) {
        db = new DbHandler(context);

        for (DbHandler.Source source : db.getSources()) {
            sourcesMap.put(source.id, source);
        }
    }

    public static List<View> make(FragmentMain fragmentMain, DbHandler.Source source, int offset, boolean onlyLiked, boolean onlyHidden, String searchText) {
        if (fragmentMain == null) return new ArrayList<>();

        Context context = fragmentMain.getContext();
        assert context != null;
        SharedPreferences settings = Settings.getSettings(context);
        assert settings != null;
        int articlesByPageRefresh = settings.getInt("articles_by_page_refresh", ARTICLES_BY_PAGE_REFRESH);
        if (offset == 0) shownArticles.clear();

        List<View> feed = new ArrayList<>();
        List<DbHandler.Article> articles = getArticles(source, offset, onlyLiked,
                onlyHidden, articlesByPageRefresh, searchText);
        for (DbHandler.Article article : articles) {
            if (shownArticles.contains(article.id)) {
                feed.add(null);
                continue;
            }
            shownArticles.add(article.id);

            DbHandler.Source s = sourcesMap.get(article.source);
            String sourceImage = null;
            String sourceName = context.getString(R.string.someone);
            if (s != null) {
                sourceImage = s.image;
                sourceName = s.name;
            }

            String text = article.title;
            text = Utils.cleanHtml(text);
            if (text == null || text.isEmpty()) {
                String precleaned = article.description != null ? article.description : "";
                text = Utils.cleanHtml(precleaned);
            } else if (article.description != null) {
                String cleaned = Utils.cleanHtml(article.description);
                cleaned = cleaned.trim();
                text += "<br>" + (cleaned.startsWith("<br>") ? "" : "<br>") + cleaned;
            }
            text = text.trim();

            JSONArray imageArr = null;
            JSONArray audioArr = null;
            JSONArray videoArr = null;
            try {
                imageArr = new JSONArray(article.image);
            } catch (Exception ignore) {
            }
            try {
                audioArr = new JSONArray(article.audio);
            } catch (Exception ignore) {
            }
            try {
                videoArr = new JSONArray(article.video);
            } catch (Exception ignore) {
            }

            feed.add(createFeedCard(
                    fragmentMain,
                    sourceImage,
                    sourceName,
                    sourcesMap.get(article.source),
                    Utils.toTimeAgo(article.published),
                    Utils.jsonArrayToList(imageArr),
                    Utils.jsonArrayToList(videoArr),
                    Utils.jsonArrayToList(audioArr),
                    text,
                    40,
                    article));

            if (article.displayed < 1) db.displayArticle(article.id);
        }

        if (articles.isEmpty()) {
            ImageView img = new ImageView(context);
            img.setImageResource(R.drawable.nothing);
            img.setId(DOG_VIEW_ID);
            if (Utils.isDarkTheme(context)) {
                img.setBackgroundColor(Color.rgb(240, 240, 240));
            }
            feed.add(img);
        }

        return feed;
    }

    private static List<DbHandler.Article> getArticles(DbHandler.Source source, int offset, boolean onlyLiked, boolean onlyHidden, int articlesByPageRefresh, String searchText) {
        List<DbHandler.Article> articles;
        if (source != null) {
            articles = db.getArticlesFromSource(source.id, offset, articlesByPageRefresh, onlyHidden);
        } else if (onlyLiked) {
            articles = db.getAllArticlesLiked(offset, articlesByPageRefresh, false, searchText);
        } else {
            articles = db.getArticles(offset, articlesByPageRefresh, false);
        }
        return articles;
    }

    @OptIn(markerClass = UnstableApi.class)
    @SuppressLint("ClickableViewAccessibility")
    public static View createFeedCard(
            FragmentMain fragmentMain,
            String profileImageUrl,
            String sourceName,
            DbHandler.Source source,
            String timeAgo,
            List<String> images,
            List<String> videos,
            List<String> audios,
            String text,
            int profileSizeDp,
            DbHandler.Article article
    ) {
        Context context = fragmentMain.getContext();
        assert context != null;
        final float density = context.getResources().getDisplayMetrics().density;
        int padding = (int) (12 * density);
        int profileSizePx = (int) (profileSizeDp * density);
        boolean isDarkTheme = Utils.isDarkTheme(context);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        card.setPadding(padding, padding, padding, padding);

        LinearLayout header = createHeader(fragmentMain, profileImageUrl, sourceName, source, timeAgo, context, profileSizePx, density, isDarkTheme);
        ImageButton moreBt = createMoreButton(text, article, context, card, isDarkTheme);
        RelativeLayout headerPlusMore = getRelativeLayout(context, header, moreBt);

        PlayerView playerView = getPlayerView(videos, audios, context, density);
        ImageView postImage = getPostImage(images, context, density);
        LinearLayout actionRow = createActionRow(article, context, density, isDarkTheme);
        TextView titleTv = createTitleTv(text, article, context, density);

        card.addView(headerPlusMore);
        card.addView(titleTv);
        if (playerView != null) {
            card.addView(playerView);
        } else if (postImage != null) {
            card.addView(postImage);
        }
        card.addView(actionRow);

        return card;
    }

    private static RelativeLayout getRelativeLayout(Context context, LinearLayout header, ImageButton moreBt) {
        RelativeLayout headerPlusMore = new RelativeLayout(context);
        headerPlusMore.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        RelativeLayout.LayoutParams lpLeft = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lpLeft.addRule(RelativeLayout.ALIGN_PARENT_START);
        RelativeLayout.LayoutParams lpRight = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lpRight.addRule(RelativeLayout.ALIGN_PARENT_END);

        headerPlusMore.addView(header, lpLeft);
        headerPlusMore.addView(moreBt, lpRight);
        return headerPlusMore;
    }

    private static ImageButton createMoreButton(String text, DbHandler.Article article, Context context, LinearLayout card, boolean isDarkTheme) {
        ImageButton moreBt = new ImageButton(context);
        moreBt.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(card.getContext(), moreBt);

            int MENU_ITEM_HIDE = 1;
            int MENU_ITEM_COPY = 2;
            int MENU_ITEM_COMMENTS = 3;
            popup.getMenu().add(Menu.NONE, MENU_ITEM_HIDE, Menu.NONE, article.hidden == 0 ?
                    context.getString(R.string.hide_post_in_menu) : context.getString(R.string.show_post_in_menu));
            popup.getMenu().add(Menu.NONE, MENU_ITEM_COPY, Menu.NONE, R.string.copy_text_in_menu);
            if (article.comments != null && !article.comments.isEmpty()) {
                popup.getMenu().add(Menu.NONE, MENU_ITEM_COMMENTS, Menu.NONE, R.string.comments_in_menu);
            }

            popup.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();
                if (id == MENU_ITEM_HIDE) {
                    if (article.hidden == 0) {
                        db.hideArticle(article.id);
                    } else {
                        db.showArticle(article.id);
                    }
                    card.removeAllViews();
                    card.setPadding(0, 0, 0, 0);
                    return true;
                } else if (id == MENU_ITEM_COPY) {
                    ClipboardManager clipboard =
                            (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText(context.getString(R.string.post_text),
                            Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY));
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
                    return true;
                } else if (id == MENU_ITEM_COMMENTS) {
                    db.openArticle(article.id);
                    openLink(context, article.comments);
                    return true;
                }
                return false;
            });

            popup.show();
        });
        Icon plusIcon = Icon.createWithResource(context, R.drawable.outline_more_vert_22);
        if (isDarkTheme) plusIcon.setTint(Color.GRAY);
        moreBt.setImageIcon(plusIcon);
        moreBt.setTooltipText("More");
        moreBt.setBackgroundColor(Color.TRANSPARENT);
        return moreBt;
    }

    private static LinearLayout createHeader(FragmentMain fragmentMain, String profileImageUrl, String sourceName, DbHandler.Source source, String timeAgo, Context context, int profileSizePx, float density, boolean isDarkTheme) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        header.setGravity(Gravity.CENTER_VERTICAL);

        //Profile image
        ImageView profile = new ImageView(context);
        LinearLayout.LayoutParams profileLp = new LinearLayout.LayoutParams(profileSizePx, profileSizePx);
        profileLp.setMargins(0, 0, (int) (10 * density), 0);
        profile.setLayoutParams(profileLp);
        profile.setScaleType(ImageView.ScaleType.FIT_CENTER);
        profile.setOnClickListener(v -> {
            if (source != null) fragmentMain.showFeed(source, false, false, null);
        });

        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            GlideApp.with(context).load(profileImageUrl).circleCrop().into(profile);
        } else {
            profile.setImageResource(R.drawable.baseline_person_24);
        }

        //Header Texts
        LinearLayout headerText = new LinearLayout(context);
        headerText.setOrientation(LinearLayout.VERTICAL);
        headerText.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView sourceTv = new TextView(context);
        sourceTv.setText(sourceName);
        sourceTv.setMaxLines(1);
        sourceTv.setEllipsize(TextUtils.TruncateAt.END);
        sourceTv.setPadding(0, 0, 45, 0);
        sourceTv.setOnClickListener(v -> {
            if (source != null) fragmentMain.showFeed(source, false, false, null);
        });
        sourceTv.setTypeface(Typeface.DEFAULT_BOLD);
        sourceTv.setTextSize(17);

        TextView timeTv = new TextView(context);
        timeTv.setText(timeAgo);
        timeTv.setTextSize(13);
        if (isDarkTheme) {
            timeTv.setTextColor(0xFF888888);
        } else {
            timeTv.setTextColor(0xFF777777);
        }

        headerText.addView(sourceTv);
        headerText.addView(timeTv);

        header.addView(profile);
        header.addView(headerText);
        return header;
    }

    private static ImageView getPostImage(List<String> images, Context context, float density) {
        if (images != null && !images.isEmpty()) {
            ImageView postImage = new ImageView(context);
            LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            imageLp.setMargins(0, (int) (8 * density), 0, (int) (8 * density));
            postImage.setLayoutParams(imageLp);
            postImage.setAdjustViewBounds(true);
            postImage.setScaleType(ImageView.ScaleType.FIT_CENTER);

            GlideApp.with(context).load(images.get(0)).into(postImage);

            return postImage;
        }
        return null;
    }

    @OptIn(markerClass = UnstableApi.class)
    private static PlayerView getPlayerView(List<String> videos, List<String> audios, Context context, float density) {
        if ((videos == null || videos.isEmpty()) && (audios == null || audios.isEmpty()))
            return null;

        PlayerView playerView = new PlayerView(context);
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        1500,
                        3000,
                        250,
                        500)
                .build();
        ExoPlayer player = new ExoPlayer.Builder(context).setLoadControl(loadControl).build();
        LinearLayout.LayoutParams videoLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (250 * density));
        videoLp.setMargins(0, (int) (8 * density), 0, (int) (8 * density));
        LinearLayout.LayoutParams audioLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (230 * density));
        audioLp.setMargins(0, (int) (8 * density), 0, (int) (8 * density));
        if (videos != null && !videos.isEmpty()) {
            playerView.setPlayer(player);
            playerView.setLayoutParams(videoLp);
            playerView.setShowNextButton(false);
            playerView.setShowPreviousButton(false);
            playerView.setFullscreenButtonClickListener(isFullscreen -> {
                if (isFullscreen && player.getCurrentMediaItem() != null
                        && player.getCurrentMediaItem().localConfiguration != null) {
                    player.stop();

                    Intent fullscreenIntent = new Intent(context, FullscreenVideoActivity.class);
                    fullscreenIntent.putExtra("uri", player.getCurrentMediaItem().localConfiguration.uri.toString());
                    fullscreenIntent.putExtra("pos", player.getCurrentPosition());
                    context.startActivity(fullscreenIntent);

                    playerView.setFullscreenButtonState(false);
                }
            });
            playerView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View v) {
                }

                @Override
                public void onViewDetachedFromWindow(@NonNull View v) {
                    player.stop();
                }
            });
            Uri uri = Uri.parse(videos.get(0));
            MediaItem mediaItem = MediaItem.fromUri(uri);
            MediaSource mediaSource = new ProgressiveMediaSource.
                    Factory(MediaHandler.cacheDataSourceFactory).createMediaSource(mediaItem);
            player.setMediaSource(mediaSource);
            player.addListener(new ExoPlayer.Listener() {
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == ExoPlayer.STATE_READY && player.getPlayWhenReady()) {
                        playerView.hideController();
                    }
                }

                @Override
                public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                    if (playWhenReady && player.getPlaybackState() == ExoPlayer.STATE_READY) {
                        playerView.hideController();
                    }
                }
            });
            player.prepare();

            return playerView;
        } else if (audios != null && !audios.isEmpty()) {
            playerView.setPlayer(player);
            playerView.setLayoutParams(audioLp);
            playerView.setShowNextButton(false);
            playerView.setShowPreviousButton(false);
            playerView.setImageDisplayMode(IMAGE_DISPLAY_MODE_FIT);
            playerView.setControllerShowTimeoutMs(0);
            playerView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(@NonNull View v) {
                }

                @Override
                public void onViewDetachedFromWindow(@NonNull View v) {
                    player.stop();
                    player.release();
                }
            });
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(audios.get(0)));
            MediaSource mediaSource = new ProgressiveMediaSource.
                    Factory(MediaHandler.cacheDataSourceFactory).createMediaSource(mediaItem);
            player.setMediaSource(mediaSource);
            player.prepare();

            return playerView;
        }
        return null;
    }

    private static LinearLayout createActionRow(DbHandler.Article article, Context context, float density, boolean isDarkTheme) {
        LinearLayout actionRow = new LinearLayout(context);
        actionRow.setOrientation(LinearLayout.HORIZONTAL);
        actionRow.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        actionRow.setGravity(Gravity.CENTER_HORIZONTAL);
        actionRow.setPadding(0, (int) (10 * density), 0, (int) (10 * density));

        ImageButton likeBt = new ImageButton(context);
        final boolean[] liked = {article.liked == 1};
        likeBt.setOnClickListener(v -> {
            liked[0] = !liked[0];
            if (liked[0]) {
                db.likeArticle(article.id, article.source);
            } else {
                db.dislikeArticle(article.id, article.source);
            }
            Icon loveIcon = Icon.createWithResource(context, liked[0] ?
                    R.drawable.baseline_favorite_22 : R.drawable.outline_favorite_22);
            if (isDarkTheme && !liked[0]) loveIcon.setTint(Color.GRAY);
            likeBt.setImageIcon(loveIcon);
        });
        Icon loveIcon = Icon.createWithResource(context, liked[0] ?
                R.drawable.baseline_favorite_22 : R.drawable.outline_favorite_22);
        if (isDarkTheme && !liked[0]) loveIcon.setTint(Color.GRAY);
        likeBt.setImageIcon(loveIcon);
        likeBt.setTooltipText(context.getString(R.string.like_tooltip));
        likeBt.setBackgroundColor(Color.TRANSPARENT);
        likeBt.setPadding((int) (20 * density), 0, (int) (20 * density), 0);

        ImageButton openBt = new ImageButton(context);
        openBt.setOnClickListener(v -> {
            db.openArticle(article.id);

            if (article.link == null) {
                Toast.makeText(context, R.string.no_link_available, Toast.LENGTH_SHORT).show();
            } else {
                openLink(context, article.link);
            }
        });

        Icon openIcon = Icon.createWithResource(context, R.drawable.outline_exit_to_app_22);
        if (isDarkTheme) openIcon.setTint(Color.GRAY);
        openBt.setImageIcon(openIcon);
        openBt.setTooltipText(context.getString(R.string.open_tooltip));
        openBt.setBackgroundColor(Color.TRANSPARENT);
        openBt.setPadding((int) (20 * density), 0, (int) (20 * density), 0);

        ImageButton shareBt = new ImageButton(context);
        shareBt.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, article.link);

            Intent chooser = Intent.createChooser(share, context.getString(R.string.share_link));
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(chooser);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(context, R.string.no_app_available_to_share, Toast.LENGTH_SHORT).show();
            }
        });

        Icon shareIcon = Icon.createWithResource(context, R.drawable.outline_share_22);
        if (isDarkTheme) shareIcon.setTint(Color.GRAY);
        shareBt.setImageIcon(shareIcon);
        shareBt.setTooltipText(context.getString(R.string.share_tooltip));
        shareBt.setBackgroundColor(Color.TRANSPARENT);
        shareBt.setPadding((int) (20 * density), 0, (int) (20 * density), 0);

        actionRow.addView(likeBt);
        actionRow.addView(openBt);
        actionRow.addView(shareBt);
        return actionRow;
    }

    @SuppressLint("ClickableViewAccessibility")
    private static TextView createTitleTv(String text, DbHandler.Article article, Context context, float density) {
        TextView titleTv = new TextView(context);
        Spanned spanned = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY);
        Spannable spannable = new SpannableString(spanned);

        titleTv.setText(spannable);
        titleTv.setMaxLines(spanned.length() > 150 ? ARTICLES_INITIAL_LINE_COUNT : ARTICLES_INITIAL_LINE_COUNT_2);
        titleTv.setEllipsize(TextUtils.TruncateAt.END);

        final boolean[] expanded = {false};
        titleTv.setOnTouchListener(new View.OnTouchListener() {
            private final int TOUCH_SLOP = ViewConfiguration.get(titleTv.getContext()).getScaledTouchSlop();
            private final int TAP_TIMEOUT = ViewConfiguration.getTapTimeout() * 2;
            private float downX, downY;
            private long downTime;

            //AI comments kept for clarity (maybe)
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event == null) return false;

                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        downY = event.getY();
                        downTime = System.currentTimeMillis();
                        return true; // consume to receive subsequent events

                    case MotionEvent.ACTION_MOVE:
                        float dx = Math.abs(event.getX() - downX);
                        float dy = Math.abs(event.getY() - downY);
                        // moved too far — not a click
                        return !(dx > TOUCH_SLOP) && !(dy > TOUCH_SLOP);

                    case MotionEvent.ACTION_UP:
                        long dt = System.currentTimeMillis() - downTime;
                        float upX = event.getX();
                        float upY = event.getY();
                        if (dt <= TAP_TIMEOUT &&
                                Math.abs(upX - downX) <= TOUCH_SLOP &&
                                Math.abs(upY - downY) <= TOUCH_SLOP) {
                            // It's a click
                            Layout layout = titleTv.getLayout();
                            int line = layout.getLineForVertical((int) upY);
                            int off = layout.getOffsetForHorizontal(line, (int) upX);

                            ClickableSpan[] link = spannable.getSpans(off, off, ClickableSpan.class);

                            if (link.length > 0) {
                                link[0].onClick(titleTv);
                            } else {
                                expanded[0] = !expanded[0];
                                titleTv.setMaxLines(expanded[0] ? Integer.MAX_VALUE :
                                        (spanned.length() > 150 ? ARTICLES_INITIAL_LINE_COUNT :
                                                ARTICLES_INITIAL_LINE_COUNT_2));
                                return true;
                            }
                        }
                        return false;

                    case MotionEvent.ACTION_CANCEL:
                        // clear state
                        return false;
                }
                return false;
            }


        });
        titleTv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        titleTv.setPadding(0, (int) (6 * density), 0, (int) (6 * density));
        titleTv.setTextSize(16);
        if (article.hidden == 1) titleTv.setTypeface(titleTv.getTypeface(), Typeface.ITALIC);
        return titleTv;
    }

    private static void openLink(Context context, String uriString) {
        if (context == null || uriString == null) return;

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        boolean isTor = false;
        if (Utils.isOnionAddress(uriString)) {
            intent.setPackage("org.torproject.torbrowser");
            isTor = true;
        }
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            if (isTor) {
                try {
                    Intent newIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
                    newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(newIntent);
                } catch (ActivityNotFoundException e2) {
                    Toast.makeText(context, R.string.no_app_available_to_open_the_link, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, R.string.no_app_available_to_open_the_link, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
