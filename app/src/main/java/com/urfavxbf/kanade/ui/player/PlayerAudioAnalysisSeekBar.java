package com.urfavxbf.kanade.ui.player;

import android.content.Context;
import android.util.AttributeSet;

import com.urfavxbf.kanade.PlayerAudioAnalysisReceiver;

/**
 * EqualizerSeekBar that automatically follows the view lifecycle and consumes
 * the playback service's audio-analysis stream.
 */
public class PlayerAudioAnalysisSeekBar extends EqualizerSeekBar {

    private PlayerAudioAnalysisReceiver audioAnalysisReceiver;

    public PlayerAudioAnalysisSeekBar(Context context) {
        super(context);
    }

    public PlayerAudioAnalysisSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PlayerAudioAnalysisSeekBar(
            Context context,
            AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        audioAnalysisReceiver = new PlayerAudioAnalysisReceiver(this);
        audioAnalysisReceiver.register(getContext());
    }

    @Override
    protected void onDetachedFromWindow() {
        if (audioAnalysisReceiver != null) {
            audioAnalysisReceiver.unregister(getContext());
            audioAnalysisReceiver = null;
        }

        clearFFTData();
        setEqualizerPlaying(false);

        super.onDetachedFromWindow();
    }
}
