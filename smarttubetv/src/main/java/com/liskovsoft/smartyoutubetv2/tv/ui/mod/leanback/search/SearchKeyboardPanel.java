package com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.search;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import com.liskovsoft.smartyoutubetv2.tv.R;

import java.util.ArrayList;
import java.util.List;

/**
 * MOD (Inge): zoekpaneel zoals de YouTube-app.
 * Links de zoekgeschiedenis/suggesties, rechts een letterraster met backspace, &123,
 * en daaronder SPACE / CLEAR / SEARCH.
 */
public class SearchKeyboardPanel extends LinearLayout {
    public interface Callback {
        String getText();
        void setText(String text);
        void submit(String text);
        boolean focusResults();
    }

    private static final int MAX_SUGGESTIONS = 5;
    private static final String[] LETTERS = {"ABCDEFG", "HIJKLMN", "OPQRSTU", "VWXYZ-'"};
    private static final String[] SYMBOLS = {"1234567", "890.,?!", "&@#:;()", "+=\"_/%*"};

    private final Callback mCallback;
    private final LinearLayout mSuggestions;
    private final LinearLayout mKeyRows;
    private final List<TextView> mCharKeys = new ArrayList<>();
    private TextView mModeKey;
    private boolean mSymbols;
    private View mFirstKey;
    private final List<View> mBottomViews = new ArrayList<>();
    private int mDownTargetId = View.NO_ID;

    public SearchKeyboardPanel(Context context, Callback callback) {
        super(context);
        mCallback = callback;
        setOrientation(HORIZONTAL);
        setClipChildren(false);
        setClipToPadding(false);

        mSuggestions = new LinearLayout(context);
        mSuggestions.setOrientation(VERTICAL);
        LayoutParams sl = new LayoutParams(dp(270), LayoutParams.WRAP_CONTENT);
        sl.rightMargin = dp(28);
        addView(mSuggestions, sl);

        LinearLayout keyboard = new LinearLayout(context);
        keyboard.setOrientation(VERTICAL);
        addView(keyboard, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        mKeyRows = new LinearLayout(context);
        mKeyRows.setOrientation(VERTICAL);
        keyboard.addView(mKeyRows, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));

        for (int r = 0; r < LETTERS.length; r++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(HORIZONTAL);
            for (int c = 0; c < LETTERS[r].length(); c++) {
                TextView key = createKey(String.valueOf(LETTERS[r].charAt(c)));
                key.setOnClickListener(v -> append(((TextView) v).getText().toString().toLowerCase()));
                mCharKeys.add(key);
                row.addView(key);
                if (mFirstKey == null) {
                    mFirstKey = key;
                }
            }
            // rechterkolom: backspace, &123
            if (r == 0) {
                ImageView back = new ImageView(context);
                Drawable icon = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_kbd_backspace)).mutate();
                DrawableCompat.setTintList(icon, ContextCompat.getColorStateList(context, R.color.nav_item_text));
                back.setImageDrawable(icon);
                back.setScaleType(ImageView.ScaleType.CENTER);
                back.setPadding(dp(8), dp(6), dp(8), dp(6));
                back.setFocusable(true);
                back.setBackgroundResource(R.drawable.kbd_key_bg);
                back.setDuplicateParentStateEnabled(false);
                back.setOnClickListener(v -> backspace());
                LayoutParams bl = new LayoutParams(dp(52), dp(34));
                bl.leftMargin = dp(8);
                row.addView(back, bl);
            } else if (r == 1) {
                mModeKey = createKey("&123");
                mModeKey.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
                mModeKey.setOnClickListener(v -> toggleMode());
                LayoutParams ml = new LayoutParams(dp(52), dp(34));
                ml.leftMargin = dp(8);
                row.addView(mModeKey, ml);
            }
            mKeyRows.addView(row, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        }

