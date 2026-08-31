package com.urfavxbf.kanade;

import android.content.Context;
import android.content.Intent;

public class MusicPlayerController {

    private final Context context;

    public MusicPlayerController(Context context) {
        this.context = context.getApplicationContext();
    }

    public void play() {
        sendAction(MusicPlayerService.ACTION_PLAY);
    }

    public void play(String songUri) {

        if (songUri == null || songUri.trim().isEmpty()) {
            return;
        }

        Intent intent = new Intent(
                context,
                MusicPlayerService.class
        );

        intent.setAction(
                MusicPlayerService.ACTION_PLAY
        );

        intent.putExtra(
                MusicPlayerService.EXTRA_SONG_URI,
                songUri
        );

        context.startService(intent);
    }

    public void pause() {
        sendAction(MusicPlayerService.ACTION_PAUSE);
    }

    public void next() {
        sendAction(MusicPlayerService.ACTION_NEXT);
    }

    public void previous() {
        sendAction(MusicPlayerService.ACTION_PREVIOUS);
    }

    public void seekTo(int position) {

        if (position < 0) {
            position = 0;
        }

        Intent intent = new Intent(
                context,
                MusicPlayerService.class
        );

        intent.setAction(
                MusicPlayerService.ACTION_SEEK
        );

        intent.putExtra(
                MusicPlayerService.EXTRA_SEEK_POSITION,
                position
        );

        context.startService(intent);
    }

    private void sendAction(String action) {

        Intent intent = new Intent(
                context,
                MusicPlayerService.class
        );

        intent.setAction(action);

        context.startService(intent);
    }
}