package com.urfavxbf.kanade.ui.dashboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.core.graphics.ColorUtils;

import com.urfavxbf.kanade.AlbumColorManager;
import com.urfavxbf.kanade.AudioFile;
import com.urfavxbf.kanade.MusicRepository;
import com.urfavxbf.kanade.PlaylistManager;
import com.urfavxbf.kanade.databinding.FragmentDashboardBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private ExecutorService statsExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private final BroadcastReceiver colorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null ||
                    !AlbumColorManager.ACTION_COLORS_CHANGED.equals(intent.getAction())) {
                return;
            }

            int accent = intent.getIntExtra(
                    AlbumColorManager.EXTRA_ACCENT_COLOR,
                    Color.rgb(201, 196, 255)
            );
            int background = intent.getIntExtra(
                    AlbumColorManager.EXTRA_BACKGROUND_COLOR,
                    Color.rgb(16, 17, 26)
            );

            applyColors(accent, background);
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);

        AlbumColorManager colorManager =
                AlbumColorManager.getInstance(requireContext().getApplicationContext());
        applyColors(
                colorManager.getCurrentAccentColor(),
                colorManager.getCurrentBackgroundColor()
        );

        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(
                AlbumColorManager.ACTION_COLORS_CHANGED
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(
                    colorReceiver,
                    filter,
                    Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            requireContext().registerReceiver(
                    colorReceiver,
                    filter
            );
        }

        loadStats();
    }

    @Override
    public void onStop() {
        try {
            requireContext().unregisterReceiver(colorReceiver);
        } catch (IllegalArgumentException ignored) {
        }

        super.onStop();
    }

    private void loadStats() {
        if (statsExecutor != null) {
            statsExecutor.shutdownNow();
        }

        statsExecutor = Executors.newSingleThreadExecutor();

        statsExecutor.execute(() -> {
            ArrayList<AudioFile> songs;
            int favorites;

            try {
                Context context = requireContext().getApplicationContext();
                MusicRepository repository = new MusicRepository(context);
                songs = repository.getAllSongs();
                favorites = new PlaylistManager(context)
                        .getPlaylistSongCount(PlaylistManager.FAVORITES_PLAYLIST);
            } catch (Exception ignored) {
                songs = new ArrayList<>();
                favorites = 0;
            }

            DashboardStats stats = buildStats(songs, favorites);

            mainHandler.post(() -> {
                if (!isAdded() || binding == null) {
                    return;
                }

                binding.librarySongCount.setText(
                        stats.songCount + " " + (stats.songCount == 1 ? "song" : "songs")
                );
                binding.libraryDuration.setText(
                        formatDuration(stats.totalDurationMs)
                );
                binding.songsCount.setText(String.valueOf(stats.songCount));
                binding.artistsCount.setText(String.valueOf(stats.artistCount));
                binding.albumsCount.setText(String.valueOf(stats.albumCount));
                binding.favoritesCount.setText(String.valueOf(stats.favoriteCount));
            });
        });
    }

    private DashboardStats buildStats(
            ArrayList<AudioFile> songs,
            int favorites) {
        Set<String> artists = new HashSet<>();
        Set<String> albums = new HashSet<>();
        long totalDuration = 0L;
        int songCount = 0;

        if (songs != null) {
            for (AudioFile song : songs) {
                if (song == null) {
                    continue;
                }

                songCount++;

                String artist = normalizeKey(song.getArtist());
                if (!artist.isEmpty()) {
                    artists.add(artist);
                }

                String album = normalizeKey(song.getAlbum());
                if (!album.isEmpty()) {
                    albums.add(album);
                }

                long duration = song.getDuration();
                if (duration > 0L) {
                    totalDuration += duration;
                }
            }
        }

        return new DashboardStats(
                songCount,
                artists.size(),
                albums.size(),
                Math.max(0, favorites),
                totalDuration
        );
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }

        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String formatDuration(long durationMs) {
        long totalMinutes = durationMs / 60000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;

        if (hours > 0L) {
            return "About " + hours + "h " + minutes + "m of music";
        }

        return "About " + minutes + " min of music";
    }

    private void applyColors(int accentColor, int backgroundColor) {
        if (binding == null) {
            return;
        }

        int cardColor = ColorUtils.blendARGB(
                backgroundColor,
                accentColor,
                0.16f
        );
        int subtleCardColor = ColorUtils.blendARGB(
                backgroundColor,
                accentColor,
                0.08f
        );

        binding.getRoot().setBackgroundColor(backgroundColor);
        binding.libraryHeroCard.setCardBackgroundColor(cardColor);
        binding.songsCard.setCardBackgroundColor(subtleCardColor);
        binding.artistsCard.setCardBackgroundColor(subtleCardColor);
        binding.albumsCard.setCardBackgroundColor(subtleCardColor);
        binding.favoritesCard.setCardBackgroundColor(subtleCardColor);

        binding.librarySongCount.setTextColor(Color.WHITE);
        binding.songsCount.setTextColor(accentColor);
        binding.artistsCount.setTextColor(accentColor);
        binding.albumsCount.setTextColor(accentColor);
        binding.favoritesCount.setTextColor(accentColor);
    }

    @Override
    public void onDestroyView() {
        if (statsExecutor != null) {
            statsExecutor.shutdownNow();
            statsExecutor = null;
        }

        mainHandler.removeCallbacksAndMessages(null);
        binding = null;
        super.onDestroyView();
    }

    private static final class DashboardStats {
        final int songCount;
        final int artistCount;
        final int albumCount;
        final int favoriteCount;
        final long totalDurationMs;

        DashboardStats(
                int songCount,
                int artistCount,
                int albumCount,
                int favoriteCount,
                long totalDurationMs) {
            this.songCount = songCount;
            this.artistCount = artistCount;
            this.albumCount = albumCount;
            this.favoriteCount = favoriteCount;
            this.totalDurationMs = totalDurationMs;
        }
    }
}