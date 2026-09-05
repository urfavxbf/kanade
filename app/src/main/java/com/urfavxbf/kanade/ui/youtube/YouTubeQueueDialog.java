package com.urfavxbf.kanade.ui.youtube;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.urfavxbf.kanade.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Final YouTube queue presentation. Playback state remains owned by
 * YouTubePlaybackManager; this class only renders and dispatches selection.
 */
public final class YouTubeQueueDialog {

    private static final int BG = Color.rgb(16, 17, 26);
    private static final int SURFACE = Color.rgb(28, 29, 40);
    private static final int TEXT = Color.rgb(245, 245, 248);
    private static final int SECONDARY = Color.rgb(165, 166, 178);
    private static final int ACCENT = Color.rgb(201, 196, 255);
    private static final int ROW_HEIGHT_DP = 72;
    private static final int THUMB_SIZE_DP = 56;

    private final Context context;
    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "Kanade-YouTube-Queue-Images");
        thread.setDaemon(true);
        return thread;
    });
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private BottomSheetDialog dialog;
    private LinearLayout listContainer;

    public YouTubeQueueDialog(@NonNull Context context) {
        this.context = context;
    }

    public void show() {
        List<YouTubePlaybackManager.QueueItem> queue = YouTubePlaybackManager.getQueue();
        if (queue.isEmpty()) {
            return;
        }

        dialog = new BottomSheetDialog(context);
        dialog.setContentView(buildContent(queue));
        dialog.setOnDismissListener(d -> imageExecutor.shutdownNow());
        dialog.show();
    }

    private View buildContent(List<YouTubePlaybackManager.QueueItem> queue) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(10), dp(20), dp(12));
        root.setBackground(roundDrawable(BG, 24));

        View handle = new View(context);
        handle.setBackground(roundDrawable(Color.rgb(90, 91, 103), 8));
        LinearLayout.LayoutParams handleParams = new LinearLayout.LayoutParams(dp(42), dp(4));
        handleParams.gravity = Gravity.CENTER_HORIZONTAL;
        handleParams.bottomMargin = dp(16);
        root.addView(handle, handleParams);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout heading = new LinearLayout(context);
        heading.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f);

        TextView title = textView("Queue", 20, TEXT, Typeface.BOLD);
        heading.addView(title);

        String subtitle = queue.size() == 1 ? "1 video" : queue.size() + " videos";
        if (YouTubePlaybackManager.isRadioEnabled()) {
            subtitle += " • Mix";
        }
        TextView count = textView(subtitle, 13, SECONDARY, Typeface.NORMAL);
        heading.addView(count);
        header.addView(heading, headingParams);

        TextView close = textView("CLOSE", 12, ACCENT, Typeface.BOLD);
        close.setGravity(Gravity.CENTER);
        close.setPadding(dp(12), dp(8), dp(4), dp(8));
        close.setOnClickListener(v -> {
            if (dialog != null) {
                dialog.dismiss();
            }
        });
        header.addView(close, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(header);

        View divider = new View(context);
        divider.setBackgroundColor(Color.rgb(45, 46, 57));
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(1));
        dividerParams.topMargin = dp(14);
        dividerParams.bottomMargin = dp(8);
        root.addView(divider, dividerParams);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        listContainer = new LinearLayout(context);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(listContainer, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < queue.size(); i++) {
            addQueueRow(queue.get(i), i);
        }

        root.addView(scrollView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f));

        return root;
    }

    private void addQueueRow(
            @NonNull YouTubePlaybackManager.QueueItem item,
            int index) {
        boolean current = TextUtils.equals(
                item.videoId,
                YouTubePlaybackManager.getVideoId());

        LinearLayout row = new LinearLayout(context);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(6), dp(4), dp(6));
        row.setMinimumHeight(dp(ROW_HEIGHT_DP));
        row.setBackground(roundDrawable(
                current ? Color.rgb(43, 43, 58) : Color.TRANSPARENT,
                16));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> {
            YouTubePlaybackManager.playQueueItem(index);
            if (dialog != null) {
                dialog.dismiss();
            }
        });

        TextView number = textView(String.valueOf(index + 1), 12, SECONDARY, Typeface.BOLD);
        number.setGravity(Gravity.CENTER);
        row.addView(number, new LinearLayout.LayoutParams(dp(28), dp(THUMB_SIZE_DP)));

        ImageView thumbnail = new ImageView(context);
        thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumbnail.setBackground(roundDrawable(SURFACE, 10));
        thumbnail.setClipToOutline(true);
        thumbnail.setImageResource(R.drawable.ic_play);
        row.addView(thumbnail, new LinearLayout.LayoutParams(
                dp(THUMB_SIZE_DP),
                dp(THUMB_SIZE_DP)));

        LinearLayout metadata = new LinearLayout(context);
        metadata.setOrientation(LinearLayout.VERTICAL);
        metadata.setGravity(Gravity.CENTER_VERTICAL);
        metadata.setPadding(dp(12), 0, dp(8), 0);

        TextView itemTitle = textView(
                safe(item.title, "YouTube video"),
                15,
                TEXT,
                current ? Typeface.BOLD : Typeface.NORMAL);
        itemTitle.setMaxLines(2);
        itemTitle.setEllipsize(TextUtils.TruncateAt.END);
        metadata.addView(itemTitle);

        TextView channel = textView(
                safe(item.channel, "YouTube"),
                12,
                SECONDARY,
                Typeface.NORMAL);
        channel.setMaxLines(1);
        channel.setEllipsize(TextUtils.TruncateAt.END);
        metadata.addView(channel);

        LinearLayout.LayoutParams metadataParams = new LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f);
        row.addView(metadata, metadataParams);

        if (current) {
            TextView nowPlaying = textView("NOW", 10, ACCENT, Typeface.BOLD);
            nowPlaying.setGravity(Gravity.CENTER);
            nowPlaying.setPadding(dp(8), dp(5), dp(8), dp(5));
            nowPlaying.setBackground(roundDrawable(Color.rgb(58, 57, 82), 10));
            row.addView(nowPlaying, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        listContainer.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        loadThumbnail(thumbnail, item.thumbnailUrl);
    }

    private void loadThumbnail(ImageView target, String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        target.setTag(url);
        imageExecutor.execute(() -> {
            BitmapResult result = download(url);
            if (result == null || result.bitmap == null) {
                return;
            }
            mainHandler.post(() -> {
                if (url.equals(target.getTag())) {
                    target.setImageBitmap(result.bitmap);
                }
            });
        });
    }

    private BitmapResult download(String url) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(7000);
            connection.setReadTimeout(9000);
            connection.setRequestMethod("GET");
            connection.setInstanceFollowRedirects(true);
            try (InputStream stream = connection.getInputStream()) {
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(stream);
                return new BitmapResult(bitmap);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private TextView textView(String value, int sizeSp, int color, int typeface) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, typeface);
        return view;
    }

    private GradientDrawable roundDrawable(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static String safe(String value, String fallback) {
        return TextUtils.isEmpty(value) ? fallback : value;
    }

    private static final class BitmapResult {
        private final android.graphics.Bitmap bitmap;

        private BitmapResult(android.graphics.Bitmap bitmap) {
            this.bitmap = bitmap;
        }
    }
}
