package com.urfavxbf.kanade.ui.player;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.urfavxbf.kanade.AudioFile;
import com.urfavxbf.kanade.MusicPlayerService;
import com.urfavxbf.kanade.PlaylistManager;
import com.urfavxbf.kanade.R;

import java.util.ArrayList;

public class QueueBottomSheet {

    private final Context context;

    private BottomSheetDialog dialog;

    private RecyclerView recyclerQueue;

    private QueueAdapter adapter;

    private TextView txtQueueTitle;
    private TextView txtQueueEmpty;

    private ImageButton btnQueueShuffle;
    private ImageButton btnQueueClear;
    private ImageButton btnQueueSave;
    private ImageButton btnQueueClose;

    private int accentColor =
            Color.rgb(
                    201,
                    196,
                    255);

    private int backgroundColor =
            Color.rgb(
                    16,
                    17,
                    26);

    private int previousBackgroundColor =
            backgroundColor;

    private boolean shuffleEnabled =
            false;

    private boolean receiverRegistered =
            false;

    private final ArrayList<AudioFile> queue =
            new ArrayList<>();

    private int currentIndex =
            -1;

    private final BroadcastReceiver queueReceiver =
            new BroadcastReceiver() {

                @Override
                public void onReceive(
                        Context context,
                        Intent intent) {

                    if (intent == null) {
                        return;
                    }

                    String action =
                            intent.getAction();

                    if (MusicPlayerService.ACTION_QUEUE_CHANGED
                            .equals(action)) {

                        readQueue(intent);

                    } else if (MusicPlayerService.ACTION_STATE_CHANGED
                            .equals(action)) {

                        shuffleEnabled =
                                intent.getBooleanExtra(
                                        MusicPlayerService.EXTRA_SHUFFLE_STATE,
                                        shuffleEnabled);

                        /*
                         * Do NOT use EXTRA_QUEUE_SIZE here.
                         * Queue size is not the current index.
                         */
                        currentIndex =
                                intent.getIntExtra(
                                        MusicPlayerService.EXTRA_QUEUE_INDEX,
                                        currentIndex);

                        if (adapter != null) {

                            adapter.setCurrentIndex(
                                    currentIndex);
                        }

                        updateButtons();
                    }
                }
            };

    public QueueBottomSheet(
            @NonNull Context context) {

        this.context =
                context;
    }

    public void show(
            int accent,
            int background) {

        accentColor =
                accent;

        backgroundColor =
                background;

        previousBackgroundColor =
                background;

        if (dialog != null
                && dialog.isShowing()) {

            return;
        }

        dialog =
                new BottomSheetDialog(
                        context);

        View view =
                LayoutInflater.from(context)
                        .inflate(
                                R.layout.queue_bottom_sheet,
                                null,
                                false);

        dialog.setContentView(
                view);

        initializeViews(
                view);

        setupRecyclerView();

        setupButtons();

        registerReceiver();

        applyColors(
                false);

        requestQueue();

        dialog.setOnDismissListener(
                d -> {

                    unregisterReceiver();

                    adapter =
                            null;

                    recyclerQueue =
                            null;
                });

        animateSheetContent(
                view);

        dialog.show();
    }

    public void dismiss() {

        if (dialog != null
                && dialog.isShowing()) {

            dialog.dismiss();

        } else {

            unregisterReceiver();
        }
    }

    private void initializeViews(
            View view) {

        recyclerQueue =
                view.findViewById(
                        R.id.recyclerQueue);

        txtQueueTitle =
                view.findViewById(
                        R.id.txtQueueTitle);

        txtQueueEmpty =
                view.findViewById(
                        R.id.txtQueueEmpty);

        btnQueueShuffle =
                view.findViewById(
                        R.id.btnQueueShuffle);

        btnQueueClear =
                view.findViewById(
                        R.id.btnQueueClear);

        btnQueueSave =
                view.findViewById(
                        R.id.btnQueueSave);

        btnQueueClose =
                view.findViewById(
                        R.id.btnQueueClose);

        if (txtQueueTitle != null) {

            txtQueueTitle.setText(
                    "Queue");
        }
    }

