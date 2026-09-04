package com.urfavxbf.kanade.ui.player;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageButton;

import com.urfavxbf.kanade.MusicPlayerService;
import com.urfavxbf.kanade.R;

/**
 * Small self-contained player control for service-backed shuffle/repeat state.
 */
public class PlayerServiceActionButton extends AppCompatImageButton {

    private int accentColor = Color.rgb(201, 196, 255);
    private boolean shuffleEnabled = false;
    private int repeatMode = MusicPlayerService.REPEAT_OFF;

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
        boolean active = getId() == R.id.btnShuffle
                ? shuffleEnabled
                : repeatMode != MusicPlayerService.REPEAT_OFF;

        setColorFilter(active ? accentColor : Color.rgb(168, 171, 185));

        if (getId() == R.id.btnRepeat) {
            setAlpha(repeatMode == MusicPlayerService.REPEAT_ONE ? 1f : 0.82f);
        } else {
            setAlpha(active ? 1f : 0.82f);
        }
    }
}
