package com.example.appington_city;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.content.ContextCompat;

/** Round on-screen joystick. Reports a vector with x and y between -1 and 1. */
public class ThumbstickView extends View {

    public interface Listener {
        void onMove(float x, float y);
        void onRelease();
    }

    private final Paint basePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float centerX, centerY, radius, knobRadius;
    private float knobX, knobY;
    private float vectorX, vectorY;
    private Listener listener;

    public ThumbstickView(Context context) {
        super(context);
        init();
    }

    public ThumbstickView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThumbstickView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        basePaint.setStyle(Paint.Style.FILL);
        basePaint.setColor(ContextCompat.getColor(getContext(), R.color.stick_base));
        knobPaint.setStyle(Paint.Style.FILL);
        knobPaint.setColor(ContextCompat.getColor(getContext(), R.color.stick_knob));
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public float getVectorX() {
        return vectorX;
    }

    public float getVectorY() {
        return vectorY;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = Math.min(w, h) / 2f;
        knobRadius = radius * 0.38f;
        knobX = centerX;
        knobY = centerY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawCircle(centerX, centerY, radius, basePaint);
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE: {
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                float dx = event.getX() - centerX;
                float dy = event.getY() - centerY;
                float max = radius - knobRadius;
                float dist = (float) Math.hypot(dx, dy);
                if (dist > max && dist > 0) {
                    dx = dx / dist * max;
                    dy = dy / dist * max;
                }
                knobX = centerX + dx;
                knobY = centerY + dy;
                vectorX = max > 0 ? dx / max : 0;
                vectorY = max > 0 ? dy / max : 0;
                invalidate();
                if (listener != null) listener.onMove(vectorX, vectorY);
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                knobX = centerX;
                knobY = centerY;
                vectorX = 0;
                vectorY = 0;
                invalidate();
                if (listener != null) listener.onRelease();
                performClick();
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