        LinearLayout buttons = new LinearLayout(context);
        buttons.setOrientation(HORIZONTAL);
        LayoutParams btl = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        btl.topMargin = dp(10);
        keyboard.addView(buttons, btl);
        View space = createButton(context.getString(R.string.search_kbd_space), v -> append(" "));
        View clear = createButton(context.getString(R.string.search_kbd_clear), v -> mCallback.setText(""));
        View search = createButton(context.getString(R.string.search_kbd_search), v -> {
            String text = mCallback.getText();
            if (!TextUtils.isEmpty(text)) {
                mCallback.submit(text);
            }
        });
        buttons.addView(space);
        buttons.addView(clear);
        buttons.addView(search);
        mBottomViews.add(space);
        mBottomViews.add(clear);
        mBottomViews.add(search);
    }

    public void setSuggestions(List<String> items) {
        List<String> clean = new ArrayList<>();
        if (items != null) {
            for (String item : items) {
                if (!TextUtils.isEmpty(item)) {
                    clean.add(item);
                    if (clean.size() >= MAX_SUGGESTIONS) {
                        break;
                    }
                }
            }
        }
        // Bestaande regels hergebruiken, zodat de focus niet verspringt
        while (mSuggestions.getChildCount() < clean.size()) {
            mSuggestions.addView(createSuggestionView(), new LayoutParams(LayoutParams.MATCH_PARENT, dp(33)));
        }
        View focused = mSuggestions.findFocus();
        int focusedIndex = focused != null ? mSuggestions.indexOfChild(focused) : -1;
        for (int i = mSuggestions.getChildCount() - 1; i >= clean.size(); i--) {
            mSuggestions.removeViewAt(i);
        }
        for (int i = 0; i < clean.size(); i++) {
            TextView view = (TextView) mSuggestions.getChildAt(i);
            view.setText(clean.get(i));
            view.setTag(clean.get(i));
        }
        if (focusedIndex >= clean.size()) {
            if (clean.isEmpty()) {
                focusKeyboard();
            } else {
                mSuggestions.getChildAt(clean.size() - 1).requestFocus();
            }
        }
        applyDownTarget();
    }

    private TextView createSuggestionView() {
        Context context = getContext();
        TextView view = new TextView(context);
        view.setSingleLine(true);
        view.setEllipsize(TextUtils.TruncateAt.END);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        view.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        view.setTextColor(ContextCompat.getColorStateList(context, R.color.nav_item_text));
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setFocusable(true);
        view.setBackgroundResource(R.drawable.nav_item_bg);
        view.setPadding(dp(10), 0, dp(10), 0);
        Drawable icon = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_search_history)).mutate();
        DrawableCompat.setTintList(icon, ContextCompat.getColorStateList(context, R.color.nav_item_text));
        icon.setBounds(0, 0, dp(20), dp(20));
        view.setCompoundDrawables(icon, null, null, null);
        view.setCompoundDrawablePadding(dp(12));
        view.setOnClickListener(v -> {
            Object text = v.getTag();
            if (text instanceof String) {
                mCallback.submit((String) text);
            }
        });
        return view;
    }

    /**
     * Omlaag vanaf de onderste rij (knoppen en laatste suggestie) gaat naar de zoekresultaten.
     */
    public void setNextFocusDownTarget(int viewId) {
        mDownTargetId = viewId;
        applyDownTarget();
    }

    private void applyDownTarget() {
        for (View v : mBottomViews) {
            v.setOnKeyListener(mDownToResults);
        }
        int n = mSuggestions.getChildCount();
        if (n > 0) {
            mSuggestions.getChildAt(n - 1).setOnKeyListener(mDownToResults);
        }
    }

    private final View.OnKeyListener mDownToResults = this::onDownKey;

    private boolean onDownKey(View v, int keyCode, android.view.KeyEvent event) {
        return keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN
                && event.getAction() == android.view.KeyEvent.ACTION_DOWN
                && mCallback.focusResults();
    }

    public boolean focusKeyboard() {
        return mFirstKey != null && mFirstKey.requestFocus();
    }

    private TextView createKey(String label) {
        TextView key = new TextView(getContext());
        key.setText(label);
        key.setGravity(Gravity.CENTER);
        key.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        key.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.nav_item_text));
        key.setFocusable(true);
        key.setBackgroundResource(R.drawable.kbd_key_bg);
        key.setLayoutParams(new LayoutParams(dp(38), dp(34)));
        return key;
    }

    private TextView createButton(String label, View.OnClickListener listener) {
        TextView button = new TextView(getContext());
        button.setText(label);
        button.setAllCaps(true);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        button.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        button.setTextColor(ContextCompat.getColorStateList(getContext(), R.color.nav_item_text));
        button.setFocusable(true);
        button.setBackgroundResource(R.drawable.kbd_button_bg);
        button.setOnClickListener(listener);
        LayoutParams lp = new LayoutParams(dp(68), dp(28));
        lp.rightMargin = dp(8);
        button.setLayoutParams(lp);
        return button;
    }

    private void append(String s) {
        String text = mCallback.getText();
        mCallback.setText((text == null ? "" : text) + s);
    }

    private void backspace() {
        String text = mCallback.getText();
        if (!TextUtils.isEmpty(text)) {
            mCallback.setText(text.substring(0, text.length() - 1));
        }
    }

    private void toggleMode() {
        mSymbols = !mSymbols;
        String[] layout = mSymbols ? SYMBOLS : LETTERS;
        int i = 0;
        for (String row : layout) {
            for (int c = 0; c < row.length(); c++) {
                mCharKeys.get(i++).setText(String.valueOf(row.charAt(c)));
            }
        }
        mModeKey.setText(mSymbols ? "ABC" : "&123");
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
