package com.example.appington_city;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.atomic.AtomicBoolean;

/** Phone screen as a laptop touchpad. */
public class TouchpadActivity extends AppCompatActivity implements TouchpadView.Listener {

    private static final int FLUSH_MS = 25;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private float density;
    private float gain;
    private float accX, accY, accScroll;
    private boolean flushScheduled = false;

    private final Runnable flush = () -> {
        flushScheduled = false;
        int dx = Math.round(accX);
        int dy = Math.round(accY);
        if ((dx != 0 || dy != 0) && busy.compareAndSet(false, true)) {
            accX -= dx;
            accY -= dy;
            Api.post(this, "/mouseMove", dx + "/" + dy, (ok, code, body) -> {
                busy.set(false);
                if (!ok) Api.toastError(getApplicationContext(), code);
            });
        }
        int scroll = Math.round(accScroll);
        if (scroll != 0) {
            accScroll -= scroll;
            Api.firePost(this, "/scroll", String.valueOf(scroll));
        }
        // Keep flushing while there is still something left to send.
        if (Math.abs(accX) >= 0.5f || Math.abs(accY) >= 0.5f || Math.abs(accScroll) >= 0.5f) {
            schedule();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touchpad);
        density = getResources().getDisplayMetrics().density;
        gain = 1.0f + Prefs.getSensitivity(this) / 100f * 3.0f;

        TouchpadView pad = findViewById(R.id.touchpad);
        pad.setListener(this);
        findViewById(R.id.leftButton).setOnClickListener(v -> Api.fireGet(this, "/lc"));
        findViewById(R.id.rightButton).setOnClickListener(v -> Api.fireGet(this, "/rc"));
    }

    private void schedule() {
        if (!flushScheduled) {
            flushScheduled = true;
            handler.postDelayed(flush, FLUSH_MS);
        }
    }

    @Override
    public void onMove(float dxPx, float dyPx) {
        float dx = dxPx / density;
        float dy = dyPx / density;
        // Faster finger movement = a bit more cursor distance.
        float speed = Math.min(Math.abs(dx) + Math.abs(dy), 30f) / 30f;
        float g = gain * (1f + speed);
        accX += dx * g;
        accY += dy * g;
        schedule();
    }

    @Override
    public void onTap() {
        Api.fireGet(this, "/lc");
    }

    @Override
    public void onTwoFingerTap() {
        Api.fireGet(this, "/rc");
    }

    @Override
    public void onScroll(float dyPx) {
        // Finger down = content follows the finger = wheel up (positive).
        accScroll += dyPx / density * 12f;
        schedule();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(flush);
        flushScheduled = false;
        accX = accY = accScroll = 0;
    }
}
