package com.liskovsoft.smartyoutubetv2.common.prefs;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * MOD (Inge): achtergrond kleurt zacht mee met de geselecteerde video of met de profielfoto.
 */
public final class BackgroundTint {
    public static final int MODE_OFF = 0;
    public static final int MODE_VIDEO = 1;
    public static final int MODE_PROFILE = 2;
    public static final int MODE_ACCENT = 3;

    private static final String PREFS = "inge_bgtint";
    private static final String KEY = "mode";
    private static int sMode = -1;

    private BackgroundTint() {
    }

    public static int getMode(Context context) {
        if (sMode < 0) {
            sMode = prefs(context).getInt(KEY, MODE_OFF);
        }
        return sMode;
    }

    public static void setMode(Context context, int mode) {
        sMode = mode;
        prefs(context).edit().putInt(KEY, mode).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
