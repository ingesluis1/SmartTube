package com.liskovsoft.smartyoutubetv2.tv.ui.profile;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.YTSignInPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.AccountSelectionPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.ProfilePickerView;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;

import java.util.List;

/**
 * MOD (Inge): profielkeuze bij het opstarten, zoals in de YouTube-app.
 * Ronde avatars naast elkaar, naam eronder, "Add account" en "Watch as guest".
 * De achtergrond kleurt mee met de profielfoto die focus heeft (vloeiende overgang).
 */
public class ProfilePickerActivity extends MotherActivity implements ProfilePickerView {
    private static final float UNFOCUSED_ALPHA = 0.55f;
    private static final int BASE_COLOR = 0xFF0F0F0F;
    private static final int NEUTRAL_ACCENT = 0xFF3A3A3A;
    private static final long FADE_MS = 600;

    private GradientDrawable mBackground;
    private int mCurrentAccent = NEUTRAL_ACCENT;
    private ValueAnimator mAnimator;
    private View mFocusedItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_picker);

        View root = findViewById(R.id.profile_root);
        mBackground = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[] {BASE_COLOR, BASE_COLOR, mCurrentAccent});
        root.setBackground(mBackground);

        List<Account> accounts = YouTubeServiceManager.instance().getSignInService().getAccounts();
        ViewGroup row = findViewById(R.id.profile_row);
        LayoutInflater inflater = LayoutInflater.from(this);
        View selectedView = null;

        if (accounts != null) {
            for (Account account : accounts) {
                View item = inflater.inflate(R.layout.item_profile, row, false);
                ImageView avatar = item.findViewById(R.id.profile_avatar);
                TextView name = item.findViewById(R.id.profile_name);
                TextView email = item.findViewById(R.id.profile_email);

                name.setText(account.getName());
                email.setText(account.getEmail() != null ? account.getEmail() : "");
                Glide.with(this)
                        .load(account.getAvatarImageUrl())
                        .apply(ViewUtil.glideOptions())
                        .circleCrop()
                        .listener(new RequestListener<Drawable>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                if (resource instanceof BitmapDrawable) {
                                    item.setTag(R.id.profile_avatar, accentFrom(((BitmapDrawable) resource).getBitmap()));
                                    if (item == mFocusedItem) {
                                        fadeTo(accentOf(item));
                                    }
                                }
                                return false;
                            }
                        })
                        .into(avatar);

                item.setAlpha(UNFOCUSED_ALPHA);
                item.setOnFocusChangeListener((v, hasFocus) -> {
                    v.setAlpha(hasFocus ? 1f : UNFOCUSED_ALPHA);
                    email.setVisibility(hasFocus ? View.VISIBLE : View.INVISIBLE);
                    if (hasFocus) {
                        mFocusedItem = v;
                        fadeTo(accentOf(v));
                    }
                });
                item.setOnClickListener(v -> select(account));

                row.addView(item);

                if (account.isSelected() || selectedView == null) {
                    selectedView = item;
                }
            }
        }

        // "Add account": grijze cirkel met plus, als laatste in de rij (zoals de YouTube-app)
        View addItem = inflater.inflate(R.layout.item_profile, row, false);
        ImageView addIcon = addItem.findViewById(R.id.profile_avatar);
        addIcon.setBackgroundResource(R.drawable.profile_add_bg);
        addIcon.setImageResource(R.drawable.profile_add_icon);
        addIcon.setScaleType(ImageView.ScaleType.CENTER);
        ((TextView) addItem.findViewById(R.id.profile_name)).setText(R.string.profile_picker_add);
        addItem.setAlpha(UNFOCUSED_ALPHA);
        addItem.setOnFocusChangeListener((v, hasFocus) -> {
            v.setAlpha(hasFocus ? 1f : UNFOCUSED_ALPHA);
            if (hasFocus) {
                mFocusedItem = v;
                fadeTo(NEUTRAL_ACCENT);
            }
        });
        addItem.setOnClickListener(v -> {
            YTSignInPresenter.instance(this).start();
            finish();
        });
        row.addView(addItem);

        View guest = findViewById(R.id.profile_guest);
        guest.setOnClickListener(v -> select(null));
        guest.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                mFocusedItem = v;
                fadeTo(NEUTRAL_ACCENT);
            }
        });

        if (selectedView != null) {
            selectedView.requestFocus();
        } else {
            guest.requestFocus();
        }
    }

    private static int accentOf(View item) {
        Object tag = item.getTag(R.id.profile_avatar);
        return tag instanceof Integer ? (Integer) tag : NEUTRAL_ACCENT;
    }

    /**
     * Hoofdkleur van de foto: gemiddelde van de pixels, met meer gewicht voor verzadigde kleuren,
     * daarna wat donkerder en voller gemaakt zodat witte tekst leesbaar blijft.
     */
    private static int accentFrom(Bitmap bitmap) {
        try {
            Bitmap small = Bitmap.createScaledBitmap(bitmap, 24, 24, true);
            double r = 0, g = 0, b = 0, total = 0;
            float[] hsl = new float[3];
            for (int x = 0; x < small.getWidth(); x++) {
                for (int y = 0; y < small.getHeight(); y++) {
                    int c = small.getPixel(x, y);
                    if (Color.alpha(c) < 128) {
                        continue; // transparante hoeken van de ronde uitsnede
                    }
                    ColorUtils.colorToHSL(c, hsl);
                    double w = 0.05 + hsl[1] * (1 - Math.abs(hsl[2] - 0.5) * 2);
                    r += Color.red(c) * w;
                    g += Color.green(c) * w;
                    b += Color.blue(c) * w;
                    total += w;
                }
            }
            if (total <= 0) {
                return NEUTRAL_ACCENT;
            }
            int avg = Color.rgb((int) (r / total), (int) (g / total), (int) (b / total));
            ColorUtils.colorToHSL(avg, hsl);
            hsl[1] = Math.min(1f, hsl[1] * 1.3f);
            hsl[2] = Math.max(0.22f, Math.min(0.38f, hsl[2]));
            return ColorUtils.HSLToColor(hsl);
        } catch (Exception e) {
            return NEUTRAL_ACCENT;
        }
    }

    private void fadeTo(int target) {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
        int from = mCurrentAccent;
        mAnimator = ValueAnimator.ofFloat(0f, 1f);
        mAnimator.setDuration(FADE_MS);
        mAnimator.addUpdateListener(a -> {
            mCurrentAccent = ColorUtils.blendARGB(from, target, (float) a.getAnimatedValue());
            mBackground.setColors(new int[] {BASE_COLOR, BASE_COLOR, mCurrentAccent});
        });
        mAnimator.start();
    }

    private void select(Account account) {
        AccountSelectionPresenter.instance(this).selectAccount(account);
        finish();
    }

    @Override
    protected void onDestroy() {
        if (mAnimator != null) {
            mAnimator.cancel();
        }
        super.onDestroy();
    }
}
