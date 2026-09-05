package com.urfavxbf.kanade.ui.youtube;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Transparent gesture surface for the in-app YouTube player.
 *
 * Single tap toggles playback. Double tap on the left/right half seeks
 * backward/forward. The surface deliberately owns no playback state; the
 * process-scoped YouTubePlaybackManager remains the single source of truth.
 */
public final class YouTubeGestureOverlay extends View {

    private static final double SEEK_SECONDS = 10d;
    private static final long STATE_POLL_MS = 250L;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final GestureDetector gestureDetector;
    private final Runnable statePoll = new Runnable() {
        @Override
        public void run() {
            if (!isAttachedToWindow()) {
                return;
            }
            setVisibility(YouTubePlaybackManager.isActive() ? VISIBLE : GONE);
            mainHandler.postDelayed(this, STATE_POLL_MS);
        }
    };

    public YouTubeGestureOverlay(Context context) {
        this(context, null);
    }

    public YouTubeGestureOverlay(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setClickable(true);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setElevation(20f);

        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(@NonNull MotionEvent event) {
                return true;
            }

            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent event) {
                if (YouTubePlaybackManager.isActive()) {
                    YouTubePlaybackManager.toggle();
                }
                return true;
            }

            @Override
            public boolean onDoubleTap(@NonNull MotionEvent event) {
                if (!YouTubePlaybackManager.isActive()) {
                    return true;
                }

                double position = YouTubePlaybackManager.getPositionSeconds();
                double delta = event.getX() < getWidth() / 2f
                        ? -SEEK_SECONDS
                        : SEEK_SECONDS;
                YouTubePlaybackManager.seekTo(position + delta);
                return true;
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mainHandler.removeCallbacks(statePoll);
        mainHandler.post(statePoll);
    }

    @Override
    protected void onDetachedFromWindow() {
        mainHandler.removeCallbacks(statePoll);
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return true;
    }
}