    private void setupRecyclerView() {

        if (recyclerQueue == null) {
            return;
        }

        recyclerQueue.setLayoutManager(
                new LinearLayoutManager(
                        context));

        recyclerQueue.setHasFixedSize(
                false);

        adapter =
                new QueueAdapter(
                        context,
                        queue,
                        currentIndex,
                        accentColor,
                        new QueueAdapter.QueueListener() {

                            @Override
                            public void onSongClick(
                                    int position) {

                                playQueueItem(
                                        position);
                            }

                            @Override
                            public void onDragStarted(
                                    RecyclerView.ViewHolder holder) {

                                if (holder == null) {
                                    return;
                                }

                                holder.itemView
                                        .animate()
                                        .scaleX(
                                                1.025f)
                                        .scaleY(
                                                1.025f)
                                        .setDuration(
                                                120)
                                        .start();
                            }

                            @Override
                            public void onDragFinished(
                                    RecyclerView.ViewHolder holder) {

                                if (holder == null) {
                                    return;
                                }

                                holder.itemView
                                        .animate()
                                        .scaleX(
                                                1f)
                                        .scaleY(
                                                1f)
                                        .setDuration(
                                                180)
                                        .start();
                            }
                        });

        recyclerQueue.setAdapter(
                adapter);

        ItemTouchHelper.SimpleCallback callback =
                new ItemTouchHelper.SimpleCallback(
                        ItemTouchHelper.UP
                                | ItemTouchHelper.DOWN,
                        0) {

                    @Override
                    public boolean onMove(
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            @NonNull RecyclerView.ViewHolder target) {

                        /*
                         * getBindingAdapterPosition()
                         * is unavailable in this project's
                         * RecyclerView version.
                         */
                        int from =
                                viewHolder
                                        .getAdapterPosition();

                        int to =
                                target
                                        .getAdapterPosition();

                        if (from
                                == RecyclerView.NO_POSITION
                                || to
                                == RecyclerView.NO_POSITION) {

                            return false;
                        }

                        adapter.moveItem(
                                from,
                                to);

                        currentIndex =
                                adapter.getCurrentIndex();

                        return true;
                    }

                    @Override
                    public void onSelectedChanged(
                            RecyclerView.ViewHolder viewHolder,
                            int actionState) {

                        super.onSelectedChanged(
                                viewHolder,
                                actionState);

                        if (actionState
                                == ItemTouchHelper.ACTION_STATE_DRAG
                                && viewHolder != null) {

                            if (adapter != null
                                    && adapter.getListener() != null) {

                                adapter.getListener()
                                        .onDragStarted(
                                                viewHolder);
                            }
                        }
                    }

                    @Override
                    public void clearView(
                            @NonNull RecyclerView recyclerView,
                            @NonNull RecyclerView.ViewHolder viewHolder) {

                        super.clearView(
                                recyclerView,
                                viewHolder);

                        if (adapter != null) {

                            if (adapter.getListener() != null) {

                                adapter.getListener()
                                        .onDragFinished(
                                                viewHolder);
                            }

                            /*
                             * Send the final reordered queue
                             * to MusicPlayerService.
                             */
                            adapter.finishDrag();

                            currentIndex =
                                    adapter.getCurrentIndex();
                        }
                    }

                    @Override
                    public void onSwiped(
                            @NonNull RecyclerView.ViewHolder viewHolder,
                            int direction) {
                    }
                };

        new ItemTouchHelper(
                callback)
                .attachToRecyclerView(
                        recyclerQueue);
    }

    private void setupButtons() {

        if (btnQueueClose != null) {

            btnQueueClose.setOnClickListener(
                    v -> {

                        animateButton(
                                btnQueueClose);

                        if (dialog != null) {

                            dialog.dismiss();
                        }
                    });
        }

        if (btnQueueShuffle != null) {

            btnQueueShuffle.setOnClickListener(
                    v -> {

                        animateButton(
                                btnQueueShuffle);

                        sendServiceAction(
                                MusicPlayerService.ACTION_TOGGLE_SHUFFLE);
                    });
        }

        if (btnQueueClear != null) {

            btnQueueClear.setOnClickListener(
                    v -> {

                        animateButton(
                                btnQueueClear);

                        clearQueue();
                    });
        }

        if (btnQueueSave != null) {

            btnQueueSave.setOnClickListener(
                    v -> {

                        animateButton(
                                btnQueueSave);

                        showSavePlaylistDialog();
                    });
        }
    }

