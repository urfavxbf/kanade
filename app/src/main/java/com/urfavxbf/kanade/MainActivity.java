package com.urfavxbf.kanade;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.urfavxbf.kanade.databinding.ActivityMainBinding;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private static final int READ_MEDIA_AUDIO_REQUEST = 100;

    private MusicRepository musicRepository;

    private AlbumColorManager albumColorManager;

    private int currentAccentColor =
            Color.rgb(201, 196, 255);

    private int currentBackgroundColor =
            Color.rgb(16, 17, 26);

    private ValueAnimator colorAnimator;

    /*
     * ---------------------------------------------------------
     * MINI PLAYER
     * ---------------------------------------------------------
     */

    private BroadcastReceiver playerReceiver;

    private boolean isMusicPlaying = false;

    private String currentPlayingUri = null;

    private String lastMiniPlayerUri = null;

    private float miniPlayerTouchDownY = 0f;

    private boolean miniPlayerSwipeTriggered = false;

    /*
     * ---------------------------------------------------------
     * PLAYER TRANSITION
     * ---------------------------------------------------------
     */

    private boolean isOpeningFullPlayer = false;

    private static final long PLAYER_TRANSITION_DURATION = 350L;

    /*
     * ---------------------------------------------------------
     * NAV CONTROLLER
     * ---------------------------------------------------------
     */

    private NavController navController;

    /*
     * ---------------------------------------------------------
     * CRASH HANDLER
     * ---------------------------------------------------------
     */

    private void installCrashHandler() {

        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {

                    @Override
                    public void uncaughtException(
                            Thread thread,
                            Throwable throwable) {

                        try {

                            String error =
                                    Log.getStackTraceString(
                                            throwable
                                    );

                            Intent intent =
                                    new Intent(
                                            MainActivity.this,
                                            DebugActivity.class
                                    );

                            intent.putExtra(
                                    DebugActivity.EXTRA_ERROR,
                                    error
                            );

                            intent.addFlags(
                                    Intent.FLAG_ACTIVITY_NEW_TASK
                                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            );

                            startActivity(intent);

                        } catch (Exception ignored) {
                        }

                        android.os.Process.killProcess(
                                android.os.Process.myPid()
                        );

                        System.exit(10);
                    }
                }
        );
    }

    /*
     * ---------------------------------------------------------
     * ALBUM COLOR RECEIVER
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

                    if (AlbumColorManager.ACTION_COLORS_CHANGED
                            .equals(intent.getAction())) {

                        int newAccent =
                                intent.getIntExtra(
                                        AlbumColorManager
                                                .EXTRA_ACCENT_COLOR,
                                        currentAccentColor
                                );

                        int newBackground =
                                intent.getIntExtra(
                                        AlbumColorManager
                                                .EXTRA_BACKGROUND_COLOR,
                                        currentBackgroundColor
                                );

                        animateThemeColors(
                                newAccent,
                                newBackground
                        );
                    }
                }
            };

    /*
     * ---------------------------------------------------------
     * ON CREATE
     * ---------------------------------------------------------
     */

    @Override
    protected void onCreate(
            @Nullable Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        installCrashHandler();

        /*
         * ViewBinding MUST be initialized before
         * binding.getRoot().
         */

        binding =
                ActivityMainBinding.inflate(
                        getLayoutInflater()
                );

        setContentView(
                binding.getRoot()
        );

        enableFullscreen();

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
                albumColorManager.getCurrentAccentColor();

        currentBackgroundColor =
                albumColorManager.getCurrentBackgroundColor();

        applyThemeColors();

        /*
         * -----------------------------------------------------
         * NAVIGATION
         * -----------------------------------------------------
         */

        NavHostFragment navHostFragment =
                (NavHostFragment)
                        getSupportFragmentManager()
                                .findFragmentById(
                                        R.id
                                                .nav_host_fragment_activity_main
                                );

        if (navHostFragment == null) {

            throw new IllegalStateException(
                    "NavHostFragment not found"
            );
        }

        navController =
                navHostFragment.getNavController();

        /*
         * -----------------------------------------------------
         * GLOBAL BOTTOM NAVIGATION
         * -----------------------------------------------------
         */

        setupBottomNavigation();

        /*
         * -----------------------------------------------------
         * DESTINATION LISTENER
         * -----------------------------------------------------
         */

        navController.addOnDestinationChangedListener(
                (controller, destination, arguments) -> {

                    int destinationId =
                            destination.getId();

                    updateBottomNavigation(
                            destinationId
                    );

                    boolean isPlayerScreen =
                            destinationId
                                    == R.id.navigation_player;

                    if (isPlayerScreen) {

                        animateIntoFullPlayer();

                    } else {

                        animateOutOfFullPlayer();
                    }
                }
        );

        /*
         * -----------------------------------------------------
         * INITIAL NAVIGATION STATE
         * -----------------------------------------------------
         */

        if (navController.getCurrentDestination()
                != null) {

            int destinationId =
                    navController
                            .getCurrentDestination()
                            .getId();

            updateBottomNavigation(
                    destinationId
            );

            if (destinationId
                    == R.id.navigation_player) {

                if (binding.bottomPart != null) {

                    binding.bottomPart.bottomPart
                            .setVisibility(
                                    View.GONE
                            );
                }
            }

        } else {

            updateBottomNavigation(
                    R.id.navigation_home
            );
        }

        /*
         * -----------------------------------------------------
         * MUSIC REPOSITORY
         * -----------------------------------------------------
         */

        musicRepository =
                new MusicRepository(
                        getApplicationContext()
                );

        /*
         * -----------------------------------------------------
         * MINI PLAYER
         * -----------------------------------------------------
         */

        setupMiniPlayer();

        /*
         * -----------------------------------------------------
         * PLAYER RECEIVER
         * -----------------------------------------------------
         */

        setupPlayerReceiver();

        /*
         * -----------------------------------------------------
         * PERMISSION
         * -----------------------------------------------------
         */

        requestMusicPermission();
    }

    /*
     * ---------------------------------------------------------
     * FULLSCREEN
     * ---------------------------------------------------------
     */

    private void enableFullscreen() {

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.R) {

            WindowInsetsController controller =
                    getWindow()
                            .getInsetsController();

            if (controller != null) {

                controller.hide(
                        WindowInsets.Type.statusBars()
                                | WindowInsets.Type.navigationBars()
                );

                controller.setSystemBarsBehavior(
                        WindowInsetsController
                                .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }

        } else {

            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    );
        }
    }

    /*
     * ---------------------------------------------------------
     * BOTTOM NAVIGATION SETUP
     * ---------------------------------------------------------
     */

    private void setupBottomNavigation() {

        if (binding == null
                || binding.bottomPart == null
                || navController == null) {

            return;
        }

        binding.bottomPart.navSongs
                .setOnClickListener(
                        v -> navigateTo(
                                R.id.navigation_home
                        )
                );

        binding.bottomPart.navArtist
                .setOnClickListener(
                        v -> navigateTo(
                                R.id.navigation_artist
                        )
                );

        binding.bottomPart.navDashboard
                .setOnClickListener(
                        v -> navigateTo(
                                R.id.navigation_dashboard
                        )
                );

        binding.bottomPart.navPlaylist
                .setOnClickListener(
                        v -> navigateTo(
                                R.id.navigation_playlist
                        )
                );

        binding.bottomPart.navYouTube
                .setOnClickListener(
                        v -> navigateTo(
                                R.id.navigation_youtube
                        )
                );
    }

    /*
     * ---------------------------------------------------------
     * NAVIGATION
     * ---------------------------------------------------------
     */

    private void navigateTo(
            int destinationId) {

        if (navController == null) {
            return;
        }

        try {

            if (navController.getCurrentDestination()
                    == null) {

                return;
            }

            if (navController
                    .getCurrentDestination()
                    .getId()
                    == destinationId) {

                return;
            }

            navController.navigate(
                    destinationId
            );

        } catch (Exception e) {

            Log.e(
                    "MainActivity",
                    "Navigation error",
                    e
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * OPEN FULL PLAYER
     * ---------------------------------------------------------
     */

    private void openFullPlayer() {

        if (navController == null
                || isOpeningFullPlayer) {

            return;
        }

        try {

            if (navController.getCurrentDestination()
                    == null) {

                return;
            }

            if (navController
                    .getCurrentDestination()
                    .getId()
                    == R.id.navigation_player) {

                return;
            }

            isOpeningFullPlayer = true;

            /*
             * Start the Mini Player transition first.
             *
             * Navigation happens after the Mini Player
             * has visually expanded/faded.
             */

            animateMiniPlayerToFullPlayer(
                    () -> {

                        if (navController == null) {

                            isOpeningFullPlayer =
                                    false;

                            return;
                        }

                        try {

                            navController.navigate(
                                    R.id.navigation_player
                            );

                        } catch (Exception e) {

                            isOpeningFullPlayer =
                                    false;

                            Log.e(
                                    "MainActivity",
                                    "Unable to open Full Player",
                                    e
                            );
                        }
                    }
            );

        } catch (Exception e) {

            isOpeningFullPlayer = false;

            Log.e(
                    "MainActivity",
                    "Unable to open Full Player",
                    e
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * MINI PLAYER → FULL PLAYER TRANSITION
     * ---------------------------------------------------------
     */

    private void animateMiniPlayerToFullPlayer(
            final Runnable onFinished) {

        if (binding == null
                || binding.bottomPart == null
                || binding.bottomPart.bottomPart == null) {

            if (onFinished != null) {
                onFinished.run();
            }

            return;
        }

        final View bottomPart =
                binding.bottomPart.bottomPart;

        final View miniPlayer =
                binding.bottomPart.miniPlayerRoot;

        final View navCard =
                binding.bottomPart.kanadeNavCard;

        /*
         * If Mini Player is not visible, simply navigate.
         */

        if (miniPlayer == null
                || miniPlayer.getVisibility()
                        != View.VISIBLE) {

            if (onFinished != null) {
                onFinished.run();
            }

            return;
        }

        /*
         * Make sure the animation starts from
         * a clean state.
         */

        miniPlayer.animate().cancel();
        navCard.animate().cancel();
        bottomPart.animate().cancel();

        miniPlayer.setAlpha(1f);
        miniPlayer.setScaleX(1f);
        miniPlayer.setScaleY(1f);
        miniPlayer.setTranslationY(0f);

        navCard.setAlpha(1f);

        /*
         * Mini Player expands slightly upward.
         *
         * This creates the feeling that the small player
         * is becoming the Full Player.
         */

        miniPlayer.animate()
                .translationY(
                        -getResources()
                                .getDisplayMetrics()
                                .density * 18f
                )
                .scaleX(1.06f)
                .scaleY(1.06f)
                .alpha(0f)
                .setDuration(
                        PLAYER_TRANSITION_DURATION
                )
                .setInterpolator(
                        new android.view.animation
                                .DecelerateInterpolator()
                )
                .start();

        /*
         * Navbar fades away slightly faster.
         */

        navCard.animate()
                .translationY(
                        getResources()
                                .getDisplayMetrics()
                                .density * 20f
                )
                .alpha(0f)
                .setDuration(
                        PLAYER_TRANSITION_DURATION - 50
                )
                .setInterpolator(
                        new android.view.animation
                                .AccelerateInterpolator()
                )
                .start();

        /*
         * Whole bottom section moves down slightly.
         */

        bottomPart.animate()
                .translationY(
                        getResources()
                                .getDisplayMetrics()
                                .density * 12f
                )
                .setDuration(
                        PLAYER_TRANSITION_DURATION
                )
                .withEndAction(
                        () -> {

                            if (onFinished != null) {
                                onFinished.run();
                            }
                        }
                )
                .start();
    }

    /*
     * ---------------------------------------------------------
     * FULL PLAYER ENTER ANIMATION
     * ---------------------------------------------------------
     */

    private void animateIntoFullPlayer() {

        if (binding == null
                || binding.bottomPart == null) {

            return;
        }

        /*
         * Keep global bottom UI hidden on Full Player.
         */

        binding.bottomPart.bottomPart
                .setVisibility(
                        View.GONE
                );

        /*
         * Find the current destination Fragment.
         */

        if (navController == null) {
            return;
        }

        NavHostFragment navHostFragment =
                (NavHostFragment)
                        getSupportFragmentManager()
                                .findFragmentById(
                                        R.id
                                                .nav_host_fragment_activity_main
                                );

        if (navHostFragment == null) {
            return;
        }

        View playerView = null;

        try {

            androidx.fragment.app.Fragment
                    currentFragment =
                    navHostFragment
                            .getChildFragmentManager()
                            .getFragments()
                            .stream()
                            .filter(
                                    fragment ->
                                            fragment != null
                                                    && fragment
                                                            .isVisible()
                                                    && fragment
                                                            .getView()
                                                            != null
                            )
                            .findFirst()
                            .orElse(null);

            if (currentFragment != null) {

                playerView =
                        currentFragment.getView();
            }

        } catch (Exception e) {

            Log.e(
                    "MainActivity",
                    "Unable to get Full Player view",
                    e
            );
        }

        if (playerView == null) {
            return;
        }

        final View finalPlayerView =
                playerView;

        /*
         * Start hidden/small.
         */

        finalPlayerView.animate().cancel();

        finalPlayerView.setAlpha(0f);
        finalPlayerView.setScaleX(0.96f);
        finalPlayerView.setScaleY(0.96f);
        finalPlayerView.setTranslationY(
                getResources()
                        .getDisplayMetrics()
                        .density * 24f
        );

        /*
         * Let Fragment finish layout before animating.
         */

        finalPlayerView.post(
                () -> {

                    finalPlayerView.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .translationY(0f)
                            .setDuration(
                                    PLAYER_TRANSITION_DURATION
                            )
                            .setInterpolator(
                                    new android.view.animation
                                            .DecelerateInterpolator()
                            )
                            .withEndAction(
                                    () -> {

                                        isOpeningFullPlayer =
                                                false;

                                        /*
                                         * Reset Mini Player
                                         * after the transition.
                                         */

                                        resetBottomPartAnimation();
                                    }
                            )
                            .start();
                }
        );
    }

    /*
     * ---------------------------------------------------------
     * FULL PLAYER → NORMAL SCREEN
     * ---------------------------------------------------------
     */

    private void animateOutOfFullPlayer() {

        if (binding == null
                || binding.bottomPart == null) {

            return;
        }

        /*
         * If we are leaving Full Player,
         * restore the global bottom UI.
         */

        final View bottomPart =
                binding.bottomPart.bottomPart;

        if (bottomPart == null) {
            return;
        }

        resetBottomPartAnimation();

        bottomPart.setVisibility(
                View.VISIBLE
        );

        bottomPart.setAlpha(0f);

        bottomPart.setTranslationY(
                getResources()
                        .getDisplayMetrics()
                        .density * 18f
        );

        bottomPart.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(
                        PLAYER_TRANSITION_DURATION
                )
                .setInterpolator(
                        new android.view.animation
                                .DecelerateInterpolator()
                )
                .start();
    }

    /*
     * ---------------------------------------------------------
     * RESET BOTTOM PART ANIMATION
     * ---------------------------------------------------------
     */

    private void resetBottomPartAnimation() {

        if (binding == null
                || binding.bottomPart == null) {

            return;
        }

        View bottomPart =
                binding.bottomPart.bottomPart;

        View miniPlayer =
                binding.bottomPart.miniPlayerRoot;

        View navCard =
                binding.bottomPart.kanadeNavCard;

        if (bottomPart != null) {

            bottomPart.animate().cancel();

            bottomPart.setAlpha(1f);
            bottomPart.setTranslationY(0f);
        }

        if (miniPlayer != null) {

            miniPlayer.animate().cancel();

            miniPlayer.setAlpha(1f);
            miniPlayer.setScaleX(1f);
            miniPlayer.setScaleY(1f);
            miniPlayer.setTranslationY(0f);
        }

        if (navCard != null) {

            navCard.animate().cancel();

            navCard.setAlpha(1f);
            navCard.setTranslationY(0f);
        }
    }

    /*
     * ---------------------------------------------------------
     * MINI PLAYER SETUP
     * ---------------------------------------------------------
     */

    private void setupMiniPlayer() {

        if (binding == null
                || binding.bottomPart == null) {

            return;
        }

        binding.bottomPart.miniPlayerRoot
                .setVisibility(
                        View.GONE
                );

        /*
         * -----------------------------------------------------
         * PLAY / PAUSE
         * -----------------------------------------------------
         */

        binding.bottomPart.miniPlayPause
                .setOnClickListener(
                        v -> {

                            Intent intent =
                                    new Intent(
                                            MainActivity.this,
                                            MusicPlayerService.class
                                    );

                            if (isMusicPlaying) {

                                intent.setAction(
                                        MusicPlayerService
                                                .ACTION_PAUSE
                                );

                            } else {

                                intent.setAction(
                                        MusicPlayerService
                                                .ACTION_PLAY
                                );

                                if (currentPlayingUri
                                        != null
                                        && !currentPlayingUri
                                                .trim()
                                                .isEmpty()) {

                                    intent.putExtra(
                                            MusicPlayerService
                                                    .EXTRA_SONG_URI,
                                            currentPlayingUri
                                    );
                                }
                            }

                            startMusicService(
                                    intent
                            );
                        }
                );

        /*
         * -----------------------------------------------------
         * PREVIOUS
         * -----------------------------------------------------
         */

        binding.bottomPart.miniPrevious
                .setOnClickListener(
                        v -> {

                            Intent intent =
                                    new Intent(
                                            MainActivity.this,
                                            MusicPlayerService.class
                                    );

                            intent.setAction(
                                    MusicPlayerService
                                            .ACTION_PREVIOUS
                            );

                            startMusicService(
                                    intent
                            );
                        }
                );

        /*
         * -----------------------------------------------------
         * NEXT
         * -----------------------------------------------------
         */

        binding.bottomPart.miniNext
                .setOnClickListener(
                        v -> {

                            Intent intent =
                                    new Intent(
                                            MainActivity.this,
                                            MusicPlayerService.class
                                    );

                            intent.setAction(
                                    MusicPlayerService
                                            .ACTION_NEXT
                            );

                            startMusicService(
                                    intent
                            );
                        }
                );

        /*
         * -----------------------------------------------------
         * MINI PLAYER TAP
         * -----------------------------------------------------
         */

        binding.bottomPart.miniPlayerRoot
                .setOnClickListener(
                        v -> openFullPlayer()
                );

        /*
         * -----------------------------------------------------
         * MINI PLAYER SWIPE UP
         * -----------------------------------------------------
         */

        binding.bottomPart.miniPlayerRoot
                .setOnTouchListener(
                        (v, event) -> {

                            switch (
                                    event.getActionMasked()
                            ) {

                                case MotionEvent.ACTION_DOWN:

                                    miniPlayerTouchDownY =
                                            event.getRawY();

                                    miniPlayerSwipeTriggered =
                                            false;

                                    return false;

                                case MotionEvent.ACTION_MOVE:

                                    float currentY =
                                            event.getRawY();

                                    float deltaY =
                                            currentY
                                                    - miniPlayerTouchDownY;

                                    /*
                                     * Swipe UP.
                                     */

                                    if (!miniPlayerSwipeTriggered
                                            && deltaY < -60f) {

                                        miniPlayerSwipeTriggered =
                                                true;

                                        openFullPlayer();

                                        return false;
                                    }

                                    return false;

                                case MotionEvent.ACTION_UP:

                                    miniPlayerTouchDownY =
                                            0f;

                                    miniPlayerSwipeTriggered =
                                            false;

                                    return false;

                                case MotionEvent.ACTION_CANCEL:

                                    miniPlayerTouchDownY =
                                            0f;

                                    miniPlayerSwipeTriggered =
                                            false;

                                    return false;
                            }

                            return false;
                        }
                );
    }

    /*
     * ---------------------------------------------------------
     * MUSIC SERVICE
     * ---------------------------------------------------------
     */

    private void startMusicService(
            Intent intent) {

        try {

            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.O) {

                ContextCompat.startForegroundService(
                        MainActivity.this,
                        intent
                );

            } else {

                startService(
                        intent
                );
            }

        } catch (Exception e) {

            Log.e(
                    "MainActivity",
                    "Unable to start MusicPlayerService",
                    e
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * PLAYER RECEIVER SETUP
     * ---------------------------------------------------------
     */

    private void setupPlayerReceiver() {

        if (playerReceiver != null) {
            return;
        }

        playerReceiver =
                new BroadcastReceiver() {

                    @Override
                    public void onReceive(
                            Context context,
                            Intent intent) {

                        if (intent == null) {
                            return;
                        }

                        if (!MusicPlayerService
                                .ACTION_STATE_CHANGED
                                .equals(
                                        intent.getAction()
                                )) {

                            return;
                        }

                        boolean playing =
                                intent.getBooleanExtra(
                                        MusicPlayerService
                                                .EXTRA_IS_PLAYING,
                                        false
                                );

                        String uri =
                                intent.getStringExtra(
                                        MusicPlayerService
                                                .EXTRA_CURRENT_URI
                                );

                        if (uri != null
                                && !uri.trim().isEmpty()) {

                            currentPlayingUri =
                                    uri;
                        }

                        isMusicPlaying =
                                playing;

                        updateMiniPlayer();
                    }
                };
    }

    /*
     * ---------------------------------------------------------
     * REGISTER PLAYER RECEIVER
     * ---------------------------------------------------------
     */

    private void registerPlayerReceiver() {

        setupPlayerReceiver();

        if (playerReceiver == null) {
            return;
        }

        try {

            IntentFilter filter =
                    new IntentFilter();

            filter.addAction(
                    MusicPlayerService
                            .ACTION_STATE_CHANGED
            );

            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.TIRAMISU) {

                registerReceiver(
                        playerReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                );

            } else {

                registerReceiver(
                        playerReceiver,
                        filter
                );
            }

        } catch (Exception e) {

            Log.e(
                    "MainActivity",
                    "Player receiver registration failed",
                    e
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * UNREGISTER PLAYER RECEIVER
     * ---------------------------------------------------------
     */

    private void unregisterPlayerReceiver() {

        if (playerReceiver == null) {
            return;
        }

        try {

            unregisterReceiver(
                    playerReceiver
            );

        } catch (Exception ignored) {
        }
    }

    /*
     * ---------------------------------------------------------
     * MINI PLAYER UI
     * ---------------------------------------------------------
     */

    private void updateMiniPlayer() {

        if (binding == null
                || binding.bottomPart == null) {

            return;
        }

        if (currentPlayingUri == null
                || currentPlayingUri
                        .trim()
                        .isEmpty()) {

            if (!isMusicPlaying) {

                binding.bottomPart.miniPlayerRoot
                        .setVisibility(
                                View.GONE
                        );
            }

            return;
        }

        AudioFile song =
                findSongByUri(
                        currentPlayingUri
                );

        if (song == null) {
            return;
        }

        binding.bottomPart.miniPlayerRoot
                .setVisibility(
                        View.VISIBLE
                );

        binding.bottomPart.miniTitle
                .setText(
                        safeText(
                                song.getTitle(),
                                "Unknown title"
                        )
                );

        binding.bottomPart.miniArtist
                .setText(
                        safeText(
                                song.getArtist(),
                                "Unknown artist"
                        )
                );

        if (!currentPlayingUri.equals(
                lastMiniPlayerUri
        )) {

            lastMiniPlayerUri =
                    currentPlayingUri;

            updateMiniAlbumArt(
                    song
            );
        }

        updateMiniPlayPauseIcon();

        updateMiniPlayerColors();
    }

    /*
     * ---------------------------------------------------------
     * MINI PLAY / PAUSE ICON
     * ---------------------------------------------------------
     */

    private void updateMiniPlayPauseIcon() {

        if (binding == null
                || binding.bottomPart == null
                || binding.bottomPart
                        .miniPlayPause == null) {

            return;
        }

        try {

            binding.bottomPart
                    .miniPlayPause
                    .setImageResource(
                            R.drawable.ic_play
                    );

        } catch (Exception e) {

            Log.e(
                    "MainActivity",
                    "Mini play/pause icon error",
                    e
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * MINI ALBUM ART
     * ---------------------------------------------------------
     */

    private void updateMiniAlbumArt(
            AudioFile song) {

        if (binding == null
                || binding.bottomPart == null
                || binding.bottomPart
                        .miniAlbumArt == null
                || song == null) {

            return;
        }

        final String requestedUri =
                song.getUri();

        new Thread(
                () -> {

                    Bitmap bitmap =
                            loadAlbumArt(
                                    song
                            );

                    runOnUiThread(
                            () -> {

                                if (binding == null
                                        || binding.bottomPart
                                                == null) {

                                    return;
                                }

                                if (currentPlayingUri
                                        == null
                                        || !currentPlayingUri
                                                .equals(
                                                        requestedUri
                                                )) {

                                    return;
                                }

                                if (bitmap != null) {

                                    binding.bottomPart
                                            .miniAlbumArt
                                            .setImageBitmap(
                                                    bitmap
                                            );

                                } else {

                                    binding.bottomPart
                                            .miniAlbumArt
                                            .setImageResource(
                                                    R.drawable.ic_play
                                            );
                                }
                            }
                    );
                }
        ).start();
    }

    /*
     * ---------------------------------------------------------
     * ALBUM ART LOADER
     * ---------------------------------------------------------
     */

    private Bitmap loadAlbumArt(
            AudioFile song) {

        if (song == null) {
            return null;
        }

        String path =
                song.getPath();

        if (path == null
                || path.trim().isEmpty()) {

            return null;
        }

        MediaMetadataRetriever retriever =
                new MediaMetadataRetriever();

        try {

            retriever.setDataSource(
                    path
            );

            byte[] artwork =
                    retriever.getEmbeddedPicture();

            if (artwork != null
                    && artwork.length > 0) {

                return BitmapFactory.decodeByteArray(
                        artwork,
                        0,
                        artwork.length
                );
            }

        } catch (Exception e) {

            Log.e(
                    "MainActivity",
                    "Unable to load album art",
                    e
            );

        } finally {

            try {

                retriever.release();

            } catch (Exception ignored) {
            }
        }

        return null;
    }

    /*
     * ---------------------------------------------------------
     * FIND SONG
     * ---------------------------------------------------------
     */

    private AudioFile findSongByUri(
            String uri) {

        if (uri == null
                || musicRepository == null) {

            return null;
        }

        try {

            ArrayList<AudioFile> songs =
                    musicRepository.getAllSongs();

            if (songs == null) {
                return null;
            }

            for (AudioFile song : songs) {

                if (song != null
                        && uri.equals(
                                song.getUri()
                        )) {

                    return song;
                }
            }

        } catch (Exception e) {

            Log.e(
                    "MainActivity",
                    "Unable to find current song",
                    e
            );
        }

        return null;
    }

    /*
     * ---------------------------------------------------------
     * SAFE TEXT
     * ---------------------------------------------------------
     */

    private String safeText(
            String value,
            String fallback) {

        if (value == null
                || value.trim().isEmpty()) {

            return fallback;
        }

        return value;
    }

    /*
     * ---------------------------------------------------------
     * MINI PLAYER COLORS
     * ---------------------------------------------------------
     */

    private void updateMiniPlayerColors() {

        if (binding == null
                || binding.bottomPart == null) {

            return;
        }

        try {

            int activeColor =
                    currentAccentColor;

            binding.bottomPart
                    .miniPlayPause
                    .setImageTintList(
                            ColorStateList.valueOf(
                                    activeColor
                            )
                    );

            binding.bottomPart
                    .miniPrevious
                    .setImageTintList(
                            ColorStateList.valueOf(
                                    activeColor
                            )
                    );

            binding.bottomPart
                    .miniNext
                    .setImageTintList(
                            ColorStateList.valueOf(
                                    activeColor
                            )
                    );

        } catch (Exception e) {

            Log.e(
                    "MainActivity",
                    "Mini Player color update failed",
                    e
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * BOTTOM NAVIGATION
     * ---------------------------------------------------------
     */

    private void updateBottomNavigation(
            int destinationId) {

        if (binding == null
                || binding.bottomPart == null) {

            return;
        }

        boolean songs =
                destinationId
                        == R.id.navigation_home;

        boolean artist =
                destinationId
                        == R.id.navigation_artist;

        boolean dashboard =
                destinationId
                        == R.id.navigation_dashboard;

        boolean playlist =
                destinationId
                        == R.id.navigation_playlist;

        boolean youtube =
                destinationId
                        == R.id.navigation_youtube;

        binding.bottomPart
                .navSongsIcon
                .setImageResource(
                        songs
                                ? R.drawable.ic_tracks_filled
                                : R.drawable.ic_tracks_blank
                );

        binding.bottomPart
                .navSongsText
                .setVisibility(
                        songs
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.bottomPart
                .navArtistIcon
                .setImageResource(
                        artist
                                ? R.drawable.ic_artist_filled
                                : R.drawable.ic_artist_blank
                );

        binding.bottomPart
                .navArtistText
                .setVisibility(
                        artist
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.bottomPart
                .navDashboardIcon
                .setImageResource(
                        dashboard
                                ? R.drawable.house_filled
                                : R.drawable.house_blank
                );

        binding.bottomPart
                .navDashboardText
                .setVisibility(
                        dashboard
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.bottomPart
                .navPlaylistIcon
                .setImageResource(
                        playlist
                                ? R.drawable.list_music_filled
                                : R.drawable.list_music_blank
                );

        binding.bottomPart
                .navPlaylistText
                .setVisibility(
                        playlist
                                ? View.VISIBLE
                                : View.GONE
                );

        binding.bottomPart
                .navYouTubeIcon
                .setImageResource(
                        R.drawable.ic_youtube
                );

        binding.bottomPart
                .navYouTubeText
                .setVisibility(
                        youtube
                                ? View.VISIBLE
                                : View.GONE
                );

        applyNavigationTint(
                binding.bottomPart.navSongsIcon,
                binding.bottomPart.navSongsText,
                songs
        );

        applyNavigationTint(
                binding.bottomPart.navArtistIcon,
                binding.bottomPart.navArtistText,
                artist
        );

        applyNavigationTint(
                binding.bottomPart.navDashboardIcon,
                binding.bottomPart.navDashboardText,
                dashboard
        );

        applyNavigationTint(
                binding.bottomPart.navPlaylistIcon,
                binding.bottomPart.navPlaylistText,
                playlist
        );

        applyNavigationTint(
                binding.bottomPart.navYouTubeIcon,
                binding.bottomPart.navYouTubeText,
                youtube
        );

        updateMiniPlayerColors();
    }

    /*
     * ---------------------------------------------------------
     * NAVIGATION TINT
     * ---------------------------------------------------------
     */

    private void applyNavigationTint(
            ImageView imageView,
            TextView textView,
            boolean selected) {

        int activeColor =
                currentAccentColor;

        int inactiveColor =
                Color.rgb(
                        120,
                        120,
                        120
                );

        int color =
                selected
                        ? activeColor
                        : inactiveColor;

        if (imageView != null) {

            imageView.setImageTintList(
                    ColorStateList.valueOf(
                            color
                    )
            );
        }

        if (textView != null) {

            textView.setTextColor(
                    color
            );
        }
    }

    /*
     * ---------------------------------------------------------
     * NAV ITEM ANIMATION
     * ---------------------------------------------------------
     */

    private void animateNavItem(
            View view,
            boolean selected) {

        if (view == null) {
            return;
        }

        view.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .alpha(
                        selected
                                ? 1.0f
                                : 0.9f
                )
                .setDuration(
                        180
                )
                .start();
    }

    /*
     * ---------------------------------------------------------
     * THEME COLORS
     * ---------------------------------------------------------
     */

    private void animateThemeColors(
            int newAccentColor,
            int newBackgroundColor) {

        if (binding == null) {
            return;
        }

        if (colorAnimator != null) {

            colorAnimator.cancel();
        }

        final int startAccent =
                currentAccentColor;

        final int startBackground =
                currentBackgroundColor;

        colorAnimator =
                ValueAnimator.ofFloat(
                        0f,
                        1f
                );

        colorAnimator.setDuration(
                450
        );

        colorAnimator.addUpdateListener(
                animation -> {

                    float fraction =
                            (float)
                                    animation
                                            .getAnimatedValue();

                    currentAccentColor =
                            (Integer)
                                    new ArgbEvaluator()
                                            .evaluate(
                                                    fraction,
                                                    startAccent,
                                                    newAccentColor
                                            );

                    currentBackgroundColor =
                            (Integer)
                                    new ArgbEvaluator()
                                            .evaluate(
                                                    fraction,
                                                    startBackground,
                                                    newBackgroundColor
                                            );

                    applyThemeColors();
                }
        );

        colorAnimator.start();
    }

    /*
     * ---------------------------------------------------------
     * APPLY THEME
     * ---------------------------------------------------------
     */

    private void applyThemeColors() {

        if (binding == null) {
            return;
        }

        try {

            binding.getRoot()
                    .setBackgroundColor(
                            currentBackgroundColor
                    );

        } catch (Exception ignored) {
        }

        updateBottomNavigation(
                getCurrentDestinationId()
        );
    }

    /*
     * ---------------------------------------------------------
     * CURRENT DESTINATION
     * ---------------------------------------------------------
     */

    private int getCurrentDestinationId() {

        try {

            if (navController != null
                    && navController
                            .getCurrentDestination()
                            != null) {

                return navController
                        .getCurrentDestination()
                        .getId();
            }

        } catch (Exception ignored) {
        }

        return R.id.navigation_home;
    }

    /*
     * ---------------------------------------------------------
     * LIFECYCLE
     * ---------------------------------------------------------
     */

    @Override
    protected void onStart() {

        super.onStart();

        try {

            IntentFilter filter =
                    new IntentFilter(
                            AlbumColorManager
                                    .ACTION_COLORS_CHANGED
                    );

            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.TIRAMISU) {

                registerReceiver(
                        colorReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED
                );

            } else {

                registerReceiver(
                        colorReceiver,
                        filter
                );
            }

        } catch (Exception e) {

            Log.e(
                    "MainActivity",
                    "Color receiver registration failed",
                    e
            );
        }

        registerPlayerReceiver();

        applyThemeColors();
    }

    @Override
    protected void onStop() {

        unregisterPlayerReceiver();

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
     * PERMISSIONS
     * ---------------------------------------------------------
     */

    private void requestMusicPermission() {

        if (Build.VERSION.SDK_INT
                >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_AUDIO
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission
                                        .READ_MEDIA_AUDIO
                        },
                        READ_MEDIA_AUDIO_REQUEST
                );

            } else {

                scanMusic();
            }

        } else {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission
                            .READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission
                                        .READ_EXTERNAL_STORAGE
                        },
                        READ_MEDIA_AUDIO_REQUEST
                );

            } else {

                scanMusic();
            }
        }
    }

    /*
     * ---------------------------------------------------------
     * MUSIC SCAN
     * ---------------------------------------------------------
     */

    private void scanMusic() {

        new Thread(
                () -> {

                    try {

                        ArrayList<AudioFile> songs =
                                musicRepository
                                        .getAllSongs();

                        int count =
                                songs != null
                                        ? songs.size()
                                        : 0;

                        Log.d(
                                "MainActivity",
                                "Music scanned: "
                                        + count
                        );

                    } catch (Exception e) {

                        Log.e(
                                "MainActivity",
                                "Music scan failed",
                                e
                        );
                    }
                }
        ).start();
    }

    /*
     * ---------------------------------------------------------
     * PERMISSION RESULT
     * ---------------------------------------------------------
     */

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

        if (requestCode
                == READ_MEDIA_AUDIO_REQUEST) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                scanMusic();
            }
        }
    }

    /*
     * ---------------------------------------------------------
     * DESTROY
     * ---------------------------------------------------------
     */

    @Override
    protected void onDestroy() {

        unregisterPlayerReceiver();

        if (colorAnimator != null) {

            colorAnimator.cancel();

            colorAnimator = null;
        }

        binding = null;

        super.onDestroy();
    }
}