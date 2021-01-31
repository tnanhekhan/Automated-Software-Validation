package me.ccrama.redditslide.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.view.MarginLayoutParamsCompat;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mikepenz.itemanimators.AlphaInAnimator;
import com.mikepenz.itemanimators.SlideUpAlphaAnimator;

import net.dean.jraw.models.Submission;

import java.util.List;
import java.util.Locale;

import me.ccrama.redditslide.Activities.BaseActivity;
import me.ccrama.redditslide.Activities.MainActivity;
import me.ccrama.redditslide.Activities.Search;
import me.ccrama.redditslide.Activities.Submit;
import me.ccrama.redditslide.Activities.SubredditView;
import me.ccrama.redditslide.Adapters.BaseAdapter;
import me.ccrama.redditslide.Adapters.CommentAdapter;
import me.ccrama.redditslide.Adapters.SubmissionAdapter;
import me.ccrama.redditslide.Adapters.SubredditPosts;
import me.ccrama.redditslide.ColorPreferences;
import me.ccrama.redditslide.Constants;
import me.ccrama.redditslide.ForceTouch.PeekView;
import me.ccrama.redditslide.HasSeen;
import me.ccrama.redditslide.Hidden;
import me.ccrama.redditslide.OfflineSubreddit;
import me.ccrama.redditslide.R;
import me.ccrama.redditslide.Reddit;
import me.ccrama.redditslide.SettingValues;
import me.ccrama.redditslide.Views.CatchStaggeredGridLayoutManager;
import me.ccrama.redditslide.Views.CreateCardView;
import me.ccrama.redditslide.Visuals.Palette;
import me.ccrama.redditslide.handler.ToolbarScrollHideHandler;

import static me.ccrama.redditslide.Hidden.id;

