package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.leanback.widget.Presenter;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.settings.SettingsGroupHeader;

/**
 * MOD (Inge): grijs, niet-focusbaar groepskopje in de instellingenlijst.
 */
public class SettingsGroupHeaderPresenter extends Presenter {
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.settings_group_header, parent, false);
        view.setFocusable(false);
        view.setFocusableInTouchMode(false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        ((TextView) viewHolder.view).setText(((SettingsGroupHeader) item).title);
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
    }
}
