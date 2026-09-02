package com.urfavxbf.kanade.ui.playlist;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.urfavxbf.kanade.PlaylistManager;

import java.util.ArrayList;

public class PlaylistViewModel extends ViewModel {

    private final MutableLiveData<ArrayList<String>> playlists =
            new MutableLiveData<>();

    private PlaylistManager playlistManager;

    public void initialize(PlaylistManager manager) {

        if (playlistManager != null) {
            return;
        }

        playlistManager = manager;

        loadPlaylists();
    }

    public LiveData<ArrayList<String>> getPlaylists() {
        return playlists;
    }

    public void loadPlaylists() {

        if (playlistManager == null) {
            return;
        }

        ArrayList<String> result =
                playlistManager.getPlaylists();

        if (result == null) {
            result = new ArrayList<>();
        }

        playlists.setValue(
                new ArrayList<>(result)
        );
    }

    public void createPlaylist(String name) {

        if (playlistManager == null ||
                name == null ||
                name.trim().isEmpty()) {
            return;
        }

        playlistManager.createPlaylist(
                name.trim()
        );

        loadPlaylists();
    }
}