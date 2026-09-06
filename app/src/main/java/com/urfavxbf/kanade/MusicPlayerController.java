package com.urfavxbf.kanade;

import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

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

        Intent intent = new Intent(context, MusicPlayerService.class);
        intent.setAction(MusicPlayerService.ACTION_PLAY);
        intent.putExtra(MusicPlayerService.EXTRA_SONG_URI, songUri);
        context.startService(intent);
    }

    public void playQueue(ArrayList<AudioFile> songs, int index) {

        if (songs == null || songs.isEmpty()) {
            return;
        }

        ArrayList<String> uris = new ArrayList<>();
        int requestedIndex = -1;

        for (int i = 0; i < songs.size(); i++) {
            AudioFile song = songs.get(i);
            if (song == null) continue;

            String uri = song.getUri();
            if (uri == null || uri.trim().isEmpty()) continue;

            if (i == index) {
                requestedIndex = uris.size();
            }
            uris.add(uri);
        }

        if (uris.isEmpty()) return;

        if (requestedIndex < 0 && index >= 0 && index < songs.size()) {
            AudioFile requestedSong = songs.get(index);
            if (requestedSong != null
                    && requestedSong.getUri() != null
                    && !requestedSong.getUri().trim().isEmpty()) {
                for (int i = 0; i < uris.size(); i++) {
                    if (requestedSong.getUri().equals(uris.get(i))) {
                        requestedIndex = i;
                        break;
                    }
                }
            }
        }

        if (requestedIndex < 0) requestedIndex = 0;
        setQueueAndPlay(uris, requestedIndex);
    }

    public void setQueueAndPlay(ArrayList<String> uris, int index) {

        if (uris == null || uris.isEmpty()) return;

        ArrayList<String> validUris = new ArrayList<>();
        for (String uri : uris) {
            if (uri != null && !uri.trim().isEmpty()) {
                validUris.add(uri);
            }
        }

        if (validUris.isEmpty()) return;

        int requestedIndex = Math.max(0, Math.min(index, validUris.size() - 1));

        Intent intent = new Intent(context, MusicPlayerService.class);
        intent.setAction(MusicPlayerService.ACTION_SET_QUEUE_AND_PLAY);
        intent.putStringArrayListExtra(MusicPlayerService.EXTRA_QUEUE_URIS, validUris);
        intent.putExtra(MusicPlayerService.EXTRA_QUEUE_INDEX, requestedIndex);
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
        if (position < 0) position = 0;

        Intent intent = new Intent(context, MusicPlayerService.class);
        intent.setAction(MusicPlayerService.ACTION_SEEK);
        intent.putExtra(MusicPlayerService.EXTRA_SEEK_POSITION, position);
        context.startService(intent);
    }

    public void setShuffle(boolean enabled) {
        Intent intent = new Intent(context, MusicPlayerService.class);
        intent.setAction(MusicPlayerService.ACTION_SET_SHUFFLE);
        intent.putExtra(MusicPlayerService.EXTRA_SHUFFLE_ENABLED, enabled);
        context.startService(intent);
    }

    public void toggleShuffle() {
        sendAction(MusicPlayerService.ACTION_TOGGLE_SHUFFLE);
    }

    public void setRepeatMode(int mode) {
        if (mode < MusicPlayerService.REPEAT_OFF || mode > MusicPlayerService.REPEAT_ONE) {
            mode = MusicPlayerService.REPEAT_OFF;
        }

        Intent intent = new Intent(context, MusicPlayerService.class);
        intent.setAction(MusicPlayerService.ACTION_SET_REPEAT);
        intent.putExtra(MusicPlayerService.EXTRA_REPEAT_MODE, mode);
        context.startService(intent);
    }

    public void toggleRepeat() {
        sendAction(MusicPlayerService.ACTION_TOGGLE_REPEAT);
    }

    public void addToQueue(String songUri) {
        if (songUri == null || songUri.trim().isEmpty()) return;

        Intent intent = new Intent(context, MusicPlayerService.class);
        intent.setAction(MusicPlayerService.ACTION_ADD_TO_QUEUE);
        intent.putExtra(MusicPlayerService.EXTRA_SONG_URI, songUri);
        context.startService(intent);
    }

    public void clearQueue() {
        sendAction(MusicPlayerService.ACTION_CLEAR_QUEUE);
    }

    public void playQueueItem(int index) {
        Intent intent = new Intent(context, MusicPlayerService.class);
        intent.setAction(MusicPlayerService.ACTION_PLAY_QUEUE_ITEM);
        intent.putExtra(MusicPlayerService.EXTRA_QUEUE_INDEX, index);
        context.startService(intent);
    }

    private void sendAction(String action) {
        Intent intent = new Intent(context, MusicPlayerService.class);
        intent.setAction(action);
        context.startService(intent);
    }
}
