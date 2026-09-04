package com.urfavxbf.kanade.ui.player;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;

import com.urfavxbf.kanade.AlbumColorManager;
import com.urfavxbf.kanade.MusicPlayerService;

import java.util.ArrayList;

/**
 * Queue control that requests the live service queue and presents it in a
 * lifecycle-safe dialog.
 */
public class PlayerQueueButton extends AppCompatImageButton {

    private boolean receiverRegistered = false;
    private AlertDialog queueDialog;
    private int accentColor = Color.rgb(201, 196, 255);

    private final BroadcastReceiver queueReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null
                    || !MusicPlayerService.ACTION_QUEUE_CHANGED.equals(intent.getAction())) {
                return;
            }

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

            showQueue(titles, artists, currentIndex);
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
            setColorFilter(accentColor);
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
        IntentFilter queueFilter = new IntentFilter(
                MusicPlayerService.ACTION_QUEUE_CHANGED
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                    queueReceiver,
                    queueFilter,
                    Context.RECEIVER_NOT_EXPORTED
            );
            context.registerReceiver(
                    colorReceiver,
                    new IntentFilter(AlbumColorManager.ACTION_COLORS_CHANGED),
                    Context.RECEIVER_NOT_EXPORTED
            );
        } else {
            ContextCompat.registerReceiver(
                    context,
                    queueReceiver,
                    queueFilter,
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );
            ContextCompat.registerReceiver(
                    context,
                    colorReceiver,
                    new IntentFilter(AlbumColorManager.ACTION_COLORS_CHANGED),
                    ContextCompat.RECEIVER_NOT_EXPORTED
            );
        }

        accentColor = AlbumColorManager.getInstance(context).getCurrentAccentColor();
        setColorFilter(accentColor);
        receiverRegistered = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        dismissQueue();

        if (receiverRegistered) {
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

        super.onDetachedFromWindow();
    }

    private void requestQueue() {
        Intent intent = new Intent(
                getContext(),
                MusicPlayerService.class
        );
        intent.setAction(MusicPlayerService.ACTION_REQUEST_QUEUE);
        getContext().startService(intent);
    }

    private void showQueue(
            ArrayList<String> titles,
            ArrayList<String> artists,
            int currentIndex) {

        if (!isAttachedToWindow()) {
            return;
        }

        ArrayList<String> items = new ArrayList<>();

        if (titles != null) {
            for (int i = 0; i < titles.size(); i++) {
                String title = safeValue(
                        titles.get(i),
                        "Unknown title"
                );
                String artist = artists != null && i < artists.size()
                        ? safeValue(artists.get(i), "Unknown artist")
                        : "Unknown artist";

                String prefix = i == currentIndex ? "▶  " : "    ";
                items.add(prefix + title + "\n       " + artist);
            }
        }

        if (items.isEmpty()) {
            items.add("Queue is empty");
        }

        ListView listView = new ListView(getContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_list_item_1,
                items
        );
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (titles == null || position >= titles.size()) {
                return;
            }

            Intent intent = new Intent(
                    getContext(),
                    MusicPlayerService.class
            );
            intent.setAction(MusicPlayerService.ACTION_PLAY_QUEUE_ITEM);
            intent.putExtra(MusicPlayerService.EXTRA_QUEUE_INDEX, position);
            getContext().startService(intent);
            dismissQueue();
        });

        dismissQueue();

        queueDialog = new AlertDialog.Builder(getContext())
                .setTitle("Queue")
                .setView(listView)
                .setPositiveButton("Close", null)
                .create();

        queueDialog.setOnDismissListener(dialog -> queueDialog = null);
        queueDialog.show();
    }

    private void dismissQueue() {
        if (queueDialog != null) {
            queueDialog.dismiss();
            queueDialog = null;
        }
    }

    private String safeValue(String value, String fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        return value;
    }
}
