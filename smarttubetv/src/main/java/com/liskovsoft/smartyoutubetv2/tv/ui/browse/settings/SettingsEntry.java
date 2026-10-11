package com.liskovsoft.smartyoutubetv2.tv.ui.browse.settings;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.SettingsItem;

/**
 * MOD (Inge): instellingen-item met eigen dun icoon en korte omschrijving.
 */
public class SettingsEntry {
    public final SettingsItem item;
    public final int iconResId;
    public final int descResId;

    public SettingsEntry(SettingsItem item, int iconResId, int descResId) {
        this.item = item;
        this.iconResId = iconResId;
        this.descResId = descResId;
    }
}
