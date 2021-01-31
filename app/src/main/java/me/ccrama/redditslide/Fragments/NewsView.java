package me.ccrama.redditslide.Fragments;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import net.dean.jraw.models.Submission;

import java.util.List;

import me.ccrama.redditslide.Activities.BaseActivity;
import me.ccrama.redditslide.Activities.MainActivity;
import me.ccrama.redditslide.Adapters.SubmissionDisplay;
import me.ccrama.redditslide.Adapters.SubmissionNewsAdapter;
import me.ccrama.redditslide.Adapters.SubredditPosts;
import me.ccrama.redditslide.Constants;
import me.ccrama.redditslide.HasSeen;
import me.ccrama.redditslide.R;
import me.ccrama.redditslide.Reddit;
import me.ccrama.redditslide.SettingValues;
import me.ccrama.redditslide.Views.CatchStaggeredGridLayoutManager;
import me.ccrama.redditslide.handler.ToolbarScrollHideHandler;

public class NewsView extends Fragment implements SubmissionDisplay {
    private static int                 adapterPosition;
    private static int                 currentPosition;
    public         SubredditPosts posts;
    public         RecyclerView        rv;
    public         SubmissionNewsAdapter   adapter;
    public         String              id;
    public         boolean             main;
    public         boolean             forced;
    int     diff;
    boolean forceLoad;
    private FloatingActionButton fab;
    private int visibleItemCount;
    private int pastVisiblesItems;
    private int totalItemCount;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private static Submission currentSubmission;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        final int currentOrientation = newConfig.orientation;

        final CatchStaggeredGridLayoutManager mLayoutManager =
                (CatchStaggeredGridLayoutManager) rv.getLayoutManager();

        mLayoutManager.setSpanCount(getNumColumns(currentOrientation, getActivity()));
    }

    Runnable mLongPressRunnable;
    GestureDetector detector =
            new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener());
    float origY;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = SharedView.onCreateView(inflater, container,
                id,
                mSwipeRefreshLayout,
                rv, true, fab,
                getActivity(), getContext(), getResources(),
                mLongPressRunnable, detector, origY,
                adapter
        );

        header = getActivity().findViewById(R.id.header);
nk=
        //TODO, have it so that if the user clicks anywhere in the rv to hide and cancel GoToSubreddit?
