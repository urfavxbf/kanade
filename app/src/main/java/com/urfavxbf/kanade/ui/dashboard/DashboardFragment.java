package com.urfavxbf.kanade.ui.dashboard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.urfavxbf.kanade.AlbumColorManager;
import com.urfavxbf.kanade.AlbumArtManager;
import com.urfavxbf.kanade.AudioFile;
import com.urfavxbf.kanade.MusicBrainzArtistPhotoClient;
import com.urfavxbf.kanade.MusicRepository;
import com.urfavxbf.kanade.PlaybackStatsManager;
import com.urfavxbf.kanade.PlaylistManager;
import com.urfavxbf.kanade.databinding.FragmentDashboardBinding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;
    private ExecutorService statsExecutor;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private DashboardSongAdapter mostPlayedAdapter;
    private DashboardSongAdapter favoritesAdapter;
    private DashboardSongAdapter recentAdapter;
    private DashboardSongAdapter leastPlayedAdapter;
    private int accentColor;

    private final BroadcastReceiver colorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !AlbumColorManager.ACTION_COLORS_CHANGED.equals(intent.getAction())) return;
            int accent = intent.getIntExtra(AlbumColorManager.EXTRA_ACCENT_COLOR, Color.rgb(201, 196, 255));
            int background = intent.getIntExtra(AlbumColorManager.EXTRA_BACKGROUND_COLOR, Color.rgb(16, 17, 26));
            applyColors(accent, background);
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        AlbumColorManager manager = AlbumColorManager.getInstance(requireContext().getApplicationContext());
        applyColors(manager.getCurrentAccentColor(), manager.getCurrentBackgroundColor());
        setupSongRecyclerViews();
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(AlbumColorManager.ACTION_COLORS_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(colorReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(colorReceiver, filter);
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

    private void setupSongRecyclerViews() {
        Context context = requireContext().getApplicationContext();
        mostPlayedAdapter = new DashboardSongAdapter(context, accentColor);
        favoritesAdapter = new DashboardSongAdapter(context, accentColor);
        recentAdapter = new DashboardSongAdapter(context, accentColor);
        leastPlayedAdapter = new DashboardSongAdapter(context, accentColor);
        setupRecycler(binding.mostPlayedRecycler, mostPlayedAdapter);
        setupRecycler(binding.favoritesRecycler, favoritesAdapter);
        setupRecycler(binding.recentRecycler, recentAdapter);
        setupRecycler(binding.leastPlayedRecycler, leastPlayedAdapter);
    }

    private void setupRecycler(androidx.recyclerview.widget.RecyclerView recycler, DashboardSongAdapter adapter) {
        recycler.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        recycler.setAdapter(adapter);
        recycler.setNestedScrollingEnabled(false);
    }

    private void loadStats() {
        if (statsExecutor != null) statsExecutor.shutdownNow();
        statsExecutor = Executors.newSingleThreadExecutor();
        final Context appContext = requireContext().getApplicationContext();

        statsExecutor.execute(() -> {
            try {
                MusicRepository repository = new MusicRepository(appContext);
                ArrayList<AudioFile> songs = repository.getAllSongs();
                PlaybackStatsManager playbackStats = new PlaybackStatsManager(appContext);
                PlaylistManager playlists = new PlaylistManager(appContext);
                int favorites = playlists.getPlaylistSongCount(PlaylistManager.FAVORITES_PLAYLIST);
                DashboardData data = buildData(songs, favorites, playbackStats, playlists);

                mainHandler.post(() -> {
                    if (!isAdded() || binding == null) return;
                    renderData(data, appContext);
                });
            } catch (Exception ignored) {
                mainHandler.post(() -> {
                    if (!isAdded() || binding == null) return;
                    renderEmptyState();
                });
            }
        });
    }

    private DashboardData buildData(ArrayList<AudioFile> songs, int favorites, PlaybackStatsManager stats, PlaylistManager playlists) {
        Set<String> artists = new HashSet<>();
        Set<String> albums = new HashSet<>();
        Map<String, AudioFile> byUri = new HashMap<>();
        long totalDuration = 0L;

        if (songs != null) {
            for (AudioFile song : songs) {
                if (song == null) continue;
                if (song.getUri() != null) byUri.put(song.getUri(), song);
                String artist = normalizeKey(song.getArtist());
                String album = normalizeKey(song.getAlbum());
                if (!artist.isEmpty()) artists.add(artist);
                if (!album.isEmpty()) albums.add(album);
                if (song.getDuration() > 0L) totalDuration += song.getDuration();
            }
        }

        ArrayList<AudioFile> most = resolve(stats.getMostPlayed(8), byUri);
        ArrayList<AudioFile> least = resolve(stats.getLeastPlayed(8), byUri);
        ArrayList<AudioFile> recent = resolve(stats.getRecentlyPlayed(8), byUri);
        ArrayList<AudioFile> favoriteSongs = new ArrayList<>();
        for (String uri : playlists.getPlaylistSongs(PlaylistManager.FAVORITES_PLAYLIST)) {
            AudioFile song = byUri.get(uri);
            if (song != null) favoriteSongs.add(song);
        }
        if (favoriteSongs.size() > 8) favoriteSongs = new ArrayList<>(favoriteSongs.subList(0, 8));

        return new DashboardData(
                songs == null ? 0 : songs.size(),
                artists.size(),
                albums.size(),
                Math.max(0, favorites),
                totalDuration,
                most,
                favoriteSongs,
                recent,
                least,
                stats.getUsageLast7Days(),
                stats.getTopArtists(1)
        );
    }

    private ArrayList<AudioFile> resolve(ArrayList<PlaybackStatsManager.SongStat> stats, Map<String, AudioFile> byUri) {
        ArrayList<AudioFile> result = new ArrayList<>();
        if (stats == null) return result;
        for (PlaybackStatsManager.SongStat stat : stats) {
            AudioFile song = byUri.get(stat.uri);
            if (song != null) result.add(song);
        }
        return result;
    }

    private void renderData(DashboardData data, Context context) {
        binding.librarySongCount.setText(data.songCount + " " + (data.songCount == 1 ? "song" : "songs"));
        binding.libraryDuration.setText(formatDuration(data.totalDurationMs));
        setStatCard(binding.songsCard, data.songCount, "Songs");
        setStatCard(binding.artistsCard, data.artistCount, "Artists");
        setStatCard(binding.albumsCard, data.albumCount, "Albums");
        setStatCard(binding.favoritesCard, data.favoriteCount, "Favorites");

        mostPlayedAdapter.submitSongs(data.mostPlayed, accentColor);
        favoritesAdapter.submitSongs(data.favorites, accentColor);
        recentAdapter.submitSongs(data.recent, accentColor);
        leastPlayedAdapter.submitSongs(data.leastPlayed, accentColor);
        binding.usageChart.setData(data.usage, accentColor);

        long totalMinutes = 0L;
        for (PlaybackStatsManager.UsagePoint point : data.usage) totalMinutes += point.minutes;
        binding.usageSummary.setText(totalMinutes + " min");

        if (!data.topArtists.isEmpty()) {
            PlaybackStatsManager.ArtistStat top = data.topArtists.get(0);
            binding.topArtistName.setText(top.artist);
            binding.topArtistPlays.setText(top.playCount + (top.playCount == 1 ? " play" : " plays"));
            loadArtistPhoto(top.artist, context);
        } else {
            binding.topArtistName.setText("No listening data yet");
            binding.topArtistPlays.setText("Play something to build your stats");
            binding.topArtistPhoto.setImageResource(com.urfavxbf.kanade.R.drawable.ic_music_note);
        }
    }

    private void loadArtistPhoto(String artist, Context context) {
        binding.topArtistPhoto.setTag(artist);
        Executors.newSingleThreadExecutor().execute(() -> {
            MusicBrainzArtistPhotoClient.ArtistPhoto photo = new MusicBrainzArtistPhotoClient().resolve(artist);
            mainHandler.post(() -> {
                if (!isAdded() || binding == null || !artist.equals(binding.topArtistPhoto.getTag())) return;
                if (photo != null && photo.bitmap != null) binding.topArtistPhoto.setImageBitmap(photo.bitmap);
            });
        });
    }

    private void renderEmptyState() {
        binding.librarySongCount.setText("0 songs");
        binding.libraryDuration.setText("No listening data yet");
        setStatCard(binding.songsCard, 0, "Songs");
        setStatCard(binding.artistsCard, 0, "Artists");
        setStatCard(binding.albumsCard, 0, "Albums");
        setStatCard(binding.favoritesCard, 0, "Favorites");
        binding.usageSummary.setText("0 min");
        binding.usageChart.setData(new ArrayList<>(), accentColor);
        binding.topArtistName.setText("No listening data yet");
        binding.topArtistPlays.setText("Play something to build your stats");
    }

    private void setStatCard(androidx.cardview.widget.CardView card, int value, String label) {
        card.removeAllViews();
        LinearLayout layout = new LinearLayout(card.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(16), dp(14), dp(16), dp(12));
        TextView count = new TextView(card.getContext());
        count.setText(String.valueOf(value));
        count.setTextColor(accentColor);
        count.setTextSize(26);
        count.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView name = new TextView(card.getContext());
        name.setText(label);
        name.setTextColor(0xFFA9AAB5);
        name.setTextSize(13);
        name.setPadding(0, dp(2), 0, 0);
        layout.addView(count, new LinearLayout.LayoutParams(-1, -2));
        layout.addView(name, new LinearLayout.LayoutParams(-1, -2));
        card.addView(layout);
    }

    private void applyColors(int accent, int background) {
        accentColor = accent;
        if (binding == null) return;
        int cardColor = ColorUtils.blendARGB(background, accent, 0.16f);
        int subtle = ColorUtils.blendARGB(background, accent, 0.08f);
        binding.getRoot().setBackgroundColor(background);
        binding.topArtistCard.setCardBackgroundColor(Color.BLACK);
        binding.usageCard.setCardBackgroundColor(subtle);
        binding.songsCard.setCardBackgroundColor(subtle);
        binding.artistsCard.setCardBackgroundColor(subtle);
        binding.albumsCard.setCardBackgroundColor(subtle);
        binding.favoritesCard.setCardBackgroundColor(subtle);
        binding.librarySongCount.setTextColor(Color.WHITE);
        binding.libraryDuration.setTextColor(ColorUtils.blendARGB(Color.WHITE, accent, 0.2f));
        binding.topArtistName.setTextColor(Color.WHITE);
        binding.topArtistPlays.setTextColor(0xFFD0D0D8);
        binding.usageSummary.setTextColor(accent);
        binding.libraryHeroCard.setCardBackgroundColor(cardColor);
        if (mostPlayedAdapter != null) {
            mostPlayedAdapter.submitSongs(new ArrayList<>(), accent);
            favoritesAdapter.submitSongs(new ArrayList<>(), accent);
            recentAdapter.submitSongs(new ArrayList<>(), accent);
            leastPlayedAdapter.submitSongs(new ArrayList<>(), accent);
        }
    }

    private String normalizeKey(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String formatDuration(long durationMs) {
        long totalMinutes = durationMs / 60000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        return hours > 0L ? "About " + hours + "h " + minutes + "m of music" : "About " + minutes + " min of music";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        if (statsExecutor != null) {
            statsExecutor.shutdownNow();
            statsExecutor = null;
        }
        if (mostPlayedAdapter != null) mostPlayedAdapter.shutdown();
        if (favoritesAdapter != null) favoritesAdapter.shutdown();
        if (recentAdapter != null) recentAdapter.shutdown();
        if (leastPlayedAdapter != null) leastPlayedAdapter.shutdown();
        mostPlayedAdapter = null;
        favoritesAdapter = null;
        recentAdapter = null;
        leastPlayedAdapter = null;
        mainHandler.removeCallbacksAndMessages(null);
        binding = null;
        super.onDestroyView();
    }

    private static final class DashboardData {
        final int songCount;
        final int artistCount;
        final int albumCount;
        final int favoriteCount;
        final long totalDurationMs;
        final ArrayList<AudioFile> mostPlayed;
        final ArrayList<AudioFile> favorites;
        final ArrayList<AudioFile> recent;
        final ArrayList<AudioFile> leastPlayed;
        final ArrayList<PlaybackStatsManager.UsagePoint> usage;
        final ArrayList<PlaybackStatsManager.ArtistStat> topArtists;

        DashboardData(int songCount, int artistCount, int albumCount, int favoriteCount, long totalDurationMs,
                      ArrayList<AudioFile> mostPlayed, ArrayList<AudioFile> favorites, ArrayList<AudioFile> recent,
                      ArrayList<AudioFile> leastPlayed, ArrayList<PlaybackStatsManager.UsagePoint> usage,
                      ArrayList<PlaybackStatsManager.ArtistStat> topArtists) {
            this.songCount = songCount;
            this.artistCount = artistCount;
            this.albumCount = albumCount;
            this.favoriteCount = favoriteCount;
            this.totalDurationMs = totalDurationMs;
            this.mostPlayed = mostPlayed;
            this.favorites = favorites;
            this.recent = recent;
            this.leastPlayed = leastPlayed;
            this.usage = usage;
            this.topArtists = topArtists;
        }
    }
}