    private void clearQueue() {

        if (queue.isEmpty()) {
            return;
        }

        new MaterialAlertDialogBuilder(
                context)
                .setTitle(
                        "Clear queue?")
                .setMessage(
                        "The currently playing song will remain in the queue.")
                .setNegativeButton(
                        "Cancel",
                        null)
                .setPositiveButton(
                        "Clear",
                        (dialogInterface, which) -> {

                            sendServiceAction(
                                    MusicPlayerService.ACTION_CLEAR_QUEUE);
                        })
                .show();
    }

    private void showSavePlaylistDialog() {

        if (queue.isEmpty()) {

            Toast.makeText(
                            context,
                            "Queue is empty",
                            Toast.LENGTH_SHORT)
                    .show();

            return;
        }

        EditText input =
                new EditText(
                        context);

        input.setSingleLine(
                true);

        input.setHint(
                "Playlist name");

        input.setPadding(
                40,
                10,
                40,
                10);

        MaterialAlertDialogBuilder builder =
                new MaterialAlertDialogBuilder(
                        context);

        builder.setTitle(
                "Save queue as playlist");

        builder.setView(
                input);

        builder.setNegativeButton(
                "Cancel",
                null);

        builder.setPositiveButton(
                "Save",
                null);

        androidx.appcompat.app.AlertDialog saveDialog =
                builder.create();

        saveDialog.setOnShowListener(
                d -> {

                    android.widget.Button positive =
                            saveDialog.getButton(
                                    androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);

                    positive.setTextColor(
                            accentColor);

                    positive.setOnClickListener(
                            v -> {

                                String name =
                                        input.getText()
                                                .toString()
                                                .trim();

                                if (name.isEmpty()) {

                                    input.setError(
                                            "Enter a playlist name");

                                    return;
                                }

                                saveQueueAsPlaylist(
                                        name);

                                saveDialog.dismiss();
                            });
                });

        saveDialog.show();
    }

    private void saveQueueAsPlaylist(
            String name) {

        try {

            PlaylistManager manager =
                    new PlaylistManager(
                            context.getApplicationContext());

            if (!manager.createPlaylist(
                    name)) {

                if (manager.playlistExists(
                        name)) {

                    Toast.makeText(
                                    context,
                                    "Playlist already exists",
                                    Toast.LENGTH_SHORT)
                            .show();

                } else {

                    Toast.makeText(
                                    context,
                                    "Unable to create playlist",
                                    Toast.LENGTH_SHORT)
                            .show();
                }

                return;
            }

            int added =
                    0;

            for (AudioFile song :
                    queue) {

                if (song == null
                        || song.getUri() == null
                        || song.getUri()
                                .trim()
                                .isEmpty()) {

                    continue;
                }

                if (manager.addSongToPlaylist(
                        name,
                        song.getUri())) {

                    added++;
                }
            }

            Toast.makeText(
                            context,
                            "Saved "
                                    + added
                                    + " songs to "
                                    + name,
                            Toast.LENGTH_SHORT)
                    .show();

        } catch (Exception e) {

            e.printStackTrace();

            Toast.makeText(
                            context,
                            "Unable to save playlist",
                            Toast.LENGTH_SHORT)
                    .show();
        }
    }

    private void playQueueItem(
            int position) {

        if (position < 0
                || position >= queue.size()) {

            return;
        }

        Intent intent =
                new Intent(
                        context,
                        MusicPlayerService.class);

        intent.setAction(
                MusicPlayerService.ACTION_PLAY_QUEUE_ITEM);

        intent.putExtra(
                MusicPlayerService.EXTRA_QUEUE_INDEX,
                position);

        startMusicService(
                intent);
    }

    private void requestQueue() {

        sendServiceAction(
                MusicPlayerService.ACTION_REQUEST_QUEUE);
    }

    private void sendServiceAction(
            String action) {

        Intent intent =
                new Intent(
                        context,
                        MusicPlayerService.class);

        intent.setAction(
                action);

        startMusicService(
                intent);
    }