//        final TextInputEditText GO_TO_SUB_FIELD = (TextInputEditText) getActivity().findViewById(R.id.toolbar_search);
//        final Toolbar TOOLBAR = ((Toolbar) getActivity().findViewById(R.id.toolbar));
//        final String PREV_TITLE = TOOLBAR.getTitle().toString();
//        final ImageView CLOSE_BUTTON = (ImageView) getActivity().findViewById(R.id.close);
//
//        rv.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                System.out.println("touched");
//                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
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

        resetScroll();

        Reddit.isLoading = false;
        if (MainActivity.shouldLoad == null
                || id == null
                || (MainActivity.shouldLoad != null
                && MainActivity.shouldLoad.equals(id))
                || !(getActivity() instanceof MainActivity)) {
            doAdapter();
        }
        return view;
    }

    View header;

    ToolbarScrollHideHandler toolbarScroll;

    @NonNull
    public static RecyclerView.LayoutManager createLayoutManager(final int numColumns) {
        return new CatchStaggeredGridLayoutManager(numColumns,
                CatchStaggeredGridLayoutManager.VERTICAL);
    }

    public static int getNumColumns(final int orientation, Activity context) {
        final int numColumns;
        boolean singleColumnMultiWindow = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            singleColumnMultiWindow =
                    context.isInMultiWindowMode() && SettingValues.singleColumnMultiWindow;
        }
        if (orientation == Configuration.ORIENTATION_LANDSCAPE
                && SettingValues.isPro
                && !singleColumnMultiWindow) {
            numColumns = Reddit.dpWidth;
        } else if (orientation == Configuration.ORIENTATION_PORTRAIT
                && SettingValues.dualPortrait) {
            numColumns = 2;
        } else {
            numColumns = 1;
        }
        return numColumns;
    }

    public void doAdapter() {
        if (!MainActivity.isRestart) {
            mSwipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(true);
                }
            });
        }

        posts = new SubredditPosts(id, getContext());
        adapter = new SubmissionNewsAdapter(getActivity(), posts, rv, id, this);
        adapter.setHasStableIds(true);
        rv.setAdapter(adapter);
        posts.loadMore(getActivity(), this, true);
        mSwipeRefreshLayout.setOnRefreshListener(this::refresh);
    }

    public void doAdapter(boolean force18) {
        mSwipeRefreshLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeRefreshLayout.setRefreshing(true);
            }
        });

        posts = new SubredditPosts(id, getContext(), force18);
        adapter = new SubmissionNewsAdapter(getActivity(), posts, rv, id, this);
        adapter.setHasStableIds(true);
        rv.setAdapter(adapter);
        posts.loadMore(getActivity(), this, true);
        mSwipeRefreshLayout.setOnRefreshListener(this::refresh);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = this.getArguments();
        id = bundle.getString("id", "");
        main = bundle.getBoolean("main", false);
        forceLoad = bundle.getBoolean("load", false);

    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null && adapterPosition > 0 && currentPosition == adapterPosition) {
            if (adapter.dataSet.getPosts().size() >= adapterPosition - 1
                    && adapter.dataSet.getPosts().get(adapterPosition - 1) == currentSubmission) {
                adapter.performClick(adapterPosition);
                adapterPosition = -1;
            }
        }
    }


    private void refresh() {
        posts.forced = true;
        forced = true;
        posts.loadMore(mSwipeRefreshLayout.getContext(), this, true, id);
    }

    @Override
    public void updateSuccess(final List<Submission> submissions, final int startIndex) {
        if (getActivity() != null) {
            if (getActivity() instanceof MainActivity) {
                if (((MainActivity) getActivity()).runAfterLoad != null) {
                    new Handler().post(((MainActivity) getActivity()).runAfterLoad);
                }
            }
            getActivity().runOnUiThread(new Runnable() {
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
            if (startIndex < 10) resetScroll();
        }
    }

    @Override
    public void updateOffline(List<Submission> submissions, final long cacheTime) {
        if (getActivity() instanceof MainActivity) {
            if (((MainActivity) getActivity()).runAfterLoad != null) {
                new Handler().post(((MainActivity) getActivity()).runAfterLoad);
            }
        }
        if (this.isAdded()) {
            if (mSwipeRefreshLayout != null) {
                mSwipeRefreshLayout.setRefreshing(false);
            }
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void updateOfflineError() {
        if (getActivity() instanceof MainActivity) {
            if (((MainActivity) getActivity()).runAfterLoad != null) {
                new Handler().post(((MainActivity) getActivity()).runAfterLoad);
            }
        }
        mSwipeRefreshLayout.setRefreshing(false);
        adapter.setError(true);
    }

    @Override
    public void updateError() {
        if (getActivity() instanceof MainActivity) {
            if (((MainActivity) getActivity()).runAfterLoad != null) {
                new Handler().post(((MainActivity) getActivity()).runAfterLoad);
            }
        }
        mSwipeRefreshLayout.setRefreshing(false);
        adapter.setError(true);
    }

    @Override
    public void updateViews() {
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

    @Override
    public void onAdapterUpdated() {
        adapter.notifyDataSetChanged();
    }

    public void resetScroll() {
        if (toolbarScroll == null) {
            toolbarScroll =
                    new ToolbarScrollHideHandler(((BaseActivity) getActivity()).mToolbar, header) {
                        @Override
                        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                            super.onScrolled(recyclerView, dx, dy);

                            if (!posts.loading
                                    && !posts.nomore
                                    && !posts.offline
                                    && !adapter.isError) {
                                visibleItemCount = rv.getLayoutManager().getChildCount();
                                totalItemCount = rv.getLayoutManager().getItemCount();

                                int[] firstVisibleItems = ((CatchStaggeredGridLayoutManager) rv.getLayoutManager()).findFirstVisibleItemPositions(
                                        null);
                                if (firstVisibleItems != null && firstVisibleItems.length > 0) {
                                    for (int firstVisibleItem : firstVisibleItems) {
                                        pastVisiblesItems = firstVisibleItem;
                                        if (SettingValues.scrollSeen
                                                && pastVisiblesItems > 0
                                                && SettingValues.storeHistory) {
                                            HasSeen.addSeenScrolling(
                                                    posts.posts.get(pastVisiblesItems - 1)
                                                            .getFullName());
                                        }
                                    }
                                }

                                if ((visibleItemCount + pastVisiblesItems) + 5 >= totalItemCount) {
                                    posts.loading = true;
                                    posts.loadMore(mSwipeRefreshLayout.getContext(), NewsView.this,
                                            false, posts.subreddit);
                                }
                            }

                /*
                if(dy <= 0 && !down){
                    (getActivity()).findViewById(R.id.header).animate().translationY(((BaseActivity)getActivity()).mToolbar.getTop()).setInterpolator(new AccelerateInterpolator()).start();
                    down = true;
                } else if(down){
                    (getActivity()).findViewById(R.id.header).animate().translationY(((BaseActivity)getActivity()).mToolbar.getTop()).setInterpolator(new AccelerateInterpolator()).start();
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
                                    fab.hide();
                                }
                            }

                        }

                        @Override
                        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
//                switch (newState) {
//                    case RecyclerView.SCROLL_STATE_IDLE:
//                        ((Reddit)getActivity().getApplicationContext()).getImageLoader().resume();
//                        break;
//                    case RecyclerView.SCROLL_STATE_DRAGGING:
//                        ((Reddit)getActivity().getApplicationContext()).getImageLoader().resume();
//                        break;
//                    case RecyclerView.SCROLL_STATE_SETTLING:
//                        ((Reddit)getActivity().getApplicationContext()).getImageLoader().pause();
//                        break;
//                }
                            super.onScrollStateChanged(recyclerView, newState);
                            //If the toolbar search is open, and the user scrolls in the Main view--close the search UI
                            if (getActivity() instanceof MainActivity
                                    && (SettingValues.subredditSearchMethod
                                    == Constants.SUBREDDIT_SEARCH_METHOD_TOOLBAR
                                    || SettingValues.subredditSearchMethod
                                    == Constants.SUBREDDIT_SEARCH_METHOD_BOTH)
                                    && ((MainActivity) getContext()).findViewById(
                                    R.id.toolbar_search).getVisibility() == View.VISIBLE) {
                                ((MainActivity) getContext()).findViewById(
                                        R.id.close_search_toolbar).performClick();
                            }
                        }
                    };
            rv.addOnScrollListener(toolbarScroll);
        } else {
            toolbarScroll.reset = true;
        }
    }

    public static void currentPosition(int adapterPosition) {
        currentPosition = adapterPosition;
    }
}
