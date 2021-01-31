package me.ccrama.redditslide.Fragments;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.core.view.MarginLayoutParamsCompat;
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator;
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

import me.ccrama.redditslide.Activities.MainActivity;
import me.ccrama.redditslide.Activities.Search;
import me.ccrama.redditslide.Activities.Submit;
import me.ccrama.redditslide.Activities.SubredditView;
import me.ccrama.redditslide.Adapters.BaseAdapter;
import me.ccrama.redditslide.Adapters.SubmissionAdapter;
import me.ccrama.redditslide.Adapters.SubmissionNewsAdapter;
import me.ccrama.redditslide.ColorPreferences;
import me.ccrama.redditslide.Constants;
import me.ccrama.redditslide.HasSeen;
import me.ccrama.redditslide.Hidden;
import me.ccrama.redditslide.OfflineSubreddit;
import me.ccrama.redditslide.R;
import me.ccrama.redditslide.Reddit;
import me.ccrama.redditslide.SettingValues;
import me.ccrama.redditslide.Views.CatchStaggeredGridLayoutManager;
import me.ccrama.redditslide.Views.CreateCardView;
import me.ccrama.redditslide.Visuals.Palette;

public class SharedView {

    public static View onCreateView(LayoutInflater inflater, ViewGroup container,
                                    String id,
                                    SwipeRefreshLayout mSwipeRefreshLayout,
                                    RecyclerView rv, boolean checkForFabSearch, FloatingActionButton fab,
                                    Activity activity, Context context, Resources resources,
                                    Runnable mLongPressRunnable, GestureDetector detector, float origY,
                                    SubmissionNewsAdapter submissionNewsAdapter) {

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
            }
            else if (checkForFabSearch && SettingValues.fabType == Constants.FAB_SEARCH) {
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
            }
            else {
                fab.setImageResource(R.drawable.hide);
                fab.setContentDescription(context.getResources().getString(R.string.btn_fab_hide));
                RecyclerView finalRv1 = rv;
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

                                                }
                                            })
                                    .show();
                        } else {
                            clearSeenPosts(false, null, submissionNewsAdapter, activity, id, finalRv1);
                        }
                    }
                });
                final Handler handler = new Handler();
                Runnable finalMLongPressRunnable = mLongPressRunnable;
                final float[] finalOrigY = {origY};
                FloatingActionButton finalFab1 = fab;
                fab.setOnTouchListener(new View.OnTouchListener() {

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        detector.onTouchEvent(event);
                        if (event.getAction() == MotionEvent.ACTION_DOWN) {
                            finalOrigY[0] = event.getY();
                            handler.postDelayed(finalMLongPressRunnable, android.view.ViewConfiguration.getLongPressTimeout());
                        }
                        if (((event.getAction() == MotionEvent.ACTION_MOVE) && Math.abs(event.getY() - origY) > finalFab1.getHeight() / 2.0f) || (event.getAction() == MotionEvent.ACTION_UP)) {
                            handler.removeCallbacks(finalMLongPressRunnable);
                        }
                        return false;
                    }
                });
                RecyclerView finalRv = rv;
                FloatingActionButton finalFab = fab;
                mLongPressRunnable = new Runnable() {
                    public void run() {
                        finalFab.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
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
                                                    clearSeenPosts(true, null, submissionNewsAdapter, activity, id, finalRv);

                                                }
                                            })
                                    .show();
                        } else {
                            clearSeenPosts(true,null,  submissionNewsAdapter, activity, id, finalRv);
                        }
                        Snackbar s = Snackbar.make(finalRv,
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

    public static List<Submission> clearSeenPosts(boolean forever,SubmissionAdapter submissionAdapter, SubmissionNewsAdapter submissionNewsAdapter, Context activity, String id, RecyclerView rv) {
        if (submissionNewsAdapter.dataSet.posts != null) {

            List<Submission> originalDataSetPosts = submissionNewsAdapter.dataSet.posts;
            OfflineSubreddit o =
                    OfflineSubreddit.getSubreddit(id.toLowerCase(Locale.ENGLISH), false, activity);

            for (int i = submissionNewsAdapter.dataSet.posts.size(); i > -1; i--) {
                try {
                    if (HasSeen.getSeen(submissionNewsAdapter.dataSet.posts.get(i))) {
                        if (forever) {
                            Hidden.setHidden(submissionNewsAdapter.dataSet.posts.get(i));
                        }
                        o.clearPost(submissionNewsAdapter.dataSet.posts.get(i));
                        submissionNewsAdapter.dataSet.posts.remove(i);
                        if (submissionNewsAdapter.dataSet.posts.isEmpty()) {
                            submissionNewsAdapter.notifyDataSetChanged();
                        } else {
                            rv.setItemAnimator(new AlphaInAnimator());
                            submissionNewsAdapter.notifyItemRemoved(i + 1);
                        }
                    }
                } catch (IndexOutOfBoundsException e) {
                    //Let the loop reset itself
                }
            }
            submissionNewsAdapter.notifyItemRangeChanged(0, submissionNewsAdapter.dataSet.posts.size());
            o.writeToMemoryNoStorage();
            rv.setItemAnimator(new SlideUpAlphaAnimator().withInterpolator(new LinearOutSlowInInterpolator()));
            return originalDataSetPosts;
        }

        return null;
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

}
