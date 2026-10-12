package com.example.appington_city;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicBoolean;

/** Turns thumbstick movement into /mouseMove requests while the stick is held. */
public final class MouseController implements ThumbstickView.Listener {

    private static final int REFRESH_MS = 30;
    private static final int STEP_SIZE = 24;

    private final Context ctx;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean busy = new AtomicBoolean(false);
    private final float factor;
    private final float deadZone;
    private float vx, vy;
    private boolean active;

    public MouseController(Context context) {
        this.ctx = context;
        int sensitivity = Prefs.getSensitivity(context);
        this.factor = 0.25f + (sensitivity / 100f) * 1.75f;
        this.deadZone = Prefs.getDeadZone(context) / 100f;
    }

    private final Runnable loop = new Runnable() {
        @Override
        public void run() {
            if (!active) return;
            float magnitude = (float) Math.hypot(vx, vy);
            float scale = 0f;
            if (magnitude > deadZone) {
                // Ignore small drift, then stretch the rest so full tilt is still full speed.
                scale = (magnitude - deadZone) / (1f - deadZone) / magnitude;
            }
            int dx = Math.round(curve(vx * scale) * STEP_SIZE * factor);
            int dy = Math.round(curve(vy * scale) * STEP_SIZE * factor);
            if ((dx != 0 || dy != 0) && busy.compareAndSet(false, true)) {
                Api.post(ctx, "/mouseMove", dx + "/" + dy, (ok, code, body) -> {
                    busy.set(false);
                    if (!ok) Api.toastError(ctx.getApplicationContext(), code);
                });
            }
            handler.postDelayed(this, REFRESH_MS);
        }
    };

    /** Small movements are slow and precise, full tilt is fast. */
    private static float curve(float v) {
        return 0.4f * v + 0.6f * v * Math.abs(v);
    }

    @Override
    public void onMove(float x, float y) {
        vx = x;
        vy = y;
        if (!active) {
            active = true;
            handler.post(loop);
        }
    }

    @Override
    public void onRelease() {
        active = false;
        vx = 0;
        vy = 0;
        handler.removeCallbacks(loop);
    }

    public void stop() {
        onRelease();
    }
}
