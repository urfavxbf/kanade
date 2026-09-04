package com.urfavxbf.kanade.ui.dashboard;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import com.urfavxbf.kanade.PlaybackStatsManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class UsageChartView extends View {

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ArrayList<PlaybackStatsManager.UsagePoint> points = new ArrayList<>();
    private int accentColor;

    public UsageChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        barPaint.setStyle(Paint.Style.FILL);
        labelPaint.setTextSize(dp(11));
        valuePaint.setTextSize(dp(10));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        setWillNotDraw(false);
    }

    public void setData(ArrayList<PlaybackStatsManager.UsagePoint> points, int accentColor) {
        this.points = points == null ? new ArrayList<>() : points;
        this.accentColor = accentColor;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (points.isEmpty()) return;

        float width = getWidth();
        float height = getHeight();
        float bottom = height - dp(22);
        float chartHeight = Math.max(dp(40), bottom - dp(8));
        long max = 1L;
        for (PlaybackStatsManager.UsagePoint point : points) max = Math.max(max, point.minutes);

        float slot = width / points.size();
        float barWidth = Math.min(dp(30), slot * 0.55f);
        SimpleDateFormat format = new SimpleDateFormat("EEE", Locale.getDefault());
        labelPaint.setColor(0xFFA9AAB5);
        valuePaint.setColor(0xFFC4C5CF);
        barPaint.setColor(accentColor);

        for (int i = 0; i < points.size(); i++) {
            PlaybackStatsManager.UsagePoint point = points.get(i);
            float x = (slot * i) + (slot / 2f);
            float barHeight = chartHeight * (point.minutes / (float) max);
            float top = bottom - barHeight;
            canvas.drawRoundRect(new RectF(x - barWidth / 2f, top, x + barWidth / 2f, bottom), dp(7), dp(7), barPaint);

            String day = format.format(new Date(point.day));
            canvas.drawText(day, x - labelPaint.measureText(day) / 2f, height - dp(5), labelPaint);
            if (point.minutes > 0) {
                String value = point.minutes + "m";
                canvas.drawText(value, x, Math.max(dp(11), top - dp(4)), valuePaint);
            }
        }
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
