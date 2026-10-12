package com.example.appington_city;

import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

/** One-tap keyboard shortcuts. Each button sends a key combination to /hotkey. */
public class ShortcutsActivity extends AppCompatActivity {

    /** label, keys (pyautogui key names joined with +) */
    private static final String[][] SHORTCUTS = {
            {"Copy", "ctrl+c"}, {"Paste", "ctrl+v"}, {"Cut", "ctrl+x"},
            {"Undo", "ctrl+z"}, {"Redo", "ctrl+y"}, {"Select all", "ctrl+a"},
            {"Save", "ctrl+s"}, {"Find", "ctrl+f"}, {"Refresh", "f5"},
            {"New tab", "ctrl+t"}, {"Close tab", "ctrl+w"}, {"Reopen tab", "ctrl+shift+t"},
            {"Switch app", "alt+tab"}, {"Task view", "win+tab"}, {"Show desktop", "win+d"},
            {"Task manager", "ctrl+shift+esc"}, {"Windows key", "win"}, {"Esc", "esc"},
            {"Enter", "enter"}, {"Tab", "tab"}, {"Delete", "delete"},
            {"Up", "up"}, {"Down", "down"}, {"Left", "left"},
            {"Right", "right"}, {"Mute", "volumemute"}, {"Play / Pause", "playpause"},
            {"Previous", "prevtrack"}, {"Next", "nexttrack"}, {"Snip screen", "win+shift+s"},
    };

    private static final int COLUMNS = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shortcuts);
        LinearLayout container = findViewById(R.id.shortcutsContainer);
        float density = getResources().getDisplayMetrics().density;
        int gap = (int) (4 * density);

        LinearLayout row = null;
        for (int i = 0; i < SHORTCUTS.length; i++) {
            if (i % COLUMNS == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                container.addView(row, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            }
            final String label = SHORTCUTS[i][0];
            final String keys = SHORTCUTS[i][1];

            MaterialButton button = new MaterialButton(this);
            button.setAllCaps(false);
            button.setText(label);
            button.setTextSize(14f);
            button.setMaxLines(2);
            button.setTextColor(ContextCompat.getColor(this, R.color.text_title));
            button.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.key_bg));
            button.setCornerRadius((int) (12 * density));
            button.setOnClickListener(v -> Api.firePost(this, "/hotkey", keys));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, (int) (64 * density), 1f);
            lp.setMargins(gap, gap, gap, gap);
            row.addView(button, lp);
        }
        // Fill the last row so the buttons keep an equal width.
        int rest = SHORTCUTS.length % COLUMNS;
        if (rest != 0 && row != null) {
            for (int i = rest; i < COLUMNS; i++) {
                row.addView(new android.view.View(this), new LinearLayout.LayoutParams(0, 1, 1f));
            }
        }
    }
}
