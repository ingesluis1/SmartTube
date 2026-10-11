package com.liskovsoft.smartyoutubetv2.tv.presenter;

import androidx.leanback.widget.ListRowPresenter;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class CustomListRowPresenter extends ListRowPresenter {
    public CustomListRowPresenter() {
        super(ViewUtil.FOCUS_ZOOM_FACTOR, ViewUtil.FOCUS_DIMMER_ENABLED);
        setSelectEffectEnabled(ViewUtil.ROW_SELECT_EFFECT_ENABLED);
        setShadowEnabled(false); // MOD (Inge): geen schaduw onder de geselecteerde tegel (YouTube heeft die niet)
    }
}
