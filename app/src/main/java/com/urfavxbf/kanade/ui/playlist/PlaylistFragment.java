package com.urfavxbf.kanade.ui.playlist;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.urfavxbf.kanade.AudioFile;
import com.urfavxbf.kanade.MusicPlayerController;
import com.urfavxbf.kanade.MusicRepository;
import com.urfavxbf.kanade.PlaylistManager;
import com.urfavxbf.kanade.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class PlaylistFragment extends Fragment {

    private PlaylistManager playlistManager;
    private MusicRepository musicRepository;
    private MusicPlayerController musicPlayerController;

    private RecyclerView recyclerView;
    private LinearLayout emptyState;

    private ImageButton btnBack;
    private ImageButton btnAddPlaylist;

    private PlaylistAdapter adapter;

    private final ArrayList<String> playlists =
            new ArrayList<>();

    private final Set<String> expandedPlaylists =
            new HashSet<>();

    private final ArrayList<AudioFile> allSongs =
            new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view =
                inflater.inflate(
                        R.layout.activity_playlist,
                        container,
                        false
                );

        playlistManager =
                new PlaylistManager(
                        requireContext()
                );

        musicRepository =
                new MusicRepository(
                        requireContext()
                );

        musicPlayerController =
                new MusicPlayerController(
                        requireContext()
                );

        recyclerView =
                view.findViewById(
                        R.id.rvPlaylists
                );

        emptyState =
                view.findViewById(
                        R.id.playlistEmptyState
                );

        btnBack =
                view.findViewById(
                        R.id.btnPlaylistBack
                );

        btnAddPlaylist =
                view.findViewById(
                        R.id.btnAddPlaylist
                );

        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        requireContext()
                )
        );

        adapter =
                new PlaylistAdapter(
                        requireContext(),
                        playlists,
                        expandedPlaylists,
                        allSongs,
                        playlistManager,
                        musicPlayerController,
                        new PlaylistAdapter.Listener() {

                            @Override
                            public void onPlaylistClick(
                                    String playlistName) {

                                togglePlaylist(
                                        playlistName
                                );
                            }

                            @Override
                            public void onMoreClick(
                                    String playlistName,
                                    View anchor) {

                                showPlaylistMenu(
                                        playlistName,
                                        anchor
                                );
                            }
                        }
                );

        recyclerView.setAdapter(
                adapter
        );

        btnBack.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        requireActivity()
                                .getOnBackPressedDispatcher()
                                .onBackPressed();
                    }
                }
        );

        btnAddPlaylist.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        showCreatePlaylistDialog();
                    }
                }
        );

        loadSongs();
        loadPlaylists();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (playlistManager != null &&
                adapter != null) {

            loadSongs();
            loadPlaylists();
        }
    }

    private void loadSongs() {

        if (musicRepository == null) {
            return;
        }

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {

                        final ArrayList<AudioFile> result =
                                musicRepository.getAllSongs();

                        if (!isAdded()) {
                            return;
                        }

                        requireActivity()
                                .runOnUiThread(
                                        new Runnable() {
                                            @Override
                                            public void run() {

                                                allSongs.clear();

                                                if (result != null) {
                                                    allSongs.addAll(
                                                            result
                                                    );
                                                }

                                                if (adapter != null) {
                                                    adapter.notifyDataSetChanged();
                                                }
                                            }
                                        }
                                );
                    }
                }
        ).start();
    }

    private void loadPlaylists() {

        if (playlistManager == null ||
                adapter == null ||
                recyclerView == null ||
                emptyState == null) {

            return;
        }

        playlists.clear();

        ArrayList<String> result =
                playlistManager.getPlaylists();

        if (result != null) {
            playlists.addAll(result);
        }

        expandedPlaylists.retainAll(
                playlists
        );

        adapter.notifyDataSetChanged();

        updateEmptyState();
    }

    private void updateEmptyState() {

        if (recyclerView == null ||
                emptyState == null) {
            return;
        }

        if (playlists.isEmpty()) {

            recyclerView.setVisibility(
                    View.GONE
            );

            emptyState.setVisibility(
                    View.VISIBLE
            );

        } else {

            recyclerView.setVisibility(
                    View.VISIBLE
            );

            emptyState.setVisibility(
                    View.GONE
            );
        }
    }

    private void togglePlaylist(
            String playlistName) {

        if (playlistName == null) {
            return;
        }

        if (expandedPlaylists.contains(
                playlistName
        )) {

            expandedPlaylists.remove(
                    playlistName
            );

        } else {

            expandedPlaylists.add(
                    playlistName
            );
        }

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showCreatePlaylistDialog() {

        if (!isAdded()) {
            return;
        }

        final EditText input =
                new EditText(
                        requireContext()
                );

        input.setHint(
                "Playlist name"
        );

        input.setSingleLine(
                true
        );

        input.setInputType(
                InputType.TYPE_CLASS_TEXT
        );

        int padding =
                dp(20);

        input.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        final AlertDialog dialog =
                new AlertDialog.Builder(
                        requireContext()
                )
                .setTitle(
                        "Create Playlist"
                )
                .setView(
                        input
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Create",
                        null
                )
                .create();

        dialog.setOnShowListener(
                new android.content.DialogInterface.OnShowListener() {

                    @Override
                    public void onShow(
                            android.content.DialogInterface dialogInterface) {

                        dialog.getButton(
                                AlertDialog.BUTTON_POSITIVE
                        ).setOnClickListener(
                                new View.OnClickListener() {

                                    @Override
                                    public void onClick(
                                            View v) {

                                        String name =
                                                input.getText()
                                                        .toString()
                                                        .trim();

                                        if (name.isEmpty()) {

                                            input.setError(
                                                    "Enter a playlist name"
                                            );

                                            return;
                                        }

                                        boolean created =
                                                playlistManager
                                                        .createPlaylist(
                                                                name
                                                        );

                                        if (!created) {

                                            input.setError(
                                                    "Playlist already exists"
                                            );

                                            return;
                                        }

                                        dialog.dismiss();

                                        loadPlaylists();

                                        Toast.makeText(
                                                requireContext(),
                                                "Playlist created",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                }
                        );
                    }
                }
        );

        dialog.show();
    }

    private void showPlaylistMenu(
            final String playlistName,
            View anchor) {

        if (!isAdded()) {
            return;
        }

        android.widget.PopupMenu popupMenu =
                new android.widget.PopupMenu(
                        requireContext(),
                        anchor
                );

        if (!PlaylistManager.FAVORITES_PLAYLIST
                .equalsIgnoreCase(
                        playlistName
                )) {

            popupMenu.getMenu().add(
                    "Rename"
            );

            popupMenu.getMenu().add(
                    "Delete"
            );

        } else {

            popupMenu.getMenu().add(
                    "Clear Favorites"
            );
        }

        popupMenu.setOnMenuItemClickListener(
                item -> {

                    String action =
                            item.getTitle()
                                    .toString();

                    if ("Rename".equals(action)) {

                        showRenamePlaylistDialog(
                                playlistName
                        );

                    } else if ("Delete".equals(action)) {

                        confirmDeletePlaylist(
                                playlistName
                        );

                    } else if (
                            "Clear Favorites".equals(action)
                    ) {

                        confirmClearFavorites();
                    }

                    return true;
                }
        );

        popupMenu.show();
    }

    private void showRenamePlaylistDialog(
            final String playlistName) {

        if (!isAdded()) {
            return;
        }

        final EditText input =
                new EditText(
                        requireContext()
                );

        input.setSingleLine(
                true
        );

        input.setText(
                playlistName
        );

        input.setSelection(
                input.length()
        );

        final AlertDialog dialog =
                new AlertDialog.Builder(
                        requireContext()
                )
                .setTitle(
                        "Rename Playlist"
                )
                .setView(
                        input
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .setPositiveButton(
                        "Save",
                        null
                )
                .create();

        dialog.setOnShowListener(
                new android.content.DialogInterface.OnShowListener() {

                    @Override
                    public void onShow(
                            android.content.DialogInterface dialogInterface) {

                        dialog.getButton(
                                AlertDialog.BUTTON_POSITIVE
                        ).setOnClickListener(
                                new View.OnClickListener() {

                                    @Override
                                    public void onClick(
                                            View v) {

                                        String newName =
                                                input.getText()
                                                        .toString()
                                                        .trim();

                                        if (newName.isEmpty()) {

                                            input.setError(
                                                    "Enter a name"
                                            );

                                            return;
                                        }

                                        boolean renamed =
                                                playlistManager
                                                        .renamePlaylist(
                                                                playlistName,
                                                                newName
                                                        );

                                        if (!renamed) {

                                            input.setError(
                                                    "Name already exists"
                                            );

                                            return;
                                        }

                                        if (expandedPlaylists
                                                .remove(
                                                        playlistName
                                                )) {

                                            expandedPlaylists.add(
                                                    newName
                                            );
                                        }

                                        dialog.dismiss();

                                        loadPlaylists();

                                        Toast.makeText(
                                                requireContext(),
                                                "Playlist renamed",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    }
                                }
                        );
                    }
                }
        );

        dialog.show();
    }

    private void confirmDeletePlaylist(
            final String playlistName) {

        if (!isAdded()) {
            return;
        }

        new AlertDialog.Builder(
                requireContext()
        )
        .setTitle(
                "Delete Playlist"
        )
        .setMessage(
                "Delete \""
                        + playlistName
                        + "\"?"
        )
        .setNegativeButton(
                "Cancel",
                null
        )
        .setPositiveButton(
                "Delete",
                new android.content.DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(
                            android.content.DialogInterface dialog,
                            int which) {

                        boolean deleted =
                                playlistManager
                                        .deletePlaylist(
                                                playlistName
                                        );

                        if (deleted) {

                            expandedPlaylists.remove(
                                    playlistName
                            );

                            loadPlaylists();

                            Toast.makeText(
                                    requireContext(),
                                    "Playlist deleted",
                                    Toast.LENGTH_SHORT
                            ).show();

                        } else {

                            Toast.makeText(
                                    requireContext(),
                                    "Unable to delete playlist",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                }
        )
        .show();
    }

    private void confirmClearFavorites() {

        if (!isAdded()) {
            return;
        }

        new AlertDialog.Builder(
                requireContext()
        )
        .setTitle(
                "Clear Favorites"
        )
        .setMessage(
                "Remove all songs from Favorites?"
        )
        .setNegativeButton(
                "Cancel",
                null
        )
        .setPositiveButton(
                "Clear",
                new android.content.DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(
                            android.content.DialogInterface dialog,
                            int which) {

                        playlistManager
                                .clearFavorites();

                        loadPlaylists();

                        Toast.makeText(
                                requireContext(),
                                "Favorites cleared",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        )
        .show();
    }

    private int dp(int value) {

        return Math.round(
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    @Override
    public void onDestroyView() {

        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }

        adapter = null;
        recyclerView = null;
        emptyState = null;
        btnBack = null;
        btnAddPlaylist = null;

        super.onDestroyView();
    }

    /*
     * =========================================================
     * PLAYLIST ADAPTER
     * =========================================================
     */

    private static class PlaylistAdapter
            extends RecyclerView.Adapter<
                    PlaylistAdapter.ViewHolder> {

        interface Listener {

            void onPlaylistClick(
                    String playlistName
            );

            void onMoreClick(
                    String playlistName,
                    View anchor
            );
        }

        private final Context context;
        private final ArrayList<String> playlists;
        private final Set<String> expandedPlaylists;
        private final ArrayList<AudioFile> allSongs;
        private final PlaylistManager playlistManager;
        private final MusicPlayerController musicPlayerController;
        private final Listener listener;

        PlaylistAdapter(
                Context context,
                ArrayList<String> playlists,
                Set<String> expandedPlaylists,
                ArrayList<AudioFile> allSongs,
                PlaylistManager playlistManager,
                MusicPlayerController musicPlayerController,
                Listener listener) {

            this.context = context;
            this.playlists = playlists;
            this.expandedPlaylists = expandedPlaylists;
            this.allSongs = allSongs;
            this.playlistManager = playlistManager;
            this.musicPlayerController =
                    musicPlayerController;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType) {

            View view =
                    LayoutInflater.from(
                            parent.getContext()
                    ).inflate(
                            R.layout.playlist_item,
                            parent,
                            false
                    );

            return new ViewHolder(
                    view
            );
        }

        @Override
        public void onBindViewHolder(
                @NonNull ViewHolder holder,
                int position) {

            String name =
                    playlists.get(
                            position
                    );

            int count =
                    playlistManager
                            .getPlaylistSongCount(
                                    name
                            );

            holder.name.setText(
                    name
            );

            holder.count.setText(
                    count
                            + (count == 1
                                    ? " song"
                                    : " songs")
            );

            boolean expanded =
                    expandedPlaylists.contains(
                            name
                    );

            holder.songsContainer.setVisibility(
                    expanded
                            ? View.VISIBLE
                            : View.GONE
            );

            holder.more.animate()
                    .rotation(
                            expanded
                                    ? 90f
                                    : 0f
                    )
                    .setDuration(180)
                    .start();

            ArrayList<AudioFile> playlistSongs =
                    new ArrayList<>();

            ArrayList<String> songUris =
                    playlistManager
                            .getPlaylistSongs(
                                    name
                            );

            if (songUris != null) {

                for (String uri : songUris) {

                    AudioFile song =
                            findSongByUri(
                                    uri
                            );

                    if (song != null) {

                        playlistSongs.add(
                                song
                        );
                    }
                }
            }

            holder.songAdapter.setSongs(
                    playlistSongs
            );

            holder.header.setOnClickListener(
                    v -> {

                        if (listener != null) {

                            listener.onPlaylistClick(
                                    name
                            );
                        }
                    }
            );

            holder.more.setOnClickListener(
                    v -> {

                        if (listener != null) {

                            listener.onMoreClick(
                                    name,
                                    v
                            );
                        }
                    }
            );
        }

        private AudioFile findSongByUri(
                String uri) {

            if (uri == null) {
                return null;
            }

            for (AudioFile song : allSongs) {

                if (song != null &&
                        uri.equals(
                                song.getUri()
                        )) {

                    return song;
                }
            }

            return null;
        }

        @Override
        public int getItemCount() {

            return playlists.size();
        }

        static class ViewHolder
                extends RecyclerView.ViewHolder {

            View header;

            ImageView icon;

            TextView name;

            TextView count;

            ImageButton more;

            RecyclerView songsContainer;

            SongAdapter songAdapter;

            ViewHolder(
                    @NonNull View itemView) {

                super(itemView);

                header =
                        itemView.findViewById(
                                R.id.playlistHeader
                        );

                icon =
                        itemView.findViewById(
                                R.id.imgPlaylistIcon
                        );

                name =
                        itemView.findViewById(
                                R.id.txtPlaylistName
                        );

                count =
                        itemView.findViewById(
                                R.id.txtPlaylistCount
                        );

                /*
                 * IMPORTANT:
                 * This matches your new playlist_item.xml:
                 *
                 * @+id/btn_more
                 */
                more =
                        itemView.findViewById(
                                R.id.btn_more
                        );

                songsContainer =
                        itemView.findViewById(
                                R.id.playlistSongsContainer
                        );

                songsContainer.setLayoutManager(
                        new LinearLayoutManager(
                                itemView.getContext()
                        )
                );

                songsContainer.setNestedScrollingEnabled(
                        false
                );

                songsContainer.setHasFixedSize(
                        false
                );

                songAdapter =
                        new SongAdapter(
                                itemView.getContext()
                        );

                songsContainer.setAdapter(
                        songAdapter
                );
            }
        }
    }

    /*
     * =========================================================
     * SONG ADAPTER
     * =========================================================
     */

    private static class SongAdapter
            extends RecyclerView.Adapter<
                    SongAdapter.SongViewHolder> {

        private final Context context;

        private final ArrayList<AudioFile> songs =
                new ArrayList<>();

        SongAdapter(
                Context context) {

            this.context = context;
        }

        void setSongs(
                ArrayList<AudioFile> newSongs) {

            songs.clear();

            if (newSongs != null) {

                songs.addAll(
                        newSongs
                );
            }

            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SongViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType) {

            View view =
                    LayoutInflater.from(
                            parent.getContext()
                    ).inflate(
                            R.layout.song_item,
                            parent,
                            false
                    );

            return new SongViewHolder(
                    view
            );
        }

        @Override
        public void onBindViewHolder(
                @NonNull SongViewHolder holder,
                int position) {

            final AudioFile song =
                    songs.get(
                            position
                    );

            holder.title.setText(
                    safeText(
                            song.getTitle(),
                            "Unknown title"
                    )
            );

            holder.artist.setText(
                    safeText(
                            song.getArtist(),
                            "Unknown artist"
                    )
            );

            /*
             * Reset recycled ImageView first.
             */
            holder.albumArt.setImageResource(
                    android.R.drawable.ic_media_play
            );

            loadAlbumArt(
                    song,
                    holder.albumArt
            );

            holder.itemView.setOnClickListener(
                    new View.OnClickListener() {

                        @Override
                        public void onClick(
                                View v) {

                            String uri =
                                    song.getUri();

                            if (uri == null ||
                                    uri.trim().isEmpty()) {

                                return;
                            }

                            MusicPlayerController controller =
                                    new MusicPlayerController(
                                            context
                                    );

                            controller.play(
                                    uri
                            );

                            Toast.makeText(
                                    context,
                                    "Playing "
                                            + safeText(
                                                    song.getTitle(),
                                                    "song"
                                            ),
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );
        }

        private void loadAlbumArt(
                final AudioFile song,
                final ImageView imageView) {

            if (song == null ||
                    imageView == null) {

                return;
            }

            final String path =
                    song.getPath();

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

                            } finally {

                                try {

                                    retriever.release();

                                } catch (Exception ignored) {
                                }
                            }

                            final Bitmap finalBitmap =
                                    bitmap;

                            imageView.post(
                                    new Runnable() {

                                        @Override
                                        public void run() {

                                            if (finalBitmap != null &&
                                                    imageView.getWindowToken()
                                                            != null) {

                                                imageView
                                                        .setImageBitmap(
                                                                finalBitmap
                                                        );
                                            }
                                        }
                                    }
                            );
                        }
                    }
            ).start();
        }

        private String safeText(
                String value,
                String fallback) {

            if (value == null ||
                    value.trim().isEmpty()) {

                return fallback;
            }

            return value;
        }

        @Override
        public int getItemCount() {

            return songs.size();
        }

        static class SongViewHolder
                extends RecyclerView.ViewHolder {

            ImageView albumArt;

            TextView title;

            TextView artist;

            SongViewHolder(
                    @NonNull View itemView) {

                super(itemView);

                albumArt =
                        itemView.findViewById(
                                R.id.imgPlaylistSongArt
                        );

                title =
                        itemView.findViewById(
                                R.id.txtPlaylistSongTitle
                        );

                artist =
                        itemView.findViewById(
                                R.id.txtPlaylistSongArtist
                        );
            }
        }
    }
}