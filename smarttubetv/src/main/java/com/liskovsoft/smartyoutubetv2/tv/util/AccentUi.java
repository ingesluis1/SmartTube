package com.liskovsoft.smartyoutubetv2.tv.util;

import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import androidx.recyclerview.widget.RecyclerView;
import com.liskovsoft.smartyoutubetv2.common.prefs.AccentColor;

/**
 * MOD (Inge): kleurt vinkjes, vakjes en schakelaars in de accentkleur.
 */
public final class AccentUi {
    private static final int[][] CHECK_STATES = {{android.R.attr.state_checked}, {}};

    private AccentUi() {
    }

    public static ColorStateList checkTint(int accent, int uncheckedColor) {
        return new ColorStateList(CHECK_STATES, new int[] {accent, uncheckedColor});
    }

    /**
     * Kleur elk item van de lijst zodra het in beeld komt.
     */
    public static void watch(RecyclerView list) {
        if (list == null) {
            return;
        }
        list.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                tint(view);
            }

            @Override
            public void onChildViewDetachedFromWindow(View view) {
            }
        });
    }

    public static void tint(View view) {
        int accent = AccentColor.get(view.getContext());
        tint(view, accent);
    }

    private static void tint(View view, int accent) {
        if (view instanceof Switch) {
            Switch sw = (Switch) view;
            sw.setThumbTintList(checkTint(accent, 0xFFBDBDBD));
            sw.setTrackTintList(checkTint((accent & 0x00FFFFFF) | 0x80000000, 0x4DFFFFFF));
        } else if (view instanceof CompoundButton) {
            ((CompoundButton) view).setButtonTintList(checkTint(accent, 0xFF8A8A8A));
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                tint(group.getChildAt(i), accent);
            }
        }
    }
}
