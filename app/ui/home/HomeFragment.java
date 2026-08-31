package com.urfavxbf.kanade.ui.home;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.urfavxbf.kanade.AlbumColorManager;
import com.urfavxbf.kanade.AudioFile;
import com.urfavxbf.kanade.AudioListAdapter;
import com.urfavxbf.kanade.FavoriteManager;
import com.urfavxbf.kanade.MusicPlayerController;
import com.urfavxbf.kanade.MusicPlayerService;
import com.urfavxbf.kanade.MusicRepository;
import com.urfavxbf.kanade.R;
import com.urfavxbf.kanade.databinding.FragmentHomeBinding;

import java.io.InputStream;
import java.util.ArrayList;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private MusicRepository musicRepository;
    private AudioListAdapter audioListAdapter;
    private FavoriteManager favoriteManager;

    private AlbumColorManager albumColorManager;

    private final ArrayList<AudioFile> songs =
            new ArrayList<>();

    private MusicPlayerController playerController;

    private String currentMiniPlayerUri;

    /*
     * Current dynamic colors.
     */
    private int currentAccentColor =
            Color.rgb(
                    201,
                    196,
                    255
            );

    private int currentBackgroundColor =
            Color.rgb(
                    16,
                    17,
                    26
            );

    private ValueAnimator colorAnimator;

    /*
     * Receives player state updates.
     */
    private final BroadcastReceiver playerReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(
                        Context context,
                        Intent intent) {

                    if (!MusicPlayerService.ACTION_STATE_CHANGED
                            .equals(intent.getAction())) {
                        return;
                    }

                    boolean isPlaying =
                            intent.getBooleanExtra(
                                    MusicPlayerService.EXTRA_IS_PLAYING,
                                    false
                            );

                    String uri =
                            intent.getStringExtra(
                                    MusicPlayerService.EXTRA_CURRENT_URI
                            );

                    updateMiniPlayer(
                            uri,
                            isPlaying
                    );
                }
            };

    /*
     * Receives centralized album color changes.
     */
    private final BroadcastReceiver colorReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(
                        Context context,
                        Intent intent) {

                    if (!AlbumColorManager.ACTION_COLORS_CHANGED
                            .equals(intent.getAction())) {
                        return;
                    }

                    int accentColor =
                            intent.getIntExtra(
                                    AlbumColorManager.EXTRA_ACCENT_COLOR,
                                    currentAccentColor
                            );

                    int backgroundColor =
                            intent.getIntExtra(
                                    AlbumColorManager.EXTRA_BACKGROUND_COLOR,
                                    currentBackgroundColor
                            );

                    animateThemeColors(
                            accentColor,
                            backgroundColor
                    );
                }
            };

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(
                inflater,
                container,
                false
        );

        View root = binding.getRoot();

        playerController =
                new MusicPlayerController(
                        requireContext()
                );

        musicRepository =
                new MusicRepository(
                        requireContext()
                );

        favoriteManager =
                new FavoriteManager(
                        requireContext()
                );

        /*
         * Initialize centralized color manager.
         */
        albumColorManager =
                AlbumColorManager.getInstance(
                        requireContext()
                                .getApplicationContext()
                );

        /*
         * Load cached colors immediately.
         */
        currentAccentColor =
                albumColorManager
                        .getCurrentAccentColor();

        currentBackgroundColor =
                albumColorManager
                        .getCurrentBackgroundColor();

        applyThemeColors(
                currentAccentColor,
                currentBackgroundColor
        );

        audioListAdapter =
                new AudioListAdapter(
                        songs,
                        new AudioListAdapter.OnSongClickListener() {

                            @Override
                            public void onSongClick(
                                    AudioFile song) {

                                showMiniPlayer(
                                        song,
                                        true
                                );

                                playerController.play(
                                        song.getUri()
                                );
                            }

                            @Override
                            public void onFavoriteClick(
                                    AudioFile song) {

                                toggleFavorite(song);
                            }

                            @Override
                            public void onMoreClick(
                                    AudioFile song,
                                    View anchor) {

                                showSongMenu(
                                        song,
                                        anchor
                                );
                            }
                        },
                        favoriteManager
                );

        binding.rvHomeMusic.setLayoutManager(
                new LinearLayoutManager(
                        requireContext()
                )
        );

        binding.rvHomeMusic.setAdapter(
                audioListAdapter
        );

        binding.swipeRefresh.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {

                        loadMusic();
                    }
                }
        );

        binding.miniPlayer.setVisibility(
                View.GONE
        );

        binding.miniPlayer.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        NavController navController =
                                Navigation.findNavController(v);

                        navController.navigate(
                                R.id.action_navigation_home_to_playerFragment
                        );
                    }
                }
        );

        binding.miniPlayPause.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        playerController.play();
                    }
                }
        );

        loadMusic();

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        /*
         * Player state receiver.
         */
        IntentFilter playerFilter =
                new IntentFilter(
                        MusicPlayerService.ACTION_STATE_CHANGED
                );

        ContextCompat.registerReceiver(
                requireContext(),
                playerReceiver,
                playerFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );

        /*
         * Dynamic album color receiver.
         */
        IntentFilter colorFilter =
                new IntentFilter(
                        AlbumColorManager.ACTION_COLORS_CHANGED
                );

        ContextCompat.registerReceiver(
                requireContext(),
                colorReceiver,
                colorFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );

        /*
         * Re-apply cached colors whenever Home
         * becomes visible again.
         */
        if (albumColorManager != null) {

            currentAccentColor =
                    albumColorManager
                            .getCurrentAccentColor();

            currentBackgroundColor =
                    albumColorManager
                            .getCurrentBackgroundColor();

            applyThemeColors(
                    currentAccentColor,
                    currentBackgroundColor
            );
        }
    }

    @Override
    public void onStop() {

        try {

            requireContext().unregisterReceiver(
                    playerReceiver
            );

        } catch (Exception ignored) {
        }

        try {

            requireContext().unregisterReceiver(
                    colorReceiver
            );

        } catch (Exception ignored) {
        }

        if (colorAnimator != null) {

            colorAnimator.cancel();
            colorAnimator = null;
        }

        super.onStop();
    }

    private void toggleFavorite(
            AudioFile song) {

        if (song == null ||
                favoriteManager == null) {
            return;
        }

        String uri = song.getUri();

        if (uri == null ||
                uri.trim().isEmpty()) {
            return;
        }

        boolean isFavorite =
                favoriteManager.toggleFavorite(uri);

        int position =
                songs.indexOf(song);

        if (position >= 0) {

            audioListAdapter.notifyItemChanged(
                    position
            );
        }

        if (isFavorite) {

            Toast.makeText(
                    requireContext(),
                    "Added to Favorites",
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            Toast.makeText(
                    requireContext(),
                    "Removed from Favorites",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void showSongMenu(
            AudioFile song,
            View anchor) {

        if (song == null) {
            return;
        }

        PopupMenu popupMenu =
                new PopupMenu(
                        requireContext(),
                        anchor
                );

        popupMenu.getMenu().add(
                "Play"
        );

        popupMenu.getMenu().add(
                "Add to Playlist"
        );

        popupMenu.getMenu().add(
                "Song Info"
        );

        popupMenu.setOnMenuItemClickListener(
                item -> {

                    String title =
                            item.getTitle().toString();

                    if ("Play".equals(title)) {

                        showMiniPlayer(
                                song,
                                true
                        );

                        playerController.play(
                                song.getUri()
                        );

                    } else if ("Add to Playlist".equals(title)) {

                        Toast.makeText(
                                requireContext(),
                                "Playlist feature coming soon",
                                Toast.LENGTH_SHORT
                        ).show();

                    } else if ("Song Info".equals(title)) {

                        Toast.makeText(
                                requireContext(),
                                song.getTitle(),
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    return true;
                }
        );

        popupMenu.show();
    }

    private void loadMusic() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                if (musicRepository == null) {
                    stopRefreshing();
                    return;
                }

                final ArrayList<AudioFile> result;

                try {

                    result =
                            musicRepository.getAllSongs();

                } catch (Exception ignored) {

                    stopRefreshing();
                    return;
                }

                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {

                                if (binding == null) {
                                    return;
                                }

                                songs.clear();
                                songs.addAll(result);

                                audioListAdapter
                                        .notifyDataSetChanged();

                                binding.swipeRefresh
                                        .setRefreshing(false);
                            }
                        }
                );
            }
        }).start();
    }

    private void stopRefreshing() {

        if (!isAdded()) {
            return;
        }

        requireActivity().runOnUiThread(
                new Runnable() {
                    @Override
                    public void run() {

                        if (binding == null) {
                            return;
                        }

                        binding.swipeRefresh
                                .setRefreshing(false);
                    }
                }
        );
    }

    private void showMiniPlayer(
            AudioFile song,
            boolean isPlaying) {

        if (binding == null ||
                song == null) {
            return;
        }

        String songUri =
                song.getUri();

        if (songUri == null ||
                songUri.trim().isEmpty()) {
            return;
        }

        currentMiniPlayerUri =
                songUri;

        binding.miniPlayer.setVisibility(
                View.VISIBLE
        );

        String title =
                song.getTitle();

        if (title == null ||
                title.trim().isEmpty()) {

            title = "Unknown song";
        }

        binding.miniTitle.setText(
                title
        );

        String artist =
                song.getArtist();

        if (artist == null ||
                artist.trim().isEmpty()) {

            artist = "Unknown artist";
        }

        binding.miniArtist.setText(
                artist
        );

        binding.miniAlbumArt.setImageResource(
                android.R.drawable.ic_media_play
        );

        loadAlbumArt(
                song,
                songUri
        );

        binding.miniPlayPause.setImageResource(
                isPlaying
                        ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play
        );

        applyThemeColors(
                currentAccentColor,
                currentBackgroundColor
        );
    }

    private void loadAlbumArt(
            final AudioFile song,
            final String requestedUri) {

        if (binding == null ||
                song == null ||
                requestedUri == null) {
            return;
        }

        final String albumArtUri =
                song.getAlbumArtUri();

        if (albumArtUri == null ||
                albumArtUri.trim().isEmpty()) {

            loadAlbumArtThumbnail(
                    song,
                    requestedUri
            );

            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                Bitmap bitmap = null;

                try {

                    Uri uri =
                            Uri.parse(
                                    albumArtUri
                            );

                    InputStream inputStream =
                            requireContext()
                                    .getContentResolver()
                                    .openInputStream(uri);

                    if (inputStream != null) {

                        bitmap =
                                BitmapFactory.decodeStream(
                                        inputStream
                                );

                        inputStream.close();
                    }

                } catch (Exception ignored) {
                }

                final Bitmap result =
                        bitmap;

                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {

                                if (binding == null) {
                                    return;
                                }

                                if (!requestedUri.equals(
                                        currentMiniPlayerUri)) {
                                    return;
                                }

                                if (result != null) {

                                    binding.miniAlbumArt
                                            .setImageBitmap(
                                                    result
                                            );

                                } else {

                                    loadAlbumArtThumbnail(
                                            song,
                                            requestedUri
                                    );
                                }
                            }
                        }
                );
            }
        }).start();
    }

    private void loadAlbumArtThumbnail(
            final AudioFile song,
            final String requestedUri) {

        if (binding == null ||
                song == null ||
                requestedUri == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {

                Bitmap bitmap = null;

                try {

                    Uri songUri =
                            Uri.parse(
                                    song.getUri()
                            );

                    bitmap =
                            requireContext()
                                    .getContentResolver()
                                    .loadThumbnail(
                                            songUri,
                                            new android.util.Size(
                                                    256,
                                                    256
                                            ),
                                            null
                                    );

                } catch (Exception ignored) {
                }

                final Bitmap result =
                        bitmap;

                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {

                                if (binding == null) {
                                    return;
                                }

                                if (!requestedUri.equals(
                                        currentMiniPlayerUri)) {
                                    return;
                                }

                                if (result != null) {

                                    binding.miniAlbumArt
                                            .setImageBitmap(
                                                    result
                                            );

                                } else {

                                    binding.miniAlbumArt
                                            .setImageResource(
                                                    android.R.drawable
                                                            .ic_media_play
                                            );
                                }
                            }
                        }
                );
            }
        }).start();
    }

    private void updateMiniPlayer(
            String uri,
            boolean isPlaying) {

        if (binding == null) {
            return;
        }

        if (uri == null ||
                uri.trim().isEmpty()) {

            currentMiniPlayerUri = null;

            binding.miniPlayer.setVisibility(
                    View.GONE
            );

            return;
        }

        AudioFile currentSong =
                null;

        for (AudioFile song : songs) {

            if (uri.equals(
                    song.getUri())) {

                currentSong =
                        song;

                break;
            }
        }

        if (currentSong == null) {
            return;
        }

        /*
         * Only reload the Mini Player
         * when the song itself changes.
         */
        boolean songChanged =
                !uri.equals(
                        currentMiniPlayerUri
                );

        if (songChanged) {

            showMiniPlayer(
                    currentSong,
                    isPlaying
            );

        } else {

            binding.miniPlayPause
                    .setImageResource(
                            isPlaying
                                    ? android.R.drawable.ic_media_pause
                                    : android.R.drawable.ic_media_play
                    );
        }
    }

    /*
     * Smoothly animate the entire Home screen
     * from the previous album colors to the new
     * album colors.
     */
    private void animateThemeColors(
            final int newAccentColor,
            final int newBackgroundColor) {

        if (binding == null) {
            return;
        }

        if (colorAnimator != null) {
            colorAnimator.cancel();
        }

        final int oldAccentColor =
                currentAccentColor;

        final int oldBackgroundColor =
                currentBackgroundColor;

        colorAnimator =
                ValueAnimator.ofFloat(
                        0.0f,
                        1.0f
                );

        colorAnimator.setDuration(
                450
        );

        colorAnimator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(
                            ValueAnimator animation) {

                        float fraction =
                                (Float)
                                        animation
                                                .getAnimatedValue();

                        int accentColor =
                                (Integer)
                                        new ArgbEvaluator()
                                                .evaluate(
                                                        fraction,
                                                        oldAccentColor,
                                                        newAccentColor
                                                );

                        int backgroundColor =
                                (Integer)
                                        new ArgbEvaluator()
                                                .evaluate(
                                                        fraction,
                                                        oldBackgroundColor,
                                                        newBackgroundColor
                                                );

                        applyThemeColors(
                                accentColor,
                                backgroundColor
                        );
                    }
                }
        );

        colorAnimator.start();
    }

    /*
     * Apply the centralized album colors
     * to the Home screen.
     */
    private void applyThemeColors(
            int accentColor,
            int backgroundColor) {

        if (binding == null) {
            return;
        }

        currentAccentColor =
                accentColor;

        currentBackgroundColor =
                backgroundColor;

        /*
         * Entire Home background.
         */
        binding.getRoot()
                .setBackgroundColor(
                        backgroundColor
                );

        /*
         * RecyclerView background.
         *
         * Transparent so the dynamic Home background
         * remains visible behind the song list.
         */
        binding.rvHomeMusic
                .setBackgroundColor(
                        Color.TRANSPARENT
                );

        /*
         * SwipeRefresh background.
         */
        binding.swipeRefresh
                .setBackgroundColor(
                        Color.TRANSPARENT
                );

        /*
         * Pull-to-refresh indicator.
         */
        binding.swipeRefresh
                .setColorSchemeColors(
                        currentAccentColor
                );

        /*
         * Mini Player.
         *
         * Slightly darker than the main album
         * background so it remains visually separated.
         */
        int miniPlayerColor =
                darkenColor(
                        backgroundColor,
                        0.78f
                );

        binding.miniPlayer
                .setBackgroundColor(
                        miniPlayerColor
                );

        /*
         * Mini Player title.
         */
        binding.miniTitle
                .setTextColor(
                        Color.WHITE
                );

        /*
         * Mini Player artist.
         */
        binding.miniArtist
                .setTextColor(
                        blendColors(
                                currentAccentColor,
                                Color.WHITE,
                                0.35f
                        )
                );

        /*
         * Mini Player play/pause icon.
         */
        binding.miniPlayPause
                .setImageTintList(
                        ColorStateList.valueOf(
                                currentAccentColor
                        )
                );
    }

    /*
     * Darken a color while preserving its hue.
     */
    private int darkenColor(
            int color,
            float factor) {

        factor =
                Math.max(
                        0.0f,
                        Math.min(
                                1.0f,
                                factor
                        )
                );

        int red =
                Math.max(
                        0,
                        Math.min(
                                255,
                                Math.round(
                                        Color.red(color)
                                                * factor
                                )
                        )
                );

        int green =
                Math.max(
                        0,
                        Math.min(
                                255,
                                Math.round(
                                        Color.green(color)
                                                * factor
                                )
                        )
                );

        int blue =
                Math.max(
                        0,
                        Math.min(
                                255,
                                Math.round(
                                        Color.blue(color)
                                                * factor
                                )
                        )
                );

        return Color.rgb(
                red,
                green,
                blue
        );
    }

    /*
     * Blend two colors.
     */
    private int blendColors(
            int color1,
            int color2,
            float ratio) {

        ratio =
                Math.max(
                        0.0f,
                        Math.min(
                                1.0f,
                                ratio
                        )
                );

        int r =
                Math.round(
                        Color.red(color1)
                                * (1.0f - ratio)
                                +
                        Color.red(color2)
                                * ratio
                );

        int g =
                Math.round(
                        Color.green(color1)
                                * (1.0f - ratio)
                                +
                        Color.green(color2)
                                * ratio
                );

        int b =
                Math.round(
                        Color.blue(color1)
                                * (1.0f - ratio)
                                +
                        Color.blue(color2)
                                * ratio
                );

        return Color.rgb(
                r,
                g,
                b
        );
    }

    @Override
    public void onDestroyView() {

        if (colorAnimator != null) {

            colorAnimator.cancel();
            colorAnimator = null;
        }

        currentMiniPlayerUri = null;

        binding = null;

        super.onDestroyView();
    }
}
