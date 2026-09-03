package com.urfavxbf.kanade.ui.player;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Build;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class CutoutRippleView extends View {

    /*
     * Mas mahabang animation duration (1.2 seconds)
     */
    private static final long RIPPLE_DURATION = 1200L;

    /*
     * Mas malaking sakop ng ripple (220dp expansion)
     */
    private static final float MAX_RIPPLE_RADIUS_DP = 220f;

    private static final int RIPPLE_LAYERS = 6;

    private final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint outerGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ripplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Rect cutoutRect = new Rect();
    private float cutoutCenterX;
    private float cutoutCenterY;
    private boolean cutoutDetected = false;

    private int accentColor = Color.rgb(201, 196, 255);

    // Audio Analysis
    private float targetEnergy = 0f;
    private float targetBass = 0f;
    private float targetBeatIntensity = 0f;

    private float visualEnergy = 0f;
    private float visualBass = 0f;
    private float visualBeat = 0f;

    private float beatIntensity = 0f;
    private boolean beatDetected = false;

    private long beatStartTime = 0L;
    private long lastBeatStartTime = 0L;

    private float baseRadius = 28f;
    private boolean audioActive = false;

    // Cache gradient allocations
    private final int[] gradientColors = new int[3];
    private final float[] gradientStops = new float[]{0f, 0.45f, 1f};

    public CutoutRippleView(Context context) {
        this(context, null);
    }

    public CutoutRippleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CutoutRippleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        glowPaint.setStyle(Paint.Style.FILL);
        outerGlowPaint.setStyle(Paint.Style.STROKE);
        ripplePaint.setStyle(Paint.Style.STROKE);
        innerPaint.setStyle(Paint.Style.STROKE);

        setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }

    public void setAccentColor(int color) {
        this.accentColor = color;
        invalidate();
    }

    public void setAudioData(float energy, float bass) {
        this.targetEnergy = clamp01(energy);
        this.targetBass = clamp01(bass);
        this.audioActive = targetEnergy > 0.001f || targetBass > 0.001f;
        invalidate();
    }

    public void setBeatDetected(boolean detected, float intensity) {
        if (!detected) {
            beatDetected = false;
            return;
        }

        float newIntensity = clamp01(intensity);
        long now = SystemClock.uptimeMillis();

        if (now == lastBeatStartTime) {
            return;
        }

        lastBeatStartTime = now;
        beatDetected = true;
        targetBeatIntensity = newIntensity;
        beatIntensity = newIntensity;
        beatStartTime = now;
        audioActive = true;

        invalidate();
    }

    public void clearAudioData() {
        targetEnergy = 0f;
        targetBass = 0f;
        targetBeatIntensity = 0f;
        visualEnergy = 0f;
        visualBass = 0f;
        visualBeat = 0f;
        beatIntensity = 0f;
        beatDetected = false;
        beatStartTime = 0L;
        lastBeatStartTime = 0L;
        audioActive = false;
        invalidate();
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            DisplayCutout displayCutout = insets.getDisplayCutout();
            if (displayCutout != null) {
                List<Rect> boundingRects = displayCutout.getBoundingRects();
                if (boundingRects != null && !boundingRects.isEmpty()) {
                    Rect bestRect = null;
                    for (Rect rect : boundingRects) {
                        if (rect == null || rect.isEmpty()) continue;
                        if (bestRect == null || rect.top < bestRect.top) {
                            bestRect = rect;
                        }
                    }

                    if (bestRect != null) {
                        int[] viewLocation = new int[2];
                        getLocationOnScreen(viewLocation);

                        float localLeft = bestRect.left - viewLocation[0];
                        float localTop = bestRect.top - viewLocation[1];
                        float localRight = bestRect.right - viewLocation[0];
                        float localBottom = bestRect.bottom - viewLocation[1];

                        cutoutRect.set(
                                Math.round(localLeft),
                                Math.round(localTop),
                                Math.round(localRight),
                                Math.round(localBottom)
                        );

                        cutoutCenterX = cutoutRect.centerX();
                        cutoutCenterY = cutoutRect.centerY();
                        cutoutDetected = true;
                        invalidate();
                        return super.onApplyWindowInsets(insets);
                    }
                }
            }
        }
        updateFallbackPosition();
        return super.onApplyWindowInsets(insets);
    }

    private void updateFallbackPosition() {
        cutoutDetected = false;
        cutoutCenterX = getWidth() / 2f;
        cutoutCenterY = dpToPx(12f);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (!cutoutDetected) {
            updateFallbackPosition();
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        smoothAudioValues();
        float audioIntensity = clamp01(visualEnergy * 0.48f + visualBass * 0.52f);

        drawAtmosphericGlow(canvas, audioIntensity);
        drawIdleRings(canvas, audioIntensity);

        if (beatStartTime > 0L) {
            long elapsed = SystemClock.uptimeMillis() - beatStartTime;
            if (elapsed >= 0L && elapsed <= RIPPLE_DURATION) {
                drawBeatRipple(canvas, elapsed);
                postInvalidateOnAnimation();
            } else {
                beatStartTime = 0L;
            }
        }

        if (shouldContinueAnimation()) {
            postInvalidateOnAnimation();
        } else {
            audioActive = false;
        }
    }

    private void drawAtmosphericGlow(Canvas canvas, float intensity) {
        if (intensity <= 0.001f) return;

        // Malaking ambient glow na nagmumula mismo sa paligid ng cutout
        float radius = getCutoutRadius() + dpToPx(40f) + dpToPx(80f) * intensity;
        int alpha = clampColor(Math.round(15f + 50f * intensity));

        int r = Color.red(accentColor);
        int g = Color.green(accentColor);
        int b = Color.blue(accentColor);

        gradientColors[0] = Color.argb(alpha, r, g, b);
        gradientColors[1] = Color.argb(Math.round(alpha * 0.35f), r, g, b);
        gradientColors[2] = Color.TRANSPARENT;

        RadialGradient gradient = new RadialGradient(
                cutoutCenterX, cutoutCenterY, radius,
                gradientColors, gradientStops, Shader.TileMode.CLAMP
        );

        glowPaint.setShader(gradient);
        canvas.drawCircle(cutoutCenterX, cutoutCenterY, radius, glowPaint);
        glowPaint.setShader(null);
    }

    private void drawIdleRings(Canvas canvas, float intensity) {
        float base = getCutoutRadius();
        float strokeWidth = dpToPx(1.5f + 1.0f * intensity);

        float breathing = (float) Math.sin(SystemClock.uptimeMillis() * 0.003);
        float breathingOffset = dpToPx(3.0f) * (breathing * 0.5f + 0.5f) * intensity;

        // Tumpak na naka-sakto sa mismong gilid ng cutout (stroke center alignment)
        float exactInnerRadius = base + (strokeWidth / 2f) + breathingOffset;
        int innerAlpha = clampColor(Math.round(40f + 80f * intensity));

        innerPaint.setColor(Color.argb(innerAlpha, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)));
        innerPaint.setStrokeWidth(strokeWidth);
        canvas.drawCircle(cutoutCenterX, cutoutCenterY, exactInnerRadius, innerPaint);

        // Outer idle subtle rings
        for (int i = 1; i <= 3; i++) {
            float distance = dpToPx(12f * i);
            float radius = base + distance + breathingOffset * (1f + i * 0.35f);
            float alphaFactor = 1f - i * 0.25f;
            int alpha = clampColor(Math.round((20f + 40f * intensity) * alphaFactor));

            ripplePaint.setColor(Color.argb(alpha, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)));
            ripplePaint.setStrokeWidth(dpToPx(0.8f + intensity * 0.5f));
            canvas.drawCircle(cutoutCenterX, cutoutCenterY, radius, ripplePaint);
        }
    }

    private void drawBeatRipple(Canvas canvas, long elapsed) {
        float progress = clamp01(elapsed / (float) RIPPLE_DURATION);
        float intensityValue = clamp01(beatIntensity);

        // Deceleration ease curve para sa mas swabeng mahabang animation
        float eased = 1f - (float) Math.pow(1f - progress, 3.0f);
        float expansion = dpToPx(MAX_RIPPLE_RADIUS_DP) * (0.5f + 0.5f * intensityValue);
        
        // Magsisimula ang ripple nang eksakto sa outer edge ng cutout
        float startRadius = getCutoutRadius() + dpToPx(1f);
        float mainRadius = startRadius + expansion * eased;

        // Smooth opacity fadeout sa dulo ng animation
        float fade = (1f - progress);
        fade = fade * fade;

        // Malaking Soft Outer Glow
        float glowRadius = mainRadius + dpToPx(20f + 30f * intensityValue);
        int glowAlpha = clampColor(Math.round((40f + 110f * intensityValue) * fade));

        outerGlowPaint.setColor(Color.argb(glowAlpha, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)));
        outerGlowPaint.setStrokeWidth(dpToPx(6f + 8f * intensityValue));
        canvas.drawCircle(cutoutCenterX, cutoutCenterY, glowRadius, outerGlowPaint);

        // Multi-layer Ripple Waves
        for (int i = 0; i < RIPPLE_LAYERS; i++) {
            float layerProgress = progress - (i * 0.06f);
            if (layerProgress < 0f) continue;

            layerProgress = clamp01(layerProgress);
            float layerEase = 1f - (float) Math.pow(1f - layerProgress, 2.8f);
            float layerSpacing = dpToPx(14f + 8f * intensityValue);
            float radius = startRadius + expansion * layerEase + layerSpacing * i;

            float layerAlphaFactor = 1f - i * 0.14f;
            int alpha = clampColor(Math.round((100f + 155f * intensityValue) * fade * layerAlphaFactor));

            ripplePaint.setColor(Color.argb(alpha, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)));
            float strokeWidth = dpToPx((i == 0 ? 2.2f : 1.2f) + 3.0f * intensityValue * (i == 0 ? 1f : 0.5f));
            ripplePaint.setStrokeWidth(strokeWidth);

            canvas.drawCircle(cutoutCenterX, cutoutCenterY, radius, ripplePaint);
        }

        // Fast Inner Cutout Flash Accent
        float innerPulse = getCutoutRadius() + dpToPx(1.5f) + dpToPx(10f) * (1f - progress) + dpToPx(6f) * intensityValue;
        int innerPulseAlpha = clampColor(Math.round((160f + 95f * intensityValue) * fade));

        innerPaint.setColor(Color.argb(innerPulseAlpha, Color.red(accentColor), Color.green(accentColor), Color.blue(accentColor)));
        innerPaint.setStrokeWidth(dpToPx(2.5f + 2.5f * intensityValue));
        canvas.drawCircle(cutoutCenterX, cutoutCenterY, innerPulse, innerPaint);
    }

    private float getCutoutRadius() {
        if (cutoutDetected && !cutoutRect.isEmpty()) {
            float radius = Math.max(cutoutRect.width(), cutoutRect.height()) / 2f;
            if (radius > 1f) return radius;
        }
        return baseRadius;
    }

    private void smoothAudioValues() {
        visualEnergy += (targetEnergy - visualEnergy) * 0.28f;
        visualBass += (targetBass - visualBass) * 0.36f;

        if (beatDetected) {
            visualBeat += (targetBeatIntensity - visualBeat) * 0.60f;
            beatDetected = false;
        } else {
            visualBeat *= 0.85f;
        }

        if (visualEnergy < 0.001f) visualEnergy = 0f;
        if (visualBass < 0.001f) visualBass = 0f;
        if (visualBeat < 0.001f) visualBeat = 0f;
    }

    private boolean shouldContinueAnimation() {
        return targetEnergy > 0.001f || targetBass > 0.001f || visualEnergy > 0.001f || visualBass > 0.001f || visualBeat > 0.001f || beatStartTime > 0L;
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private int clampColor(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDetachedFromWindow() {
        clearAudioData();
        super.onDetachedFromWindow();
    }
}
