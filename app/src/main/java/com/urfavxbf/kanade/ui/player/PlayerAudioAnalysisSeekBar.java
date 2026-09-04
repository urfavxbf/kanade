package com.urfavxbf.kanade.ui.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;

import androidx.core.content.ContextCompat;

import com.urfavxbf.kanade.AlbumColorManager;
import com.urfavxbf.kanade.PlayerAudioAnalysisReceiver;

/**
 * Lifecycle-bound playback visualizer using the Kanade capsule/gradient
 * language and a real beat-synchronized pulse.
 */
public class PlayerAudioAnalysisSeekBar extends EqualizerSeekBar {

    private static final int SEGMENT_COUNT = 28;
    private static final float MIN_LEVEL = 0.08f;
    private static final long BEAT_DURATION_MS = 260L;

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint activePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private PlayerAudioAnalysisReceiver audioAnalysisReceiver;
    private boolean colorReceiverRegistered;
    private boolean playing;
    private float audioLevel;
    private float bassLevel;
    private long beatTime;
    private boolean beatActive;

    private int accentColor = Color.rgb(201, 196, 255);
    private int secondaryColor = Color.rgb(139, 132, 232);

    private final BroadcastReceiver colorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null
                    || !AlbumColorManager.ACTION_COLORS_CHANGED.equals(intent.getAction())) {
                return;
            }

            accentColor = intent.getIntExtra(
                    AlbumColorManager.EXTRA_ACCENT_COLOR,
                    accentColor
            );
            updateSecondaryColor();
            invalidate();
        }
    };

    private final Runnable animationRunnable = new Runnable() {
        @Override
        public void run() {
            if (!playing) {
                return;
            }
            invalidate();
            postOnAnimation(this);
        }
    };

    public PlayerAudioAnalysisSeekBar(Context context) {
        super(context);
        initializeModernStyle();
    }

    public PlayerAudioAnalysisSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initializeModernStyle();
    }

    public PlayerAudioAnalysisSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initializeModernStyle();
    }

    private void initializeModernStyle() {
        trackPaint.setStyle(Paint.Style.FILL);
        activePaint.setStyle(Paint.Style.FILL);
        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(dp(1.5f));
        updateSecondaryColor();
    }

    private void updateSecondaryColor() {
        float[] hsv = new float[3];
        Color.colorToHSV(accentColor, hsv);
        hsv[1] = Math.min(1f, hsv[1] * 0.82f);
        hsv[2] = Math.min(1f, hsv[2] * 0.82f + 0.08f);
        secondaryColor = Color.HSVToColor(hsv);
    }

    @Override
    public void setAudioLevel(float level) {
        audioLevel = clamp(level);
        super.setAudioLevel(level);
        invalidate();
    }

    @Override
    public void setBassLevel(float level) {
        bassLevel = clamp(level);
        super.setBassLevel(level);
        invalidate();
    }

    @Override
    public synchronized void setBeatDetected(boolean detected) {
        super.setBeatDetected(detected);
        if (!detected) {
            return;
        }
        beatActive = true;
        beatTime = System.currentTimeMillis();
        invalidate();
    }

    @Override
    public void setEqualizerPlaying(boolean playing) {
        this.playing = playing;
        super.setEqualizerPlaying(playing);
        removeCallbacks(animationRunnable);
        if (playing) {
            postOnAnimation(animationRunnable);
        }
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        Context applicationContext = getContext().getApplicationContext();
        AlbumColorManager colorManager = AlbumColorManager.getInstance(applicationContext);
        accentColor = colorManager.getCurrentAccentColor();
        updateSecondaryColor();

        if (colorReceiverRegistered) {
            return;
        }

        IntentFilter filter = new IntentFilter(AlbumColorManager.ACTION_COLORS_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applicationContext.registerReceiver(
                    colorReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            ContextCompat.registerReceiver(
                    applicationContext,
                    colorReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );
        }
        colorReceiverRegistered = true;

        audioAnalysisReceiver = new PlayerAudioAnalysisReceiver(this);
        audioAnalysisReceiver.register(applicationContext);
    }

    @Override
    protected void onDetachedFromWindow() {
        playing = false;
        removeCallbacks(animationRunnable);

        if (audioAnalysisReceiver != null) {
            audioAnalysisReceiver.unregister(getContext());
            audioAnalysisReceiver = null;
        }

        if (colorReceiverRegistered) {
            try {
                getContext().getApplicationContext().unregisterReceiver(colorReceiver);
            } catch (IllegalArgumentException ignored) {
                // Already unregistered.
            }
            colorReceiverRegistered = false;
        }

        clearFFTData();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        float centerY = height * 0.5f;
        float gap = dp(2f);
        float segmentWidth = Math.max(
                dp(3f),
                (width - gap * (SEGMENT_COUNT - 1)) / SEGMENT_COUNT
        );
        float maxHeight = height * 0.72f;
        float minHeight = Math.max(dp(3f), height * 0.10f);
        float progress = getMax() > 0
                ? getProgress() / (float) getMax()
                : 0f;

        trackPaint.setColor(Color.argb(
                34,
                Color.red(accentColor),
                Color.green(accentColor),
                Color.blue(accentColor)
        ));
        activePaint.setShader(new LinearGradient(
                0f,
                0f,
                width,
                0f,
                secondaryColor,
                accentColor,
                Shader.TileMode.CLAMP
        ));

        float beatPulse = getBeatPulse();
        float audio = Math.max(MIN_LEVEL, audioLevel * 0.78f + bassLevel * 0.30f);

        for (int i = 0; i < SEGMENT_COUNT; i++) {
            float x = i * (segmentWidth + gap);
            float position = i / (float) (SEGMENT_COUNT - 1);
            float shape = (float) (0.35f + 0.65f * Math.sin(position * Math.PI));
            float progressBoost = position <= progress ? 1f : 0.36f;

            float level = MIN_LEVEL + audio * shape * progressBoost;
            level += beatPulse * (0.10f + bassWeight(position) * 0.28f);
            level = clamp(level);

            float barHeight = minHeight + (maxHeight - minHeight) * level;
            float radius = Math.min(segmentWidth, barHeight) * 0.5f;
            RectF rect = new RectF(
                    x,
                    centerY - barHeight * 0.5f,
                    x + segmentWidth,
                    centerY + barHeight * 0.5f
            );

            canvas.drawRoundRect(
                    rect,
                    radius,
                    radius,
                    position <= progress ? activePaint : trackPaint
            );
        }

        if (beatActive) {
            long elapsed = System.currentTimeMillis() - beatTime;
            if (elapsed >= BEAT_DURATION_MS) {
                beatActive = false;
            } else {
                float t = elapsed / (float) BEAT_DURATION_MS;
                float radius = dp(7f) + dp(13f) * t;
                pulsePaint.setColor(Color.argb(
                        (int) (95f * (1f - t)),
                        Color.red(accentColor),
                        Color.green(accentColor),
                        Color.blue(accentColor)
                ));
                canvas.drawCircle(width * progress, centerY, radius, pulsePaint);
                postInvalidateOnAnimation();
            }
        }
    }

    private float getBeatPulse() {
        if (!beatActive) {
            return 0f;
        }

        long elapsed = System.currentTimeMillis() - beatTime;
        if (elapsed >= BEAT_DURATION_MS) {
            beatActive = false;
            return 0f;
        }

        float remaining = 1f - elapsed / (float) BEAT_DURATION_MS;
        return remaining * remaining;
    }

    private float bassWeight(float position) {
        return 1f - Math.min(1f, position * 1.65f);
    }

    private float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}
