package com.example.appington_city;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Live view of the PC screen. Touch to move the cursor, tap to click, long press to right-click.
 */
public class ScreenActivity extends AppCompatActivity {

    private static final int WIDTH_PX = 900;
    private static final int QUALITY = 55;
    private static final int FRAME_DELAY_MS = 150;
    private static final long LONG_PRESS_MS = 500;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean moveBusy = new AtomicBoolean(false);
    private ImageView screenImage;
    private TextView screenStatus;
    private volatile boolean running = false;
    private Thread worker;
    private int touchSlop;

    private long downTime;
    private float downX, downY;
    private boolean dragged;
    private long lastMoveSent;

    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen);
        screenImage = findViewById(R.id.screenImage);
        screenStatus = findViewById(R.id.screenStatus);
        touchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        screenImage.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downTime = e.getEventTime();
                    downX = e.getX();
                    downY = e.getY();
                    dragged = false;
                    moveTo(e.getX(), e.getY(), true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (Math.hypot(e.getX() - downX, e.getY() - downY) > touchSlop) dragged = true;
                    if (dragged) moveTo(e.getX(), e.getY(), false);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!dragged) {
                        boolean longPress = e.getEventTime() - downTime >= LONG_PRESS_MS;
                        Api.fireGet(this, longPress ? "/rc" : "/lc");
                    }
                    return true;
                default:
                    return false;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        running = true;
        worker = new Thread(this::frameLoop, "screen-loop");
        worker.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        running = false;
        if (worker != null) worker.interrupt();
    }

    // ---------------- pulling frames ----------------

    private void frameLoop() {
        boolean first = true;
        while (running) {
            String ip = Prefs.getIp(this);
            if (ip.isEmpty()) {
                setStatus("No PC selected - open Settings first");
                sleep(1500);
                continue;
            }
            HttpURLConnection conn = null;
            try {
                URL url = new URL("http://" + ip + ":" + Prefs.PORT + "/screen?w=" + WIDTH_PX + "&q=" + QUALITY);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2500);
                conn.setReadTimeout(5000);
                if (conn.getResponseCode() == 200) {
                    try (InputStream in = conn.getInputStream()) {
                        final Bitmap bitmap = BitmapFactory.decodeStream(in);
                        if (bitmap != null) {
                            handler.post(() -> screenImage.setImageBitmap(bitmap));
                            if (first) {
                                setStatus("Touch to move, tap = click, long press = right click");
                                hideStatusLater();
                                first = false;
                            }
                        }
                    }
                    sleep(FRAME_DELAY_MS);
                } else {
                    setStatus("PC returned an error");
                    sleep(1500);
                }
            } catch (Exception e) {
                if (!running) return;
                setStatus("Could not reach the PC");
                first = true;
                sleep(1500);
            } finally {
                if (conn != null) conn.disconnect();
            }
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    private void setStatus(final String text) {
        handler.post(() -> {
            screenStatus.setText(text);
            screenStatus.setVisibility(android.view.View.VISIBLE);
        });
    }

    private void hideStatusLater() {
        handler.postDelayed(() -> screenStatus.setVisibility(android.view.View.GONE), 3500);
    }

    // ---------------- touch -> cursor ----------------

    /** Maps a touch point to a 0..1 position on the shown picture and moves the PC cursor there. */
    private void moveTo(float touchX, float touchY, boolean force) {
        if (screenImage.getDrawable() == null) return;
        long now = System.currentTimeMillis();
        if (!force && now - lastMoveSent < 50) return;

        RectF shown = shownImageRect();
        if (shown.width() <= 0 || shown.height() <= 0) return;
        float fx = (touchX - shown.left) / shown.width();
        float fy = (touchY - shown.top) / shown.height();
        fx = Math.max(0f, Math.min(1f, fx));
        fy = Math.max(0f, Math.min(1f, fy));

        if (!force && !moveBusy.compareAndSet(false, true)) return;
        if (force) moveBusy.set(true);
        lastMoveSent = now;
        String body = String.format(java.util.Locale.US, "%.4f/%.4f", fx, fy);
        Api.post(this, "/mouseTo", body, (ok, code, resp) -> {
            moveBusy.set(false);
            if (!ok) Api.toastError(getApplicationContext(), code);
        });
    }

    /** Where the picture really sits inside the ImageView (fitCenter leaves bars around it). */
    private RectF shownImageRect() {
        int vw = screenImage.getWidth();
        int vh = screenImage.getHeight();
        int bw = screenImage.getDrawable().getIntrinsicWidth();
        int bh = screenImage.getDrawable().getIntrinsicHeight();
        if (bw <= 0 || bh <= 0) return new RectF();
        float scale = Math.min(vw / (float) bw, vh / (float) bh);
        float w = bw * scale;
        float h = bh * scale;
        float left = (vw - w) / 2f;
        float top = (vh - h) / 2f;
        return new RectF(left, top, left + w, top + h);
    }
}
