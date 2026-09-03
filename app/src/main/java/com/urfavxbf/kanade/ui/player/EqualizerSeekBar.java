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

public class EqualizerSeekBar extends AppCompatSeekBar {

    private final Paint activePaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Paint inactivePaint =
            new Paint(Paint.ANTI_ALIAS_FLAG);

    private float[] barHeights;
    private float[] targetHeights;

    private float[] fftMagnitudes;

    private int barCount = 100;

    private static final int MIN_BAR_COUNT = 34;
    private static final int MAX_BAR_COUNT = 100;

    private static final float MIN_FREQUENCY = 20f;
    private static final float MAX_FREQUENCY = 16000f;

    private static final float MIN_VISIBLE_LEVEL = 0.06f;

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

    private boolean hasFFTData =
            false;

    private boolean beatDetected =
            false;

    private float audioLevel =
            0f;

    private float bassLevel =
            0f;

    private int fftSampleRate =
            44100;

    private long lastFrameTime =
            0L;

    private long beatPulseTime =
            0L;

    private static final long BEAT_PULSE_DURATION =
            220L;

    private OnSeekBarChangeListener
            externalListener;

    private final Runnable animationRunnable =
            new Runnable() {

                @Override
                public void run() {

                    if (!equalizerPlaying) {
                        return;
                    }

                    updateAnimation();

                    invalidate();

                    postOnAnimation(
                            this
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

            barHeights[i] =
                    MIN_VISIBLE_LEVEL;

            targetHeights[i] =
                    MIN_VISIBLE_LEVEL;
        }
    }

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

    public int getBarCount() {

        return barCount;
    }

    public int getMinBarCount() {

        return MIN_BAR_COUNT;
    }

    public int getMaxBarCount() {

        return MAX_BAR_COUNT;
    }

    /**
     * Receives raw FFT data from MusicPlayerService.
     *
     * Android Visualizer FFT format:
     *
     * [real0, imag0, real1, imag1, ...]
     *
     * This method remains for compatibility with
     * existing PlayerFragment code.
     */
    public synchronized void setFFTData(
            byte[] fft) {

        setFFTData(
                fft,
                fftSampleRate
        );
    }

    /**
     * Receives raw FFT data together with the actual
     * audio sample rate used by the analyzer.
     *
     * Supplying the real sample rate makes the
     * frequency mapping considerably more accurate.
     */
    public synchronized void setFFTData(
            byte[] fft,
            int sampleRate) {

        if (fft == null ||
                fft.length < 4) {

            hasFFTData =
                    false;

            return;
        }

        if (sampleRate > 0) {

            fftSampleRate =
                    sampleRate;
        }

        int magnitudeCount =
                fft.length / 2;

        if (magnitudeCount <= 0) {

            hasFFTData =
                    false;

            return;
        }

        if (fftMagnitudes == null ||
                fftMagnitudes.length != magnitudeCount) {

            fftMagnitudes =
                    new float[magnitudeCount];
        }

        for (int i = 0;
             i < magnitudeCount;
             i++) {

            int realIndex =
                    i * 2;

            int imaginaryIndex =
                    realIndex + 1;

            if (imaginaryIndex >= fft.length) {
                break;
            }

            /*
             * Visualizer FFT values are signed bytes.
             */
            float real =
                    fft[realIndex];

            float imaginary =
                    fft[imaginaryIndex];

            float magnitude =
                    (float)
                            Math.sqrt(
                                    real * real
                                            +
                                    imaginary
                                            * imaginary
                            );

            /*
             * Normalize.
             */
            magnitude /=
                    128f;

            /*
             * Log compression.
             *
             * This makes quieter frequencies visible
             * without allowing large peaks to dominate
             * the entire visualizer.
             */
            magnitude =
                    (float)
                            Math.log1p(
                                    magnitude * 8f
                            )
                                    /
                            (float)
                                    Math.log1p(
                                            8f
                                    );

            magnitude =
                    clamp(
                            magnitude
                    );

            /*
             * FFT temporal smoothing.
             *
             * Attack is intentionally faster than
             * decay so transients remain visible.
             */
            float old =
                    fftMagnitudes[i];

            float coefficient;

            if (magnitude > old) {

                coefficient =
                        0.42f;

            } else {

                coefficient =
                        0.18f;
            }

            fftMagnitudes[i] =
                    old
                            +
                    (
                            magnitude
                                    -
                            old
                    )
                            *
                    coefficient;
        }

        hasFFTData =
                true;
    }

    /**
     * Clears FFT data.
     *
     * Used when playback stops or the Fragment
     * is destroyed.
     */
    public synchronized void clearFFTData() {

        hasFFTData =
                false;

        if (fftMagnitudes != null) {

            for (int i = 0;
                 i < fftMagnitudes.length;
                 i++) {

                fftMagnitudes[i] =
                        0f;
            }
        }

        audioLevel =
                0f;

        bassLevel =
                0f;

        beatDetected =
                false;

        beatPulseTime =
                0L;

        invalidate();
    }

    /**
     * Receives general audio energy.
     *
     * Expected range:
     * 0.0 - 1.0
     */
    public synchronized void setAudioLevel(
            float level) {

        audioLevel =
                clamp(
                        level
                );
    }

    /**
     * Receives bass energy.
     *
     * Expected range:
     * 0.0 - 1.0
     */
    public synchronized void setBassLevel(
            float level) {

        bassLevel =
                clamp(
                        level
                );
    }

    /**
     * Called by PlayerFragment when the service
     * detects an actual transient/beat.
     *
     * There is NO internal timer generating beats.
     */
    public synchronized void setBeatDetected(
            boolean detected) {

        if (!detected) {

            return;
        }

        beatDetected =
                true;

        beatPulseTime =
                System.currentTimeMillis();
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

        float availableWidth =
                width;

        float requiredWidth =
                barCount * barWidth
                        +
                (
                        barCount - 1
                )
                        * gap;

        if (requiredWidth > availableWidth) {

            gap =
                    (
                            availableWidth
                                    -
                            barCount
                                    * barWidth
                    )
                            /
                    (float)
                            Math.max(
                                    1,
                                    barCount - 1
                            );

            if (gap < 0f) {

                gap =
                        dp(0.5f);

                barWidth =
                        (
                                availableWidth
                                        -
                                (
                                        barCount - 1
                                )
                                        * gap
                        )
                                /
                        (float)
                                barCount;

                barWidth =
                        Math.max(
                                dp(1),
                                barWidth
                        );
            }
        }

        float totalWidth =
                barCount * barWidth
                        +
                (
                        barCount - 1
                )
                        * gap;

        float startX =
                Math.max(
                        0f,
                        (
                                width
                                        -
                                totalWidth
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
                                            /
                                    (float)
                                            max
                            )
                    );
        }

        float maxHeight =
                height * 0.90f;

        float minHeight =
                Math.max(
                        dp(3),
                        height * 0.10f
                );

        float beatPulse =
                getBeatPulse();

        for (int i = 0;
             i < barCount;
             i++) {

            float x =
                    startX
                            +
                    i
                            *
                    (
                            barWidth
                                    +
                            gap
                    );

            float position =
                    barCount <= 1
                            ? 0f
                            : i
                            /
                            (float)
                            (
                                    barCount - 1
                            );

            boolean active =
                    position
                            <=
                    progressRatio;

            float value =
                    barHeights[i];

            /*
             * Actual audio energy.
             */
            value =
                    value * 0.78f
                            +
                    audioLevel * 0.22f;

            /*
             * Bass emphasis.
             */
            float bassWeight =
                    getBassWeight(
                            position
                    );

            value +=
                    bassLevel
                            *
                    bassWeight
                            *
                    0.30f;

            /*
             * Actual beat impulse.
             *
             * This only exists after the service has
             * reported a detected beat.
             */
            value +=
                    beatPulse
                            *
                    (
                            0.06f
                                    +
                            bassWeight
                                    *
                            0.20f
                    );

            value =
                    clamp(
                            value
                    );

            /*
             * Keep the bars minimally visible during
             * active playback, but do not animate them
             * artificially.
             */
            if (equalizerPlaying &&
                    value < MIN_VISIBLE_LEVEL) {

                value =
                        MIN_VISIBLE_LEVEL;
            }

            float barHeight =
                    minHeight
                            +
                    (
                            maxHeight
                                    -
                            minHeight
                    )
                            *
                    value;

            /*
             * Inactive section remains visible.
             */
            if (!active) {

                barHeight *=
                        0.42f;
            }

            /*
             * Smooth edge falloff.
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
                                    +
                            (
                                    i
                                            /
                                    (float)
                                    edgeBars
                            )
                                    * 0.35f;

                } else if (
                        i >=
                                barCount
                                        -
                                edgeBars
                ) {

                    edgeFactor =
                            0.65f
                                    +
                            (
                                    (
                                            barCount
                                                    - 1
                                                    - i
                                    )
                                            /
                                    (float)
                                    edgeBars
                            )
                                    * 0.35f;
                }
            }

            barHeight *=
                    edgeFactor;

            float top =
                    centerY
                            -
                    barHeight / 2f;

            float bottom =
                    centerY
                            +
                    barHeight / 2f;

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

    /**
     * Converts a bar's normalized position into
     * an actual FFT frequency.
     */
    private float getFrequencyForPosition(
            float position) {

        double minLog =
                Math.log(
                        MIN_FREQUENCY
                );

        double maxLog =
                Math.log(
                        Math.min(
                                MAX_FREQUENCY,
                                fftSampleRate / 2f
                        )
                );

        if (maxLog <= minLog) {

            return MIN_FREQUENCY;
        }

        return (float)
                Math.exp(
                        minLog
                                +
                        (
                                maxLog
                                        -
                                minLog
                        )
                                * position
                );
    }

    /**
     * Converts an actual frequency into an FFT
     * bin index.
     */
    private int frequencyToFFTIndex(
            float frequency) {

        if (fftMagnitudes == null ||
                fftMagnitudes.length == 0) {

            return 0;
        }

        float nyquist =
                fftSampleRate / 2f;

        if (nyquist <= 0f) {

            nyquist =
                    22050f;
        }

        frequency =
                Math.max(
                        0f,
                        Math.min(
                                nyquist,
                                frequency
                        )
                );

        float normalized =
                frequency
                        /
                nyquist;

        int index =
                Math.round(
                        normalized
                                *
                        (
                                fftMagnitudes.length - 1
                        )
                );

        return Math.max(
                0,
                Math.min(
                        fftMagnitudes.length - 1,
                        index
                )
        );
    }

    private float getFrequencyValue(
            float position,
            int barIndex) {

        if (!hasFFTData ||
                fftMagnitudes == null ||
                fftMagnitudes.length == 0) {

            return MIN_VISIBLE_LEVEL;
        }

        float frequency =
                getFrequencyForPosition(
                        position
                );

        int index =
                frequencyToFFTIndex(
                        frequency
                );

        /*
         * Wider averaging at low frequencies,
         * narrower averaging at high frequencies.
         */
        int radius;

        if (frequency < 150f) {

            radius =
                    Math.max(
                            2,
                            fftMagnitudes.length / 48
                    );

        } else if (frequency < 1000f) {

            radius =
                    Math.max(
                            1,
                            fftMagnitudes.length / 80
                    );

        } else {

            radius =
                    Math.max(
                            1,
                            fftMagnitudes.length / 120
                    );
        }

        float total =
                0f;

        int count =
                0;

        int start =
                Math.max(
                        1,
                        index - radius
                );

        int end =
                Math.min(
                        fftMagnitudes.length - 1,
                        index + radius
                );

        for (int i = start;
             i <= end;
             i++) {

            total +=
                    fftMagnitudes[i];

            count++;
        }

        if (count <= 0) {

            return MIN_VISIBLE_LEVEL;
        }

        float value =
                total
                        /
                count;

        /*
         * Musical compression.
         */
        value =
                (float)
                        Math.pow(
                                value,
                                0.72f
                        );

        /*
         * Additional bass emphasis.
         *
         * This is applied according to the actual
         * frequency rather than merely the visual
         * position.
         */
        if (frequency <= 250f) {

            float bassFactor =
                    1f
                            -
                    (
                            frequency
                                    /
                            250f
                    );

            value +=
                    bassFactor
                            *
                    bassLevel
                            *
                    0.22f;
        }

        return clamp(
                value
        );
    }

    private float getBassWeight(
            float position) {

        if (position >= 0.35f) {

            return 0f;
        }

        float value =
                1f
                        -
                (
                        position
                                /
                        0.35f
                );

        return value * value;
    }

    /**
     * Smooths bar movement toward actual FFT targets.
     *
     * Fast attack:
     *   makes kicks/snare transients visible.
     *
     * Slow decay:
     *   prevents jitter.
     */
    private void updateAnimation() {

        long now =
                System.currentTimeMillis();

        if (lastFrameTime == 0L) {

            lastFrameTime =
                    now;
        }

        long elapsed =
                now - lastFrameTime;

        lastFrameTime =
                now;

        if (elapsed > 100L) {

            elapsed =
                    100L;
        }

        if (elapsed < 0L) {

            elapsed =
                    0L;
        }

        float delta =
                elapsed / 1000f;

        for (int i = 0;
             i < barCount;
             i++) {

            float current =
                    barHeights[i];

            float target =
                    getTargetHeight(
                            i
                    );

            float difference =
                    target - current;

            /*
             * Faster response when rising,
             * slower response when falling.
             */
            float speed;

            if (difference > 0f) {

                speed =
                        12.0f;

            } else {

                speed =
                        5.5f;
            }

            float interpolation =
                    1f
                            -
                    (float)
                            Math.exp(
                                    -speed
                                            * delta
                            );

            barHeights[i] =
                    current
                            +
                    difference
                            *
                    interpolation;

            targetHeights[i] =
                    target;
        }
    }

    private float getTargetHeight(
            int index) {

        if (!equalizerPlaying) {

            return MIN_VISIBLE_LEVEL;
        }

        if (!hasFFTData ||
                fftMagnitudes == null ||
                fftMagnitudes.length == 0) {

            /*
             * IMPORTANT:
             *
             * No FFT = no fake animation.
             */
            return MIN_VISIBLE_LEVEL;
        }

        float position =
                barCount <= 1
                        ? 0f
                        : index
                        /
                        (float)
                        (
                                barCount - 1
                        );

        float value =
                getFrequencyValue(
                        position,
                        index
                );

        /*
         * General audio energy.
         */
        value =
                value * 0.84f
                        +
                audioLevel * 0.16f;

        /*
         * Bass response.
         */
        value +=
                bassLevel
                        *
                getBassWeight(
                        position
                )
                        *
                0.24f;

        return clamp(
                value
        );
    }

    private float getBeatPulse() {

        if (!beatDetected ||
                beatPulseTime <= 0L) {

            return 0f;
        }

        long elapsed =
                System.currentTimeMillis()
                        -
                beatPulseTime;

        if (elapsed >=
                BEAT_PULSE_DURATION) {

            beatDetected =
                    false;

            beatPulseTime =
                    0L;

            return 0f;
        }

        float progress =
                elapsed
                        /
                (float)
                BEAT_PULSE_DURATION;

        /*
         * Fast attack + smooth decay.
         */
        float pulse =
                1f - progress;

        return pulse * pulse;
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

            lastFrameTime =
                    System.currentTimeMillis();

            post(
                    animationRunnable
            );

        } else {

            lastFrameTime =
                    0L;

            beatDetected =
                    false;

            beatPulseTime =
                    0L;

            audioLevel =
                    0f;

            bassLevel =
                    0f;

            if (barHeights != null) {

                for (int i = 0;
                     i < barHeights.length;
                     i++) {

                    barHeights[i] =
                            MIN_VISIBLE_LEVEL;

                    targetHeights[i] =
                            MIN_VISIBLE_LEVEL;
                }
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
            @Nullable OnSeekBarChangeListener listener) {

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

    private float clamp(
            float value) {

        return Math.max(
                0f,
                Math.min(
                        1f,
                        value
                )
        );
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
                        *
                getResources()
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