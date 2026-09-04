package com.urfavxbf.kanade.ui.player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.urfavxbf.kanade.AlbumColorManager;
import com.urfavxbf.kanade.AudioFile;
import com.urfavxbf.kanade.MusicPlayerService;
import com.urfavxbf.kanade.R;

import java.util.ArrayList;

/**
 * Opens the live playback queue in a draggable Material bottom sheet.
 */
public class PlayerQueueButton extends androidx.appcompat.widget.AppCompatImageButton {

    private boolean receiverRegistered = false;
    private BottomSheetDialog queueSheet;
    private QueueAdapter queueAdapter;
    private int accentColor = Color.rgb(201, 196, 255);
    private int backgroundColor = Color.rgb(16, 17, 26);

    private final BroadcastReceiver queueReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null
                    || !MusicPlayerService.ACTION_QUEUE_CHANGED.equals(intent.getAction())) {
                return;
            }

            ArrayList<String> uris = intent.getStringArrayListExtra(
                    MusicPlayerService.EXTRA_QUEUE_URIS
            );
            ArrayList<String> titles = intent.getStringArrayListExtra(
                    MusicPlayerService.EXTRA_QUEUE_TITLES
            );
            ArrayList<String> artists = intent.getStringArrayListExtra(
                    MusicPlayerService.EXTRA_QUEUE_ARTISTS
            );
            int currentIndex = intent.getIntExtra(
                    MusicPlayerService.EXTRA_QUEUE_INDEX,
                    -1
            );

            showQueue(uris, titles, artists, currentIndex);
        }
    };

    private final BroadcastReceiver colorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null
                    || !AlbumColorManager.ACTION_COLORS_CHANGED.equals(intent.getAction())) {
                return;
            }

            accentColor = intent.getIntExtra(
                    AlbumColorManager.EXTRA_ACCENT_COLOR,
                    accentColor
            );
            backgroundColor = intent.getIntExtra(
                    AlbumColorManager.EXTRA_BACKGROUND_COLOR,
                    backgroundColor
            );
            setColorFilter(accentColor);
            if (queueAdapter != null) {
                queueAdapter.setAccentColor(accentColor);
            }
            if (queueSheet != null) {
                View root = queueSheet.findViewById(R.id.queueSheetRoot);
                if (root != null) {
                    root.setBackgroundColor(backgroundColor);
                }
                TextView title = queueSheet.findViewById(R.id.txtQueueTitle);
                if (title != null) {
                    title.setTextColor(accentColor);
                }
            }
        }
    };

    public PlayerQueueButton(Context context) {
        super(context);
        initialize();
    }

    public PlayerQueueButton(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public PlayerQueueButton(
            Context context,
            android.util.AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initialize();
    }

    private void initialize() {
        setOnClickListener(view -> requestQueue());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (receiverRegistered) {
            return;
        }

        Context context = getContext().getApplicationContext();
        AlbumColorManager colorManager = AlbumColorManager.getInstance(context);
        accentColor = colorManager.getCurrentAccentColor();
        backgroundColor = colorManager.getCurrentBackgroundColor();
        setColorFilter(accentColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                    queueReceiver,
                    new IntentFilter(MusicPlayerService.ACTION_QUEUE_CHANGED),
                    Context.RECEIVER_NOT_EXPORTED
            );
            context.registerReceiver(
                    colorReceiver,
                    new IntentFilter(AlbumColorManager.ACTION_COLORS_CHANGED),
                    Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            androidx.core.content.ContextCompat.registerReceiver(
                    context,
                    queueReceiver,
                    new IntentFilter(MusicPlayerService.ACTION_QUEUE_CHANGED),
                    androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            );
            androidx.core.content.ContextCompat.registerReceiver(
                    context,
                    colorReceiver,
                    new IntentFilter(AlbumColorManager.ACTION_COLORS_CHANGED),
                    androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            );
        }

        receiverRegistered = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        dismissQueue();
        unregisterReceivers();
        super.onDetachedFromWindow();
    }

    private void requestQueue() {
        Intent intent = new Intent(getContext(), MusicPlayerService.class);
        intent.setAction(MusicPlayerService.ACTION_REQUEST_QUEUE);
        getContext().startService(intent);
    }

    private void showQueue(
            ArrayList<String> uris,
            ArrayList<String> titles,
            ArrayList<String> artists,
            int currentIndex) {
        if (!isAttachedToWindow()) {
            return;
        }

        ArrayList<AudioFile> songs = new ArrayList<>();
        int size = uris == null ? 0 : uris.size();

        for (int i = 0; i < size; i++) {
            String uri = safeAt(uris, i);
            if (uri == null || uri.trim().isEmpty()) {
                continue;
            }

            songs.add(new AudioFile(
                    0L,
                    safeValue(safeAt(titles, i), "Unknown song"),
                    safeValue(safeAt(artists, i), "Unknown artist"),
                    "",
                    uri,
                    "",
                    0L,
                    0L
            ));
        }

        int normalizedCurrentIndex = currentIndex;
        if (currentIndex >= 0) {
            normalizedCurrentIndex = Math.min(currentIndex, songs.size() - 1);
        }

        dismissQueue();

        View content = LayoutInflater.from(getContext())
                .inflate(R.layout.queue_bottom_sheet, null, false);

        View sheetRoot = content.findViewById(R.id.queueSheetRoot);
        TextView title = content.findViewById(R.id.txtQueueTitle);
        TextView empty = content.findViewById(R.id.txtQueueEmpty);
        RecyclerView recycler = content.findViewById(R.id.recyclerQueue);
        ImageButton shuffle = content.findViewById(R.id.btnQueueShuffle);
        ImageButton clear = content.findViewById(R.id.btnQueueClear);
        ImageButton close = content.findViewById(R.id.btnQueueClose);
        View save = content.findViewById(R.id.btnQueueSave);

        sheetRoot.setBackgroundColor(backgroundColor);
        title.setTextColor(accentColor);
        title.setText("Queue");
        shuffle.setColorFilter(accentColor);
        clear.setColorFilter(accentColor);
        close.setColorFilter(Color.rgb(170, 172, 185));
        save.setVisibility(View.GONE);

        queueSheet = new BottomSheetDialog(getContext());
        queueSheet.setContentView(content);
        queueSheet.setOnDismissListener(dialog -> cleanupAdapter());

        if (songs.isEmpty()) {
            empty.setVisibility(View.VISIBLE);
            recycler.setVisibility(View.GONE);
        } else {
            empty.setVisibility(View.GONE);
            recycler.setVisibility(View.VISIBLE);

            queueAdapter = new QueueAdapter(
                    getContext(),
                    songs,
                    normalizedCurrentIndex,
                    accentColor,
                    new QueueAdapter.QueueListener() {
                        @Override
                        public void onSongClick(int position) {
                            Intent intent = new Intent(
                                    getContext(),
                                    MusicPlayerService.class
                            );
                            intent.setAction(MusicPlayerService.ACTION_PLAY_QUEUE_ITEM);
                            intent.putExtra(MusicPlayerService.EXTRA_QUEUE_INDEX, position);
                            getContext().startService(intent);
                            dismissQueue();
                        }

                        @Override
                        public void onDragStarted(@NonNull RecyclerView.ViewHolder holder) {
                            if (recycler.getScrollState() == RecyclerView.SCROLL_STATE_IDLE) {
                                itemTouchHelper.startDrag(holder);
                            }
                        }

                        @Override
                        public void onDragFinished(@NonNull RecyclerView.ViewHolder holder) {
                            if (queueAdapter != null) {
                                queueAdapter.finishDrag();
                            }
                        }
                    }
            );

            recycler.setLayoutManager(new LinearLayoutManager(getContext()));
            recycler.setAdapter(queueAdapter);
            itemTouchHelper.attachToRecyclerView(recycler);
        }

        shuffle.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), MusicPlayerService.class);
            intent.setAction(MusicPlayerService.ACTION_TOGGLE_SHUFFLE);
            getContext().startService(intent);
        });

        clear.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), MusicPlayerService.class);
            intent.setAction(MusicPlayerService.ACTION_CLEAR_QUEUE);
            getContext().startService(intent);
        });

        close.setOnClickListener(v -> dismissQueue());

        queueSheet.show();
    }

    private final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
            new ItemTouchHelper.SimpleCallback(
                    ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                    0
            ) {
                @Override
                public boolean onMove(
                        @NonNull RecyclerView recyclerView,
                        @NonNull RecyclerView.ViewHolder viewHolder,
                        @NonNull RecyclerView.ViewHolder target) {
                    if (queueAdapter == null) {
                        return false;
                    }
                    queueAdapter.moveItem(
                            viewHolder.getAdapterPosition(),
                            target.getAdapterPosition()
                    );
                    return true;
                }

                @Override
                public void onSwiped(
                        @NonNull RecyclerView.ViewHolder viewHolder,
                        int direction) {
                    // Swipe-to-delete is intentionally disabled.
                }

                @Override
                public void clearView(
                        @NonNull RecyclerView recyclerView,
                        @NonNull RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    if (queueAdapter != null) {
                        queueAdapter.finishDrag();
                    }
                }
            }
    );

    private void cleanupAdapter() {
        if (queueAdapter != null) {
            queueAdapter.shutdown();
            queueAdapter = null;
        }
    }

    private void dismissQueue() {
        if (queueSheet != null) {
            queueSheet.dismiss();
            queueSheet = null;
        }
        cleanupAdapter();
    }

    private void unregisterReceivers() {
        if (!receiverRegistered) {
            return;
        }

        Context context = getContext().getApplicationContext();
        try {
            context.unregisterReceiver(queueReceiver);
        } catch (IllegalArgumentException ignored) {
            // Already unregistered.
        }
        try {
            context.unregisterReceiver(colorReceiver);
        } catch (IllegalArgumentException ignored) {
            // Already unregistered.
        }
        receiverRegistered = false;
    }

    private String safeAt(ArrayList<String> values, int index) {
        if (values == null || index < 0 || index >= values.size()) {
            return null;
        }
        return values.get(index);
    }

    private String safeValue(String value, String fallback) {
        if (value == null || value.trim().isEmpty() || "<unknown>".equalsIgnoreCase(value)) {
            return fallback;
        }
        return value;
    }
}
