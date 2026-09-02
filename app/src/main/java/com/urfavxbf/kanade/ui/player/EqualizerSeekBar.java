package com.urfavxbf.kanade.ui.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatSeekBar;

import java.util.Random;

public class EqualizerSeekBar extends AppCompatSeekBar {

    private final Paint activePaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint inactivePaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Random random =
            new Random();

    private float[] barHeights;
    private float[] targetHeights;

    /*
     * Default:
     * 100 bars
     *
     * Allowed:
     * 34 - 100
     */
    private int barCount = 100;

    private static final int MIN_BAR_COUNT = 34;
    private static final int MAX_BAR_COUNT = 100;

    private int activeColor =
            Color.rgb(
                    201,
                    196,
                    255
            );

    private int inactiveColor =
            Color.argb(
                    45,
                    201,
                    196,
                    255
            );

    private int backgroundColor =
            Color.rgb(
                    16,
                    17,
                    26
            );

    private boolean equalizerPlaying =
            false;

    private boolean trackingTouch =
            false;

    private long lastAnimationTime =
            0L;

    private float animationTime =
            0f;

    private OnSeekBarChangeListener
            externalListener;

    private final Runnable animationRunnable =
            new Runnable() {

                @Override
                public void run() {

                    if (!equalizerPlaying) {
                        return;
                    }

                    animateBars();

                    invalidate();

                    postDelayed(
                            this,
                            16
                    );
                }
            };

    public EqualizerSeekBar(
            Context context) {

        super(context);

        initialize();
    }

    public EqualizerSeekBar(
            Context context,
            AttributeSet attrs) {

        super(
                context,
                attrs
        );

        initialize();
    }

