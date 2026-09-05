package com.urfavxbf.kanade.ui.youtube;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
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
    private final Handler mainHandler;
    private final android.content.BroadcastReceiver receiver;
    private boolean receiverRegistered;

    public YouTubeFullPlayerView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setBackgroundColor(Color.rgb(12, 13, 19));
        setPadding(dp(16), dp(8), dp(16), dp(12));
        setVisibility(GONE);

        imageExecutor = Executors.newFixedThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "Kanade-YouTube-Art");
            thread.setDaemon(true);
            return thread;
        });
        mainHandler = new Handler(Looper.getMainLooper());

        LinearLayout top = new LinearLayout(context);
        top.setGravity(Gravity.CENTER_VERTICAL);
        addView(top, new LayoutParams(LayoutParams.MATCH_PARENT, dp(52)));

        ImageButton back = button(R.drawable.ic_previous, "Back");
        top.addView(back, new LayoutParams(dp(44), dp(44)));
        back.setOnClickListener(v -> {
            if (getContext() instanceof ComponentActivity) {
                ((ComponentActivity) getContext()).getOnBackPressedDispatcher().onBackPressed();
            } else if (getContext() instanceof android.app.Activity) {
                ((android.app.Activity) getContext()).finish();
            }
        });

        TextView heading = text("YouTube", 18, Color.WHITE);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);
        top.addView(heading, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        radioButton = button(R.drawable.ic_shuffle, "Enable or disable YouTube Mix");
        top.addView(radioButton, new LayoutParams(dp(44), dp(44)));
        radioButton.setOnClickListener(v -> {
            YouTubePlaybackManager.setRadioEnabled(!YouTubePlaybackManager.isRadioEnabled());
            refresh();
        });

        artwork = new ImageView(context);
        artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
        artwork.setBackground(cardBackground());
        artwork.setClipToOutline(true);
        LayoutParams artParams = new LayoutParams(dp(250), dp(250));
        artParams.gravity = Gravity.CENTER_HORIZONTAL;
        artParams.topMargin = dp(8);
        addView(artwork, artParams);

        title = text("No YouTube video", 20, Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        LayoutParams titleParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(12);
        addView(title, titleParams);

        channel = text("YouTube", 14, Color.rgb(164, 166, 180));
        channel.setGravity(Gravity.CENTER);
        channel.setMaxLines(1);
        channel.setEllipsize(TextUtils.TruncateAt.END);
        addView(channel, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        mode = text("Audio only", 12, Color.rgb(220, 220, 230));
        mode.setGravity(Gravity.CENTER);
        mode.setPadding(dp(14), dp(7), dp(14), dp(7));
        mode.setBackground(pillBackground());
        LayoutParams modeParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        modeParams.gravity = Gravity.CENTER_HORIZONTAL;
        modeParams.topMargin = dp(8);
        addView(mode, modeParams);
        mode.setOnClickListener(v -> YouTubePlaybackManager.setAudioOnly(!YouTubePlaybackManager.isAudioOnly()));

        LinearLayout controls = new LinearLayout(context);
        controls.setGravity(Gravity.CENTER);
        LayoutParams controlsParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(72));
        controlsParams.topMargin = dp(5);
        addView(controls, controlsParams);

        ImageButton previous = button(R.drawable.ic_previous, "Previous YouTube item");
        controls.addView(previous, new LayoutParams(dp(52), dp(52)));
        previous.setOnClickListener(v -> playPrevious());

        playPause = button(R.drawable.ic_play, "Play or pause YouTube playback");
        playPause.setPadding(dp(13), dp(13), dp(13), dp(13));
        controls.addView(playPause, new LayoutParams(dp(64), dp(64)));
        playPause.setOnClickListener(v -> YouTubePlaybackManager.toggle());

        ImageButton next = button(R.drawable.ic_next, "Next YouTube item");
        controls.addView(next, new LayoutParams(dp(52), dp(52)));
        next.setOnClickListener(v -> YouTubePlaybackManager.next());

        queueTitle = text("Next up", 16, Color.WHITE);
        queueTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        LayoutParams queueTitleParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(34));
        queueTitleParams.topMargin = dp(2);
        addView(queueTitle, queueTitleParams);

        ScrollView queueScroll = new ScrollView(context);
        queueScroll.setClipToPadding(false);
        queueScroll.setPadding(0, 0, 0, dp(4));
        queueContainer = new LinearLayout(context);
        queueContainer.setOrientation(VERTICAL);
        queueScroll.addView(queueContainer, new ScrollView.LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        addView(queueScroll, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        receiver = new android.content.BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                refresh();
            }
        };
    }

    public YouTubeFullPlayerView(Context context, AttributeSet attrs) {
        this(context);
    }

    public YouTubeFullPlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context);
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

    private GradientDrawable cardBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.rgb(27, 28, 38));
        drawable.setCornerRadius(dp(16));
        return drawable;
    }

    private GradientDrawable pillBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.rgb(35, 36, 49));
        drawable.setCornerRadius(dp(24));
        drawable.setStroke(dp(1), Color.rgb(72, 74, 92));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void registerReceiverIfNeeded() {
        if (receiverRegistered) return;
        IntentFilter filter = new IntentFilter(YouTubePlaybackManager.ACTION_STATE_CHANGED);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getContext().registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                getContext().registerReceiver(receiver, filter);
            }
            receiverRegistered = true;
        } catch (Exception ignored) {
            receiverRegistered = false;
        }
    }

    private void unregisterReceiverIfNeeded() {
        if (!receiverRegistered) return;
        try {
            getContext().unregisterReceiver(receiver);
        } catch (Exception ignored) {
        }
        receiverRegistered = false;
    }

    private void refresh() {
        post(() -> {
            boolean active = YouTubePlaybackManager.isActive();
            setVisibility(active ? VISIBLE : GONE);
            if (!active) return;

            title.setText(YouTubePlaybackManager.getTitle());
            channel.setText(YouTubePlaybackManager.getChannel());
            boolean audio = YouTubePlaybackManager.isAudioOnly();
            mode.setText(audio ? "AUDIO ONLY" : "YOUTUBE VIDEO");
            mode.setContentDescription(audio ? "Switch to YouTube video" : "Switch to audio only");
            mode.setOnClickListener(v -> YouTubePlaybackManager.setAudioOnly(!YouTubePlaybackManager.isAudioOnly()));
            playPause.setImageResource(YouTubePlaybackManager.isPlaying()
                    ? R.drawable.ic_pause : R.drawable.ic_play);
            playPause.setContentDescription(YouTubePlaybackManager.isPlaying()
                    ? "Pause YouTube playback" : "Play YouTube playback");

            boolean mix = YouTubePlaybackManager.isRadioEnabled();
            radioButton.setImageResource(R.drawable.ic_shuffle);
            radioButton.setAlpha(mix ? 1f : 0.55f);
            radioButton.setContentDescription(mix ? "Disable YouTube Mix" : "Enable YouTube Mix");
            queueTitle.setText(mix
                    ? (YouTubePlaybackManager.isRadioLoading() ? "YouTube Mix • Loading…" : "YouTube Mix • Next up")
                    : "Next up");

            renderQueue();
            loadArtwork(YouTubePlaybackManager.getThumbnailUrl());
        });
    }

    private void playPrevious() {
        List<YouTubePlaybackManager.QueueItem> items = YouTubePlaybackManager.getQueue();
        String current = YouTubePlaybackManager.getVideoId();
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (current.equals(items.get(i).videoId)) {
                index = i;
                break;
            }
        }
        if (index > 0) {
            YouTubePlaybackManager.playQueueItem(index - 1);
        }
    }

    private void renderQueue() {
        queueContainer.removeAllViews();
        List<YouTubePlaybackManager.QueueItem> items = YouTubePlaybackManager.getQueue();
        String current = YouTubePlaybackManager.getVideoId();
        if (items.isEmpty()) {
            TextView empty = text("Queue is empty", 13, Color.rgb(150, 152, 168));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(18), 0, dp(18));
            queueContainer.addView(empty);
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            YouTubePlaybackManager.QueueItem item = items.get(i);
            LinearLayout row = new LinearLayout(getContext());
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(7), dp(8), dp(7));
            row.setBackground(cardBackground());

            ImageView thumb = new ImageView(getContext());
            thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.setBackground(cardBackground());
            thumb.setClipToOutline(true);
            row.addView(thumb, new LayoutParams(dp(72), dp(44)));

            LinearLayout texts = new LinearLayout(getContext());
            texts.setOrientation(VERTICAL);
            texts.setGravity(Gravity.CENTER_VERTICAL);
            texts.setPadding(dp(10), 0, dp(6), 0);
            row.addView(texts, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

            TextView itemTitle = text(item.title, 14, Color.WHITE);
            itemTitle.setMaxLines(1);
            itemTitle.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(itemTitle);

            TextView itemChannel = text(item.channel, 11, Color.rgb(145, 147, 164));
            itemChannel.setMaxLines(1);
            itemChannel.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(itemChannel);

            TextView index = text(String.valueOf(i + 1), 11, Color.rgb(125, 127, 143));
            index.setGravity(Gravity.CENTER);
            row.addView(index, new LayoutParams(dp(24), dp(40)));

            if (item.videoId.equals(current)) {
                itemTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                itemTitle.setTextColor(Color.rgb(214, 210, 255));
                row.setAlpha(1f);
            }

            final int queueIndex = i;
            row.setOnClickListener(v -> YouTubePlaybackManager.playQueueItem(queueIndex));
            LayoutParams rowParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(58));
            rowParams.bottomMargin = dp(6);
            queueContainer.addView(row, rowParams);
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
            if (bitmap == null) return;
            mainHandler.post(() -> {
                if (url.equals(YouTubePlaybackManager.getThumbnailUrl())) {
                    artwork.setImageBitmap(bitmap);
                }
            });
        });
    }

    private void loadRowArtwork(ImageView target, String url) {
        if (TextUtils.isEmpty(url)) return;
        imageExecutor.execute(() -> {
            Bitmap bitmap = download(url);
            if (bitmap != null) {
                mainHandler.post(() -> {
                    if (target.getParent() != null) target.setImageBitmap(bitmap);
                });
            }
        });
    }

    private Bitmap download(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(9000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod("GET");
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
        registerReceiverIfNeeded();
        refresh();
    }

    @Override
    protected void onDetachedFromWindow() {
        unregisterReceiverIfNeeded();
        mainHandler.removeCallbacksAndMessages(null);
        super.onDetachedFromWindow();
    }
}
