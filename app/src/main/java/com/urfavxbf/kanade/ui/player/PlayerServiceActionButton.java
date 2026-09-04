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
        setOnClickListener(view -> toggleState());
    }

    private void toggleState() {
        Intent intent = new Intent(getContext(), MusicPlayerService.class);

        if (getId() == R.id.btnShuffle) {
            intent.setAction(MusicPlayerService.ACTION_TOGGLE_SHUFFLE);
        } else if (getId() == R.id.btnRepeat) {
            intent.setAction(MusicPlayerService.ACTION_TOGGLE_REPEAT);
        } else {
            return;
        }

        getContext().startService(intent);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (receiverRegistered) {
            return;
        }

        Context context = getContext().getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                    stateReceiver,
                    new IntentFilter(MusicPlayerService.ACTION_STATE_CHANGED),
                    Context.RECEIVER_NOT_EXPORTED
            );
            context.registerReceiver(
                    colorReceiver,
                    new IntentFilter(AlbumColorManager.ACTION_COLORS_CHANGED),
                    Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            ContextCompat.registerReceiver(
                    context,
                    stateReceiver,
                    new IntentFilter(MusicPlayerService.ACTION_STATE_CHANGED),
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );
            ContextCompat.registerReceiver(
                    context,
                    colorReceiver,
                    new IntentFilter(AlbumColorManager.ACTION_COLORS_CHANGED),
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );
        }

        receiverRegistered = true;
        accentColor = AlbumColorManager.getInstance(context).getCurrentAccentColor();
        updateVisualState();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (receiverRegistered) {
            Context context = getContext().getApplicationContext();
            try {
                context.unregisterReceiver(stateReceiver);
            } catch (IllegalArgumentException ignored) {
                // Already unregistered.
            }
            try {
                context.unregisterReceiver(colorReceiver);
            } catch (IllegalArgumentException ignored) {
                // Already unregistered.
            }
            receiverRegistered = false;
        }

        super.onDetachedFromWindow();
    }

    public void setPlayerState(boolean shuffleEnabled, int repeatMode) {
        this.shuffleEnabled = shuffleEnabled;
        this.repeatMode = repeatMode;
        updateVisualState();
    }

    public void setAccentColor(int color) {
        accentColor = color;
        updateVisualState();
    }

    private void updateVisualState() {
        if (getId() == R.id.btnShuffle) {
            boolean active = shuffleEnabled;
            setImageResource(
                    active
                            ? R.drawable.ic_shuffle
                            : R.drawable.ic_no_shuffle
            );
            setColorFilter(
                    active
                            ? accentColor
                            : Color.rgb(168, 171, 185)
            );
            setAlpha(1f);
            return;
        }

        if (getId() == R.id.btnRepeat) {
            boolean active = repeatMode != MusicPlayerService.REPEAT_OFF;
            setImageResource(
                    active
                            ? R.drawable.ic_repeat
                            : R.drawable.ic_no_repeat
            );
            setColorFilter(
                    active
                            ? accentColor
                            : Color.rgb(168, 171, 185)
            );
            setAlpha(1f);
        }
    }
}
