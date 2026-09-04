package com.urfavxbf.kanade.ui.dashboard;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.urfavxbf.kanade.AlbumArtManager;
import com.urfavxbf.kanade.AudioFile;
import com.urfavxbf.kanade.MusicPlayerService;
import com.urfavxbf.kanade.PlaybackStatsManager;
import com.urfavxbf.kanade.R;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardSongAdapter extends RecyclerView.Adapter<DashboardSongAdapter.ViewHolder> {

    private final Context context;
    private final ArrayList<AudioFile> songs = new ArrayList<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private int accentColor;

    public DashboardSongAdapter(Context context, int accentColor) {
        this.context = context.getApplicationContext();
        this.accentColor = accentColor;
    }

    public void submitSongs(ArrayList<AudioFile> values, int accentColor) {
        songs.clear();
        if (values != null) songs.addAll(values);
        this.accentColor = accentColor;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dashboard_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AudioFile song = songs.get(position);
        holder.title.setText(song.getTitle() == null || song.getTitle().trim().isEmpty() ? "Unknown title" : song.getTitle());
        holder.artist.setText(song.getArtist() == null || song.getArtist().trim().isEmpty() ? "Unknown artist" : song.getArtist());
        holder.art.setImageResource(R.drawable.ic_music_note);
        holder.art.setTag(song.getUri());

        PlaybackStatsManager stats = new PlaybackStatsManager(context);
        PlaybackStatsManager.SongStat stat = stats.getSongStat(song.getUri());
        holder.itemView.setContentDescription(song.getTitle() + ", " + song.getArtist());

        Bitmap cached = AlbumArtManager.getInstance(context).loadCachedBitmap(song);
        if (cached != null) {
            holder.art.setImageBitmap(cached);
        } else {
            executor.execute(() -> {
                Bitmap bitmap = loadEmbeddedArt(song);
                holder.art.post(() -> {
                    if (song.getUri() != null && song.getUri().equals(holder.art.getTag())) {
                        if (bitmap != null) holder.art.setImageBitmap(bitmap);
                    }
                });
            });
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MusicPlayerService.class);
            intent.setAction(MusicPlayerService.ACTION_PLAY);
            intent.putExtra(MusicPlayerService.EXTRA_SONG_URI, song.getUri());
            context.startService(intent);
        });
    }

    private Bitmap loadEmbeddedArt(AudioFile song) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            if (song.getUri() == null || song.getUri().trim().isEmpty()) return null;
            retriever.setDataSource(context, Uri.parse(song.getUri()));
            byte[] data = retriever.getEmbeddedPicture();
            if (data == null || data.length == 0) return null;
            return android.graphics.BitmapFactory.decodeByteArray(data, 0, data.length);
        } catch (Exception ignored) {
            return null;
        } finally {
            try { retriever.release(); } catch (Exception ignored) { }
        }
    }

    @Override
    public int getItemCount() { return songs.size(); }

    public void shutdown() { executor.shutdownNow(); }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView art;
        final TextView title;
        final TextView artist;
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            art = itemView.findViewById(R.id.songArt);
            title = itemView.findViewById(R.id.songTitle);
            artist = itemView.findViewById(R.id.songArtist);
        }
    }
}
