package com.liskovsoft.smartyoutubetv2.tv.ui.browse.settings;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.ClassPresenterSelector;
import androidx.leanback.widget.VerticalGridView;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.BrowsePresenter;
import com.liskovsoft.smartyoutubetv2.common.prefs.GeneralData;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.SimpleEditDialog;
import com.liskovsoft.smartyoutubetv2.common.utils.Utils;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.presenter.SettingsGroupHeaderPresenter;
import com.liskovsoft.smartyoutubetv2.tv.presenter.SettingsRowPresenter;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.interfaces.SettingsSection;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.video.GridFragmentHelper;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.LeanbackActivity;
import com.liskovsoft.smartyoutubetv2.tv.ui.common.UriBackgroundManager;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.fragments.GridFragment;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SettingsGridFragment extends GridFragment implements SettingsSection {
    private static final String TAG = SettingsGridFragment.class.getSimpleName();
    private ArrayObjectAdapter mSettingsAdapter;
    private BrowsePresenter mMainPresenter;
    private UriBackgroundManager mBackgroundManager;
    private final List<SettingsGroup> mPendingUpdates = new ArrayList<>();
    // MOD (Inge): uitlegvak rechts
    private ImageView mDetailIcon;
    private TextView mDetailTitle;
    private TextView mDetailDesc;
    private View mDetailPane;
    private SettingsEntry mPendingDetail;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMainPresenter = BrowsePresenter.instance(getContext());
        mBackgroundManager = ((LeanbackActivity) getActivity()).getBackgroundManager();

        setupAdapter();
        setupEventListeners();
        applyPendingUpdates();

        if (getMainFragmentAdapter().getFragmentHost() != null) {
            getMainFragmentAdapter().getFragmentHost().notifyDataReady(getMainFragmentAdapter());
        }
    }

    @Override
    protected void showOrHideTitle() {
        // MOD (Inge): geen zoekbalk/profielbalk op de instellingenpagina, zoals de YouTube-app
        if (getMainFragmentAdapter().getFragmentHost() != null) {
            getMainFragmentAdapter().getFragmentHost().showTitleView(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        showOrHideTitle();
    }

    private void applyPendingUpdates() {
        for (SettingsGroup group : mPendingUpdates) {
            update(group);
        }

        mPendingUpdates.clear();
    }

    private void setupEventListeners() {
        setOnItemViewClickedListener(new ItemViewClickedListener());
        // MOD (Inge): uitlegvak volgt de selectie
        setOnItemViewSelectedListener((itemViewHolder, item, rowViewHolder, row) -> {
            if (item instanceof SettingsEntry) {
                showDetail((SettingsEntry) item);
            }
        });
    }

    private void setupAdapter() {
        // MOD (Inge): één kolom, lijst zoals de YouTube-app
        VerticalGridPresenter presenter = new VerticalGridPresenter(ViewUtil.FOCUS_ZOOM_FACTOR, false);
        presenter.setShadowEnabled(false);
        presenter.setNumberOfColumns(1);
        setGridPresenter(presenter);

        if (mSettingsAdapter == null) {
            ClassPresenterSelector selector = new ClassPresenterSelector();
            selector.addClassPresenter(SettingsEntry.class, new SettingsRowPresenter());
            selector.addClassPresenter(SettingsGroupHeader.class, new SettingsGroupHeaderPresenter());
            mSettingsAdapter = new ArrayObjectAdapter(selector);
            setAdapter(mSettingsAdapter);
        }
    }

    // MOD (Inge): titel vast bovenaan, lijst eronder (niet meer onder de zoekbalk door), uitlegvak rechts
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewGroup root = (ViewGroup) view;
        View dock = view.findViewById(R.id.browse_grid_dock);
        int listTop = getResources().getDimensionPixelSize(R.dimen.settings_list_top);
        ViewGroup.MarginLayoutParams dockParams = (ViewGroup.MarginLayoutParams) dock.getLayoutParams();
        dockParams.topMargin = listTop;
        dock.setLayoutParams(dockParams);

        VerticalGridView grid = getBrowseGrid();
        int left = 0;
        if (grid != null) {
            left = grid.getPaddingLeft();
            grid.setPadding(left, dp(2), grid.getPaddingRight(), dp(40));
            grid.setClipToPadding(true);
            grid.setVerticalSpacing(dp(4));
            grid.setHorizontalSpacing(0);
        }

        TextView title = new TextView(getContext());
        title.setText(R.string.settings_page_title);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        title.setTextColor(0xFFF1F1F1);
        FrameLayout.LayoutParams tl = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tl.leftMargin = left + dp(14);
        tl.topMargin = getResources().getDimensionPixelSize(R.dimen.settings_title_top);
        root.addView(title, tl);

        root.addView(createDetailPane(), createDetailParams(left, listTop));

        if (mPendingDetail != null) {
            showDetail(mPendingDetail);
        }
    }

    private View createDetailPane() {
        LinearLayout pane = new LinearLayout(getContext());
        pane.setOrientation(LinearLayout.VERTICAL);
        pane.setGravity(Gravity.CENTER_HORIZONTAL);
        pane.setFocusable(false);

        FrameLayout circle = new FrameLayout(getContext());
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(0xFF272727);
        circle.setBackground(bg);
        mDetailIcon = new ImageView(getContext());
        circle.addView(mDetailIcon, new FrameLayout.LayoutParams(dp(72), dp(72), Gravity.CENTER));
        pane.addView(circle, new LinearLayout.LayoutParams(dp(150), dp(150)));

        mDetailTitle = new TextView(getContext());
        mDetailTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        mDetailTitle.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        mDetailTitle.setTextColor(0xFFF1F1F1);
        mDetailTitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ttl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ttl.topMargin = dp(26);
        pane.addView(mDetailTitle, ttl);

        mDetailDesc = new TextView(getContext());
        mDetailDesc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        mDetailDesc.setTextColor(0xFFAAAAAA);
        mDetailDesc.setGravity(Gravity.CENTER);
        mDetailDesc.setLineSpacing(0, 1.15f);
        mDetailDesc.setMaxWidth(dp(400));
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dl.topMargin = dp(10);
        pane.addView(mDetailDesc, dl);

        TextView hint = new TextView(getContext());
        hint.setText(R.string.settings_open_hint);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        hint.setTextColor(0xFF717171);
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hl.topMargin = dp(28);
        pane.addView(hint, hl);

        mDetailPane = pane;
        pane.setVisibility(View.INVISIBLE);
        return pane;
    }

    private FrameLayout.LayoutParams createDetailParams(int left, int listTop) {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.leftMargin = left + getResources().getDimensionPixelSize(R.dimen.settings_row_width) + dp(40);
        params.rightMargin = dp(40);
        params.topMargin = listTop + dp(10);
        return params;
    }

    private void showDetail(SettingsEntry entry) {
        if (mDetailPane == null) {
            mPendingDetail = entry;
            return;
        }
        mPendingDetail = null;
        mDetailPane.setVisibility(View.VISIBLE);
        mDetailIcon.setImageResource(entry.iconResId);
        mDetailTitle.setText(entry.item.title);
        if (entry.descResId > 0) {
            mDetailDesc.setText(entry.descResId);
            mDetailDesc.setVisibility(View.VISIBLE);
        } else {
            mDetailDesc.setVisibility(View.GONE);
        }
    }

    private void requestInlineDialog() {
        VerticalGridView grid = getBrowseGrid();
        int left = grid != null ? grid.getPaddingLeft() : dp(78);
        com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.AppDialogFragment.requestInline(
                left + getResources().getDimensionPixelSize(R.dimen.settings_row_width) + dp(24));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void clear() {
        if (mSettingsAdapter != null) {
            mSettingsAdapter.clear();
        }
    }

    @Override
    public boolean isEmpty() {
        if (mSettingsAdapter == null) {
            return mPendingUpdates.isEmpty();
        }

        return mSettingsAdapter.size() == 0;
    }

    @Override
    public void update(SettingsGroup group) {
        if (mSettingsAdapter == null) {
            mPendingUpdates.add(group);
            return;
        }

        // Always clear (continuation not supported)
        clear();

        if (group != null) {
            addGrouped(group.getItems());
        }
    }

    // MOD (Inge): groepen zoals de YouTube-app, met eigen dunne iconen en een korte omschrijving
    private static final int[][] GROUPS = {
            {R.string.settings_group_account, R.drawable.settings_account, R.drawable.settings_cast},
            {R.string.settings_group_app, R.drawable.settings_language, R.drawable.settings_app, R.drawable.settings_main_ui, R.drawable.settings_search},
            {R.string.settings_group_playback, R.drawable.settings_player, R.drawable.settings_afr, R.drawable.settings_subtitles},
            {R.string.settings_group_addons, R.drawable.settings_block, R.drawable.settings_dearrow},
            {R.string.settings_group_more, R.drawable.settings_backup, R.drawable.settings_about},
    };

    private static int[] lookup(int imageResId) {
        if (imageResId == R.drawable.settings_account) return new int[] {R.drawable.ic_set_account, R.string.settings_desc_accounts};
        if (imageResId == R.drawable.settings_cast) return new int[] {R.drawable.ic_set_cast, R.string.settings_desc_remote};
        if (imageResId == R.drawable.settings_language) return new int[] {R.drawable.ic_set_language, R.string.settings_desc_language};
        if (imageResId == R.drawable.settings_app) return new int[] {R.drawable.ic_set_general, R.string.settings_desc_general};
        if (imageResId == R.drawable.settings_main_ui) return new int[] {R.drawable.ic_set_ui, R.string.settings_desc_ui};
        if (imageResId == R.drawable.settings_player) return new int[] {R.drawable.ic_set_player, R.string.settings_desc_player};
        if (imageResId == R.drawable.settings_afr) return new int[] {R.drawable.ic_set_afr, R.string.settings_desc_afr};
        if (imageResId == R.drawable.settings_subtitles) return new int[] {R.drawable.ic_set_subtitles, R.string.settings_desc_subtitles};
        if (imageResId == R.drawable.settings_search) return new int[] {R.drawable.ic_set_search, R.string.settings_desc_search};
        if (imageResId == R.drawable.settings_block) return new int[] {R.drawable.ic_set_sponsorblock, R.string.settings_desc_sponsorblock};
        if (imageResId == R.drawable.settings_dearrow) return new int[] {R.drawable.ic_set_dearrow, R.string.settings_desc_dearrow};
        if (imageResId == R.drawable.settings_backup) return new int[] {R.drawable.ic_set_backup, R.string.settings_desc_backup};
        if (imageResId == R.drawable.settings_about) return new int[] {R.drawable.ic_set_about, R.string.settings_desc_about};
        return new int[] {R.drawable.ic_set_general, 0};
    }

    private void addGrouped(List<SettingsItem> items) {
        Map<Integer, SettingsItem> byIcon = new LinkedHashMap<>();
        List<SettingsItem> rest = new ArrayList<>();
        for (SettingsItem item : items) {
            if (item.imageResId > 0 && !byIcon.containsKey(item.imageResId)) {
                byIcon.put(item.imageResId, item);
            } else {
                rest.add(item);
            }
        }

        int firstEntry = -1;
        for (int[] group : GROUPS) {
            boolean headerAdded = false;
            for (int i = 1; i < group.length; i++) {
                SettingsItem item = byIcon.remove(group[i]);
                if (item == null) {
                    continue;
                }
                if (!headerAdded) {
                    mSettingsAdapter.add(new SettingsGroupHeader(getContext().getString(group[0])));
                    headerAdded = true;
                }
                if (firstEntry < 0) {
                    firstEntry = mSettingsAdapter.size();
                }
                int[] info = lookup(item.imageResId);
                mSettingsAdapter.add(new SettingsEntry(item, info[0], info[1]));
            }
        }

        // Onbekende (nieuwe) items van upstream onderaan, zodat er niets verdwijnt
        rest.addAll(byIcon.values());
        for (SettingsItem item : rest) {
            int[] info = lookup(item.imageResId);
            mSettingsAdapter.add(new SettingsEntry(item, info[0], info[1]));
        }

        if (firstEntry >= 0) {
            if (getBrowseGrid() != null) {
                getBrowseGrid().setSelectedPosition(firstEntry);
            }
            showDetail((SettingsEntry) mSettingsAdapter.get(firstEntry));
        }
    }

    private MainUIData getMainUIData() {
        return MainUIData.instance(getContext());
    }

    private GeneralData getGeneralData() {
        return GeneralData.instance(getContext());
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object clickedItem,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            // MOD (Inge): items zitten nu in een SettingsEntry; kopjes doen niets
            final Object item = clickedItem instanceof SettingsEntry ? ((SettingsEntry) clickedItem).item : clickedItem;

            if (item instanceof SettingsItem) {
                // MOD (Inge): opties openen rechts naast de lijst
                requestInlineDialog();
                String password = getGeneralData().getSettingsPassword();

                if (password == null) {
                    ((SettingsItem) item).onClick.run();
                } else {
                    SimpleEditDialog.showPassword(
                            getContext(),
                            getContext().getString(R.string.enter_settings_password),
                            null,
                            newValue -> {
                                if (Utils.passwordMatch(password, newValue)) {
                                    ((SettingsItem) item).onClick.run();
                                    return true;
                                }
                                return false;
                            });
                }

                // Close PIP inside Settings section
                //if (PlaybackPresenter.instance(getContext()).isInPipMode()) {
                //    PlaybackPresenter.instance(getContext()).forceFinish();
                //}
            } else if (!(item instanceof SettingsGroupHeader)) {
                Toast.makeText(getContext(), item.toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}
