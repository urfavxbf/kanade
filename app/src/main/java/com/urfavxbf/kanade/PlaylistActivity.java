package com.urfavxbf.kanade;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlaylistActivity extends AppCompatActivity {

    private PlaylistManager playlistManager;
    private MusicRepository musicRepository;
    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private ImageButton btnBack;
    private ImageButton btnAddPlaylist;
    private AppCompatButton btnEmptyCreatePlaylist;
    private TextView txtPlaylistSubtitle;
    private PlaylistAdapter adapter;

    private final ArrayList<String> playlists = new ArrayList<>();
    private int accentColor = Color.rgb(201, 196, 255);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist);

        playlistManager = new PlaylistManager(getApplicationContext());
        musicRepository = new MusicRepository(getApplicationContext());

        recyclerView = findViewById(R.id.rvPlaylists);
        emptyState = findViewById(R.id.playlistEmptyState);
        btnBack = findViewById(R.id.btnPlaylistBack);
        btnAddPlaylist = findViewById(R.id.btnAddPlaylist);
        btnEmptyCreatePlaylist = findViewById(R.id.btnEmptyCreatePlaylist);
        txtPlaylistSubtitle = findViewById(R.id.txtPlaylistSubtitle);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(false);

        adapter = new PlaylistAdapter(
                this,
                playlists,
                playlistManager,
                musicRepository,
                new PlaylistAdapter.Listener() {
                    @Override
                    public void onPlaylistClick(String playlistName) {
                        adapter.toggleExpanded(playlistName);
                    }

                    @Override
                    public void onMoreClick(String playlistName, View anchor) {
                        showPlaylistMenu(playlistName, anchor);
                    }
                }
        );

        recyclerView.setAdapter(adapter);
        btnBack.setOnClickListener(v -> finish());
        btnAddPlaylist.setOnClickListener(v -> showCreatePlaylistDialog());
        btnEmptyCreatePlaylist.setOnClickListener(v -> showCreatePlaylistDialog());

        loadPlaylists();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playlistManager != null) {
            loadPlaylists();
        }
    }

    private void loadPlaylists() {
        playlists.clear();
        playlists.addAll(playlistManager.getPlaylists());
        adapter.notifyDataSetChanged();

        int playlistCount = playlists.size();
        txtPlaylistSubtitle.setText(
                playlistCount + (playlistCount == 1 ? " playlist" : " playlists")
                        + " • tap to browse songs"
        );

        boolean empty = playlists.isEmpty();
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    private void showCreatePlaylistDialog() {
        EditText input = new EditText(this);
        input.setHint("Playlist name");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        int padding = dp(16);
        input.setPadding(padding, padding, padding, padding);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Create playlist")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Create", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                input.setError("Enter a playlist name");
                return;
            }
            if (!playlistManager.createPlaylist(name)) {
                input.setError("Playlist already exists");
                return;
            }
            dialog.dismiss();
            loadPlaylists();
            Toast.makeText(this, "Playlist created", Toast.LENGTH_SHORT).show();
        }));

        dialog.show();
    }

    private void showPlaylistMenu(String playlistName, View anchor) {
        android.widget.PopupMenu popupMenu = new android.widget.PopupMenu(this, anchor);

        if (!PlaylistManager.FAVORITES_PLAYLIST.equalsIgnoreCase(playlistName)) {
            popupMenu.getMenu().add("Rename");
            popupMenu.getMenu().add("Delete");
        } else {
            popupMenu.getMenu().add("Clear Favorites");
        }

        popupMenu.setOnMenuItemClickListener(item -> {
            String action = item.getTitle().toString();
            if ("Rename".equals(action)) {
                showRenamePlaylistDialog(playlistName);
            } else if ("Delete".equals(action)) {
                confirmDeletePlaylist(playlistName);
            } else if ("Clear Favorites".equals(action)) {
                confirmClearFavorites();
            }
            return true;
        });
        popupMenu.show();
    }

    private void showRenamePlaylistDialog(String playlistName) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(playlistName);
        input.setSelection(input.length());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Rename playlist")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String newName = input.getText().toString().trim();
            if (newName.isEmpty()) {
                input.setError("Enter a name");
                return;
            }
            if (!playlistManager.renamePlaylist(playlistName, newName)) {
                input.setError("Name already exists");
                return;
            }
            dialog.dismiss();
            loadPlaylists();
        }));
        dialog.show();
    }

    private void confirmDeletePlaylist(String playlistName) {
        new AlertDialog.Builder(this)
                .setTitle("Delete playlist")
                .setMessage("Delete \"" + playlistName + "\"? Songs on your device will not be deleted.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    playlistManager.deletePlaylist(playlistName);
                    loadPlaylists();
                    Toast.makeText(this, "Playlist deleted", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void confirmClearFavorites() {
        new AlertDialog.Builder(this)
                .setTitle("Clear favorites")
                .setMessage("Remove all songs from Favorites?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Clear", (dialog, which) -> {
                    playlistManager.clearFavorites();
                    loadPlaylists();
                    Toast.makeText(this, "Favorites cleared", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static final class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {

        interface Listener {
            void onPlaylistClick(String playlistName);
            void onMoreClick(String playlistName, View anchor);
        }

        private final Context context;
        private final ArrayList<String> playlists;
        private final PlaylistManager playlistManager;
        private final MusicRepository musicRepository;
        private final Listener listener;
        private final Set<String> expandedPlaylists = new HashSet<>();
        private final Map<String, ArrayList<AudioFile>> songCache = new HashMap<>();

        PlaylistAdapter(
                Context context,
                ArrayList<String> playlists,
                PlaylistManager playlistManager,
                MusicRepository musicRepository,
                Listener listener) {
            this.context = context;
            this.playlists = playlists;
            this.playlistManager = playlistManager;
            this.musicRepository = musicRepository;
            this.listener = listener;
        }

        void toggleExpanded(String playlistName) {
            if (!expandedPlaylists.add(playlistName)) {
                expandedPlaylists.remove(playlistName);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.playlist_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String name = playlists.get(position);
            int count = playlistManager.getPlaylistSongCount(name);
            boolean expanded = expandedPlaylists.contains(name);

            holder.name.setText(name);
            holder.count.setText(count + (count == 1 ? " song" : " songs"));
            holder.icon.setImageResource(
                    PlaylistManager.FAVORITES_PLAYLIST.equalsIgnoreCase(name)
                            ? R.drawable.ic_favorite
                            : R.drawable.ic_playlist
            );
            holder.icon.setColorFilter(accentColor(context));
            holder.songsContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
            holder.itemView.setContentDescription(name + ", " + count + " songs");

            holder.itemView.setOnClickListener(v -> listener.onPlaylistClick(name));
            holder.more.setOnClickListener(v -> listener.onMoreClick(name, v));

            if (expanded) {
                bindSongs(holder.songsContainer, name);
            } else {
                holder.songsContainer.removeAllViews();
            }
        }

        private void bindSongs(RecyclerView container, String playlistName) {
            ArrayList<AudioFile> songs = getSongs(playlistName);
            SongAdapter songAdapter = new SongAdapter(context, songs);
            container.setLayoutManager(new LinearLayoutManager(context));
            container.setAdapter(songAdapter);
            container.setNestedScrollingEnabled(false);
        }

        private ArrayList<AudioFile> getSongs(String playlistName) {
            ArrayList<AudioFile> cached = songCache.get(playlistName);
            if (cached != null) {
                return cached;
            }

            ArrayList<String> uris = playlistManager.getPlaylistSongs(playlistName);
            ArrayList<AudioFile> allSongs = musicRepository.getAllSongs();
            Map<String, AudioFile> byUri = new HashMap<>();
            for (AudioFile song : allSongs) {
                if (song != null && song.getUri() != null) {
                    byUri.put(song.getUri(), song);
                }
            }

            ArrayList<AudioFile> result = new ArrayList<>();
            for (String uri : uris) {
                AudioFile song = byUri.get(uri);
                if (song != null) {
                    result.add(song);
                }
            }
            songCache.put(playlistName, result);
            return result;
        }

        @Override
        public int getItemCount() {
            return playlists.size();
        }

        private static int accentColor(Context context) {
            return new AlbumColorManager(context.getApplicationContext()).getCurrentAccentColor();
        }

        static final class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView icon;
            final TextView name;
            final TextView count;
            final ImageButton more;
            final RecyclerView songsContainer;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                icon = itemView.findViewById(R.id.imgPlaylistIcon);
                name = itemView.findViewById(R.id.txtPlaylistName);
                count = itemView.findViewById(R.id.txtPlaylistCount);
                more = itemView.findViewById(R.id.btn_more);
                songsContainer = itemView.findViewById(R.id.playlistSongsContainer);
            }
        }
    }

    private static final class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {
        private final Context context;
        private final ArrayList<AudioFile> songs;

        SongAdapter(Context context, ArrayList<AudioFile> songs) {
            this.context = context;
            this.songs = songs;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setPadding(dp(parent, 8), dp(parent, 5), dp(parent, 8), dp(parent, 5));

            TextView number = new TextView(parent.getContext());
            number.setTextColor(Color.rgb(130, 132, 145));
            number.setTextSize(12);
            row.addView(number, new LinearLayout.LayoutParams(dp(parent, 30), dp(parent, 48)));

            LinearLayout text = new LinearLayout(parent.getContext());
            text.setOrientation(LinearLayout.VERTICAL);
            TextView title = new TextView(parent.getContext());
            title.setTextColor(Color.WHITE);
            title.setTextSize(14);
            title.setMaxLines(1);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            TextView artist = new TextView(parent.getContext());
            artist.setTextColor(Color.rgb(145, 147, 160));
            artist.setTextSize(11);
            artist.setMaxLines(1);
            artist.setEllipsize(android.text.TextUtils.TruncateAt.END);
            text.addView(title);
            text.addView(artist);
            row.addView(text, new LinearLayout.LayoutParams(0, dp(parent, 48), 1f));

            return new ViewHolder(row, number, title, artist);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            AudioFile song = songs.get(position);
            String title = song.getTitle() == null || song.getTitle().trim().isEmpty()
                    ? "Unknown song" : song.getTitle().trim();
            String artist = song.getArtist() == null || song.getArtist().trim().isEmpty()
                    ? "Unknown artist" : song.getArtist().trim();
            holder.number.setText(String.valueOf(position + 1));
            holder.title.setText(title);
            holder.artist.setText(artist);
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, MusicPlayerService.class);
                intent.setAction(MusicPlayerService.ACTION_PLAY);
                intent.putExtra(MusicPlayerService.EXTRA_SONG_URI, song.getUri());
                context.startService(intent);
            });
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }

        private static int dp(View view, int value) {
            return Math.round(value * view.getResources().getDisplayMetrics().density);
        }

        static final class ViewHolder extends RecyclerView.ViewHolder {
            final TextView number;
            final TextView title;
            final TextView artist;

            ViewHolder(View itemView, TextView number, TextView title, TextView artist) {
                super(itemView);
                this.number = number;
                this.title = title;
                this.artist = artist;
            }
        }
    }
}
