package com.urfavxbf.kanade;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.urfavxbf.kanade.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private static final int READ_MEDIA_AUDIO_REQUEST = 100;

    private MusicRepository musicRepository;

    /*
     * Dynamic album colors.
     */
    private AlbumColorManager albumColorManager;

    private int currentAccentColor =
            Color.rgb(201, 196, 255);

    private int currentBackgroundColor =
            Color.rgb(16, 17, 26);

    private ValueAnimator colorAnimator;

    /*
     * Receives dynamic colors whenever the current
     * song changes.
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding =
                ActivityMainBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        /*
         * Initialize centralized color manager.
         */
        albumColorManager =
                AlbumColorManager.getInstance(
                        getApplicationContext()
                );

        /*
         * Use the currently cached colors immediately.
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

        final NavController navController =
                Navigation.findNavController(
                        this,
                        R.id.nav_host_fragment_activity_main
                );

        /*
         * HOME
         */
        binding.kanadeNav.navHome.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (navController.getCurrentDestination() != null &&
                                navController.getCurrentDestination().getId()
                                        != R.id.navigation_home) {

                            navController.navigate(
                                    R.id.navigation_home
                            );
                        }
                    }
                }
        );

        /*
         * PLAYER
         */
        binding.kanadeNav.navPlayer.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (navController.getCurrentDestination() != null &&
                                navController.getCurrentDestination().getId()
                                        != R.id.navigation_player) {

                            navController.navigate(
                                    R.id.navigation_player
                            );
                        }
                    }
                }
        );

        /*
         * PLAYLIST
         */
        binding.kanadeNav.navPlaylist.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (navController.getCurrentDestination() != null &&
                                navController.getCurrentDestination().getId()
                                        != R.id.navigation_playlist) {

                            navController.navigate(
                                    R.id.navigation_playlist
                            );
                        }
                    }
                }
        );

        /*
         * Update navbar whenever Navigation
         * changes destination.
         */
        navController.addOnDestinationChangedListener(
                new NavController.OnDestinationChangedListener() {
                    @Override
                    public void onDestinationChanged(
                            NavController controller,
                            androidx.navigation.NavDestination destination,
                            Bundle arguments) {

                        updateBottomNavigation(
                                destination.getId()
                        );
                    }
                }
        );

        /*
         * Initial navbar state.
         */
        if (navController.getCurrentDestination() != null) {

            updateBottomNavigation(
                    navController
                            .getCurrentDestination()
                            .getId()
            );
        }

        musicRepository =
                new MusicRepository(this);

        requestMusicPermission();
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter =
                new IntentFilter(
                        AlbumColorManager.ACTION_COLORS_CHANGED
                );

        ContextCompat.registerReceiver(
                this,
                colorReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );

        /*
         * Re-apply current cached colors when the
         * Activity becomes visible again.
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
    protected void onStop() {

        try {

            unregisterReceiver(
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

    private void updateBottomNavigation(
            int selectedId) {

        if (binding == null ||
                binding.kanadeNav == null) {
            return;
        }

        /*
         * HOME
         */
        boolean homeSelected =
                selectedId == R.id.navigation_home;

        binding.kanadeNav.navHomeIcon.setImageResource(
                homeSelected
                        ? R.drawable.house_filled
                        : R.drawable.house_blank
        );

        binding.kanadeNav.navHomeText.setVisibility(
                homeSelected
                        ? View.VISIBLE
                        : View.GONE
        );

        /*
         * PLAYER
         *
         * Currently only one player icon
         * exists, so it remains the same icon.
         */
        boolean playerSelected =
                selectedId == R.id.navigation_player;

        binding.kanadeNav.navPlayerIcon.setImageResource(
                R.drawable.ic_play
        );

        binding.kanadeNav.navPlayerText.setVisibility(
                playerSelected
                        ? View.VISIBLE
                        : View.GONE
        );

        /*
         * PLAYLIST
         */
        boolean playlistSelected =
                selectedId == R.id.navigation_playlist;

        binding.kanadeNav.navPlaylistIcon.setImageResource(
                playlistSelected
                        ? R.drawable.list_music_filled
                        : R.drawable.list_music_blank
        );

        binding.kanadeNav.navPlaylistText.setVisibility(
                playlistSelected
                        ? View.VISIBLE
                        : View.GONE
        );

        /*
         * Apply current dynamic accent color
         * to navigation icons and labels.
         */
        applyNavigationTint();

        /*
         * Animation
         */
        animateNavItem(
                binding.kanadeNav.navHome,
                homeSelected
        );

        animateNavItem(
                binding.kanadeNav.navPlayer,
                playerSelected
        );

        animateNavItem(
                binding.kanadeNav.navPlaylist,
                playlistSelected
        );
    }

    private void applyNavigationTint() {

        if (binding == null ||
                binding.kanadeNav == null) {
            return;
        }

        ColorStateList accentTint =
                ColorStateList.valueOf(
                        currentAccentColor
                );

        ColorStateList inactiveTint =
                ColorStateList.valueOf(
                        Color.rgb(
                                120,
                                120,
                                120
                        )
                );

        binding.kanadeNav.navHomeIcon
                .setImageTintList(
                        accentTint
                );

        binding.kanadeNav.navPlayerIcon
                .setImageTintList(
                        accentTint
                );

        binding.kanadeNav.navPlaylistIcon
                .setImageTintList(
                        accentTint
                );

        binding.kanadeNav.navHomeText
                .setTextColor(
                        currentAccentColor
                );

        binding.kanadeNav.navPlayerText
                .setTextColor(
                        currentAccentColor
                );

        binding.kanadeNav.navPlaylistText
                .setTextColor(
                        currentAccentColor
                );
    }

    private void animateNavItem(
            final View view,
            boolean selected) {

        if (view == null) {
            return;
        }

        view.animate().cancel();

        if (selected) {

            view.setScaleX(0.92f);
            view.setScaleY(0.92f);
            view.setAlpha(0.75f);

            view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .alpha(1.0f)
                    .setDuration(180)
                    .start();

        } else {

            view.setScaleX(1.0f);
            view.setScaleY(1.0f);
            view.setAlpha(1.0f);
        }
    }

    /*
     * Smoothly transition from the previous album
     * color to the new album color.
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

        colorAnimator.setDuration(450);

        colorAnimator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(
                            ValueAnimator animation) {

                        float fraction =
                                (Float) animation.getAnimatedValue();

                        int accent =
                                (Integer)
                                        new ArgbEvaluator()
                                                .evaluate(
                                                        fraction,
                                                        oldAccentColor,
                                                        newAccentColor
                                                );

                        int background =
                                (Integer)
                                        new ArgbEvaluator()
                                                .evaluate(
                                                        fraction,
                                                        oldBackgroundColor,
                                                        newBackgroundColor
                                                );

                        applyThemeColors(
                                accent,
                                background
                        );
                    }
                }
        );

        colorAnimator.addListener(
                new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(
                            android.animation.Animator animation) {

                        currentAccentColor =
                                newAccentColor;

                        currentBackgroundColor =
                                newBackgroundColor;

                        applyThemeColors(
                                currentAccentColor,
                                currentBackgroundColor
                        );
                    }
                }
        );

        colorAnimator.start();
    }

    /*
     * Applies the centralized colors to the
     * Activity-level UI.
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
         * Main Activity background.
         */
        binding.getRoot()
                .setBackgroundColor(
                        backgroundColor
                );

        /*
         * Navigation icons and labels.
         */
        applyNavigationTint();
    }

    private void requestMusicPermission() {

        if (Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU) {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_AUDIO
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.READ_MEDIA_AUDIO
                        },
                        READ_MEDIA_AUDIO_REQUEST
                );

            } else {

                scanMusic();
            }

        } else {

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        },
                        READ_MEDIA_AUDIO_REQUEST
                );

            } else {

                scanMusic();
            }
        }
    }

    private void scanMusic() {

        new Thread(new Runnable() {
            @Override
            public void run() {

                final ArrayList<AudioFile> songs =
                        musicRepository.getAllSongs();

                Log.d(
                        "KANade_MUSIC",
                        "Music scan complete. Songs found: "
                                + songs.size()
                );
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode ==
                READ_MEDIA_AUDIO_REQUEST) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                scanMusic();

            } else {

                Log.d(
                        "KANade_MUSIC",
                        "Music permission denied."
                );
            }
        }
    }
}