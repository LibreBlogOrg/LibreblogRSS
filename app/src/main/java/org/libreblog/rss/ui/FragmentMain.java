package org.libreblog.rss.ui;

import static org.libreblog.rss.core.ArticlesSorter.NBR_STARS;
import static org.libreblog.rss.core.FeedMaker.DOG_VIEW_ID;
import static org.libreblog.rss.core.SourceCrawler.TIME_TO_REFRESH;
import static org.libreblog.rss.ui.FragmentSettings.ARTICLES_BY_PAGE_REFRESH;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RatingBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import org.libreblog.rss.R;
import org.libreblog.rss.core.DbHandler;
import org.libreblog.rss.core.FeedMaker;
import org.libreblog.rss.core.SourceCrawler;
import org.libreblog.rss.proxy.GlideApp;
import org.libreblog.rss.utils.Settings;
import org.libreblog.rss.utils.Utils;

import java.util.List;
import java.util.Objects;

public class FragmentMain extends Fragment {
    public static final int HOME_BUTTON_ID = 98765;
    private static final int CAROUSEL_BUTTON_SIZE = 60;
    private static final int SOURCE_DESCRIPTION_INITIAL_LINE_COUNT = 5;
    private DbHandler db;
    private SourceCrawler sourceCrawler;
    private Button loadMoreButton;

    public FragmentMain() {
        super(R.layout.fragment_main);
    }

    public static void changeHomeIcon(ImageButton homeButton) {
        if (homeButton != null) homeButton.setImageResource(R.drawable.outline_autorenew_32);
    }

    private static TextView createDescriptionTv(DbHandler.Source source, Context context, float density) {
        TextView descriptionTv = new TextView(context);
        Spanned spanned = Html.fromHtml(source.description, Html.FROM_HTML_MODE_LEGACY);
        Spannable spannable = new SpannableString(spanned);
        descriptionTv.setText(spannable);
        descriptionTv.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        descriptionTv.setPadding(0, (int) (8 * density), 0, (int) (8 * density));
        descriptionTv.setTextSize(18);
        descriptionTv.setMaxLines(SOURCE_DESCRIPTION_INITIAL_LINE_COUNT);
        descriptionTv.setEllipsize(TextUtils.TruncateAt.END);

        final boolean[] expanded = {false};
        descriptionTv.setOnClickListener(v -> {
            descriptionTv.setMaxLines(expanded[0] ? SOURCE_DESCRIPTION_INITIAL_LINE_COUNT : Integer.MAX_VALUE);
            expanded[0] = !expanded[0];
        });
        return descriptionTv;
    }

    private static RelativeLayout getHeaderPlusMore(Context context, LinearLayout header, ImageButton moreBt) {
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

    private static LinearLayout createHeader(DbHandler.Source source, Context context, int profileSizePx, float density) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout headerText = new LinearLayout(context);
        headerText.setOrientation(LinearLayout.VERTICAL);
        headerText.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        if (source.image != null && !source.image.isEmpty()) {
            ImageView profile = new ImageView(context);
            LinearLayout.LayoutParams profileLp = new LinearLayout.LayoutParams(profileSizePx, profileSizePx);
            profileLp.setMargins(0, 0, (int) (10 * density), 0);
            profile.setLayoutParams(profileLp);
            profile.setScaleType(ImageView.ScaleType.FIT_CENTER);

            if (context != null)
                GlideApp.with(context).load(source.image).circleCrop().into(profile);
            header.addView(profile);
        }

        TextView titleTv = new TextView(context);
        titleTv.setText(source.title != null && !source.title.isEmpty() ? source.title : source.name);
        titleTv.setTextSize(21);
        titleTv.setEllipsize(TextUtils.TruncateAt.END);
        titleTv.setMaxLines(1);
        titleTv.setPadding(0, 0, 45, 0);
        titleTv.setTypeface(Typeface.DEFAULT_BOLD);
        headerText.addView(titleTv);

        if (source.title != null && !source.title.isEmpty()) {
            TextView nameTv = new TextView(context);
            nameTv.setText(source.name);
            nameTv.setTextSize(18);
            nameTv.setEllipsize(TextUtils.TruncateAt.END);
            nameTv.setMaxLines(1);
            headerText.addView(nameTv);
        }

        header.addView(headerText);
        return header;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SourceCrawler.OnRefreshListener onRefreshListener = showHomeButton();
        if (db == null) db = new DbHandler(view.getContext());
        if (sourceCrawler == null)
            sourceCrawler = new SourceCrawler(view.getContext(), onRefreshListener);
        FeedMaker.init(view.getContext());

        refreshApp();
    }

