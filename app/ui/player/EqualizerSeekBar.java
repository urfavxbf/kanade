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

    private int barCount = 34;

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
                            70
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

        /*
         * Remove the normal SeekBar visuals.
         */
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

        barHeights =
                new float[barCount];

        targetHeights =
                new float[barCount];

        for (int i = 0; i < barCount; i++) {

            barHeights[i] =
                    0.20f
                            + random.nextFloat()
                            * 0.80f;

            targetHeights[i] =
                    0.20f
                            + random.nextFloat()
                            * 0.80f;
        }
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

        float centerY =
                height / 2f;

        /*
         * Equalizer dimensions.
         */
        float barWidth =
                dp(3);

        float gap =
                dp(3);

        float totalWidth =
                barCount * barWidth
                        + (barCount - 1) * gap;

        /*
         * Scale down spacing if necessary.
         */
        if (totalWidth > width) {

            gap =
                    Math.max(
                            dp(1),
                            (
                                    width
                                            - barCount
                                            * barWidth
                            )
                                    / (float)
                                    (barCount - 1)
                    );

            totalWidth =
                    barCount * barWidth
                            + (barCount - 1)
                            * gap;
        }

        float startX =
                (width - totalWidth)
                        / 2f;

        if (startX < 0) {
            startX = 0;
        }

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

        float maxHeight =
                height * 0.88f;

        float minHeight =
                height * 0.16f;

        for (int i = 0;
             i < barCount;
             i++) {

            float x =
                    startX
                            + i
                            * (barWidth + gap);

            float position =
                    i
                            / (float)
                            (barCount - 1);

            boolean active =
                    position <= progressRatio;

            float value =
                    barHeights[i];

            /*
             * Active bars are taller.
             */
            float barHeight =
                    minHeight
                            + (
                                    maxHeight
                                            - minHeight
                            )
                                    * value;

            if (!active) {

                /*
                 * Inactive bars are still visible,
                 * but much more subtle.
                 */
                barHeight *= 0.48f;
            }

            /*
             * Make the ends slightly smaller.
             * This gives it a more natural
             * waveform/equalizer appearance.
             */
            float edgeFactor =
                    1f;

            if (i < 3) {

                edgeFactor =
                        0.55f
                                + i * 0.15f;

            } else if (i >= barCount - 3) {

                edgeFactor =
                        0.55f
                                + (
                                        barCount
                                                - 1
                                                - i
                                )
                                * 0.15f;
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

            canvas.drawRoundRect(
                    rect,
                    barWidth / 2f,
                    barWidth / 2f,
                    paint
            );
        }
    }

    private void animateBars() {

        long now =
                System.currentTimeMillis();

        if (now - lastAnimationTime < 50) {
            return;
        }

        lastAnimationTime =
                now;

        for (int i = 0;
             i < barCount;
             i++) {

            float current =
                    barHeights[i];

            float target =
                    targetHeights[i];

            float difference =
                    target - current;

            /*
             * Smooth movement.
             */
            barHeights[i] =
                    current
                            + difference
                            * 0.38f;

            /*
             * Pick a new target once
             * the bar is close enough.
             */
            if (Math.abs(difference)
                    < 0.06f) {

                targetHeights[i] =
                        0.12f
                                + random.nextFloat()
                                * 0.88f;
            }
        }
    }

    public void setEqualizerPlaying(
            boolean playing) {

        if (equalizerPlaying == playing) {
            return;
        }

        equalizerPlaying =
                playing;

        removeCallbacks(
                animationRunnable
        );

        if (playing) {

            post(
                    animationRunnable
            );
        }

        invalidate();
    }

    public boolean isEqualizerPlaying() {

        return equalizerPlaying;
    }

    /*
     * Main album accent color.
     */
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

    /*
     * Album background is also used to
     * calculate the inactive bar appearance.
     */
    public void setEqualizerBackgroundColor(
            int color) {

        backgroundColor =
                color;

        updateInactiveColor();

        invalidate();
    }

    private void updateInactiveColor() {

        /*
         * Blend the accent toward the album
         * background instead of simply using
         * transparent white.
         */
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

    /*
     * Keep the existing SeekBar API.
     */
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
