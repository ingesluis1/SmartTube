package com.liskovsoft.smartyoutubetv2.tv.ui.widgets.browse;

import android.content.Context;
import android.os.Build.VERSION;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * MOD (Inge): smalle strook met sectie-iconen links in beeld, zoals de YouTube-app.
 * Niet bedienbaar: naar links drukken opent het volledige menu (Leanback headers).
 */
public class NavStripView extends LinearLayout {
    public static class Item {
        public final long id;
        public final int resId;
        public final String iconUrl;

        public Item(long id, int resId, String iconUrl) {
            this.id = id;
            this.resId = resId;
            this.iconUrl = iconUrl;
        }
    }

    private static final float UNSELECTED_ALPHA = 0.55f;
    private final List<Item> mItems = new ArrayList<>();
    private long mSelectedId = -1;
    private int mIconSizePx;
    private int mMaxPitchPx;
    private int mMinPitchPx;

    public NavStripView(Context context) {
        super(context);
        init();
    }

    public NavStripView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public NavStripView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setFocusable(false);
        setFocusableInTouchMode(false);
        float density = getResources().getDisplayMetrics().density;
        mIconSizePx = Math.round(24 * density);
        mMaxPitchPx = Math.round(39 * density); // YouTube: 39dp tussen de iconen
        mMinPitchPx = Math.round(30 * density);
    }

    public void setItems(List<Item> items) {
        mItems.clear();
        if (items != null) {
            mItems.addAll(items);
        }
        removeAllViews();
        for (Item item : mItems) {
            ImageView icon = new ImageView(getContext());
            icon.setFocusable(false);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setTag(item.id);
            LayoutParams lp = new LayoutParams(mIconSizePx, mMaxPitchPx);
            lp.gravity = android.view.Gravity.CENTER_HORIZONTAL;
            icon.setLayoutParams(lp);
            icon.setPadding(0, (mMaxPitchPx - mIconSizePx) / 2, 0, (mMaxPitchPx - mIconSizePx) / 2);
            if (item.iconUrl != null) {
                Glide.with(getContext()).load(item.iconUrl).apply(ViewUtil.glideOptions()).circleCrop().into(icon);
            } else if (item.resId > 0) {
                icon.setImageResource(item.resId);
                if (VERSION.SDK_INT >= 21) {
                    icon.setImageTintList(ContextCompat.getColorStateList(getContext(), R.color.nav_strip_icon));
                }
            }
            addView(icon);
        }
        applySelection();
        requestLayout();
    }

    public void setSelectedId(long id) {
        mSelectedId = id;
        applySelection();
        requestLayout(); // zichtbaarheid kan veranderen
    }

    private void applySelection() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            boolean selected = child.getTag() instanceof Long && (Long) child.getTag() == mSelectedId;
            child.setSelected(selected);
            child.setAlpha(selected ? 1f : UNSELECTED_ALPHA);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Vaste afstand zoals YouTube (39dp). Passen niet alle secties, dan vallen de middelste weg;
        // de eerste secties en de laatste (Instellingen) blijven staan, net als de geselecteerde.
        int height = MeasureSpec.getSize(heightMeasureSpec);
        int count = getChildCount();
        if (height > 0 && count > 0) {
            int available = height - getPaddingTop() - getPaddingBottom();
            int maxCount = Math.max(1, available / mMaxPitchPx);
            int selectedIndex = -1;
            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                if (child.getTag() instanceof Long && (Long) child.getTag() == mSelectedId) {
                    selectedIndex = i;
                }
            }
            for (int i = 0; i < count; i++) {
                boolean visible;
                if (count <= maxCount) {
                    visible = true;
                } else if (i == count - 1) {
                    visible = true; // laatste (Instellingen)
                } else if (selectedIndex >= maxCount - 1 && selectedIndex < count - 1) {
                    // geselecteerde sectie zit in het verborgen deel: toon hem op de plek van de laatste zichtbare
                    visible = i < maxCount - 2 || i == selectedIndex;
                } else {
                    visible = i < maxCount - 1;
                }
                View child = getChildAt(i);
                child.setVisibility(visible ? View.VISIBLE : View.GONE);
                LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.height != mMaxPitchPx) {
                    lp.height = mMaxPitchPx;
                    int pad = Math.max(0, (mMaxPitchPx - mIconSizePx) / 2);
                    child.setPadding(0, pad, 0, pad);
                    child.setLayoutParams(lp);
                }
            }
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