    private void startMusicService(
            Intent intent) {

        try {

            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.O) {

                context.startForegroundService(
                        intent);

            } else {

                context.startService(
                        intent);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void readQueue(
            Intent intent) {

        int size =
                intent.getIntExtra(
                        MusicPlayerService.EXTRA_QUEUE_SIZE,
                        0);

        currentIndex =
                intent.getIntExtra(
                        MusicPlayerService.EXTRA_QUEUE_INDEX,
                        -1);

        ArrayList<String> uris =
                intent.getStringArrayListExtra(
                        MusicPlayerService.EXTRA_QUEUE_URIS);

        ArrayList<String> titles =
                intent.getStringArrayListExtra(
                        MusicPlayerService.EXTRA_QUEUE_TITLES);

        ArrayList<String> artists =
                intent.getStringArrayListExtra(
                        MusicPlayerService.EXTRA_QUEUE_ARTISTS);

        ArrayList<String> albums =
                intent.getStringArrayListExtra(
                        MusicPlayerService.EXTRA_QUEUE_ALBUMS);

        if (uris == null) {

            uris =
                    new ArrayList<>();
        }

        queue.clear();

        /*
         * AudioFile requires:
         *
         * long
         * String
         * String
         * String
         * String
         * String
         * long
         * long
         *
         * There is no empty constructor and no setUri().
         */
        for (int i = 0;
                i < uris.size();
                i++) {

            String uri =
                    uris.get(i);

            if (uri == null
                    || uri.trim().isEmpty()) {

                continue;
            }

            String title =
                    "";

            String artist =
                    "";

            String album =
                    "";

            if (titles != null
                    && i < titles.size()
                    && titles.get(i) != null) {

                title =
                        titles.get(i);
            }

            if (artists != null
                    && i < artists.size()
                    && artists.get(i) != null) {

                artist =
                        artists.get(i);
            }

            if (albums != null
                    && i < albums.size()
                    && albums.get(i) != null) {

                album =
                        albums.get(i);
            }

            AudioFile song =
                    new AudioFile(
                            0L,
                            title,
                            artist,
                            album,
                            "",
                            uri,
                            0L,
                            0L);

            queue.add(
                    song);
        }

        if (size != queue.size()) {

            size =
                    queue.size();
        }

        /*
         * Clamp current index so the adapter
         * never receives an invalid position.
         */
        if (queue.isEmpty()) {

            currentIndex =
                    -1;

        } else if (currentIndex < 0) {

            currentIndex =
                    0;

        } else if (currentIndex >= queue.size()) {

            currentIndex =
                    queue.size() - 1;
        }

        if (adapter != null) {

            adapter.setCurrentIndex(
                    currentIndex);

            adapter.notifyQueueChanged();
        }

        updateEmptyState();

        updateButtons();
    }

    private void updateEmptyState() {

        boolean empty =
                queue.isEmpty();

        if (recyclerQueue != null) {

            recyclerQueue.setVisibility(
                    empty
                            ? View.GONE
                            : View.VISIBLE);
        }

        if (txtQueueEmpty != null) {

            txtQueueEmpty.setVisibility(
                    empty
                            ? View.VISIBLE
                            : View.GONE);
        }

        if (txtQueueTitle != null) {

            txtQueueTitle.setText(
                    empty
                            ? "Queue"
                            : "Queue • "
                                    + queue.size());
        }
    }

    private void updateButtons() {

        if (btnQueueShuffle != null) {

            btnQueueShuffle.setColorFilter(
                    shuffleEnabled
                            ? accentColor
                            : createInactiveAccentColor(
                                    accentColor));
        }

        if (btnQueueClear != null) {

            btnQueueClear.setColorFilter(
                    createInactiveAccentColor(
                            accentColor));
        }

        if (btnQueueSave != null) {

            btnQueueSave.setColorFilter(
                    createInactiveAccentColor(
                            accentColor));
        }
    }

    private int createInactiveAccentColor(
            int color) {

        int r =
                Color.red(
                        color);

        int g =
                Color.green(
                        color);

        int b =
                Color.blue(
                        color);

        return Color.rgb(
                clamp(
                        Math.round(
                                r * 0.62f
                                        + 168f * 0.38f)),
                clamp(
                        Math.round(
                                g * 0.62f
                                        + 171f * 0.38f)),
                clamp(
                        Math.round(
                                b * 0.62f
                                        + 185f * 0.38f)));
    }

    private int clamp(
            int value) {

        return Math.max(
                0,
                Math.min(
                        255,
                        value));
    }

    private void applyColors(
            boolean animate) {

        View root =
                dialog != null
                        ? dialog.findViewById(
                                R.id.queueSheetRoot)
                        : null;

        if (root != null) {

            GradientDrawable background =
                    new GradientDrawable();

            background.setColor(
                    backgroundColor);

            background.setCornerRadii(
                    new float[]{
                            28f, 28f,
                            28f, 28f,
                            0f, 0f,
                            0f, 0f
                    });

            root.setBackground(
                    background);
        }

        if (txtQueueTitle != null) {

            txtQueueTitle.setTextColor(
                    accentColor);
        }

        if (txtQueueEmpty != null) {

            txtQueueEmpty.setTextColor(
                    createInactiveAccentColor(
                            accentColor));
        }

        updateButtons();

        if (adapter != null) {

            adapter.setAccentColor(
                    accentColor);
        }
    }

    public void updateColors(
            int accent,
            int background) {

        int oldAccent =
                accentColor;

        int oldBackground =
                backgroundColor;

        accentColor =
                accent;

        backgroundColor =
                background;

        previousBackgroundColor =
                oldBackground;

        View root =
                dialog != null
                        ? dialog.findViewById(
                                R.id.queueSheetRoot)
                        : null;

        if (root != null) {

            ValueAnimator animator =
                    ValueAnimator.ofObject(
                            new ArgbEvaluator(),
                            oldBackground,
                            backgroundColor);

            animator.setDuration(
                    350);

            animator.setInterpolator(
                    new DecelerateInterpolator());

            animator.addUpdateListener(
                    animation -> {

                        int color =
                                (Integer)
                                        animation
                                                .getAnimatedValue();

                        GradientDrawable drawable =
                                new GradientDrawable();

                        drawable.setColor(
                                color);

                        drawable.setCornerRadii(
                                new float[]{
                                        28f, 28f,
                                        28f, 28f,
                                        0f, 0f,
                                        0f, 0f
                                });

                        root.setBackground(
                                drawable);
                    });

            animator.start();
        }

        if (adapter != null) {

            adapter.animateAccentColor(
                    oldAccent,
                    accentColor);
        }

        if (txtQueueTitle != null) {

            ValueAnimator titleAnimator =
                    ValueAnimator.ofObject(
                            new ArgbEvaluator(),
                            oldAccent,
                            accentColor);

            titleAnimator.setDuration(
                    300);

            titleAnimator.addUpdateListener(
                    animation ->
                            txtQueueTitle.setTextColor(
                                    (Integer)
                                            animation
                                                    .getAnimatedValue()));

            titleAnimator.start();
        }

        updateButtons();
    }

    private void animateSheetContent(
            View view) {

        if (view == null) {
            return;
        }

        view.setAlpha(
                0f);

        view.setTranslationY(
                40f);

        view.animate()
                .alpha(
                        1f)
                .translationY(
                        0f)
                .setDuration(
                        280)
                .setInterpolator(
                        new DecelerateInterpolator())
                .start();
    }

    private void animateButton(
            View view) {

        if (view == null) {
            return;
        }

        view.animate()
                .scaleX(
                        0.82f)
                .scaleY(
                        0.82f)
                .setDuration(
                        80)
                .withEndAction(
                        () ->
                                view.animate()
                                        .scaleX(
                                                1f)
                                        .scaleY(
                                                1f)
                                        .setDuration(
                                                150)
                                        .start())
                .start();
    }

    private void registerReceiver() {

        if (receiverRegistered) {
            return;
        }

        IntentFilter filter =
                new IntentFilter();

        filter.addAction(
                MusicPlayerService.ACTION_QUEUE_CHANGED);

        filter.addAction(
                MusicPlayerService.ACTION_STATE_CHANGED);

        try {

            if (Build.VERSION.SDK_INT
                    >= Build.VERSION_CODES.TIRAMISU) {

                context.registerReceiver(
                        queueReceiver,
                        filter,
                        Context.RECEIVER_NOT_EXPORTED);

            } else {

                context.registerReceiver(
                        queueReceiver,
                        filter);
            }

            receiverRegistered =
                    true;

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void unregisterReceiver() {

        if (!receiverRegistered) {
            return;
        }

        try {

            context.unregisterReceiver(
                    queueReceiver);

        } catch (Exception ignored) {
        }

        receiverRegistered =
                false;
    }
}