package com.urfavxbf.kanade.ui.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.urfavxbf.kanade.AlbumArtManager;
import com.urfavxbf.kanade.AudioFile;
import com.urfavxbf.kanade.MusicPlayerService;
import com.urfavxbf.kanade.R;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    public interface QueueListener {
        void onSongClick(int position);
        void onDragStarted(RecyclerView.ViewHolder holder);
        void onDragFinished(RecyclerView.ViewHolder holder);
    }

    private final Context context;
    private final ArrayList<AudioFile> songs;
    private final QueueListener listener;
    private final AlbumArtManager albumArtManager;
    private final ExecutorService artworkExecutor = Executors.newFixedThreadPool(2);
    private int currentIndex = -1;
    private int accentColor = Color.rgb(201, 196, 255);

    public QueueAdapter(Context context, ArrayList<AudioFile> songs, int currentIndex, int accentColor, QueueListener listener) {
        this.context = context.getApplicationContext();
        this.songs = songs;
        this.currentIndex = currentIndex;
        this.accentColor = accentColor;
        this.listener = listener;
        this.albumArtManager = new AlbumArtManager(this.context);
        setHasStableIds(false);
    }

    public QueueListener getListener() { return listener; }

    public void setCurrentIndex(int index) {
        int old = currentIndex;
        currentIndex = index;
        if (old >= 0 && old < songs.size()) notifyItemChanged(old);
        if (currentIndex >= 0 && currentIndex < songs.size() && currentIndex != old) notifyItemChanged(currentIndex);
    }

    public int getCurrentIndex() { return currentIndex; }

    public void setAccentColor(int color) {
        accentColor = color;
        if (getItemCount() > 0) notifyItemRangeChanged(0, getItemCount());
    }

    public void animateAccentColor(int oldColor, int newColor) {
        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofObject(
                new android.animation.ArgbEvaluator(), oldColor, newColor);
        animator.setDuration(250);
        animator.addUpdateListener(animation -> {
            accentColor = (Integer) animation.getAnimatedValue();
            if (getItemCount() > 0) notifyItemRangeChanged(0, getItemCount());
        });
        animator.start();
    }

    public void moveItem(int from, int to) {
        if (from < 0 || to < 0 || from >= songs.size() || to >= songs.size() || from == to) return;
        AudioFile moved = songs.remove(from);
        songs.add(to, moved);
        if (currentIndex == from) {
            currentIndex = to;
        } else if (from < currentIndex && to >= currentIndex) {
            currentIndex--;
        } else if (from > currentIndex && to <= currentIndex) {
            currentIndex++;
        }
        notifyItemMoved(from, to);
        notifyItemRangeChanged(Math.min(from, to), Math.abs(to - from) + 1);
    }

    public void finishDrag() {
        if (songs.isEmpty()) return;
        ArrayList<String> uris = new ArrayList<>(songs.size());
        for (AudioFile song : songs) {
            if (song == null || song.getUri() == null || song.getUri().trim().isEmpty()) continue;
            uris.add(song.getUri());
        }
        if (uris.isEmpty()) return;
        IntentHelper.sendQueueOrder(context, uris, currentIndex);
    }

    public void notifyQueueChanged() { notifyDataSetChanged(); }

    public void shutdown() { artworkExecutor.shutdownNow(); }

    @Override public int getItemCount() { return songs.size(); }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue, parent, false);
        return new QueueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QueueViewHolder holder, int position) {
        AudioFile song = songs.get(position);
        if (song == null) return;
        String uri = song.getUri();
        holder.itemView.setTag(uri);
        holder.albumArt.setTag(uri);
        holder.title.setText(safeValue(song.getTitle(), "Unknown song"));
        holder.artist.setText(safeValue(song.getArtist(), "Unknown artist"));
        boolean current = position == currentIndex;
        holder.currentIndicator.setVisibility(current ? View.VISIBLE : View.INVISIBLE);
        holder.currentIndicator.setColorFilter(current ? accentColor : Color.TRANSPARENT);
        holder.title.setTextColor(current ? accentColor : Color.WHITE);
        holder.artist.setTextColor(current ? createSecondaryColor(accentColor) : Color.rgb(170, 170, 180));
        holder.itemView.setAlpha(current ? 1f : 0.92f);
        holder.itemView.setOnClickListener(v -> {
            int adapterPosition = holder.getAdapterPosition();
            if (adapterPosition == RecyclerView.NO_POSITION || listener == null) return;
            listener.onSongClick(adapterPosition);
        });
        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN && listener != null) listener.onDragStarted(holder);
            return true;
        });
        loadArtwork(song, holder.albumArt);
    }

    private void loadArtwork(AudioFile song, ImageView imageView) {
        imageView.setImageResource(R.drawable.album_art);
        final String uriString = song.getUri();
        if (uriString == null || uriString.trim().isEmpty()) return;
        artworkExecutor.execute(() -> {
            Bitmap bitmap = null;
            try { bitmap = albumArtManager.loadCachedBitmap(song); } catch (Exception ignored) { }
            if (bitmap == null) bitmap = loadEmbeddedArtwork(uriString);
            Bitmap result = bitmap;
            imageView.post(() -> {
                Object tag = imageView.getTag();
                if (uriString.equals(tag) && result != null && !result.isRecycled()) imageView.setImageBitmap(result);
            });
        });
    }

    private Bitmap loadEmbeddedArtwork(String uriString) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, Uri.parse(uriString));
            byte[] artwork = retriever.getEmbeddedPicture();
            if (artwork == null || artwork.length == 0) return null;
            return decodeArtwork(artwork);
        } catch (Exception ignored) {
            return null;
        } finally {
            try { retriever.release(); } catch (Exception ignored) { }
        }
    }

    private Bitmap decodeArtwork(byte[] artwork) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(artwork, 0, artwork.length, bounds);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        options.inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, 300, 300);
        try { return BitmapFactory.decodeByteArray(artwork, 0, artwork.length, options); }
        catch (OutOfMemoryError ignored) { return null; }
    }

    private int calculateSampleSize(int width, int height, int maxWidth, int maxHeight) {
        if (width <= 0 || height <= 0) return 1;
        int sample = 1;
        while (width / sample > maxWidth * 2 || height / sample > maxHeight * 2) sample *= 2;
        return Math.max(1, sample);
    }

    private int createSecondaryColor(int color) {
        return Color.rgb(Math.min(255, Color.red(color) + 25), Math.min(255, Color.green(color) + 25), Math.min(255, Color.blue(color) + 25));
    }

    private String safeValue(String value, String fallback) {
        if (value == null || value.trim().isEmpty() || "<unknown>".equalsIgnoreCase(value)) return fallback;
        return value;
    }

    static class QueueViewHolder extends RecyclerView.ViewHolder {
        ImageView albumArt;
        ImageView currentIndicator;
        ImageView dragHandle;
        TextView title;
        TextView artist;
        QueueViewHolder(@NonNull View itemView) {
            super(itemView);
            albumArt = itemView.findViewById(R.id.queueAlbumArt);
            currentIndicator = itemView.findViewById(R.id.queueCurrentIndicator);
            dragHandle = itemView.findViewById(R.id.queueDragHandle);
            title = itemView.findViewById(R.id.queueSongTitle);
            artist = itemView.findViewById(R.id.queueSongArtist);
        }
    }

    private static final class IntentHelper {
        private IntentHelper() { }
        static void sendQueueOrder(Context context, ArrayList<String> uris, int currentIndex) {
            android.content.Intent intent = new android.content.Intent(context, MusicPlayerService.class);
            intent.setAction(MusicPlayerService.ACTION_SET_QUEUE_ORDER);
            intent.putStringArrayListExtra(MusicPlayerService.EXTRA_QUEUE_URIS, uris);
            intent.putExtra(MusicPlayerService.EXTRA_QUEUE_INDEX, currentIndex);
            try { context.startService(intent); } catch (Exception ignored) { }
        }
    }
}
