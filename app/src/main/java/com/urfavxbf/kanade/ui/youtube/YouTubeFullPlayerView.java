package com.urfavxbf.kanade.ui.youtube;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.urfavxbf.kanade.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class YouTubeFullPlayerView extends LinearLayout {
    private final ImageView artwork;
    private final TextView title;
    private final TextView channel;
    private final TextView mode;
    private final TextView queueTitle;
    private final LinearLayout queueContainer;
    private final ImageButton playPause;
    private final ImageButton radioButton;
    private final ExecutorService imageExecutor;
    private final android.os.Handler mainHandler;
    private final android.content.BroadcastReceiver receiver;

    public YouTubeFullPlayerView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);
        setBackgroundColor(Color.rgb(16, 17, 26));
        setPadding(dp(18), dp(12), dp(18), dp(10));
        setVisibility(GONE);

        imageExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Kanade-YouTube-Full-Art");
            thread.setDaemon(true);
            return thread;
        });
        mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        LinearLayout top = new LinearLayout(context);
        top.setGravity(Gravity.CENTER_VERTICAL);
        addView(top, new LayoutParams(LayoutParams.MATCH_PARENT, dp(48)));

        ImageButton back = button(R.drawable.ic_previous, "Back");
        top.addView(back, new LayoutParams(dp(44), dp(44)));
        back.setOnClickListener(v -> {
            if (getContext() instanceof android.app.Activity) {
                ((android.app.Activity) getContext()).onBackPressed();
            }
        });

        TextView heading = text("YouTube", 18, Color.WHITE);
        heading.setGravity(Gravity.CENTER);
        top.addView(heading, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        radioButton = button(R.drawable.ic_shuffle, "Toggle YouTube radio");
        top.addView(radioButton, new LayoutParams(dp(44), dp(44)));
        radioButton.setOnClickListener(v -> {
            YouTubePlaybackManager.setRadioEnabled(!YouTubePlaybackManager.isRadioEnabled());
            refresh();
        });

        artwork = new ImageView(context);
        artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        artwork.setBackgroundColor(Color.rgb(27, 28, 39));
        LayoutParams artParams = new LayoutParams(dp(270), dp(270));
        artParams.gravity = Gravity.CENTER_HORIZONTAL;
        artParams.topMargin = dp(8);
        addView(artwork, artParams);

        title = text("No YouTube video", 20, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        LayoutParams titleParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(12);
        addView(title, titleParams);

        channel = text("YouTube", 14, Color.rgb(150, 152, 170));
        channel.setGravity(Gravity.CENTER);
        addView(channel, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        mode = text("", 12, Color.rgb(150, 152, 170));
        mode.setGravity(Gravity.CENTER);
        addView(mode, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout controls = new LinearLayout(context);
        controls.setGravity(Gravity.CENTER);
        LayoutParams controlsParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(72));
        controlsParams.topMargin = dp(6);
        addView(controls, controlsParams);

        ImageButton previous = button(R.drawable.ic_previous, "Previous YouTube item");
        controls.addView(previous, new LayoutParams(dp(56), dp(56)));
        previous.setOnClickListener(v -> {
            List<YouTubePlaybackManager.QueueItem> items = YouTubePlaybackManager.getQueue();
            String current = YouTubePlaybackManager.getVideoId();
            int index = -1;
            for (int i = 0; i < items.size(); i++) {
                if (current.equals(items.get(i).videoId)) { index = i; break; }
            }
            if (index > 0) YouTubePlaybackManager.playQueueItem(index - 1);
        });

        playPause = button(R.drawable.ic_play, "Play or pause YouTube playback");
        controls.addView(playPause, new LayoutParams(dp(68), dp(68)));
        playPause.setOnClickListener(v -> YouTubePlaybackManager.toggle());

        ImageButton next = button(R.drawable.ic_next, "Next YouTube item");
        controls.addView(next, new LayoutParams(dp(56), dp(56)));
        next.setOnClickListener(v -> YouTubePlaybackManager.next());

        queueTitle = text("Queue", 16, Color.WHITE);
        queueTitle.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        addView(queueTitle, new LayoutParams(LayoutParams.MATCH_PARENT, dp(32)));

        ScrollView queueScroll = new ScrollView(context);
        queueContainer = new LinearLayout(context);
        queueContainer.setOrientation(VERTICAL);
        queueScroll.addView(queueContainer, new ScrollView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        LayoutParams scrollParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f);
        addView(queueScroll, scrollParams);

        receiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refresh();
            }
        };
        registerReceiver();
    }

    private ImageButton button(int drawable, String description) {
        ImageButton button = new ImageButton(getContext());
        button.setImageResource(drawable);
        button.setColorFilter(Color.WHITE);
        button.setBackground(ContextCompat.getDrawable(getContext(), android.R.drawable.btn_default));
        button.setContentDescription(description);
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        return button;
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter(YouTubePlaybackManager.ACTION_STATE_CHANGED);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getContext().registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                getContext().registerReceiver(receiver, filter);
            }
        } catch (Exception ignored) {
        }
    }

    private void refresh() {
        post(() -> {
            boolean active = YouTubePlaybackManager.isActive();
            setVisibility(active ? VISIBLE : GONE);
            if (!active) return;

            title.setText(YouTubePlaybackManager.getTitle());
            channel.setText(YouTubePlaybackManager.getChannel());
            mode.setText(YouTubePlaybackManager.isAudioOnly() ? "Audio only" : "YouTube video");
            playPause.setImageResource(YouTubePlaybackManager.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
            radioButton.setImageResource(YouTubePlaybackManager.isRadioEnabled() ? R.drawable.ic_pause : R.drawable.ic_shuffle);
            queueTitle.setText(YouTubePlaybackManager.isRadioEnabled() ? "YouTube Radio Queue" : "YouTube Queue");
            renderQueue();
            loadArtwork(YouTubePlaybackManager.getThumbnailUrl());
        });
    }

    private void renderQueue() {
        queueContainer.removeAllViews();
        List<YouTubePlaybackManager.QueueItem> items = YouTubePlaybackManager.getQueue();
        String current = YouTubePlaybackManager.getVideoId();
        for (int i = 0; i < items.size(); i++) {
            YouTubePlaybackManager.QueueItem item = items.get(i);
            LinearLayout row = new LinearLayout(getContext());
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(4), dp(5), dp(4), dp(5));

            ImageView thumb = new ImageView(getContext());
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            row.addView(thumb, new LayoutParams(dp(62), dp(36)));

            LinearLayout texts = new LinearLayout(getContext());
            texts.setOrientation(VERTICAL);
            texts.setPadding(dp(10), 0, dp(4), 0);
            row.addView(texts, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            TextView itemTitle = text(item.title, 14, Color.WHITE);
            itemTitle.setMaxLines(1);
            itemTitle.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(itemTitle);
            TextView itemChannel = text(item.channel, 11, Color.rgb(150, 152, 170));
            itemChannel.setMaxLines(1);
            itemChannel.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(itemChannel);

            if (item.videoId.equals(current)) {
                itemTitle.setTextColor(Color.rgb(201, 196, 255));
            }
            final int index = i;
            row.setOnClickListener(v -> YouTubePlaybackManager.playQueueItem(index));
            queueContainer.addView(row);
            loadRowArtwork(thumb, item.thumbnailUrl);
        }
    }

    private void loadArtwork(String url) {
        if (TextUtils.isEmpty(url)) {
            artwork.setImageResource(android.R.drawable.ic_menu_gallery);
            return;
        }
        imageExecutor.execute(() -> {
            Bitmap bitmap = download(url);
            if (bitmap != null) mainHandler.post(() -> {
                if (url.equals(YouTubePlaybackManager.getThumbnailUrl())) artwork.setImageBitmap(bitmap);
            });
        });
    }

    private void loadRowArtwork(ImageView target, String url) {
        if (TextUtils.isEmpty(url)) return;
        imageExecutor.execute(() -> {
            Bitmap bitmap = download(url);
            if (bitmap != null) mainHandler.post(() -> target.setImageBitmap(bitmap));
        });
    }

    private Bitmap download(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(9000);
            connection.setInstanceFollowRedirects(true);
            try (InputStream input = connection.getInputStream()) {
                return BitmapFactory.decodeStream(input);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        refresh();
    }

    @Override
    protected void onDetachedFromWindow() {
        try {
            getContext().unregisterReceiver(receiver);
        } catch (Exception ignored) {
        }
        imageExecutor.shutdownNow();
        mainHandler.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }
}
