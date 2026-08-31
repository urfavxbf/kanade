package com.urfavxbf.kanade;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class AudioListAdapter
        extends RecyclerView.Adapter<AudioListAdapter.ViewHolder> {

    private final ArrayList<AudioFile> songs;
    private final OnSongClickListener listener;
    private final FavoriteManager favoriteManager;

    public interface OnSongClickListener {

        void onSongClick(AudioFile song);

        void onFavoriteClick(AudioFile song);

        void onMoreClick(AudioFile song, View anchor);
    }

    public AudioListAdapter(
            ArrayList<AudioFile> songs,
            OnSongClickListener listener,
            FavoriteManager favoriteManager) {

        this.songs = songs;
        this.listener = listener;
        this.favoriteManager = favoriteManager;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view =
                LayoutInflater.from(parent.getContext())
                        .inflate(
                                R.layout.item_audio,
                                parent,
                                false
                        );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        AudioFile song = songs.get(position);

        String title = song.getTitle();

        if (title == null ||
                title.trim().isEmpty()) {

            title = "Unknown title";
        }

        holder.title.setText(title);

        String artist = song.getArtist();

        if (artist == null ||
                artist.trim().isEmpty()) {

            artist = "Unknown artist";
        }

        holder.artist.setText(artist);

        String album = song.getAlbum();

        if (album == null ||
                album.trim().isEmpty()) {

            album = "Unknown album";
        }

        holder.album.setText(album);

        /*
         * Reset album art before loading.
         */
        holder.albumArt.setImageResource(
                android.R.drawable.ic_media_play
        );

        loadAlbumArt(
                holder.albumArt,
                song.getPath()
        );

        /*
         * Favorite icon.
         */
        updateFavoriteIcon(
                holder,
                song
        );

        /*
         * Whole item = play song.
         */
        holder.itemView.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (listener != null) {

                            listener.onSongClick(
                                    song
                            );
                        }
                    }
                }
        );

        /*
         * Favorite button.
         */
        holder.favorite.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (listener != null) {

                            listener.onFavoriteClick(
                                    song
                            );
                        }
                    }
                }
        );

        /*
         * More button.
         */
        holder.more.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (listener != null) {

                            listener.onMoreClick(
                                    song,
                                    v
                            );
                        }
                    }
                }
        );
    }

    private void updateFavoriteIcon(
            ViewHolder holder,
            AudioFile song) {

        if (favoriteManager == null ||
                song == null) {

            holder.favorite.setImageResource(
                    R.drawable.heart_blank
            );

            return;
        }

        String uri =
                song.getUri();

        if (uri == null ||
                uri.trim().isEmpty()) {

            holder.favorite.setImageResource(
                    R.drawable.heart_blank
            );

            return;
        }

        boolean isFavorite =
                favoriteManager.isFavorite(
                        uri
                );

        if (isFavorite) {

            holder.favorite.setImageResource(
                    R.drawable.heart_filled
            );

        } else {

            holder.favorite.setImageResource(
                    R.drawable.heart_blank
            );
        }
    }

    private void loadAlbumArt(
            ImageView imageView,
            String path) {

        if (path == null ||
                path.trim().isEmpty()) {

            return;
        }

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {

                        Bitmap bitmap = null;

                        MediaMetadataRetriever retriever =
                                new MediaMetadataRetriever();

                        try {

                            retriever.setDataSource(
                                    path
                            );

                            byte[] artwork =
                                    retriever
                                            .getEmbeddedPicture();

                            if (artwork != null &&
                                    artwork.length > 0) {

                                bitmap =
                                        BitmapFactory
                                                .decodeByteArray(
                                                        artwork,
                                                        0,
                                                        artwork.length
                                                );
                            }

                        } catch (Exception ignored) {

                            bitmap = null;

                        } finally {

                            try {
                                retriever.release();
                            } catch (Exception ignored) {
                            }
                        }

                        final Bitmap result =
                                bitmap;

                        imageView.post(
                                new Runnable() {
                                    @Override
                                    public void run() {

                                        if (result != null) {

                                            imageView
                                                    .setImageBitmap(
                                                            result
                                                    );
                                        }
                                    }
                                }
                        );
                    }
                }
        ).start();
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        ImageView albumArt;
        ImageView more;

        ImageButton favorite;

        TextView title;
        TextView artist;
        TextView album;

        public ViewHolder(
                @NonNull View itemView) {

            super(itemView);

            albumArt =
                    itemView.findViewById(
                            R.id.imgAlbumArt
                    );

            title =
                    itemView.findViewById(
                            R.id.txtTitle
                    );

            artist =
                    itemView.findViewById(
                            R.id.txtArtist
                    );

            album =
                    itemView.findViewById(
                            R.id.txtAlbum
                    );

            more =
                    itemView.findViewById(
                            R.id.btnMore
                    );

            favorite =
                    itemView.findViewById(
                            R.id.btnFave
                    );
        }
    }
}