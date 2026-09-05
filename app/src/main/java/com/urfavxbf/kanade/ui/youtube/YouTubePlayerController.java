package com.urfavxbf.kanade.ui.youtube;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.urfavxbf.kanade.AlbumColorManager;
import com.urfavxbf.kanade.MusicPlayerService;
import com.urfavxbf.kanade.R;
import com.urfavxbf.kanade.ui.player.QueueBottomSheet;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bridges process-scoped YouTube playback into Kanade's existing full player.
 * This class owns no player layout of its own.
 */
public final class YouTubePlayerController {

    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newSingleThreadExecutor(
            runnable -> {
                Thread thread = new Thread(runnable, "Kanade-YouTube-FullPlayer-Images");
                thread.setDaemon(true);
                return thread;
            });

    private static WeakReference<Activity> activityReference = new WeakReference<>(null);
    private static WeakReference<View> boundPlayerRoot = new WeakReference<>(null);
    private static boolean installed;
    private static boolean receiverRegistered;
    private static boolean localPlaying;
    private static int localDuration;

    private static final Runnable BIND_POLL = new Runnable() {
        @Override
        public void run() {
            Activity activity = activityReference.get();
            if (activity == null) {
                return;
            }
            bind(activity);
            if (YouTubePlaybackManager.isActive()) {
                MAIN.postDelayed(this, 400L);
            }
        }
    };