public class SharedView {

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState, String id, SwipeRefreshLayout mSwipeRefreshLayout,
                             RecyclerView rv, boolean checkForFabSearch, FloatingActionButton fab,
                             Activity activity, Context context, Resources resources, Runnable mLongPressRunnable, Activity detector, float origY, SubredditPosts posts, CommentAdapter adapter, ToolbarScrollHideHandler toolbarScroll) {

        final Context contextThemeWrapper = new ContextThemeWrapper(activity,
                new ColorPreferences(inflater.getContext()).getThemeSubreddit(id));
        final View v = LayoutInflater.from(contextThemeWrapper)
                .inflate(R.layout.fragment_verticalcontent, container, false);

        if (activity instanceof MainActivity) {
            v.findViewById(R.id.back).setBackgroundResource(0);
        }
        rv = v.findViewById(R.id.vertical_content);

        rv.setHasFixedSize(true);

        final RecyclerView.LayoutManager mLayoutManager =
                createLayoutManager(getNumColumns(resources.getConfiguration().orientation, activity));

        if (!(activity instanceof SubredditView)) {
            v.findViewById(R.id.back).setBackground(null);
        }
        rv.setLayoutManager(mLayoutManager);
        rv.setItemAnimator(new SlideUpAlphaAnimator().withInterpolator(new LinearOutSlowInInterpolator()));
        rv.getLayoutManager().scrollToPosition(0);

        mSwipeRefreshLayout = v.findViewById(R.id.activity_main_swipe_refresh_layout);
        mSwipeRefreshLayout.setColorSchemeColors(Palette.getColors(id, context));

        /**
         * If using List view mode, we need to remove the start margin from the SwipeRefreshLayout.
         * The scrollbar style of "outsideInset" creates a 4dp padding around it. To counter this,
         * change the scrollbar style to "insideOverlay" when list view is enabled.
         * To recap: this removes the margins from the start/end so list view is full-width.
         */
        if (SettingValues.defaultCardView == CreateCardView.CardEnum.LIST) {
            RelativeLayout.LayoutParams params =
                    new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT);
            MarginLayoutParamsCompat.setMarginStart(params, 0);
            rv.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
            mSwipeRefreshLayout.setLayoutParams(params);
        }

        /**
         * If we use 'findViewById(R.id.header).getMeasuredHeight()', 0 is always returned.
         * So, we estimate the height of the header in dp.
         * If the view type is "single" (and therefore "commentPager"), we need a different offset
         */
        final int HEADER_OFFSET = (SettingValues.single || activity instanceof SubredditView)
                ? Constants.SINGLE_HEADER_VIEW_OFFSET : Constants.TAB_HEADER_VIEW_OFFSET;

        mSwipeRefreshLayout.setProgressViewOffset(false, HEADER_OFFSET - Constants.PTR_OFFSET_TOP,
                HEADER_OFFSET + Constants.PTR_OFFSET_BOTTOM);

        if (SettingValues.fab) {
            fab = v.findViewById(R.id.post_floating_action_button);

            if (SettingValues.fabType == Constants.FAB_POST) {
                fab.setImageResource(R.drawable.add);
                fab.setContentDescription(context.getResources().getString(R.string.btn_fab_post));
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent inte = new Intent(activity, Submit.class);
                        inte.putExtra(Submit.EXTRA_SUBREDDIT, id);
                        activity.startActivity(inte);
                    }
                });
            } else if (checkForFabSearch && SettingValues.fabType == Constants.FAB_SEARCH) {
                fab.setImageResource(R.drawable.search);
                fab.setContentDescription(context.getResources().getString(R.string.btn_fab_search));
                fab.setOnClickListener(new View.OnClickListener() {
                    String term;

                    @Override
                    public void onClick(View v) {
                        MaterialDialog.Builder builder = new MaterialDialog.Builder(activity)
                                .title(R.string.search_title)
                                .alwaysCallInputCallback()
                                .input(context.getResources().getString(R.string.search_msg), "",
                                        new MaterialDialog.InputCallback() {
                                            @Override
                                            public void onInput(MaterialDialog materialDialog,
                                                                CharSequence charSequence) {
                                                term = charSequence.toString();
                                            }
                                        });

                        //Add "search current sub" if it is not frontpage/all/random
                        if (!id.equalsIgnoreCase("frontpage")
                                && !id.equalsIgnoreCase("all")
                                && !id.contains(".")
                                && !id.contains("/m/")
                                && !id.equalsIgnoreCase("friends")
                                && !id.equalsIgnoreCase("random")
                                && !id.equalsIgnoreCase("popular")
                                && !id.equalsIgnoreCase("myrandom")
                                && !id.equalsIgnoreCase("randnsfw")) {
                            builder.positiveText(context.getResources().getString(R.string.search_subreddit, id))
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog materialDialog,
                                                            @NonNull DialogAction dialogAction) {
                                            Intent i = new Intent(activity, Search.class);
                                            i.putExtra(Search.EXTRA_TERM, term);
                                            i.putExtra(Search.EXTRA_SUBREDDIT, id);
                                            context.startActivity(i);
                                        }
                                    });
                            builder.neutralText(R.string.search_all)
                                    .onNeutral(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog materialDialog,
                                                            @NonNull DialogAction dialogAction) {
                                            Intent i = new Intent(activity, Search.class);
                                            i.putExtra(Search.EXTRA_TERM, term);
                                            context.startActivity(i);
                                        }
                                    });
                        } else {
                            builder.positiveText(R.string.search_all)
                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                        @Override
                                        public void onClick(@NonNull MaterialDialog materialDialog,
                                                            @NonNull DialogAction dialogAction) {
                                            Intent i = new Intent(activity, Search.class);
                                            i.putExtra(Search.EXTRA_TERM, term);
                                            context.startActivity(i);
                                        }
                                    });
                        }
                        builder.show();
                    }
                });
            } else {
                fab.setImageResource(R.drawable.hide);
                fab.setContentDescription(context.getResources().getString(R.string.btn_fab_hide));
                fab.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!Reddit.fabClear) {
                            new AlertDialogWrapper.Builder(activity).setTitle(
                                    R.string.settings_fabclear)
                                    .setMessage(R.string.settings_fabclear_msg)
                                    .setPositiveButton(R.string.btn_ok,
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog,
                                                                    int which) {
                                                    Reddit.colors.edit()
                                                            .putBoolean(
                                                                    SettingValues.PREF_FAB_CLEAR,
                                                                    true)
                                                            .apply();
                                                    Reddit.fabClear = true;
                                                    clearSeenPosts(false, adapter, activity);

                                                }
                                            })
                                    .show();
                        } else {
                            clearSeenPosts(false, adapter, activity);
                        }
                    }
                });
                final Handler handler = new Handler();
                fab.setOnTouchListener(new View.OnTouchListener() {

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        detector.onTouchEvent(event);
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            origY = event.getY();
                            handler.postDelayed(mLongPressRunnable, android.view.ViewConfiguration.getLongPressTimeout());
                        }
                        if (((event.getAction() == MotionEvent.ACTION_MOVE) && Math.abs(event.getY() - origY) > fab.getHeight() / 2.0f) || (event.getAction() == MotionEvent.ACTION_UP)) {
                            handler.removeCallbacks(mLongPressRunnable);
                        }
                        return false;
                    }
                });
                mLongPressRunnable = new Runnable() {
                    public void run() {
                        fab.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        if (!Reddit.fabClear) {
                            new AlertDialogWrapper.Builder(activity).setTitle(
                                    R.string.settings_fabclear)
                                    .setMessage(R.string.settings_fabclear_msg)
                                    .setPositiveButton(R.string.btn_ok,
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog,
                                                                    int which) {
                                                    Reddit.colors.edit()
                                                            .putBoolean(
                                                                    SettingValues.PREF_FAB_CLEAR,
                                                                    true)
                                                            .apply();
                                                    Reddit.fabClear = true;
                                                    clearSeenPosts(true, adapter, activity);

                                                }
                                            })
                                    .show();
                        } else {
                            clearSeenPosts(true, adapter, activity);
                        }
                        Snackbar s = Snackbar.make(rv,
                                context.getResources().getString(R.string.posts_hidden_forever),
                                Snackbar.LENGTH_LONG);
                       /*Todo a way to unhide
                        s.setAction(R.string.btn_undo, new View.OnClickListener() {

                            @Override
                            public void onClick(View v) {

                            }
                        });*/
                        View view = s.getView();
                        TextView tv = view.findViewById(
                                com.google.android.material.R.id.snackbar_text);
                        tv.setTextColor(Color.WHITE);
                        s.show();
                    }
                };
            }
        } else {
            v.findViewById(R.id.post_floating_action_button).setVisibility(View.GONE);
        }
        if (fab != null) fab.show();

        header = activity.findViewById(R.id.header);

        //TODO, have it so that if the user clicks anywhere in the rv to hide and cancel GoToSubreddit?
