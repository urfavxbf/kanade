package com.urfavxbf.kanade;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
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
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class PlaylistActivity extends AppCompatActivity {

    private PlaylistManager playlistManager;

    private RecyclerView recyclerView;

    private LinearLayout emptyState;

    private ImageButton btnBack;
    private ImageButton btnAddPlaylist;

    private PlaylistAdapter adapter;

    private final ArrayList<String> playlists =
            new ArrayList<>();

    private int accentColor =
            Color.rgb(
                    201,
                    196,
                    255
            );

    private int backgroundColor =
            Color.rgb(
                    16,
                    17,
                    26
            );

    @Override
    protected void onCreate(
            Bundle savedInstanceState) {

        super.onCreate(
                savedInstanceState
        );

        setContentView(
                R.layout.activity_playlist
        );

        playlistManager =
                new PlaylistManager(
                        this
                );

        recyclerView =
                findViewById(
                        R.id.rvPlaylists
                );

        emptyState =
                findViewById(
                        R.id.playlistEmptyState
                );

        btnBack =
                findViewById(
                        R.id.btnPlaylistBack
                );

        btnAddPlaylist =
                findViewById(
                        R.id.btnAddPlaylist
                );

        recyclerView.setLayoutManager(
                new LinearLayoutManager(
                        this
                )
        );

        adapter =
                new PlaylistAdapter(
                        this,
                        playlists,
                        new PlaylistAdapter.Listener() {

                            @Override
                            public void onPlaylistClick(
                                    String playlistName) {

                                showPlaylistInfo(
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
                        },
                        playlistManager
                );

        recyclerView.setAdapter(
                adapter
        );

        btnBack.setOnClickListener(
                v -> finish()
        );

        btnAddPlaylist.setOnClickListener(
                v -> showCreatePlaylistDialog()
        );

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

        playlists.addAll(
                playlistManager.getPlaylists()
        );

        adapter.notifyDataSetChanged();

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

    private void showCreatePlaylistDialog() {

        EditText input =
                new EditText(
                        this
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

        AlertDialog dialog =
                new AlertDialog.Builder(
                        this
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
                d -> {

                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(
                            v -> {

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
                                        this,
                                        "Playlist created",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                    );
                }
        );

        dialog.show();
    }

    private void showPlaylistInfo(
            String playlistName) {

        int count =
                playlistManager
                        .getPlaylistSongCount(
                                playlistName
                        );

        Toast.makeText(
                this,
                playlistName
                        + " • "
                        + count
                        + (count == 1
                                ? " song"
                                : " songs"),
                Toast.LENGTH_SHORT
        ).show();
    }

    private void showPlaylistMenu(
            String playlistName,
            View anchor) {

        android.widget.PopupMenu popupMenu =
                new android.widget.PopupMenu(
                        this,
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

                    if ("Rename".equals(
                            action
                    )) {

                        showRenamePlaylistDialog(
                                playlistName
                        );

                    } else if ("Delete".equals(
                            action
                    )) {

                        confirmDeletePlaylist(
                                playlistName
                        );

                    } else if (
                            "Clear Favorites"
                                    .equals(action)
                    ) {

                        confirmClearFavorites();
                    }

                    return true;
                }
        );

        popupMenu.show();
    }

    private void showRenamePlaylistDialog(
            String playlistName) {

        EditText input =
                new EditText(
                        this
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

        AlertDialog dialog =
                new AlertDialog.Builder(
                        this
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
                d -> {

                    dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                    ).setOnClickListener(
                            v -> {

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

                                dialog.dismiss();

                                loadPlaylists();
                            }
                    );
                }
        );

        dialog.show();
    }

    private void confirmDeletePlaylist(
            String playlistName) {

        new AlertDialog.Builder(
                this
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
                (dialog, which) -> {

                    playlistManager
                            .deletePlaylist(
                                    playlistName
                            );

                    loadPlaylists();

                    Toast.makeText(
                            this,
                            "Playlist deleted",
                            Toast.LENGTH_SHORT
                    ).show();
                }
        )
        .show();
    }

    private void confirmClearFavorites() {

        new AlertDialog.Builder(
                this
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
                (dialog, which) -> {

                    playlistManager
                            .clearFavorites();

                    loadPlaylists();

                    Toast.makeText(
                            this,
                            "Favorites cleared",
                            Toast.LENGTH_SHORT
                    ).show();
                }
        )
        .show();
    }

    private int dp(
            int value) {

        return Math.round(
                value
                        * getResources()
                                .getDisplayMetrics()
                                .density
        );
    }

    /*
     * ---------------------------------------------------------
     * PLAYLIST ADAPTER
     * ---------------------------------------------------------
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

        private final Listener listener;

        private final PlaylistManager playlistManager;

        PlaylistAdapter(
                Context context,
                ArrayList<String> playlists,
                Listener listener,
                PlaylistManager playlistManager) {

            this.context = context;
            this.playlists = playlists;
            this.listener = listener;
            this.playlistManager =
                    playlistManager;
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

            holder.itemView.setOnClickListener(
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

        @Override
        public int getItemCount() {

            return playlists.size();
        }

        static class ViewHolder
                extends RecyclerView.ViewHolder {

            ImageView icon;

            TextView name;
            TextView count;

            ImageButton more;

            ViewHolder(
                    @NonNull View itemView) {

                super(itemView);

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

                more =
                        itemView.findViewById(
                                R.id.btn_more
                        );
            }
        }
    }
}