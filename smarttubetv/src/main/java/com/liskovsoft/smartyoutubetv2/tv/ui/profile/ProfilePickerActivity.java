package com.liskovsoft.smartyoutubetv2.tv.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import com.liskovsoft.mediaserviceinterfaces.oauth.Account;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.AccountSelectionPresenter;
import com.liskovsoft.smartyoutubetv2.common.app.views.ProfilePickerView;
import com.liskovsoft.smartyoutubetv2.common.misc.MotherActivity;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;

import java.util.List;

/**
 * MOD (Inge): profielkeuze bij het opstarten, zoals in de YouTube-app.
 * Ronde avatars naast elkaar, naam eronder, "Watch as guest" onderin.
 */
public class ProfilePickerActivity extends MotherActivity implements ProfilePickerView {
    private static final float UNFOCUSED_ALPHA = 0.55f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_picker);

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
                        .into(avatar);

                item.setAlpha(UNFOCUSED_ALPHA);
                item.setOnFocusChangeListener((v, hasFocus) -> {
                    v.setAlpha(hasFocus ? 1f : UNFOCUSED_ALPHA);
                    email.setVisibility(hasFocus ? View.VISIBLE : View.INVISIBLE);
                });
                item.setOnClickListener(v -> select(account));

                row.addView(item);

                if (account.isSelected() || selectedView == null) {
                    selectedView = item;
                }
            }
        }

        View guest = findViewById(R.id.profile_guest);
        guest.setOnClickListener(v -> select(null));

        if (selectedView != null) {
            selectedView.requestFocus();
        } else {
            guest.requestFocus();
        }
    }

    private void select(Account account) {
        AccountSelectionPresenter.instance(this).selectAccount(account);
        finish();
    }
}
