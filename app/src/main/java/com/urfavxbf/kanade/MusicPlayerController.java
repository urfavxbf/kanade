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

    /*
     * SHUFFLE
     */

    public void setShuffle(boolean enabled) {

        Intent intent = new Intent(
                context,
                MusicPlayerService.class
        );

        intent.setAction(
                MusicPlayerService.ACTION_SET_SHUFFLE
        );

        intent.putExtra(
                MusicPlayerService.EXTRA_SHUFFLE_ENABLED,
                enabled
        );

        context.startService(intent);
    }

    public void toggleShuffle() {
        sendAction(
                MusicPlayerService.ACTION_TOGGLE_SHUFFLE
        );
    }

    /*
     * REPEAT
     *
     * 0 = OFF
     * 1 = ALL
     * 2 = ONE
     */

    public void setRepeatMode(int mode) {

        if (mode < MusicPlayerService.REPEAT_OFF ||
                mode > MusicPlayerService.REPEAT_ONE) {

            mode = MusicPlayerService.REPEAT_OFF;
        }

        Intent intent = new Intent(
                context,
                MusicPlayerService.class
        );

        intent.setAction(
                MusicPlayerService.ACTION_SET_REPEAT
        );

        intent.putExtra(
                MusicPlayerService.EXTRA_REPEAT_MODE,
                mode
        );

        context.startService(intent);
    }

    public void toggleRepeat() {
        sendAction(
                MusicPlayerService.ACTION_TOGGLE_REPEAT
        );
    }

    /*
     * QUEUE
     */

    public void addToQueue(String songUri) {

        if (songUri == null ||
                songUri.trim().isEmpty()) {

            return;
        }

        Intent intent = new Intent(
                context,
                MusicPlayerService.class
        );

        intent.setAction(
                MusicPlayerService.ACTION_ADD_TO_QUEUE
        );

        intent.putExtra(
                MusicPlayerService.EXTRA_SONG_URI,
                songUri
        );

        context.startService(intent);
    }

    public void clearQueue() {

        sendAction(
                MusicPlayerService.ACTION_CLEAR_QUEUE
        );
    }

    public void playQueueItem(int index) {

        Intent intent = new Intent(
                context,
                MusicPlayerService.class
        );

        intent.setAction(
                MusicPlayerService.ACTION_PLAY_QUEUE_ITEM
        );

        intent.putExtra(
                MusicPlayerService.EXTRA_QUEUE_INDEX,
                index
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