    private void refreshApp() {
        SharedPreferences settings = Settings.getSettings(getContext());
        assert settings != null;
        SharedPreferences.Editor editor = settings.edit();

        showSourcesBar();
        showFeed(null, false, false, null);

        long now = System.currentTimeMillis();
        if (settings.getLong("last_refresh", 0) + TIME_TO_REFRESH < now) {
            editor.putLong("last_refresh", now - TIME_TO_REFRESH + 15000);
            editor.apply();
            sourceCrawler.refresh(getContext());
        }
    }

    private SourceCrawler.OnRefreshListener showHomeButton() {
        LinearLayout homeLayout = requireActivity().findViewById(R.id.home_view);
        ImageButton homeButton = getHomeButton();
        homeLayout.addView(homeButton);

        return () -> changeHomeIcon(homeButton);
    }

    private LinearLayout.LayoutParams getCarouselButtonLayout() {
        double density = 2.5;
        if (getContext() != null && getContext().getResources() != null) {
            density = getContext().getResources().getDisplayMetrics().density;
        }
        int profileSizePx = (int) (CAROUSEL_BUTTON_SIZE * density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(profileSizePx, profileSizePx);
        params.setMargins(15, 10, 15, 10);

        return params;
    }

    private void showSourcesBar() {
        LinearLayout sourcesCarousel = requireActivity().findViewById(R.id.sources_carousel);
        sourcesCarousel.removeAllViews();

        ImageButton loveButton = getLoveButton();
        sourcesCarousel.addView(loveButton);

        ImageButton plusButton = getPlusButton();
        sourcesCarousel.addView(plusButton);

        Context context = getContext();
        final float density = context != null ? context.getResources().getDisplayMetrics().density : 2.5f;
        int paddingForSvg = (int) (10 * density);

        List<DbHandler.Source> sources = db.getSources();
        for (DbHandler.Source s : sources) {
            if (s.image != null && !s.image.isEmpty()) {
                ImageButton button = new ImageButton(getContext());
                button.setOnClickListener(v -> showFeed(s, false, false, null));
                button.setOnLongClickListener(v -> openDeleteDialog(s));
                button.setLayoutParams(getCarouselButtonLayout());
                button.setTooltipText(s.name);

                GradientDrawable gd = new GradientDrawable();
                gd.setColor(Color.WHITE);
                gd.setStroke(1, Color.rgb(240, 240, 240));
                button.setBackground(gd);

                if (Utils.isSvg(s.image)) {
                    button.setPadding(paddingForSvg, paddingForSvg, paddingForSvg, paddingForSvg);
                    GlideApp.with(getContext()).load(s.image).into(button);
                } else {
                    button.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                    GlideApp.with(getContext()).load(s.image).circleCrop().into(button);
                }
                sourcesCarousel.addView(button);
            } else {
                Button button = new Button(getContext());
                button.setOnClickListener(v -> showFeed(s, false, false, null));
                button.setOnLongClickListener(v -> openDeleteDialog(s));
                button.setLayoutParams(getCarouselButtonLayout());
                button.setBackgroundColor(Color.WHITE);
                button.setOutlineSpotShadowColor(Color.TRANSPARENT);
                button.setText(Utils.getInitials(s.name));
                button.setTextSize(20);
                button.setTooltipText(s.name);
                sourcesCarousel.addView(button);
            }
        }

        HorizontalScrollView scrollView = requireActivity().findViewById(R.id.sources_carousel_view);
        scrollView.scrollTo(0, 0);
    }

    private boolean openDeleteDialog(DbHandler.Source source) {
        if (source == null) return false;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle(R.string.delete_source_in_dialog);
        builder.setMessage(getString(R.string.do_you_want_to_delete) + source.name +
                getString(R.string.do_you_want_to_delete2));
        builder.setPositiveButton(R.string.ok_in_dialog, (dialog, which) -> {
            db.deleteSource(source.id);
            dialog.dismiss();
            showSourcesBar();
            showFeed(null, false, false, null);
        });
        builder.setNegativeButton(R.string.cancel_in_dialog, (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();

        return true;
    }

    private ImageButton getPlusButton() {
        ImageButton plusButton = new ImageButton(getContext());
        LinearLayout.LayoutParams params = getCarouselButtonLayout();
        plusButton.setLayoutParams(params);
        plusButton.setBackgroundColor(Color.rgb(150, 230, 150));
        plusButton.setOutlineSpotShadowColor(Color.TRANSPARENT);
        plusButton.setImageResource(R.drawable.outline_add_32);

        plusButton.setOnClickListener(v -> {
            FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_view, FragmentAddSource.class, null)
                    .setReorderingAllowed(true)
                    .addToBackStack(getString(R.string.add_source_in_dialog))
                    .commit();
        });
        return plusButton;
    }

    private ImageButton getHomeButton() {
        ImageButton homeButton = new ImageButton(getContext());
        LinearLayout.LayoutParams params = getCarouselButtonLayout();
        homeButton.setLayoutParams(params);
        homeButton.setBackgroundColor(Color.rgb(150, 140, 230));
        homeButton.setOutlineSpotShadowColor(Color.TRANSPARENT);
        homeButton.setImageResource(R.drawable.baseline_home_32);
        homeButton.setId(HOME_BUTTON_ID);

        final long[] refreshing = {System.currentTimeMillis()};
        homeButton.setOnClickListener(v -> {
            homeButton.setImageResource(R.drawable.baseline_home_32);
            if (refreshing[0] > System.currentTimeMillis() - 1000) {
                return;
            }
            refreshApp();
            refreshing[0] = System.currentTimeMillis();
        });
        return homeButton;
    }

    private ImageButton getLoveButton() {
        ImageButton loveButton = new ImageButton(getContext());
        LinearLayout.LayoutParams params = getCarouselButtonLayout();
        loveButton.setLayoutParams(params);
        loveButton.setBackgroundColor(Color.rgb(230, 140, 140));
        loveButton.setOutlineSpotShadowColor(Color.TRANSPARENT);
        loveButton.setImageResource(R.drawable.baseline_favorite_32);

        loveButton.setOnClickListener(v -> showFeed(null, true, false, null));
        return loveButton;
    }

    public void showFeed(DbHandler.Source source, boolean onlyLiked, boolean onlyHidden, String searchText) {
        SharedPreferences settings = Settings.getSettings(getContext());
        assert settings != null;
        int articlesByPageRefresh = settings.getInt("articles_by_page_refresh", ARTICLES_BY_PAGE_REFRESH);

        LinearLayout mainLayout = requireActivity().findViewById(R.id.main_layout);
        mainLayout.removeAllViews();

        if (source != null) {
            mainLayout.addView(createSourceCard(source, onlyHidden));
        }

        List<View> views = FeedMaker.make(this, source, 0, onlyLiked, onlyHidden, searchText);
        if (onlyLiked && (isViewListNotEmpty(views) || (searchText != null && !searchText.isEmpty()))) {
            mainLayout.addView(createSearchBar(searchText != null ? searchText : ""));
        }

        for (View view : views) {
            if (view != null) mainLayout.addView(view);
        }

        if (views.size() == articlesByPageRefresh) {
            addLoadMoreButton(mainLayout, source, articlesByPageRefresh, onlyLiked, onlyHidden, searchText);
        } else {
            mainLayout.removeView(loadMoreButton);
        }

        ScrollView scrollView = requireActivity().findViewById(R.id.main_scroll_view);
        scrollView.scrollTo(0, 0);
    }

    private boolean isViewListNotEmpty(List<View> views) {
        if (views.size() != 1) return true;

        try {
            return views.get(0).findViewById(DOG_VIEW_ID) == null;
        } catch (Exception e) {
            return false;
        }
    }

    private View createSearchBar(String searchText) {
        SearchView searchView = new SearchView(getContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        searchView.setLayoutParams(lp);
        searchView.setIconifiedByDefault(true);
        searchView.setQueryHint(getString(R.string.search));
        searchView.setQuery(searchText, false);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query == null || query.length() < 3) {
                    Toast.makeText(getContext(), R.string.type_at_least_3_characters, Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (!Objects.equals(query, searchText)) showFeed(null, true, false, query);
                return true;
            }
        });
        searchView.setOnCloseListener(() -> {
            if (searchText == null || searchText.isEmpty()) return false;
            showFeed(null, true, false, null);
            return true;
        });

        return searchView;
    }

    private View createSourceCard(DbHandler.Source source, boolean onlyHidden) {
        Context context = getContext();
        final float density = context != null ? context.getResources().getDisplayMetrics().density : 2.5f;
        int padding = (int) (12 * density);
        int profileSizePx = (int) (60 * density);

        LinearLayout card = new LinearLayout(context);
        if (!Utils.isDarkTheme(context)) {
            card.setBackgroundColor(Color.rgb(240, 240, 180));
        }
        card.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(padding, padding, padding, padding * 3);
        card.setLayoutParams(params);
        card.setPadding(padding, padding, padding, padding);

        LinearLayout header = createHeader(source, context, profileSizePx, density);
        ImageButton moreBt = getMoreButton(source, onlyHidden, context, card);
        RelativeLayout headerPlusMore = getHeaderPlusMore(context, header, moreBt);
        card.addView(headerPlusMore);

        if (source.description != null && !source.description.isEmpty()) {
            TextView descriptionTv = createDescriptionTv(source, context, density);
            card.addView(descriptionTv);
        }

        LinearLayout ratingLayout1 = getRatingLayout(source, context);
        card.addView(ratingLayout1);

        return card;
    }

    private LinearLayout getRatingLayout(DbHandler.Source source, Context context) {
        LinearLayout ratingLayout1 = new LinearLayout(context);
        ratingLayout1.setOrientation(LinearLayout.HORIZONTAL);
        ratingLayout1.setGravity(Gravity.CENTER_HORIZONTAL);
        ratingLayout1.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout ratingLayout2 = new LinearLayout(context);
        ratingLayout2.setOrientation(LinearLayout.HORIZONTAL);
        ratingLayout2.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        RatingBar ratingBar = new RatingBar(context);
        ratingBar.setNumStars(NBR_STARS);
        ratingBar.setRating((float) source.score);
        ratingBar.setOnRatingBarChangeListener((rb, r, u) -> {
            if (u) db.setSourceScore(source.id, r);
            source.score = r;
        });

        ratingLayout2.addView(ratingBar);
        ratingLayout1.addView(ratingLayout2);
        return ratingLayout1;
    }

    private ImageButton getMoreButton(DbHandler.Source source, boolean onlyHidden, Context context, LinearLayout card) {
        ImageButton moreBt = new ImageButton(context);
        moreBt.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(card.getContext(), moreBt);

            int MENU_ITEM_EDIT_SOURCE = 1;
            int MENU_ITEM_DELETE_SOURCE = 2;
            int SHOW_HIDDEN_POSTS = 3;

            popup.getMenu().add(Menu.NONE, MENU_ITEM_EDIT_SOURCE, Menu.NONE, R.string.edit_source_in_menu);
            popup.getMenu().add(Menu.NONE, MENU_ITEM_DELETE_SOURCE, Menu.NONE, R.string.delete_source_in_menu);
            popup.getMenu().add(Menu.NONE, SHOW_HIDDEN_POSTS, Menu.NONE, onlyHidden ? getString(R.string.visible_posts_in_menu) : getString(R.string.hidden_posts_in_menu));

            popup.setOnMenuItemClickListener(menuItem -> {
                int id = menuItem.getItemId();
                if (id == MENU_ITEM_EDIT_SOURCE) {
                    Bundle args = new Bundle();
                    args.putString("id", source.id);
                    args.putString("name", source.name);
                    args.putString("image", source.image);
                    FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                    fragmentManager.beginTransaction()
                            .replace(R.id.fragment_container_view, FragmentAddSource.class, args)
                            .setReorderingAllowed(true)
                            .addToBackStack(getString(R.string.edit_source_backstack))
                            .commit();
                    return true;
                } else if (id == MENU_ITEM_DELETE_SOURCE) {
                    openDeleteDialog(source);
                    return true;
                } else if (id == SHOW_HIDDEN_POSTS) {
                    showFeed(source, false, !onlyHidden, null);
                    return true;
                }
                return false;
            });

            popup.show();
        });