    private static final BroadcastReceiver youtubeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null
                    || !YouTubePlaybackManager.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                return;
            }
            Activity activity = activityReference.get();
            if (activity != null) {
                bind(activity);
            }
        }
    };

    private static final BroadcastReceiver localPlayerReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null
                    || !MusicPlayerService.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                return;
            }

            localPlaying = intent.getBooleanExtra(
                    MusicPlayerService.EXTRA_IS_PLAYING,
                    false);
            localDuration = intent.getIntExtra(
                    MusicPlayerService.EXTRA_DURATION,
                    localDuration);

            if (localPlaying && YouTubePlaybackManager.isActive()) {
                YouTubePlaybackManager.stop();
            }

            Activity activity = activityReference.get();
            if (activity != null) {
                bind(activity);
            }
        }
    };

    private YouTubePlayerController() {
    }

    public static void install(@NonNull Activity activity) {
        activityReference = new WeakReference<>(activity);

        if (!installed) {
            installed = true;
            registerReceivers(activity.getApplicationContext());
            registerLifecycleCallbacks(activity.getApplication());
        }

        MAIN.removeCallbacks(BIND_POLL);
        MAIN.post(BIND_POLL);
    }

    private static void registerReceivers(Context context) {
        if (receiverRegistered) {
            return;
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                        youtubeReceiver,
                        new IntentFilter(YouTubePlaybackManager.ACTION_STATE_CHANGED),
                        Context.RECEIVER_NOT_EXPORTED);
                context.registerReceiver(
                        localPlayerReceiver,
                        new IntentFilter(MusicPlayerService.ACTION_STATE_CHANGED),
                        Context.RECEIVER_NOT_EXPORTED);
            } else {
                context.registerReceiver(
                        youtubeReceiver,
                        new IntentFilter(YouTubePlaybackManager.ACTION_STATE_CHANGED));
                context.registerReceiver(
                        localPlayerReceiver,
                        new IntentFilter(MusicPlayerService.ACTION_STATE_CHANGED));
            }
            receiverRegistered = true;
        } catch (Exception ignored) {
            receiverRegistered = false;
        }
    }

    private static void registerLifecycleCallbacks(Application application) {
        application.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(
                    @NonNull Activity activity,
                    android.os.Bundle savedInstanceState) {
                activityReference = new WeakReference<>(activity);
                MAIN.post(() -> bind(activity));
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                activityReference = new WeakReference<>(activity);
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                activityReference = new WeakReference<>(activity);
                MAIN.post(() -> bind(activity));
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
            }

            @Override
            public void onActivitySaveInstanceState(
                    @NonNull Activity activity,
                    @NonNull android.os.Bundle outState) {
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                Activity current = activityReference.get();
                if (current == activity) {
                    activityReference.clear();
                }
            }
        });
    }

    private static void bind(Activity activity) {
        if (activity.isFinishing()
                || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1
                && activity.isDestroyed())) {
            return;
        }

        View playerVisualContainer = activity.findViewById(R.id.playerVisualContainer);
        if (playerVisualContainer == null) {
            return;
        }

        TextView title = activity.findViewById(R.id.fullPlayerTitle);
        TextView artist = activity.findViewById(R.id.fullPlayerArtist);
        TextView elapsed = activity.findViewById(R.id.fullPlayerElapsed);
        TextView duration = activity.findViewById(R.id.fullPlayerDuration);
        ImageView artwork = activity.findViewById(R.id.fullPlayerAlbumArt);
        SeekBar seekBar = activity.findViewById(R.id.seekFullPlayer);
        ImageButton playPause = activity.findViewById(R.id.btnFullPlayPause);
        ImageButton previous = activity.findViewById(R.id.btnFullPrevious);
        ImageButton next = activity.findViewById(R.id.btnFullNext);
        View queueButton = activity.findViewById(R.id.btnQueue);
        View shuffleButton = activity.findViewById(R.id.btnShuffle);
        View repeatButton = activity.findViewById(R.id.btnRepeat);
        TextView audioOnlyButton = activity.findViewById(R.id.btnYouTubeAudioOnly);
        TextView radioButton = activity.findViewById(R.id.btnYouTubeRadio);

        if (title == null
                || artist == null
                || elapsed == null
                || duration == null
                || artwork == null
                || seekBar == null
                || playPause == null
                || previous == null
                || next == null
                || queueButton == null
                || audioOnlyButton == null
                || radioButton == null) {
            return;
        }

        if (!YouTubePlaybackManager.isActive()) {
            restoreLocalPlayerControls(
                    activity,
                    playPause,
                    previous,
                    next,
                    queueButton,
                    seekBar,
                    shuffleButton,
                    repeatButton,
                    audioOnlyButton,
                    radioButton);
            return;
        }

        if (shuffleButton != null) {
            shuffleButton.setVisibility(View.GONE);
        }
        if (repeatButton != null) {
            repeatButton.setVisibility(View.GONE);
        }
        audioOnlyButton.setVisibility(View.VISIBLE);
        radioButton.setVisibility(View.VISIBLE);

        YouTubePlaybackManager.attachTo(playerVisualContainer);

        title.setText(safeText(YouTubePlaybackManager.getTitle(), "YouTube video"));
        artist.setText(safeText(YouTubePlaybackManager.getChannel(), "YouTube"));
        elapsed.setText(formatTime(YouTubePlaybackManager.getPositionSeconds()));
        duration.setText(formatTime(YouTubePlaybackManager.getDurationSeconds()));

        seekBar.setMax(1000);
        double currentDuration = YouTubePlaybackManager.getDurationSeconds();
        double currentPosition = YouTubePlaybackManager.getPositionSeconds();
        int progress = currentDuration > 0d
                ? (int) Math.round((currentPosition / currentDuration) * 1000d)
                : 0;
        seekBar.setProgress(Math.max(0, Math.min(1000, progress)));

        loadArtwork(artwork, YouTubePlaybackManager.getThumbnailUrl());
        updatePlayPause(playPause);
        updateModeButtons(audioOnlyButton, radioButton);

        View previousRoot = boundPlayerRoot.get();
        if (previousRoot == playerVisualContainer) {
            return;
        }

        boundPlayerRoot = new WeakReference<>(playerVisualContainer);
        playPause.setOnClickListener(v -> YouTubePlaybackManager.toggle());
        previous.setOnClickListener(v -> previousYouTubeItem());
        next.setOnClickListener(v -> YouTubePlaybackManager.next());
        audioOnlyButton.setOnClickListener(v ->
                YouTubePlaybackManager.setAudioOnly(!YouTubePlaybackManager.isAudioOnly()));
        radioButton.setOnClickListener(v ->
                YouTubePlaybackManager.setRadioEnabled(!YouTubePlaybackManager.isRadioEnabled()));
        queueButton.setOnClickListener(v -> showYouTubeQueue(activity));

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            private boolean fromUser;

            @Override
            public void onProgressChanged(
                    SeekBar bar,
                    int progressValue,
                    boolean user) {
                fromUser = user;
                if (!user) {
                    return;
                }
                double total = YouTubePlaybackManager.getDurationSeconds();
                if (total <= 0d) {
                    return;
                }
                elapsed.setText(formatTime((progressValue / 1000d) * total));
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
                fromUser = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                if (!fromUser) {
                    return;
                }
                double total = YouTubePlaybackManager.getDurationSeconds();
                if (total > 0d) {
                    YouTubePlaybackManager.seekTo((bar.getProgress() / 1000d) * total);
                }
                fromUser = false;
            }
        });
    }

    private static void restoreLocalPlayerControls(
            Activity activity,
            ImageButton playPause,
            ImageButton previous,
            ImageButton next,
            View queueButton,
            SeekBar seekBar,
            View shuffleButton,
            View repeatButton,
            TextView audioOnlyButton,
            TextView radioButton) {
        if (shuffleButton != null) {
            shuffleButton.setVisibility(View.VISIBLE);
        }
        if (repeatButton != null) {
            repeatButton.setVisibility(View.VISIBLE);
        }
        audioOnlyButton.setVisibility(View.GONE);
        radioButton.setVisibility(View.GONE);

        playPause.setOnClickListener(v -> sendLocalAction(v, localPlaying
                ? MusicPlayerService.ACTION_PAUSE
                : MusicPlayerService.ACTION_PLAY));
        previous.setOnClickListener(v -> sendLocalAction(v, MusicPlayerService.ACTION_PREVIOUS));
        next.setOnClickListener(v -> sendLocalAction(v, MusicPlayerService.ACTION_NEXT));
        queueButton.setOnClickListener(v -> {
            try {
                AlbumColorManager colorManager = AlbumColorManager.getInstance(
                        activity.getApplicationContext());
                QueueBottomSheet sheet = new QueueBottomSheet(activity);
                sheet.show(
                        colorManager.getCurrentAccentColor(),
                        colorManager.getCurrentBackgroundColor());
            } catch (Exception ignored) {
                new QueueBottomSheet(activity).show(
                        0xFFC9C4FF,
                        0xFF10111A);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar bar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar bar) {
                if (!localPlaying || localDuration <= 0) {
                    return;
                }
                int position = Math.round((bar.getProgress() / 1000f) * localDuration);
                Intent intent = new Intent(bar.getContext(), MusicPlayerService.class);
                intent.setAction(MusicPlayerService.ACTION_SEEK);
                intent.putExtra(MusicPlayerService.EXTRA_SEEK_POSITION, position);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    bar.getContext().startForegroundService(intent);
                } else {
                    bar.getContext().startService(intent);
                }
            }
        });
        boundPlayerRoot.clear();
    }

    private static void sendLocalAction(View view, String action) {
        Context context = view.getContext();
        Intent intent = new Intent(context, MusicPlayerService.class);
        intent.setAction(action);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private static void previousYouTubeItem() {
        List<YouTubePlaybackManager.QueueItem> queue = YouTubePlaybackManager.getQueue();
        String currentId = YouTubePlaybackManager.getVideoId();
        int currentIndex = -1;
        for (int i = 0; i < queue.size(); i++) {
            if (currentId.equals(queue.get(i).videoId)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex > 0) {
            YouTubePlaybackManager.playQueueItem(currentIndex - 1);
        } else {
            YouTubePlaybackManager.seekTo(0d);
        }
    }

    private static void updatePlayPause(ImageButton button) {
        button.setImageResource(YouTubePlaybackManager.isPlaying()
                ? R.drawable.ic_pause
                : R.drawable.ic_play);
        button.setContentDescription(YouTubePlaybackManager.isPlaying()
                ? "Pause YouTube playback"
                : "Play YouTube playback");
    }

    private static void updateModeButtons(TextView audioOnlyButton, TextView radioButton) {
        audioOnlyButton.setText(
                YouTubePlaybackManager.isAudioOnly() ? "VIDEO" : "AUDIO");
        radioButton.setText(
                YouTubePlaybackManager.isRadioEnabled() ? "MIX ON" : "MIX");
    }

    private static void showYouTubeQueue(Activity activity) {
        List<YouTubePlaybackManager.QueueItem> queue = YouTubePlaybackManager.getQueue();
        if (queue.isEmpty()) {
            return;
        }

        String[] labels = new String[queue.size()];
        String currentId = YouTubePlaybackManager.getVideoId();
        int checked = -1;
        for (int i = 0; i < queue.size(); i++) {
            YouTubePlaybackManager.QueueItem item = queue.get(i);
            labels[i] = (i + 1) + ". " + item.title + "\n" + item.channel;
            if (currentId.equals(item.videoId)) {
                checked = i;
            }
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setTitle(
                        YouTubePlaybackManager.isRadioEnabled()
                                ? "YouTube Mix Queue"
                                : "YouTube Queue")
                .setSingleChoiceItems(labels, checked, (dialogInterface, which) -> {
                    YouTubePlaybackManager.playQueueItem(which);
                    dialogInterface.dismiss();
                })
                .setNegativeButton("Close", null)
                .create();
        dialog.show();
    }

    private static void loadArtwork(ImageView target, String url) {
        if (TextUtils.isEmpty(url)) {
            target.setImageResource(R.drawable.ic_play);
            return;
        }

        target.setTag(url);
        IMAGE_EXECUTOR.execute(() -> {
            Bitmap bitmap = downloadBitmap(url);
            if (bitmap == null) {
                return;
            }
            MAIN.post(() -> {
                Object tag = target.getTag();
                if (url.equals(tag)) {
                    target.setImageBitmap(bitmap);
                }
            });
        });
    }

    private static Bitmap downloadBitmap(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(10000);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            try (InputStream stream = connection.getInputStream()) {
                return BitmapFactory.decodeStream(stream);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String formatTime(double seconds) {
        if (Double.isNaN(seconds) || Double.isInfinite(seconds) || seconds < 0d) {
            seconds = 0d;
        }
        long totalSeconds = Math.round(seconds);
        long minutes = totalSeconds / 60L;
        long remainingSeconds = totalSeconds % 60L;
        if (minutes >= 60L) {
            long hours = minutes / 60L;
            minutes %= 60L;
            return String.format(
                    Locale.getDefault(),
                    "%d:%02d:%02d",
                    hours,
                    minutes,
                    remainingSeconds);
        }
        return String.format(
                Locale.getDefault(),
                "%d:%02d",
                minutes,
                remainingSeconds);
    }

    private static String safeText(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }
}
