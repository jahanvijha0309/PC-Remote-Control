package com.example.appington_city;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/** Free-Mode: mouse, scroll bar, volume bar, clicks, space bar, screenshot and keyboard input. */
public class PCActivity extends AppCompatActivity {

    private static final int SCROLL_UNITS_PER_STEP = 15;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private MouseController mouse;

    private int lastSound = 50;
    private int pendingSound = 0;
    private boolean soundScheduled = false;

    private int lastScroll = 50;
    private int pendingScroll = 0;
    private boolean scrollScheduled = false;
    private boolean resettingScroll = false;

    private boolean ignoreTextChanges = false;

    private final Runnable flushSound = () -> {
        soundScheduled = false;
        if (pendingSound != 0) {
            float change = pendingSound / 100f;
            pendingSound = 0;
            Api.firePost(this, "/sound", String.format(Locale.US, "%.2f", change));
        }
    };

    private final Runnable flushScroll = () -> {
        scrollScheduled = false;
        if (pendingScroll != 0) {
            int amount = pendingScroll * SCROLL_UNITS_PER_STEP;
            pendingScroll = 0;
            Api.firePost(this, "/scroll", String.valueOf(amount));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pc);

        ThumbstickView stick = findViewById(R.id.thumbstick);
        mouse = new MouseController(this);
        stick.setListener(mouse);

        findViewById(R.id.screenshotButton).setOnClickListener(v ->
                Api.get(this, "/screenshot", (ok, code, body) -> {
                    if (ok) {
                        Toast.makeText(this, "Screenshot saved on the PC", Toast.LENGTH_SHORT).show();
                    } else {
                        Api.toastError(getApplicationContext(), code);
                    }
                }));
        findViewById(R.id.closeButton).setOnClickListener(v -> Api.fireGet(this, "/clP"));
        findViewById(R.id.leftButton).setOnClickListener(v -> Api.fireGet(this, "/lc"));
        findViewById(R.id.rightButton).setOnClickListener(v -> Api.fireGet(this, "/rc"));
        findViewById(R.id.spaceButton).setOnClickListener(v -> Api.fireGet(this, "/space"));

        setupSoundBar();
        setupScrollBar();
        setupKeyboard();
    }

    private void setupSoundBar() {
        SeekBar sound = findViewById(R.id.soundSeekBar);
        sound.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                pendingSound += progress - lastSound;
                lastSound = progress;
                if (!soundScheduled) {
                    soundScheduled = true;
                    handler.postDelayed(flushSound, 120);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
    }

    private void setupScrollBar() {
        final VerticalSeekBar scroll = findViewById(R.id.scrollSeekBar);
        scroll.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (resettingScroll) return;
                pendingScroll += progress - lastScroll;
                lastScroll = progress;
                if (!scrollScheduled) {
                    scrollScheduled = true;
                    handler.postDelayed(flushScroll, 80);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });
        // Let the bar spring back to the middle when the finger is lifted.
        scroll.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                scroll.post(() -> {
                    resettingScroll = true;
                    scroll.setProgress(50);
                    lastScroll = 50;
                    resettingScroll = false;
                });
            }
            return false;
        });
    }

    private void setupKeyboard() {
        final EditText input = findViewById(R.id.keyboardInput);
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (ignoreTextChanges) return;
                if (count > before) {
                    String added = s.toString().substring(start + before, start + count);
                    if (!added.isEmpty()) Api.firePost(PCActivity.this, "/sendKeys", added);
                } else if (before > count) {
                    for (int i = 0; i < before - count; i++) {
                        Api.fireGet(PCActivity.this, "/backspace");
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Keep the box from growing forever.
                if (s.length() > 40) {
                    ignoreTextChanges = true;
                    s.clear();
                    ignoreTextChanges = false;
                }
            }
        });
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE) {
                Api.fireGet(this, "/enter");
                return true;
            }
            return false;
        });
        input.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                Api.fireGet(this, "/enter");
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_DEL && input.getText().length() == 0) {
                Api.fireGet(this, "/backspace");
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        mouse.stop();
    }
}
