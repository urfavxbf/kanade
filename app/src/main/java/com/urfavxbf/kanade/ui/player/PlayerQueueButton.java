package com.urfavxbf.kanade.ui.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import com.urfavxbf.kanade.AlbumColorManager;
import com.urfavxbf.kanade.ui.youtube.YouTubePlaybackManager;
import com.urfavxbf.kanade.ui.youtube.YouTubeQueueDialog;

/**
 * Queue action button.
 *
 * Local playback continues to use the existing QueueBottomSheet. When the
 * process-scoped YouTube player is active, this button routes to the dedicated
 * YouTube queue UI instead of the local music queue.
 */
public class PlayerQueueButton extends AppCompatImageButton {

    private boolean colorReceiverRegistered;
    private int accentColor = Color.rgb(201, 196, 255);
    private OnClickListener delegatedClickListener;

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
            setColorFilter(accentColor);
        }
    };

    public PlayerQueueButton(Context context) {
        super(context);
    }

    public PlayerQueueButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerQueueButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        delegatedClickListener = listener;
        super.setOnClickListener(v -> {
            if (YouTubePlaybackManager.isActive()) {
                new YouTubeQueueDialog(getContext()).show();
                return;
            }

            if (delegatedClickListener != null) {
                delegatedClickListener.onClick(v);
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        Context applicationContext = getContext().getApplicationContext();
        AlbumColorManager colorManager = AlbumColorManager.getInstance(applicationContext);
        accentColor = colorManager.getCurrentAccentColor();
        setColorFilter(accentColor);

        if (colorReceiverRegistered) {
            return;
        }

        IntentFilter filter = new IntentFilter(
                AlbumColorManager.ACTION_COLORS_CHANGED
        );

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
    }

    @Override
    protected void onDetachedFromWindow() {
        if (colorReceiverRegistered) {
            try {
                getContext().getApplicationContext().unregisterReceiver(colorReceiver);
            } catch (IllegalArgumentException ignored) {
                // Already unregistered.
            }
            colorReceiverRegistered = false;
        }

        super.onDetachedFromWindow();
    }
}
