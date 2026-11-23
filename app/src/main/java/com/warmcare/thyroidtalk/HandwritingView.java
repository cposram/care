package com.warmcare.thyroidtalk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class HandwritingView extends View {

    private static class Stroke {
        final Path path;
        final float width;

        Stroke(Path path, float width) {
            this.path = path;
            this.width = width;
        }
    }

    private final List<Stroke> strokes = new ArrayList<>();
    private Path currentPath;
    private float currentStrokeWidth = 14f;
    private float lastX;
    private float lastY;

    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint guideLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public HandwritingView(Context context) {
        super(context);
        init();
    }

    public HandwritingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HandwritingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        strokePaint.setColor(Color.parseColor("#1E1B18"));
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);

        guideLinePaint.setColor(Color.parseColor("#ECE4DA"));
        guideLinePaint.setStrokeWidth(2f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        int step = 110;
        for (int y = step; y < h; y += step) {
            canvas.drawLine(24f, y, w - 24f, y, guideLinePaint);
        }

        for (Stroke stroke : strokes) {
            strokePaint.setStrokeWidth(stroke.width);
            canvas.drawPath(stroke.path, strokePaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                currentPath = new Path();
                currentPath.moveTo(x, y);
                currentPath.lineTo(x + 0.1f, y + 0.1f);
                strokes.add(new Stroke(currentPath, currentStrokeWidth));
                lastX = x;
                lastY = y;
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (currentPath != null) {
                    float midX = (x + lastX) / 2f;
                    float midY = (y + lastY) / 2f;
                    currentPath.quadTo(lastX, lastY, midX, midY);
                    lastX = x;
                    lastY = y;
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (currentPath != null) {
                    currentPath.lineTo(x, y);
                    currentPath = null;
                    invalidate();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    public void clearCanvas() {
        strokes.clear();
        currentPath = null;
        invalidate();
    }

    public void undoLastStroke() {
        if (!strokes.isEmpty()) {
            strokes.remove(strokes.size() - 1);
            invalidate();
        }
    }

    public boolean toggleThickStroke() {
        if (currentStrokeWidth < 20f) {
            currentStrokeWidth = 26f;
            return true;
        } else {
            currentStrokeWidth = 14f;
            return false;
        }
    }
}