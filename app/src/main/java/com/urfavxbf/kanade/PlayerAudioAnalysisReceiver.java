package com.urfavxbf.kanade;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;

import androidx.core.content.ContextCompat;

import com.urfavxbf.kanade.ui.player.EqualizerSeekBar;

/**
 * Lifecycle-bound bridge between MusicPlayerService audio analysis broadcasts
 * and the full-player equalizer view.
 */
public class PlayerAudioAnalysisReceiver extends BroadcastReceiver {

    private final EqualizerSeekBar equalizerSeekBar;

    private boolean registered = false;

    public PlayerAudioAnalysisReceiver(EqualizerSeekBar equalizerSeekBar) {
        this.equalizerSeekBar = equalizerSeekBar;
    }

    public void register(Context context) {
        if (registered || equalizerSeekBar == null || context == null) {
            return;
        }

        IntentFilter filter = new IntentFilter(
                MusicPlayerService.ACTION_AUDIO_ANALYSIS
        );

        Context applicationContext = context.getApplicationContext();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            applicationContext.registerReceiver(
                    this,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            ContextCompat.registerReceiver(
                    applicationContext,
                    this,
                    filter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );
        }

        registered = true;
    }

    public void unregister(Context context) {
        if (!registered || context == null) {
            return;
        }

        try {
            context.getApplicationContext().unregisterReceiver(this);
        } catch (IllegalArgumentException ignored) {
            // Already unregistered.
        }

        registered = false;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || equalizerSeekBar == null) {
            return;
        }

        if (!MusicPlayerService.ACTION_AUDIO_ANALYSIS.equals(intent.getAction())) {
            return;
        }

        byte[] fft = intent.getByteArrayExtra(
                MusicPlayerService.EXTRA_FFT
        );

        int sampleRate = intent.getIntExtra(
                MusicPlayerService.EXTRA_SAMPLE_RATE,
                44100
        );

        float bass = intent.getFloatExtra(
                MusicPlayerService.EXTRA_BASS,
                0f
        );

        float energy = intent.getFloatExtra(
                MusicPlayerService.EXTRA_ENERGY,
                0f
        );

        boolean beat = intent.getBooleanExtra(
                MusicPlayerService.EXTRA_BEAT,
                false
        );

        equalizerSeekBar.setFFTData(fft, sampleRate);
        equalizerSeekBar.setBassLevel(bass);
        equalizerSeekBar.setAudioLevel(energy);
        equalizerSeekBar.setBeatDetected(beat);
    }
}
