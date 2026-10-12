package com.example.appington_city;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.core.content.ContextCompat;

/**
 * Laptop-style touchpad.
 * One finger drags the cursor, a tap is a left click, a two-finger tap is a right click
 * and two fingers dragging up or down scroll.
 */
public class TouchpadView extends View {

    public interface Listener {
        void onMove(float dxPx, float dyPx);
        void onTap();
        void onTwoFingerTap();
        void onScroll(float dyPx);
    }

    private static final long TAP_MAX_MS = 250;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float slop;
    private Listener listener;

    private long downTime;
    private float startX, startY, lastX, lastY, lastCentroidY;
    private int maxPointers;
    private boolean moved;

    public TouchpadView(Context context) {
        this(context, null);
    }

    public TouchpadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        bgPaint.setColor(ContextCompat.getColor(context, R.color.key_bg));
        textPaint.setColor(ContextCompat.getColor(context, R.color.text_subtitle));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(15f * context.getResources().getDisplayMetrics().scaledDensity);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float r = 24f * getResources().getDisplayMetrics().density;
        rect.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(rect, r, r, bgPaint);
        float y = getHeight() / 2f;
        float line = textPaint.getTextSize() * 1.5f;
        canvas.drawText("Drag to move the mouse", getWidth() / 2f, y - line, textPaint);
        canvas.drawText("Tap = left click", getWidth() / 2f, y, textPaint);
        canvas.drawText("Two-finger tap = right click", getWidth() / 2f, y + line, textPaint);
        canvas.drawText("Two fingers up/down = scroll", getWidth() / 2f, y + 2 * line, textPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                downTime = e.getEventTime();
                startX = lastX = e.getX();
                startY = lastY = e.getY();
                maxPointers = 1;
                moved = false;
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                maxPointers = Math.max(maxPointers, e.getPointerCount());
                lastCentroidY = centroidY(e);
                return true;

            case MotionEvent.ACTION_MOVE:
                if (e.getPointerCount() >= 2) {
                    float cy = centroidY(e);
                    float dy = cy - lastCentroidY;
                    lastCentroidY = cy;
                    if (Math.abs(cy - startY) > slop) moved = true;
                    if (moved && listener != null) listener.onScroll(dy);
                } else {
                    float x = e.getX();
                    float y = e.getY();
                    if (!moved && Math.hypot(x - startX, y - startY) > slop) moved = true;
                    if (moved && listener != null) listener.onMove(x - lastX, y - lastY);
                    lastX = x;
                    lastY = y;
                }
                return true;

            case MotionEvent.ACTION_POINTER_UP: {
                // Continue smoothly with the finger that stays down.
                int up = e.getActionIndex();
                int remaining = up == 0 ? 1 : 0;
                if (remaining < e.getPointerCount()) {
                    lastX = e.getX(remaining);
                    lastY = e.getY(remaining);
                }
                return true;
            }

            case MotionEvent.ACTION_UP:
                if (!moved && e.getEventTime() - downTime < TAP_MAX_MS && listener != null) {
                    if (maxPointers == 1) listener.onTap();
                    else if (maxPointers == 2) listener.onTwoFingerTap();
                }
                performClick();
                return true;

            default:
                return super.onTouchEvent(e);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private static float centroidY(MotionEvent e) {
        float sum = 0;
        for (int i = 0; i < e.getPointerCount(); i++) sum += e.getY(i);
        return sum / e.getPointerCount();
    }
}
