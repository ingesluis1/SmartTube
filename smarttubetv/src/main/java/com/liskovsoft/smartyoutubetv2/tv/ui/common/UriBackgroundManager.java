package com.liskovsoft.smartyoutubetv2.tv.ui.common;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.leanback.app.BackgroundManager;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;
import com.liskovsoft.smartyoutubetv2.tv.util.ColorExtract;
import com.liskovsoft.smartyoutubetv2.common.prefs.BackgroundTint;

public class UriBackgroundManager {
    private static final String TAG = UriBackgroundManager.class.getSimpleName();
    private static final int BACKGROUND_UPDATE_DELAY_MS = 300;
    private Uri mBackgroundURI;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Runnable mBackgroundTask;
    private BackgroundManager mBackgroundManager;
    private final Activity mActivity;
    private final Handler mHandler;
    private int mBackgroundColor = -1;

    public UriBackgroundManager(Activity activity) {
        mActivity = activity;
        mHandler = new Handler();
        prepareBackgroundManager();
        setDefaultBackground();
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(mActivity);
        mBackgroundManager.attach(mActivity.getWindow());
        mDefaultBackground = ContextCompat.getDrawable(mActivity, Helpers.getThemeAttr(mActivity, R.attr.shelfBackground));
        mBackgroundTask = new UpdateBackgroundTask();
        mMetrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void startBackgroundTimer(Uri backgroundURI) {
        mBackgroundURI = backgroundURI;
        mHandler.removeCallbacks(mBackgroundTask);
        mHandler.postDelayed(mBackgroundTask, BACKGROUND_UPDATE_DELAY_MS);
    }

    public void startBackgroundTimer(String bgImageUrl) {
        if (bgImageUrl != null) {
            startBackgroundTimer(Uri.parse(bgImageUrl));
        }
    }

    public void setBackgroundFrom(Video item) {
        // Selectively change background picture
        // MOD (Inge): achtergrond kleurt zacht mee met de geselecteerde video
        if (item == null || BackgroundTint.getMode(mActivity) != BackgroundTint.MODE_VIDEO) {
            return;
        }
        mPendingTintUrl = item.cardImageUrl;
        mHandler.removeCallbacks(mTintTask);
        mHandler.postDelayed(mTintTask, TINT_DELAY_MS);
    }

    public void onStart() {
        if (mBackgroundURI != null) {
            showBackground(mBackgroundURI.toString());
        } else if (mBackgroundColor != -1) {
            setBackgroundColor(mBackgroundColor);
        } else if (BackgroundTint.getMode(mActivity) != BackgroundTint.MODE_OFF && mTintColor != 0) {
            applyTint(mTintColor); // MOD (Inge)
        } else {
            setDefaultBackground();
        }
    }

    // MOD (Inge): achtergrondkleur uit video of profielfoto
    private static final int TINT_DELAY_MS = 350;
    private static final int TINT_SIZE = 48;
    private String mPendingTintUrl;
    private String mTintUrl;
    private int mTintColor;
    private String mProfileUrl;
    private int mProfileColor;
    private final Runnable mTintTask = () -> loadTint(mPendingTintUrl);

    /**
     * Achtergrond volgens de gekozen stand: uit = standaard, profiel = kleur van de profielfoto,
     * video = neutraal tot er een video geselecteerd wordt.
     */
    public void applyModeBackground() {
        mHandler.removeCallbacks(mTintTask);
        int mode = BackgroundTint.getMode(mActivity);
        if (mode == BackgroundTint.MODE_PROFILE) {
            applyProfileTint();
        } else if (mode == BackgroundTint.MODE_ACCENT) {
            // gedempte versie van de accentkleur
            float[] hsl = new float[3];
            androidx.core.graphics.ColorUtils.colorToHSL(com.liskovsoft.smartyoutubetv2.common.prefs.AccentColor.get(mActivity), hsl);
            hsl[2] = Math.max(0.22f, Math.min(0.38f, hsl[2]));
            applyTint(androidx.core.graphics.ColorUtils.HSLToColor(hsl));
        } else {
            mTintUrl = null;
            mTintColor = 0;
            if (mBackgroundManager != null) {
                setDefaultBackground();
            }
        }
    }

    private void applyProfileTint() {
        String url = null;
        try {
            java.util.List<com.liskovsoft.mediaserviceinterfaces.oauth.Account> accounts =
                    com.liskovsoft.youtubeapi.service.YouTubeServiceManager.instance().getSignInService().getAccounts();
            if (accounts != null) {
                for (com.liskovsoft.mediaserviceinterfaces.oauth.Account account : accounts) {
                    if (account.isSelected()) {
                        url = account.getAvatarImageUrl();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // NOP
        }
        if (url == null) {
            mTintColor = 0;
            setDefaultBackground();
            return;
        }
        if (url.equals(mProfileUrl) && mProfileColor != 0) {
            applyTint(mProfileColor);
            return;
        }
        final String profileUrl = url;
        Glide.with(mActivity)
                .asBitmap()
                .load(profileUrl)
                .apply(ViewUtil.glideOptions())
                .into(new SimpleTarget<Bitmap>(TINT_SIZE, TINT_SIZE) {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                        mProfileUrl = profileUrl;
                        mProfileColor = ColorExtract.dominant(resource, 0);
                        if (BackgroundTint.getMode(mActivity) == BackgroundTint.MODE_PROFILE && mProfileColor != 0) {
                            applyTint(mProfileColor);
                        }
                    }
                });
    }

    private void loadTint(String url) {
        if (url == null || url.equals(mTintUrl) || mActivity.isFinishing()) {
            return;
        }
        Glide.with(mActivity)
                .asBitmap()
                .load(url)
                .apply(ViewUtil.glideOptions())
                .into(new SimpleTarget<Bitmap>(TINT_SIZE, TINT_SIZE) {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                        if (!url.equals(mPendingTintUrl) || BackgroundTint.getMode(mActivity) != BackgroundTint.MODE_VIDEO) {
                            return; // inmiddels verder gescrold of uitgezet
                        }
                        mTintUrl = url;
                        int color = ColorExtract.dominant(resource, 0);
                        if (color != 0) {
                            applyTint(color);
                        }
                    }
                });
    }

    /**
     * Donkere basis met rechtsboven een zachte gloed in de gegeven kleur.
     */
    private void applyTint(int color) {
        if (mBackgroundManager == null) {
            return;
        }
        mTintColor = color;
        android.graphics.drawable.GradientDrawable glow = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TR_BL,
                new int[] {withAlpha(color, 0.38f), withAlpha(color, 0.12f), 0x00000000, 0x00000000});
        android.graphics.drawable.LayerDrawable layers = new android.graphics.drawable.LayerDrawable(
                new Drawable[] {new android.graphics.drawable.ColorDrawable(0xFF0F0F0F), glow});
        mBackgroundManager.setDrawable(layers);
    }

    private static int withAlpha(int color, float alpha) {
        return (Math.round(alpha * 255) << 24) | (color & 0x00FFFFFF);
    }

    public void onStop() {
        mBackgroundManager.release();
    }

    public void onDestroy() {
        mHandler.removeCallbacks(mBackgroundTask);
        mBackgroundManager = null;
    }

    public void removeBackground() {
        mBackgroundManager.setDrawable(null);
    }

    public void setDefaultBackground() {
        mBackgroundManager.setDrawable(mDefaultBackground);
    }

    public void setBackgroundColor(int color) {
        mBackgroundColor = color;
        mBackgroundManager.setColor(color);
    }

    private class UpdateBackgroundTask implements Runnable {
        @Override
        public void run() {
            if (mBackgroundURI != null) {
                showBackground(mBackgroundURI.toString());
            }
        }
    }

    public BackgroundManager getBackgroundManager() {
        return mBackgroundManager;
    }

    public void showBackground(String uri) {
        View videoView = mActivity.findViewById(R.id.video_surface);

        if (videoView != null) {
            videoView.setVisibility(uri == null ? View.VISIBLE : View.INVISIBLE);
        }

        if (uri == null) {
            removeBackground();
            return;
        }

        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;

        RequestOptions options = ViewUtil.glideOptions()
                .centerCrop()
                .error(mDefaultBackground);

        Glide.with(mActivity)
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(
                            @NonNull Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        mBackgroundManager.setBitmap(resource);
                    }
                });
    }

    public void showBackgroundColor(int colorResId) {
        View videoView = mActivity.findViewById(R.id.video_surface);

        if (videoView != null) {
            videoView.setVisibility(colorResId == -1 ? View.VISIBLE : View.INVISIBLE);
        }

        if (colorResId == -1) {
            removeBackground();
            return;
        }

        if (mBackgroundManager != null) {
            mBackgroundManager.setColor(ContextCompat.getColor(mActivity, colorResId));
        }
    }
}
