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

    private AlbumColorManager albumColorManager;

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
     * ---------------------------------------------------------
     * CRASH DEBUGGER
     * ---------------------------------------------------------
     *
     * Keep the crash handler alive independently from the
     * current Activity instance.
     *
     * This is important because a crash can happen in
     * PlayerActivity, PlaylistActivity, Fragment, Service, etc.
     */

    private void installCrashHandler() {

        final Context applicationContext =
                getApplicationContext();

        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {

                    @Override
                    public void uncaughtException(
                            Thread thread,
                            Throwable throwable) {

                        StringBuilder error =
                                new StringBuilder();

                        error.append(
                                "========== KANADE CRASH ==========\n\n"
                        );

                        error.append(
                                "Thread:\n"
                        );

                        error.append(
                                thread != null
                                        ? thread.getName()
                                        : "Unknown"
                        );

                        error.append(
                                "\n\nException:\n"
                        );

                        error.append(
                                throwable != null
                                        ? throwable.toString()
                                        : "Unknown exception"
                        );

                        error.append(
                                "\n\nStack Trace:\n"
                        );

                        java.io.StringWriter sw =
                                new java.io.StringWriter();

                        java.io.PrintWriter pw =
                                new java.io.PrintWriter(
                                        sw
                                );

                        if (throwable != null) {

                            throwable.printStackTrace(
                                    pw
                            );

                        }

                        error.append(
                                sw.toString()
                        );

                        Throwable cause =
                                throwable != null
                                        ? throwable.getCause()
                                        : null;

                        while (cause != null) {

                            error.append(
                                    "\n\n========== CAUSED BY ==========\n"
                            );

                            error.append(
                                    cause.toString()
                            );

                            java.io.StringWriter causeWriter =
                                    new java.io.StringWriter();

                            java.io.PrintWriter causePrinter =
                                    new java.io.PrintWriter(
                                            causeWriter
                                    );

                            cause.printStackTrace(
                                    causePrinter
                            );

                            error.append(
                                    causeWriter.toString()
                            );

                            cause =
                                    cause.getCause();
                        }

                        try {

                            Intent debugIntent =
                                    new Intent(
                                            applicationContext,
                                            DebugActivity.class
                                    );

                            debugIntent.putExtra(
                                    DebugActivity.EXTRA_ERROR,
                                    error.toString()
                            );

                            debugIntent.addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK |
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP |
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                            );

                            applicationContext.startActivity(
                                    debugIntent
                            );

                        } catch (Exception debugException) {

                            /*
                             * Last-resort logging.
                             *
                             * Do not allow the debugger itself
                             * to cause another crash.
                             */

                            Log.e(
                                    "KANADE_CRASH",
                                    error.toString(),
                                    debugException
                            );
                        }

                        /*
                         * Give DebugActivity a short moment to
                         * launch before killing the crashed process.
                         */

                        try {

                            Thread.sleep(
                                    300
                            );

                        } catch (InterruptedException ignored) {
                        }

                        android.os.Process.killProcess(
                                android.os.Process.myPid()
                        );

                        System.exit(
                                10
                        );
                    }
                }
        );
    }

    /*
     * ---------------------------------------------------------
     * COLOR RECEIVER
     * ---------------------------------------------------------
     */

    private final BroadcastReceiver colorReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(
                        Context context,
                        Intent intent) {

                    if (intent == null) {
                        return;
                    }

                    if (!AlbumColorManager.ACTION_COLORS_CHANGED
                            .equals(
                                    intent.getAction()
                            )) {

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
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(
                savedInstanceState
        );

        /*
         * Install debugger BEFORE initializing the rest
         * of the Activity so initialization crashes are
         * captured too.
         */

        installCrashHandler();

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
         * -----------------------------------------------------
         * ALBUM COLOR MANAGER
         * -----------------------------------------------------
         */

        albumColorManager =
                AlbumColorManager.getInstance(
                        getApplicationContext()
                );

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

        /*
         * -----------------------------------------------------
         * NAVIGATION
         * -----------------------------------------------------
         */

        final NavController navController =
                Navigation.findNavController(
                        this,
                        R.id.nav_host_fragment_activity_main
                );

        /*
         * -----------------------------------------------------
         * SONGS
         *
         * navigation_home
         * -> SongsFragment
         * -----------------------------------------------------
         */

        binding.kanadeNav.navSongs.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(
                            View v) {

                        navigateTo(
                                navController,
                                R.id.navigation_home
                        );
                    }
                }
        );

        /*
         * -----------------------------------------------------
         * ARTISTS
         *
         * navigation_artist
         * -> ArtistFragment
         * -----------------------------------------------------
         */

        binding.kanadeNav.navArtist.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(
                            View v) {

                        navigateTo(
                                navController,
                                R.id.navigation_artist
                        );
                    }
                }
        );

        /*
         * -----------------------------------------------------
         * DASHBOARD
         *
         * navigation_dashboard
         * -> HomeFragment
         * -----------------------------------------------------
         */

        binding.kanadeNav.navDashboard.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(
                            View v) {

                        navigateTo(
                                navController,
                                R.id.navigation_dashboard
                        );
                    }
                }
        );

        /*
         * -----------------------------------------------------
         * PLAYLIST
         *
         * navigation_playlist
         * -> PlaylistFragment
         * -----------------------------------------------------
         */

        binding.kanadeNav.navPlaylist.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(
                            View v) {

                        navigateTo(
                                navController,
                                R.id.navigation_playlist
                        );
                    }
                }
        );

        /*
         * -----------------------------------------------------
         * YOUTUBE
         *
         * navigation_youtube
         * -> YouTubeFragment
         * -----------------------------------------------------
         */

        binding.kanadeNav.navYouTube.setOnClickListener(
                new View.OnClickListener() {

                    @Override
                    public void onClick(
                            View v) {

                        navigateTo(
                                navController,
                                R.id.navigation_youtube
                        );
                    }
                }
        );

        /*
         * -----------------------------------------------------
         * DESTINATION LISTENER
         * -----------------------------------------------------
         */

        navController.addOnDestinationChangedListener(
                new NavController.OnDestinationChangedListener() {

                    @Override
                    public void onDestinationChanged(
                            NavController controller,
                            androidx.navigation.NavDestination destination,
                            Bundle arguments) {

                        if (destination == null) {
                            return;
                        }

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

        /*
         * -----------------------------------------------------
         * MUSIC REPOSITORY
         * -----------------------------------------------------
         */

        musicRepository =
                new MusicRepository(
                        this
                );

        requestMusicPermission();
    }

    /*
     * ---------------------------------------------------------
     * SAFE NAVIGATION
     * ---------------------------------------------------------
     */

    private void navigateTo(
            NavController navController,
            int destinationId) {

        if (navController == null) {
            return;
        }

        androidx.navigation.NavDestination destination =
                navController.getCurrentDestination();

        if (destination == null) {
            return;
        }

        if (destination.getId() ==
                destinationId) {

            return;
        }

        try {

            navController.navigate(
                    destinationId
            );

        } catch (Exception e) {

            /*
             * Navigation should never kill the app because
             * a destination is temporarily unavailable.
             *
             * The global crash handler will still catch
             * unexpected failures elsewhere.
             */

            Log.e(
                    "KANADE_NAV",
                    "Navigation failed: "
                            + destinationId,
                    e
            );
        }
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

    /*
     * ---------------------------------------------------------
     * BOTTOM NAVIGATION
     * ---------------------------------------------------------
     */

    private void updateBottomNavigation(
            int selectedId) {

        if (binding == null ||
                binding.kanadeNav == null) {

            return;
        }

        boolean songsSelected =
                selectedId == R.id.navigation_home;

        boolean artistSelected =
                selectedId == R.id.navigation_artist;

        boolean dashboardSelected =
                selectedId == R.id.navigation_dashboard;

        boolean playlistSelected =
                selectedId == R.id.navigation_playlist;

        boolean youtubeSelected =
                selectedId == R.id.navigation_youtube;

        /*
         * SONGS
         */

        binding.kanadeNav.navSongsIcon.setImageResource(
                songsSelected
                        ? R.drawable.ic_tracks_filled
                        : R.drawable.ic_tracks_blank
        );

        binding.kanadeNav.navSongsText.setVisibility(
                songsSelected
                        ? View.VISIBLE
                        : View.GONE
        );

        /*
         * ARTISTS
         */

        binding.kanadeNav.navArtistIcon.setImageResource(
                artistSelected
                        ? R.drawable.ic_artist_filled
                        : R.drawable.ic_artist_blank
        );

        binding.kanadeNav.navArtistText.setVisibility(
                artistSelected
                        ? View.VISIBLE
                        : View.GONE
        );

        /*
         * DASHBOARD
         */

        binding.kanadeNav.navDashboardIcon.setImageResource(
                dashboardSelected
                        ? R.drawable.house_filled
                        : R.drawable.house_blank
        );

        binding.kanadeNav.navDashboardText.setVisibility(
                dashboardSelected
                        ? View.VISIBLE
                        : View.GONE
        );

        /*
         * PLAYLIST
         */

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
         * YOUTUBE
         */

        binding.kanadeNav.navYouTubeIcon.setImageResource(
                R.drawable.ic_youtube
        );

        binding.kanadeNav.navYouTubeText.setVisibility(
                youtubeSelected
                        ? View.VISIBLE
                        : View.GONE
        );

        /*
         * COLORS
         */

        applyNavigationTint(
                songsSelected,
                artistSelected,
                dashboardSelected,
                playlistSelected,
                youtubeSelected
        );

        /*
         * ANIMATION
         */

        animateNavItem(
                binding.kanadeNav.navSongs,
                songsSelected
        );

        animateNavItem(
                binding.kanadeNav.navArtist,
                artistSelected
        );

        animateNavItem(
                binding.kanadeNav.navDashboard,
                dashboardSelected
        );

        animateNavItem(
                binding.kanadeNav.navPlaylist,
                playlistSelected
        );

        animateNavItem(
                binding.kanadeNav.navYouTube,
                youtubeSelected
        );
    }

    private void applyNavigationTint(
            boolean songsSelected,
            boolean artistSelected,
            boolean dashboardSelected,
            boolean playlistSelected,
            boolean youtubeSelected) {

        if (binding == null ||
                binding.kanadeNav == null) {

            return;
        }

        int inactiveColor =
                Color.rgb(
                        120,
                        120,
                        120
                );

        /*
         * SONGS
         */

        binding.kanadeNav.navSongsIcon
                .setImageTintList(
                        ColorStateList.valueOf(
                                songsSelected
                                        ? currentAccentColor
                                        : inactiveColor
                        )
                );

        binding.kanadeNav.navSongsText
                .setTextColor(
                        currentAccentColor
                );

        /*
         * ARTISTS
         */

        binding.kanadeNav.navArtistIcon
                .setImageTintList(
                        ColorStateList.valueOf(
                                artistSelected
                                        ? currentAccentColor
                                        : inactiveColor
                        )
                );

        binding.kanadeNav.navArtistText
                .setTextColor(
                        currentAccentColor
                );

        /*
         * DASHBOARD
         */

        binding.kanadeNav.navDashboardIcon
                .setImageTintList(
                        ColorStateList.valueOf(
                                dashboardSelected
                                        ? currentAccentColor
                                        : inactiveColor
                        )
                );

        binding.kanadeNav.navDashboardText
                .setTextColor(
                        currentAccentColor
                );

        /*
         * PLAYLIST
         */

        binding.kanadeNav.navPlaylistIcon
                .setImageTintList(
                        ColorStateList.valueOf(
                                playlistSelected
                                        ? currentAccentColor
                                        : inactiveColor
                        )
                );

        binding.kanadeNav.navPlaylistText
                .setTextColor(
                        currentAccentColor
                );

        /*
         * YOUTUBE
         */

        binding.kanadeNav.navYouTubeIcon
                .setImageTintList(
                        ColorStateList.valueOf(
                                youtubeSelected
                                        ? currentAccentColor
                                        : inactiveColor
                        )
                );

        binding.kanadeNav.navYouTubeText
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

            view.setScaleX(
                    0.92f
            );

            view.setScaleY(
                    0.92f
            );

            view.setAlpha(
                    0.75f
            );

            view.animate()
                    .scaleX(
                            1.0f
                    )
                    .scaleY(
                            1.0f
                    )
                    .alpha(
                            1.0f
                    )
                    .setDuration(
                            180
                    )
                    .start();

        } else {

            view.setScaleX(
                    1.0f
            );

            view.setScaleY(
                    1.0f
            );

            view.setAlpha(
                    1.0f
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * THEME COLORS
     * ---------------------------------------------------------
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

        binding.getRoot()
                .setBackgroundColor(
                        backgroundColor
                );

        NavController navController;

        try {

            navController =
                    Navigation.findNavController(
                            this,
                            R.id.nav_host_fragment_activity_main
                    );

        } catch (Exception e) {

            return;
        }

        if (navController.getCurrentDestination() != null) {

            updateBottomNavigation(
                    navController
                            .getCurrentDestination()
                            .getId()
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * MUSIC PERMISSION
     * ---------------------------------------------------------
     */

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

        if (musicRepository == null) {
            return;
        }

        new Thread(
                new Runnable() {

                    @Override
                    public void run() {

                        try {

                            final ArrayList<AudioFile> songs =
                                    musicRepository
                                            .getAllSongs();

                            Log.d(
                                    "KANade_MUSIC",
                                    "Music scan complete. Songs found: "
                                            + songs.size()
                            );

                        } catch (Exception e) {

                            Log.e(
                                    "KANade_MUSIC",
                                    "Music scan failed.",
                                    e
                            );
                        }
                    }
                }
        ).start();
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

    @Override
    protected void onDestroy() {

        if (colorAnimator != null) {

            colorAnimator.cancel();

            colorAnimator = null;
        }

        binding = null;

        super.onDestroy();
    }
}