//        final TextInputEditText GO_TO_SUB_FIELD = (TextInputEditText) activity.findViewById(R.id.toolbar_search);
//        final Toolbar TOOLBAR = ((Toolbar) activity.findViewById(R.id.toolbar));
//        final String PREV_TITLE = TOOLBAR.getTitle().toString();
//        final ImageView CLOSE_BUTTON = (ImageView) activity.findViewById(R.id.close);
//
//        rv.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                System.out.println("touched");
//                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
//                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
//
//                GO_TO_SUB_FIELD.setText("");
//                GO_TO_SUB_FIELD.setVisibility(View.GONE);
//                CLOSE_BUTTON.setVisibility(View.GONE);
//                TOOLBAR.setTitle(PREV_TITLE);
//
//                return false;
//            }
//        });

        resetScroll(toolbarScroll);

        Reddit.isLoading = false;
        if (MainActivity.shouldLoad == null
                || id == null
                || (MainActivity.shouldLoad != null
                && MainActivity.shouldLoad.equals(id))
                || !(activity instanceof MainActivity)) {
            doAdapter(context, posts, adapter, activity, mSwipeRefreshLayout);
        }
        return v;
    }

    @NonNull
    public static RecyclerView.LayoutManager createLayoutManager(final int numColumns) {
        return new CatchStaggeredGridLayoutManager(numColumns,
                CatchStaggeredGridLayoutManager.VERTICAL);
    }


    public static int getNumColumns(final int orientation, Activity context) {
        final int numColumns;
        boolean singleColumnMultiWindow = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            singleColumnMultiWindow = context.isInMultiWindowMode() && SettingValues.singleColumnMultiWindow;
        }
        if (orientation == Configuration.ORIENTATION_LANDSCAPE && SettingValues.isPro && !singleColumnMultiWindow) {
            numColumns = Reddit.dpWidth;
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT
                && SettingValues.dualPortrait) {
            numColumns = 2;
        } else {
            numColumns = 1;
        }
        return numColumns;
    }


    public void doAdapter(Context context,
                          SubredditPosts posts,
                          SubmissionAdapter adapter,
                          Activity activity,
                          SwipeRefreshLayout mSwipeRefreshLayout) {
        if (!MainActivity.isRestart) {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
        }

        posts = new SubredditPosts(id, context);
        adapter = new SubmissionAdapter(activity, posts, rv, id, this);
        adapter.setHasStableIds(true);
        rv.setAdapter(adapter);
        posts.loadMore(activity, this, true);
        mSwipeRefreshLayout.setOnRefreshListener(() -> refresh(, , mSwipeRefreshLayout));
    }

    public void doAdapter(boolean force18, SwipeRefreshLayout mSwipeRefreshLayout, Context context, String id, SubmissionAdapter adapter, Activity activity, SubredditPosts posts, RecyclerView rv) {
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });

        posts = new SubredditPosts(id, context, force18);
        adapter = new SubmissionAdapter(activity, posts, rv, id, this);
        adapter.setHasStableIds(true);
        rv.setAdapter(adapter);
        posts.loadMore(activity, this, true);
        mSwipeRefreshLayout.setOnRefreshListener(() -> refresh(, , mSwipeRefreshLayout));
    }

    public List<Submission> clearSeenPosts(boolean forever, SubmissionAdapter adapter, Context activity, String id, RecyclerView rv) {
        if (adapter.dataSet.posts != null) {

            List<Submission> originalDataSetPosts = adapter.dataSet.posts;
            OfflineSubreddit o =
                    OfflineSubreddit.getSubreddit(id.toLowerCase(Locale.ENGLISH), false, activity);

            for (int i = adapter.dataSet.posts.size(); i > -1; i--) {
                try {
                    if (HasSeen.getSeen(adapter.dataSet.posts.get(i))) {
                        if (forever) {
                            Hidden.setHidden(adapter.dataSet.posts.get(i));
                        }
                        o.clearPost(adapter.dataSet.posts.get(i));
                        adapter.dataSet.posts.remove(i);
                        if (adapter.dataSet.posts.isEmpty()) {
                            adapter.notifyDataSetChanged();
                        } else {
                            rv.setItemAnimator(new AlphaInAnimator());
                            adapter.notifyItemRemoved(i + 1);
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    //Let the loop reset itself
                }
            }
            adapter.notifyItemRangeChanged(0, adapter.dataSet.posts.size());
            o.writeToMemoryNoStorage();
            rv.setItemAnimator(new SlideUpAlphaAnimator().withInterpolator(new LinearOutSlowInInterpolator()));
            return originalDataSetPosts;
        }

        return null;
    }

    public void onCreate(Bundle savedInstanceState, boolean main, boolean forceLoad) {
//        super.onCreate(savedInstanceState);

        Bundle bundle = this.getArguments();
        id = bundle.getString("id", "");
        main = bundle.getBoolean("main", false);
        forceLoad = bundle.getBoolean("load", false);

    }

    public void onResume(boolean adapterPosition, Object currentSubmission, boolean adapter, boolean currentPosition) {
//        super.onResume();
        if (adapter != null && adapterPosition > 0 && currentPosition == adapterPosition) {
            if (adapter.dataSet.getPosts().size() >= adapterPosition - 1
                    && adapter.dataSet.getPosts().get(adapterPosition - 1) == currentSubmission) {
                adapter.performClick(adapterPosition);
                adapterPosition = -1;
            }
        }
    }
    private void refresh(boolean forced, SubredditPosts posts, Object mSwipeRefreshLayout) {
        posts.forced = true;
        forced = true;
        posts.loadMore(mSwipeRefreshLayout.context, this, true, id);
    }

    public void forceRefresh(ToolbarScrollHideHandler toolbarScroll, OrientationHelper rv, SwipeRefreshLayout mSwipeRefreshLayout, boolean forced, SubredditPosts posts) {
        toolbarScroll.toolbarShow();
        rv.getLayoutManager().scrollToPosition(0);
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
                refresh(forced, posts, mSwipeRefreshLayout);
            }
        });
        mSwipeRefreshLayout.setRefreshing(false);
    }

    public void updateSuccess(List<Submission> submissions, int startIndex, SwipeRefreshLayout mSwipeRefreshLayout, Activity activity, boolean forced, RecyclerView rv, SubmissionsView posts, SubmissionAdapter adapter, ToolbarScrollHideHandler toolbarScroll) {
        if (activity != null) {
            if (activity instanceof MainActivity) {
                if (((MainActivity) activity).runAfterLoad != null) {
                    new Handler().post(((MainActivity) activity).runAfterLoad);
                }
            }
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mSwipeRefreshLayout != null) {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    if (startIndex != -1 && !forced) {
                        adapter.notifyItemRangeInserted(startIndex + 1, posts.posts.size());
                    } else {
                        forced = false;
                        rv.scrollToPosition(0);
                    }
                    adapter.notifyDataSetChanged();

                }
            });

            if (MainActivity.isRestart) {
                MainActivity.isRestart = false;
                posts.offline = false;
                rv.getLayoutManager().scrollToPosition(MainActivity.restartPage + 1);
            }
            if (startIndex < 10) resetScroll(toolbarScroll);
        }
    }

    public void updateOffline(List<Submission> submissions, long cacheTime, SwipeRefreshLayout mSwipeRefreshLayout, ArrayAdapter<Object> adapter, Object activity) {
        if (activity instanceof MainActivity) {
            if (((MainActivity) activity).runAfterLoad != null) {
                new Handler().post(((MainActivity) activity).runAfterLoad);
            }
        }
        if (this.isAdded()) {
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
            adapter.notifyDataSetChanged();
        }
    }

    public void updateOfflineError(Object activity, SwipeRefreshLayout mSwipeRefreshLayout, BaseAdapter adapter) {
        if (activity instanceof MainActivity) {
            if (((MainActivity) activity).runAfterLoad != null) {
                new Handler().post(((MainActivity) activity).runAfterLoad);
            }
        }
        mSwipeRefreshLayout.setRefreshing(false);
        adapter.setError(true);
    }

    public void updateError(Object activity, SwipeRefreshLayout mSwipeRefreshLayout) {
        if (activity instanceof MainActivity) {
            if (((MainActivity) activity).runAfterLoad != null) {
                new Handler().post(((MainActivity) activity).runAfterLoad);
            }
        }
        mSwipeRefreshLayout.setRefreshing(false);
    }

    public void updateViews(CommentAdapter adapter) {
        if (adapter.dataSet.posts != null) {
            for (int i = adapter.dataSet.posts.size(); i > -1; i--) {
                try {
                    if (HasSeen.getSeen(adapter.dataSet.posts.get(i))) {
                        adapter.notifyItemChanged(i + 1);
                    }
                } catch (IndexOutOfBoundsException e) {
                    //Let the loop reset itself
                }
            }
        }
    }

    public void onAdapterUpdated(ArrayAdapter<Object> adapter) {
        adapter.notifyDataSetChanged();
    }

    public void resetScroll(ToolbarScrollHideHandler toolbarScroll, Object activity, View header, RecyclerView rv) {
        if (toolbarScroll == null) {
            toolbarScroll =
                    new ToolbarScrollHideHandler(((BaseActivity) activity).mToolbar, header) {
                        public void onScrolled(RecyclerView recyclerView, int dx, int dy, SubmissionAdapter adapter, SubredditPosts posts, OrientationHelper rv, int visibleItemCount, int totalItemCount, int pastVisiblesItems, Object mSwipeRefreshLayout, int diff, PeekView fab) {
                            super.onScrolled(recyclerView, dx, dy);

                            if (!posts.loading && !posts.nomore && !posts.offline && !adapter.isError) {
                                visibleItemCount = rv.getLayoutManager().getChildCount();
                                totalItemCount = rv.getLayoutManager().getItemCount();

                                int[] firstVisibleItems =
                                        ((CatchStaggeredGridLayoutManager) rv.getLayoutManager()).findFirstVisibleItemPositions(
                                                null);
                                if (firstVisibleItems != null && firstVisibleItems.length > 0) {
                                    for (int firstVisibleItem : firstVisibleItems) {
                                        pastVisiblesItems = firstVisibleItem;
                                        if (SettingValues.scrollSeen
                                                && pastVisiblesItems > 0
                                                && SettingValues.storeHistory) {
                                            HasSeen.addSeenScrolling(posts.posts.get(pastVisiblesItems - 1)
                                                    .getFullName());
                                        }
                                    }
                                }

                                if ((visibleItemCount + pastVisiblesItems) + 5 >= totalItemCount) {
                                    posts.loading = true;
                                    posts.loadMore(mSwipeRefreshLayout.context,
                                            SubmissionsView.this, false, posts.subreddit);
                                }
                            }

                /*
                if(dy <= 0 && !down){
                    (activity).findViewById(R.id.header).animate().translationY(((BaseActivity)activity).mToolbar.getTop()).setInterpolator(new AccelerateInterpolator()).start();
                    down = true;
                } else if(down){
                    (activity).findViewById(R.id.header).animate().translationY(((BaseActivity)activity).mToolbar.getTop()).setInterpolator(new AccelerateInterpolator()).start();
                    down = false;
                }*///todo For future implementation instead of scrollFlags

                            if (recyclerView.getScrollState()
                                    == RecyclerView.SCROLL_STATE_DRAGGING) {
                                diff += dy;
                            } else {
                                diff = 0;
                            }
                            if (fab != null) {
                                if (dy <= 0 && fab.getId() != 0 && SettingValues.fab) {
                                    if (recyclerView.getScrollState()
                                            != RecyclerView.SCROLL_STATE_DRAGGING
                                            || diff < -fab.getHeight() * 2) {
                                        fab.show();
                                    }
                                } else {
                                    if (!SettingValues.alwaysShowFAB) {
                                        fab.hide();
                                    }
                                }
                            }

                        }

                        @Override
                        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
//                switch (newState) {
//                    case RecyclerView.SCROLL_STATE_IDLE:
//                        ((Reddit)activity.getApplicationContext()).getImageLoader().resume();
//                        break;
//                    case RecyclerView.SCROLL_STATE_DRAGGING:
//                        ((Reddit)activity.getApplicationContext()).getImageLoader().resume();
//                        break;
//                    case RecyclerView.SCROLL_STATE_SETTLING:
//                        ((Reddit)activity.getApplicationContext()).getImageLoader().pause();
//                        break;
//                }
                            super.onScrollStateChanged(recyclerView, newState);
                            //If the toolbar search is open, and the user scrolls in the Main view--close the search UI
                            if (activity instanceof MainActivity
                                    && (SettingValues.subredditSearchMethod
                                    == Constants.SUBREDDIT_SEARCH_METHOD_TOOLBAR
                                    || SettingValues.subredditSearchMethod
                                    == Constants.SUBREDDIT_SEARCH_METHOD_BOTH)
                                    && ((MainActivity) context).findViewById(
                                    R.id.toolbar_search).getVisibility() == View.VISIBLE) {
                                ((MainActivity) context).findViewById(
                                        R.id.close_search_toolbar).performClick();
                            }
                        }
                    };
            rv.addOnScrollListener(toolbarScroll);
        } else {
            toolbarScroll.reset = true;
        }
    }

    public static void currentPosition(int adapterPosition, int currentPosition) {
        currentPosition = adapterPosition;
    }

    public static void currentSubmission(Submission current, Submission currentSubmission) {
        currentSubmission = current;
    }

}
