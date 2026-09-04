package com.urfavxbf.kanade;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Application entry point for Kanade-wide visual behavior.
 *
 * Existing vector geometry is preserved while icon rendering follows the
 * active album palette. This keeps view IDs, click behavior and layouts intact.
 */
public class KanadeApplication extends Application
        implements Application.ActivityLifecycleCallbacks {

    private final Map<Activity, IconSession> sessions =
            new WeakHashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();
        registerActivityLifecycleCallbacks(this);
    }

    @Override
    public void onActivityCreated(
            @NonNull Activity activity,
            @Nullable Bundle savedInstanceState) {
        IconSession session = new IconSession(activity);
        sessions.put(activity, session);
        session.start();
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        IconSession session = sessions.remove(activity);
        if (session != null) {
            session.stop();
        }
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
    }

    @Override
    public void onActivitySaveInstanceState(
            @NonNull Activity activity,
            @NonNull Bundle outState) {
    }

    private static final class IconSession {

        private static final long SCAN_DELAY_MS = 80L;
        private static final float MAX_ICON_DP = 48f;

        private final WeakReference<Activity> activityRef;
        private final Handler mainHandler =
                new Handler(Looper.getMainLooper());

        private final BroadcastReceiver colorReceiver =
                new BroadcastReceiver() {
                    @Override
                    public void onReceive(
                            Context context,
                            Intent intent) {
                        if (!AlbumColorManager.ACTION_COLORS_CHANGED.equals(
                                intent.getAction())) {
                            return;
                        }

                        int accent = intent.getIntExtra(
                                AlbumColorManager.EXTRA_ACCENT_COLOR,
                                Color.rgb(201, 196, 255)
                        );
                        updateColors(accent);
                    }
                };

        private final Runnable scanRunnable =
                new Runnable() {
                    @Override
                    public void run() {
                        scan();
                    }
                };

        private final ViewTreeObserver.OnGlobalLayoutListener layoutListener =
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        scheduleScan();
                    }
                };

        private final Map<GradientIconDrawable, Boolean> iconDrawables =
                new WeakHashMap<>();

        private int accentColor =
                Color.rgb(201, 196, 255);

        private IconSession(@NonNull Activity activity) {
            activityRef = new WeakReference<>(activity);
        }

        private void start() {
            Activity activity = activityRef.get();
            if (activity == null) {
                return;
            }

            accentColor = AlbumColorManager
                    .getInstance(activity.getApplicationContext())
                    .getCurrentAccentColor();

            IntentFilter filter = new IntentFilter(
                    AlbumColorManager.ACTION_COLORS_CHANGED
            );

            ContextCompat.registerReceiver(
                    activity.getApplicationContext(),
                    colorReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );

            View decor = activity.getWindow().getDecorView();
            ViewTreeObserver observer = decor.getViewTreeObserver();
            if (observer.isAlive()) {
                observer.addOnGlobalLayoutListener(layoutListener);
            }

            scheduleScan();
        }

        private void stop() {
            Activity activity = activityRef.get();
            mainHandler.removeCallbacks(scanRunnable);

            if (activity != null) {
                try {
                    activity.getApplicationContext()
                            .unregisterReceiver(colorReceiver);
                } catch (IllegalArgumentException ignored) {
                }

                View decor = activity.getWindow().getDecorView();
                ViewTreeObserver observer = decor.getViewTreeObserver();
                if (observer.isAlive()) {
                    observer.removeOnGlobalLayoutListener(layoutListener);
                }
            }

            iconDrawables.clear();
        }

        private void scheduleScan() {
            mainHandler.removeCallbacks(scanRunnable);
            mainHandler.postDelayed(scanRunnable, SCAN_DELAY_MS);
        }

        private void scan() {
            Activity activity = activityRef.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }

            applyToView(activity.getWindow().getDecorView());
        }

        private void applyToView(@NonNull View view) {
            if (view instanceof ImageView) {
                applyToImageView((ImageView) view);
            }

            if (!(view instanceof ViewGroup)) {
                return;
            }

            ViewGroup group = (ViewGroup) view;
            for (int index = 0; index < group.getChildCount(); index++) {
                applyToView(group.getChildAt(index));
            }
        }

        private void applyToImageView(@NonNull ImageView imageView) {
            Drawable drawable = imageView.getDrawable();
            if (drawable == null) {
                return;
            }

            if (drawable instanceof GradientIconDrawable) {
                GradientIconDrawable gradient =
                        (GradientIconDrawable) drawable;
                gradient.setColors(accentColor);
                iconDrawables.put(gradient, Boolean.TRUE);
                return;
            }

            if (!isVectorIcon(drawable) || !isIconSized(drawable)) {
                return;
            }

            GradientIconDrawable gradient =
                    new GradientIconDrawable(drawable, accentColor);
            imageView.setImageDrawable(gradient);
            iconDrawables.put(gradient, Boolean.TRUE);
        }

        private boolean isVectorIcon(@NonNull Drawable drawable) {
            String name = drawable.getClass().getName();
            return name.contains("VectorDrawable")
                    && !name.contains("AnimatedVectorDrawable");
        }

        private boolean isIconSized(@NonNull Drawable drawable) {
            Activity activity = activityRef.get();
            if (activity == null) {
                return false;
            }

            float density = activity.getResources().getDisplayMetrics().density;
            int maxPixels = Math.round(MAX_ICON_DP * density);
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();

            if (width <= 0 || height <= 0) {
                return false;
            }

            return width <= maxPixels && height <= maxPixels;
        }

        private void updateColors(int newAccentColor) {
            if (accentColor == newAccentColor) {
                return;
            }

            accentColor = newAccentColor;
            for (GradientIconDrawable drawable : iconDrawables.keySet()) {
                if (drawable != null) {
                    drawable.setColors(newAccentColor);
                }
            }
            scheduleScan();
        }
    }

    /**
     * Renders an existing vector as an alpha mask over an album-derived
     * gradient. The source vector remains the shape definition.
     */
    private static final class GradientIconDrawable extends Drawable
            implements Drawable.Callback {

        private final Drawable source;
        private final Paint gradientPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Rect sourceBounds = new Rect();

        private Bitmap maskBitmap;
        private Bitmap outputBitmap;
        private Canvas maskCanvas;
        private Canvas outputCanvas;
        private int accentColor;
        private int lastWidth;
        private int lastHeight;
        private boolean dirty = true;

        private GradientIconDrawable(
                @NonNull Drawable drawable,
                int accentColor) {
            Drawable.ConstantState state = drawable.getConstantState();
            source = state != null
                    ? state.newDrawable().mutate()
                    : drawable.mutate();
            source.setCallback(this);
            this.accentColor = accentColor;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            Rect bounds = getBounds();
            int width = bounds.width();
            int height = bounds.height();
            if (width <= 0 || height <= 0) {
                return;
            }

            ensureBitmap(width, height);
            if (dirty) {
                rebuild(width, height);
            }

            Paint outputPaint = new Paint(
                    Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG
            );
            canvas.drawBitmap(outputBitmap, null, bounds, outputPaint);
        }

        @Override
        protected void onBoundsChange(@NonNull Rect bounds) {
            super.onBoundsChange(bounds);
            dirty = true;
        }

        @Override
        public void setAlpha(int alpha) {
            gradientPaint.setAlpha(alpha);
            dirty = true;
            invalidateSelf();
        }

        @Override
        public int getAlpha() {
            return gradientPaint.getAlpha();
        }

        @Override
        public void setColorFilter(@Nullable android.graphics.ColorFilter colorFilter) {
            // The album-derived gradient owns icon color treatment.
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }

        @Override
        public int getIntrinsicWidth() {
            return source.getIntrinsicWidth();
        }

        @Override
        public int getIntrinsicHeight() {
            return source.getIntrinsicHeight();
        }

        @Override
        protected boolean onStateChange(@NonNull int[] state) {
            boolean changed = source.setState(state);
            if (changed) {
                dirty = true;
                invalidateSelf();
            }
            return changed;
        }

        @Override
        public boolean isStateful() {
            return source.isStateful();
        }

        @Override
        public boolean setLevel(int level) {
            boolean changed = source.setLevel(level);
            if (changed) {
                dirty = true;
                invalidateSelf();
            }
            return changed;
        }

        @Override
        public boolean setVisible(
                boolean visible,
                boolean restart) {
            boolean changed = super.setVisible(visible, restart);
            source.setVisible(visible, restart);
            if (changed) {
                dirty = true;
                invalidateSelf();
            }
            return changed;
        }

        @Override
        public void invalidateDrawable(@NonNull Drawable who) {
            dirty = true;
            invalidateSelf();
        }

        @Override
        public void scheduleDrawable(
                @NonNull Drawable who,
                @NonNull Runnable what,
                long when) {
            scheduleSelf(what, when);
        }

        @Override
        public void unscheduleDrawable(
                @NonNull Drawable who,
                @NonNull Runnable what) {
            unscheduleSelf(what);
        }

        private void setColors(int newAccentColor) {
            if (accentColor == newAccentColor) {
                return;
            }

            accentColor = newAccentColor;
            dirty = true;
            invalidateSelf();
        }

        private void ensureBitmap(int width, int height) {
            if (width == lastWidth
                    && height == lastHeight
                    && maskBitmap != null
                    && outputBitmap != null) {
                return;
            }

            recycleBitmaps();

            maskBitmap = Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888
            );
            outputBitmap = Bitmap.createBitmap(
                    width,
                    height,
                    Bitmap.Config.ARGB_8888
            );
            maskCanvas = new Canvas(maskBitmap);
            outputCanvas = new Canvas(outputBitmap);
            lastWidth = width;
            lastHeight = height;
            dirty = true;
        }

        private void rebuild(int width, int height) {
            maskCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            outputCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

            sourceBounds.set(0, 0, width, height);
            source.setBounds(sourceBounds);
            source.draw(maskCanvas);

            int highlight = createHighlightColor(accentColor);
            int shadow = createShadowColor(accentColor);

            Shader shader = new LinearGradient(
                    0f,
                    0f,
                    width,
                    height,
                    new int[]{highlight, accentColor, shadow},
                    new float[]{0f, 0.55f, 1f},
                    Shader.TileMode.CLAMP
            );

            gradientPaint.setShader(shader);
            gradientPaint.setXfermode(new PorterDuffXfermode(
                    PorterDuff.Mode.SRC_IN
            ));
            outputCanvas.drawBitmap(maskBitmap, 0f, 0f, null);
            outputCanvas.drawRect(
                    0f,
                    0f,
                    width,
                    height,
                    gradientPaint
            );
            gradientPaint.setXfermode(null);
            gradientPaint.setShader(null);
            dirty = false;
        }

        private int createHighlightColor(int color) {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[1] = Math.max(0.20f, hsv[1] * 0.82f);
            hsv[2] = Math.min(1f, hsv[2] * 1.22f);
            return Color.HSVToColor(hsv);
        }

        private int createShadowColor(int color) {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[0] = (hsv[0] + 18f) % 360f;
            hsv[1] = Math.min(1f, hsv[1] * 0.95f);
            hsv[2] = Math.max(0.42f, hsv[2] * 0.72f);
            return Color.HSVToColor(hsv);
        }

        private void recycleBitmaps() {
            if (maskBitmap != null && !maskBitmap.isRecycled()) {
                maskBitmap.recycle();
            }
            if (outputBitmap != null && !outputBitmap.isRecycled()) {
                outputBitmap.recycle();
            }
            maskBitmap = null;
            outputBitmap = null;
            maskCanvas = null;
            outputCanvas = null;
            lastWidth = 0;
            lastHeight = 0;
        }
    }
}
