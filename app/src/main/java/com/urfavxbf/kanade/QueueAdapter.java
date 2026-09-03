package com.urfavxbf.kanade.ui.player;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.urfavxbf.kanade.AudioFile;
import com.urfavxbf.kanade.R;

import java.util.ArrayList;

public class QueueAdapter
        extends RecyclerView.Adapter<QueueAdapter.QueueViewHolder> {

    public interface QueueListener {
        void onSongClick(int position);

        void onDragStarted(
                RecyclerView.ViewHolder holder);

        void onDragFinished(
                RecyclerView.ViewHolder holder);
    }

    private final Context context;
    private final ArrayList<AudioFile> songs;
    private final QueueListener listener;

    private int currentIndex = -1;

    private int accentColor =
            Color.rgb(
                    201,
                    196,
                    255);

    public QueueAdapter(
            Context context,
            ArrayList<AudioFile> songs,
            int currentIndex,
            int accentColor,
            QueueListener listener) {

        this.context =
                context;

        this.songs =
                songs;

        this.currentIndex =
                currentIndex;

        this.accentColor =
                accentColor;

        this.listener =
                listener;

        /*
         * Stable IDs are intentionally disabled.
         *
         * Queue items can be reordered and the same
         * MediaStore URI/hash can otherwise cause
         * RecyclerView stable-ID collisions.
         */
        setHasStableIds(false);
    }

    public QueueListener getListener() {
        return listener;
    }

    public void setCurrentIndex(
            int index) {

        int old =
                currentIndex;

        currentIndex =
                index;

        if (old >= 0
                && old < songs.size()) {

            notifyItemChanged(old);
        }

        if (currentIndex >= 0
                && currentIndex < songs.size()
                && currentIndex != old) {

            notifyItemChanged(
                    currentIndex);
        }
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setAccentColor(
            int color) {

        accentColor =
                color;

        if (getItemCount() > 0) {

            notifyItemRangeChanged(
                    0,
                    getItemCount());
        }
    }

    public void animateAccentColor(
            int oldColor,
            int newColor) {

        android.animation.ValueAnimator animator =
                android.animation.ValueAnimator.ofObject(
                        new android.animation.ArgbEvaluator(),
                        oldColor,
                        newColor);

        animator.setDuration(
                300);

        animator.addUpdateListener(
                animation -> {

                    accentColor =
                            (Integer)
                                    animation.getAnimatedValue();

                    if (getItemCount() > 0) {

                        notifyItemRangeChanged(
                                0,
                                getItemCount());
                    }
                });

        animator.start();
    }

    public void moveItem(
            int from,
            int to) {

        if (from < 0
                || to < 0
                || from >= songs.size()
                || to >= songs.size()
                || from == to) {

            return;
        }

        AudioFile moved =
                songs.remove(from);

        songs.add(
                to,
                moved);

        /*
         * Keep the currently playing item's index
         * synchronized with the visual queue.
         */
        if (currentIndex == from) {

            currentIndex =
                    to;

        } else if (from < currentIndex
                && to >= currentIndex) {

            currentIndex--;

        } else if (from > currentIndex
                && to <= currentIndex) {

            currentIndex++;
        }

        notifyItemMoved(
                from,
                to);

        int start =
                Math.min(
                        from,
                        to);

        int count =
                Math.abs(
                        to - from)
                        + 1;

        notifyItemRangeChanged(
                start,
                count);
    }

    public void finishDrag() {

        if (context == null
                || songs.isEmpty()) {

            return;
        }

        ArrayList<String> uris =
                new ArrayList<>();

        for (AudioFile song :
                songs) {

            if (song != null
                    && song.getUri() != null
                    && !song.getUri()
                            .trim()
                            .isEmpty()) {

                uris.add(
                        song.getUri());
            }
        }

        if (uris.isEmpty()) {
            return;
        }

        android.content.Intent intent =
                new android.content.Intent(
                        context,
                        com.urfavxbf.kanade.MusicPlayerService.class);

        intent.setAction(
                com.urfavxbf.kanade.MusicPlayerService.ACTION_SET_QUEUE_ORDER);

        intent.putStringArrayListExtra(
                com.urfavxbf.kanade.MusicPlayerService.EXTRA_QUEUE_URIS,
                uris);

        intent.putExtra(
                com.urfavxbf.kanade.MusicPlayerService.EXTRA_QUEUE_INDEX,
                currentIndex);

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

    public void notifyQueueChanged() {
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    @NonNull
    @Override
    public QueueViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view =
                LayoutInflater.from(context)
                        .inflate(
                                R.layout.item_queue,
                                parent,
                                false);

        return new QueueViewHolder(
                view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull QueueViewHolder holder,
            int position) {

        AudioFile song =
                songs.get(position);

        if (song == null) {
            return;
        }

        String title =
                song.getTitle();

        String artist =
                song.getArtist();

        holder.title.setText(
                title == null
                        || title.trim().isEmpty()
                        ? "Unknown song"
                        : title);

        holder.artist.setText(
                artist == null
                        || artist.trim().isEmpty()
                        ? "Unknown artist"
                        : artist);

        boolean current =
                position == currentIndex;

        holder.currentIndicator
                .setVisibility(
                        current
                                ? View.VISIBLE
                                : View.INVISIBLE);

        holder.title.setTextColor(
                current
                        ? accentColor
                        : Color.WHITE);

        holder.artist.setTextColor(
                current
                        ? createSecondaryColor(
                                accentColor)
                        : Color.rgb(
                                170,
                                170,
                                180));

        holder.itemView.setAlpha(
                current
                        ? 1f
                        : 0.92f);

        holder.itemView.setOnClickListener(
                v -> {

                    int adapterPosition =
                            holder.getAdapterPosition();

                    if (adapterPosition
                            == RecyclerView.NO_POSITION) {

                        return;
                    }

                    if (listener != null) {

                        listener.onSongClick(
                                adapterPosition);
                    }
                });

        holder.itemView.setOnLongClickListener(
                v -> {

                    if (listener != null) {

                        listener.onDragStarted(
                                holder);
                    }

                    return true;
                });

        holder.albumArt.setTag(
                song.getUri());

        loadArtwork(
                song,
                holder.albumArt);
    }

    private int createSecondaryColor(
            int color) {

        int r =
                Math.min(
                        255,
                        Color.red(color)
                                + 25);

        int g =
                Math.min(
                        255,
                        Color.green(color)
                                + 25);

        int b =
                Math.min(
                        255,
                        Color.blue(color)
                                + 25);

        return Color.rgb(
                r,
                g,
                b);
    }

    private void loadArtwork(
            final AudioFile song,
            final ImageView imageView) {

        imageView.setImageResource(
                R.drawable.ic_play);

        final String uri =
                song.getUri();

        final String path =
                song.getPath();

        new Thread(
                () -> {

                    Bitmap bitmap =
                            null;

                    try {

                        if (Build.VERSION.SDK_INT
                                >= Build.VERSION_CODES.Q
                                && uri != null
                                && !uri.trim().isEmpty()) {

                            bitmap =
                                    context
                                            .getContentResolver()
                                            .loadThumbnail(
                                                    Uri.parse(uri),
                                                    new Size(
                                                            300,
                                                            300),
                                                    null);
                        }

                    } catch (Exception ignored) {
                    }

                    if (bitmap == null
                            && path != null
                            && !path.trim().isEmpty()) {

                        MediaMetadataRetriever retriever =
                                new MediaMetadataRetriever();

                        try {

                            retriever.setDataSource(
                                    path);

                            byte[] artwork =
                                    retriever.getEmbeddedPicture();

                            if (artwork != null
                                    && artwork.length > 0) {

                                bitmap =
                                        BitmapFactory.decodeByteArray(
                                                artwork,
                                                0,
                                                artwork.length);
                            }

                        } catch (Exception ignored) {

                        } finally {

                            try {

                                retriever.release();

                            } catch (Exception ignored) {
                            }
                        }
                    }

                    final Bitmap result =
                            bitmap;

                    imageView.post(
                            () -> {

                                Object tag =
                                        imageView.getTag();

                                if (uri == null
                                        || !uri.equals(tag)) {

                                    return;
                                }

                                if (result != null) {

                                    imageView.setImageBitmap(
                                            result);

                                } else {

                                    imageView.setImageResource(
                                            R.drawable.ic_play);
                                }
                            });

                })
                .start();
    }

    static class QueueViewHolder
            extends RecyclerView.ViewHolder {

        ImageView albumArt;
        ImageView currentIndicator;
        TextView title;
        TextView artist;

        QueueViewHolder(
                @NonNull View itemView) {

            super(itemView);

            albumArt =
                    itemView.findViewById(
                            R.id.queueAlbumArt);

            currentIndicator =
                    itemView.findViewById(
                            R.id.queueCurrentIndicator);

            title =
                    itemView.findViewById(
                            R.id.queueSongTitle);

            artist =
                    itemView.findViewById(
                            R.id.queueSongArtist);
        }
    }
}