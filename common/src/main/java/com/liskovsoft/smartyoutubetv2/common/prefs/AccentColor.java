package com.liskovsoft.smartyoutubetv2.common.prefs;

import android.content.Context;
import android.content.SharedPreferences;
import com.liskovsoft.smartyoutubetv2.common.R;

/**
 * MOD (Inge): accentkleur voor de voortgangsbalk in de speler, de "bekeken"-lijn onder tegels,
 * vinkjes en schakelaars. Vervangt de oude kleurenschema's (die doen niets meer in de YouTube-stijl).
 * Wordt direct toegepast, zonder herstart.
 */
public final class AccentColor {
    private static final String PREFS = "inge_accent";
    private static final String KEY = "accent_index";

    public static final int[] NAMES = {
            R.string.accent_red, R.string.accent_blue, R.string.accent_teal, R.string.accent_green,
            R.string.accent_purple, R.string.accent_pink, R.string.accent_orange, R.string.accent_yellow,
            R.string.accent_white
    };

    public static final int[] COLORS = {
            0xFFFF0033, 0xFF3EA6FF, 0xFF00BFA5, 0xFF2BA640,
            0xFFA970FF, 0xFFFF4FA3, 0xFFFF8A00, 0xFFFFC400,
            0xFFF1F1F1
    };

    private static int sIndex = -1;

    private AccentColor() {
    }

    public static int getIndex(Context context) {
        if (sIndex < 0) {
            sIndex = prefs(context).getInt(KEY, 0);
            if (sIndex < 0 || sIndex >= COLORS.length) {
                sIndex = 0;
            }
        }
        return sIndex;
    }

    public static int get(Context context) {
        return COLORS[getIndex(context)];
    }

    public static void setIndex(Context context, int index) {
        if (index < 0 || index >= COLORS.length) {
            return;
        }
        sIndex = index;
        prefs(context).edit().putInt(KEY, index).apply(); // direct bewaren (niet pas na 10 s)
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
