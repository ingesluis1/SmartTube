package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Presenter;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.settings.SettingsEntry;

/**
 * MOD (Inge): één regel in de instellingenlijst (icoon + label), zoals de YouTube-app.
 */
public class SettingsRowPresenter extends Presenter {
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.settings_row, parent, false);
        ImageView icon = view.findViewById(R.id.settings_row_icon);
        icon.setImageTintList(ContextCompat.getColorStateList(parent.getContext(), R.color.nav_item_text));
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        SettingsEntry entry = (SettingsEntry) item;
        TextView title = viewHolder.view.findViewById(R.id.settings_row_title);
        ImageView icon = viewHolder.view.findViewById(R.id.settings_row_icon);
        title.setText(entry.item.title);
        if (entry.iconResId > 0) {
            icon.setImageResource(entry.iconResId);
            icon.setVisibility(View.VISIBLE);
        } else {
            icon.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
    }
}
