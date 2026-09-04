package com.urfavxbf.kanade.ui.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import com.urfavxbf.kanade.AlbumColorManager;

/**
 * Queue action button.
 *
 * Queue presentation is owned by PlayerFragment/QueueBottomSheet.
 * This view intentionally contains no second queue receiver or sheet
 * controller, preventing one queue request from opening two sheets.
 */
public class PlayerQueueButton extends AppCompatImageButton {

    private boolean colorReceiverRegistered;
    private int accentColor = Color.rgb(201, 196, 255);

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