    public EqualizerSeekBar(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {

        super(
                context,
                attrs,
                defStyleAttr
        );

        initialize();
    }

    private void initialize() {

        setProgressDrawable(
                null
        );

        setThumb(
                null
        );

        setSplitTrack(
                false
        );

        setPadding(
                0,
                0,
                0,
                0
        );

        activePaint.setStyle(
                Paint.Style.FILL
        );

        inactivePaint.setStyle(
                Paint.Style.FILL
        );

        activePaint.setColor(
                activeColor
        );

        inactivePaint.setColor(
                inactiveColor
        );

        createBars();
    }

    private void createBars() {

        barCount =
                Math.max(
                        MIN_BAR_COUNT,
                        Math.min(
                                MAX_BAR_COUNT,
                                barCount
                        )
                );

        barHeights =
                new float[barCount];

        targetHeights =
                new float[barCount];

        for (int i = 0;
             i < barCount;
             i++) {

            float value =
                    0.25f
                            + random.nextFloat()
                            * 0.55f;

            barHeights[i] =
                    value;

            targetHeights[i] =
                    value;
        }
    }

    /**
     * Set number of equalizer bars.
     *
     * Allowed range:
     * 34 - 100
     */
    public void setBarCount(
            int count) {

        count =
                Math.max(
                        MIN_BAR_COUNT,
                        Math.min(
                                MAX_BAR_COUNT,
                                count
                        )
                );

        if (barCount == count) {
            return;
        }

        barCount =
                count;

        createBars();

        invalidate();
    }

    /**
     * Returns current number of bars.
     */
    public int getBarCount() {

        return barCount;
    }

    /**
     * Minimum supported bar count.
     */
    public int getMinBarCount() {

        return MIN_BAR_COUNT;
    }

    /**
     * Maximum supported bar count.
     */
    public int getMaxBarCount() {

        return MAX_BAR_COUNT;
    }

    @Override
    protected synchronized void onDraw(
            Canvas canvas) {

        int width =
                getWidth();

        int height =
                getHeight();

        if (width <= 0 ||
                height <= 0) {

            return;
        }

        if (barHeights == null ||
                barHeights.length != barCount) {

            createBars();
        }

        /*
         * The equalizer uses the FULL available
         * width of the SeekBar.
         *
         * This is what makes the seekbar long
         * instead of leaving a huge empty area.
         */
        float startX =
                0f;

        float availableWidth =
                width;

        /*
         * Keep bars compact while still filling
         * the entire available width.
         */
        float barWidth;

        float gap;

        if (barCount <= 50) {

            barWidth =
                    dp(3);

            gap =
                    dp(2);

        } else if (barCount <= 75) {

            barWidth =
                    dp(2.5f);

            gap =
                    dp(1.5f);

        } else {

            barWidth =
                    dp(2);

            gap =
                    dp(1);
        }

        float requiredWidth =
                barCount * barWidth
                        + (barCount - 1) * gap;

        /*
         * If the calculated bars are too wide,
         * automatically compress the gap first.
         */
        if (requiredWidth > availableWidth) {

            gap =
                    (
                            availableWidth
                                    - barCount
                                    * barWidth
                    )
                            / (float)
                            Math.max(
                                    1,
                                    barCount - 1
                            );

            /*
             * If even the bars themselves do not fit,
             * calculate a smaller bar width.
             */
            if (gap < 0f) {

                gap =
                        dp(0.5f);

                barWidth =
                        (
                                availableWidth
                                        - (
                                                barCount
                                                        - 1
                                        )
                                        * gap
                        )
                                / (float)
                                barCount;

                barWidth =
                        Math.max(
                                dp(1),
                                barWidth
                        );
            }
        }

        /*
         * Center only if there is a tiny remaining
         * difference. Normally the equalizer occupies
         * the complete width.
         */
        float totalWidth =
                barCount * barWidth
                        + (
                                barCount - 1
                        ) * gap;

        startX =
                Math.max(
                        0f,
                        (
                                width
                                        - totalWidth
                        ) / 2f
                );

        float centerY =
                height / 2f;

        int max =
                getMax();

        int progress =
                getProgress();

        float progressRatio =
                0f;

        if (max > 0) {

            progressRatio =
                    Math.max(
                            0f,
                            Math.min(
                                    1f,
                                    progress
                                            / (float) max
                            )
                    );
        }

        /*
         * Use most of the vertical space.
         */
        float maxHeight =
                height * 0.90f;

        float minHeight =
                Math.max(
                        dp(3),
                        height * 0.10f
                );

        for (int i = 0;
             i < barCount;
             i++) {

            float x =
                    startX
                            + i
                            * (
                                    barWidth
                                            + gap
                            );

            float position =
                    barCount <= 1
                            ? 0f
                            : i
                            / (float)
                            (barCount - 1);

            boolean active =
                    position <= progressRatio;

            float value =
                    barHeights[i];

            /*
             * Animated waveform.
             */
            if (equalizerPlaying) {

                float wave =
                        (float)
                                Math.sin(
                                        animationTime
                                                * 2.2f
                                                + i
                                                * 0.42f
                                );

                float wave2 =
                        (float)
                                Math.sin(
                                        animationTime
                                                * 3.7f
                                                + i
                                                * 0.19f
                                );

                float waveValue =
                        wave * 0.5f
                                + wave2 * 0.25f;

                value +=
                        waveValue * 0.10f;

                value =
                        Math.max(
                                0.08f,
                                Math.min(
                                        1f,
                                        value
                                )
                        );
            }

            float barHeight =
                    minHeight
                            + (
                                    maxHeight
                                            - minHeight
                            )
                                    * value;

            /*
             * Inactive bars are still visible,
             * just dimmer and slightly shorter.
             */
            if (!active) {

                barHeight *=
                        0.42f;
            }

            /*
             * Soft edges.
             */
            float edgeFactor =
                    1f;

            int edgeBars =
                    Math.min(
                            5,
                            barCount / 8
                    );

            if (edgeBars > 0) {

                if (i < edgeBars) {

                    edgeFactor =
                            0.65f
                                    + (
                                            i
                                                    / (float)
                                                    edgeBars
                                    ) * 0.35f;

                } else if (
                        i >= barCount - edgeBars
                ) {

                    edgeFactor =
                            0.65f
                                    + (
                                            (
                                                    barCount
                                                            - 1
                                                            - i
                                            )
                                                    / (float)
                                                    edgeBars
                                    ) * 0.35f;
                }
            }

            barHeight *=
                    edgeFactor;

            float top =
                    centerY
                            - barHeight / 2f;

            float bottom =
                    centerY
                            + barHeight / 2f;

            Paint paint =
                    active
                            ? activePaint
                            : inactivePaint;

            RectF rect =
                    new RectF(
                            x,
                            top,
                            x + barWidth,
                            bottom
                    );

            float radius =
                    Math.min(
                            barWidth / 2f,
                            dp(2)
                    );

            canvas.drawRoundRect(
                    rect,
                    radius,
                    radius,
                    paint
            );
        }
    }

    private void animateBars() {

        long now =
                System.currentTimeMillis();

        if (lastAnimationTime == 0L) {

            lastAnimationTime =
                    now;
        }

        long elapsed =
                now - lastAnimationTime;

        lastAnimationTime =
                now;

        if (elapsed > 100) {
            elapsed = 100;
        }

        float delta =
                elapsed / 1000f;

        animationTime +=
                delta;

        for (int i = 0;
             i < barCount;
             i++) {

            float current =
                    barHeights[i];

            float target =
                    targetHeights[i];

            float difference =
                    target - current;

            float speed =
                    5.5f;

            float interpolation =
                    1f
                            - (float)
                            Math.exp(
                                    -speed
                                            * delta
                            );

            barHeights[i] =
                    current
                            + difference
                            * interpolation;

            if (Math.abs(difference) < 0.025f) {

                float center =
                        0.50f
                                + (
                                        (float)
                                                Math.sin(
                                                        animationTime
                                                                * 1.7f
                                                                + i
                                                                * 0.31f
                                                )
                                )
                                * 0.18f;

                float randomVariation =
                        (
                                random.nextFloat()
                                        - 0.5f
                        ) * 0.22f;

                float next =
                        center
                                + randomVariation;

                targetHeights[i] =
                        Math.max(
                                0.12f,
                                Math.min(
                                        0.95f,
                                        next
                                )
                        );
            }
        }
    }

    public void setEqualizerPlaying(
            boolean playing) {

        if (equalizerPlaying == playing) {

            invalidate();

            return;
        }

        equalizerPlaying =
                playing;

        removeCallbacks(
                animationRunnable
        );

        if (playing) {

            lastAnimationTime =
                    System.currentTimeMillis();

            animationTime =
                    0f;

            for (int i = 0;
                 i < barCount;
                 i++) {

                targetHeights[i] =
                        0.25f
                                + random.nextFloat()
                                * 0.70f;
            }

            post(
                    animationRunnable
            );

        } else {

            for (int i = 0;
                 i < barCount;
                 i++) {

                targetHeights[i] =
                        0.22f;
            }
        }

        invalidate();
    }

    public boolean isEqualizerPlaying() {

        return equalizerPlaying;
    }

    public void setEqualizerColor(
            int color) {

        activeColor =
                color;

        activePaint.setColor(
                activeColor
        );

        updateInactiveColor();

        invalidate();
    }

    public void setEqualizerBackgroundColor(
            int color) {

        backgroundColor =
                color;

        updateInactiveColor();

        invalidate();
    }

    private void updateInactiveColor() {

        int blended =
                blendColors(
                        backgroundColor,
                        activeColor,
                        0.35f
                );

        inactiveColor =
                Color.argb(
                        150,
                        Color.red(blended),
                        Color.green(blended),
                        Color.blue(blended)
                );

        inactivePaint.setColor(
                inactiveColor
        );
    }

    @Override
    public void setOnSeekBarChangeListener(
            OnSeekBarChangeListener listener) {

        externalListener =
                listener;

        super.setOnSeekBarChangeListener(
                listener
        );
    }

    @Override
    public boolean onTouchEvent(
            MotionEvent event) {

        if (!isEnabled()) {
            return false;
        }

        if (event == null) {
            return false;
        }

        switch (event.getActionMasked()) {

            case MotionEvent.ACTION_DOWN:

                trackingTouch =
                        true;

                if (externalListener != null) {

                    externalListener
                            .onStartTrackingTouch(
                                    this
                            );
                }

                updateProgressFromTouch(
                        event.getX()
                );

                return true;

            case MotionEvent.ACTION_MOVE:

                if (!trackingTouch) {
                    return false;
                }

                updateProgressFromTouch(
                        event.getX()
                );

                return true;

            case MotionEvent.ACTION_UP:

                updateProgressFromTouch(
                        event.getX()
                );

                trackingTouch =
                        false;

                if (externalListener != null) {

                    externalListener
                            .onStopTrackingTouch(
                                    this
                            );
                }

                performClick();

                return true;

            case MotionEvent.ACTION_CANCEL:

                trackingTouch =
                        false;

                if (externalListener != null) {

                    externalListener
                            .onStopTrackingTouch(
                                    this
                            );
                }

                return true;
        }

        return true;
    }

    private void updateProgressFromTouch(
            float x) {

        int width =
                getWidth();

        if (width <= 0) {
            return;
        }

        float ratio =
                x / (float) width;

        ratio =
                Math.max(
                        0f,
                        Math.min(
                                1f,
                                ratio
                        )
                );

        int max =
                getMax();

        int newProgress =
                Math.round(
                        ratio * max
                );

        super.setProgress(
                newProgress
        );

        invalidate();

        if (externalListener != null) {

            externalListener
                    .onProgressChanged(
                            this,
                            newProgress,
                            true
                    );
        }
    }

    @Override
    public void setProgress(
            int progress) {

        super.setProgress(
                progress
        );

        invalidate();
    }

    private int blendColors(
            int color1,
            int color2,
            float ratio) {

        ratio =
                Math.max(
                        0f,
                        Math.min(
                                1f,
                                ratio
                        )
                );

        int r =
                Math.round(
                        Color.red(color1)
                                * (1f - ratio)
                                +
                        Color.red(color2)
                                * ratio
                );

        int g =
                Math.round(
                        Color.green(color1)
                                * (1f - ratio)
                                +
                        Color.green(color2)
                                * ratio
                );

        int b =
                Math.round(
                        Color.blue(color1)
                                * (1f - ratio)
                                +
                        Color.blue(color2)
                                * ratio
                );

        return Color.rgb(
                r,
                g,
                b
        );
    }

    private int dp(
            float value) {

        return Math.round(
                value
                        * getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    @Override
    public boolean performClick() {

        super.performClick();

        return true;
    }

    @Override
    protected void onDetachedFromWindow() {

        equalizerPlaying =
                false;

        removeCallbacks(
                animationRunnable
        );

        super.onDetachedFromWindow();
    }
}