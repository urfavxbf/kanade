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

import com.urfavxbf.kanade.MusicPlayerService;
import com.urfavxbf.kanade.R;

/**
 * Lifecycle-bound shuffle/repeat control backed directly by MusicPlayerService.
 */
public class PlayerServiceActionButton extends AppCompatImageButton {

    private int accentColor = Color.rgb(201, 196, 255);
    private boolean shuffleEnabled = false;
    private int repeatMode = MusicPlayerService.REPEAT_OFF;
    private boolean receiverRegistered = false;

    private final BroadcastReceiver stateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null
                    || !MusicPlayerService.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                return;
            }

            shuffleEnabled = intent.getBooleanExtra(
                    MusicPlayerService.EXTRA_SHUFFLE_STATE,
                    shuffleEnabled
            );
            repeatMode = intent.getIntExtra(
                    MusicPlayerService.EXTRA_REPEAT_STATE,
                    repeatMode
            );
            updateVisualState();
        }
    };

    public PlayerServiceActionButton(Context context) {
        super(context);
        initialize();
    }

    public PlayerServiceActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public PlayerServiceActionButton(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), MusicPlayerService.class);

            if (getId() == R.id.btnShuffle) {
                intent.setAction(MusicPlayerService.ACTION_TOGGLE_SHUFFLE);
            } else if (getId() == R.id.btnRepeat) {
                intent.setAction(MusicPlayerService.ACTION_TOGGLE_REPEAT);
            } else {
                return;
            }

            getContext().startService(intent);
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (receiverRegistered) {
            return;
        }

        IntentFilter filter = new IntentFilter(
                MusicPlayerService.ACTION_STATE_CHANGED
        );

        Context context = getContext().getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                    stateReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            ContextCompat.registerReceiver(
                    context,
                    stateReceiver,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );
        }

        receiverRegistered = true;
        requestState();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (receiverRegistered) {
            try {
                getContext().getApplicationContext().unregisterReceiver(stateReceiver);
            } catch (IllegalArgumentException ignored) {
                // Already unregistered.
            }
            receiverRegistered = false;
        }

        super.onDetachedFromWindow();
    }

    private void requestState() {
        Intent intent = new Intent(
                getContext(),
                MusicPlayerService.class
        );
        intent.setAction(MusicPlayerService.ACTION_REQUEST_QUEUE);
        getContext().startService(intent);
    }

    public void setAccentColor(int color) {
        accentColor = color;
        updateVisualState();
    }

    private void updateVisualState() {
        boolean active = getId() == R.id.btnShuffle
                ? shuffleEnabled
                : repeatMode != MusicPlayerService.REPEAT_OFF;

        setColorFilter(active ? accentColor : Color.rgb(168, 171, 185));
        setAlpha(active ? 1f : 0.82f);
    }
}