        Icon plusIcon = Icon.createWithResource(context, R.drawable.outline_more_vert_22);
        if (Utils.isDarkTheme(context)) plusIcon.setTint(Color.GRAY);
        moreBt.setImageIcon(plusIcon);
        moreBt.setTooltipText("More");
        moreBt.setBackgroundColor(Color.TRANSPARENT);
        return moreBt;
    }

    private void addLoadMoreButton(LinearLayout linearLayout, DbHandler.Source source, int offset, boolean onlyLiked, boolean onlyHidden, String searchText) {
        if (linearLayout == null) return;

        SharedPreferences settings = Settings.getSettings(getContext());
        assert settings != null;
        int articlesByPageRefresh = settings.getInt("articles_by_page_refresh", ARTICLES_BY_PAGE_REFRESH);
        linearLayout.removeView(loadMoreButton);

        loadMoreButton = new Button(getContext());
        loadMoreButton.setOnClickListener(v -> {
            List<View> views = FeedMaker.make(this, source, offset, onlyLiked, onlyHidden, searchText);
            for (View view : views) {
                if (view != null) linearLayout.addView(view);
            }
            if (views.size() == articlesByPageRefresh) {
                addLoadMoreButton(linearLayout, source, offset + articlesByPageRefresh, onlyLiked, onlyHidden, searchText);
            } else {
                linearLayout.removeView(loadMoreButton);
            }
        });
        loadMoreButton.setBackgroundColor(Color.rgb(140, 140, 255));

        double density = 2.5;
        if (getContext() != null && getContext().getResources() != null) {
            density = getContext().getResources().getDisplayMetrics().density;
        }
        loadMoreButton.setPadding((int) (20 * density), 0, (int) (20 * density), 0);
        loadMoreButton.setTextSize(17);
        if (Utils.isDarkTheme(getContext())) {
            loadMoreButton.setTextColor(Color.rgb(50, 50, 50));
        } else {
            loadMoreButton.setTextColor(Color.WHITE);
        }
        loadMoreButton.setText(R.string.load_more);

        linearLayout.addView(loadMoreButton);
    }
}
