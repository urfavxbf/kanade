package com.urfavxbf.kanade.ui.songs;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.urfavxbf.kanade.AlbumColorManager;
import com.urfavxbf.kanade.AudioFile;
import com.urfavxbf.kanade.AudioListAdapter;
import com.urfavxbf.kanade.PlaylistManager;
import com.urfavxbf.kanade.MusicPlayerController;
import com.urfavxbf.kanade.MusicPlayerService;
import com.urfavxbf.kanade.MusicRepository;
import com.urfavxbf.kanade.R;
import com.urfavxbf.kanade.databinding.FragmentHomeBinding;
import com.urfavxbf.kanade.ChromaprintTestHelper;
import com.urfavxbf.kanade.AcoustIdTestHelper;

import com.urfavxbf.kanade.MusicBrainzTestHelper;
import com.urfavxbf.kanade.MusicIdentifierTestHelper;

import android.app.AlertDialog;
import android.widget.EditText;

import java.io.InputStream;
import java.util.ArrayList;
import android.content.Intent;
import com.urfavxbf.kanade.PlayerActivity;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    private MusicRepository musicRepository;
    private AudioListAdapter audioListAdapter;
    private PlaylistManager playlistManager;

    private AlbumColorManager albumColorManager;

    private final ArrayList<AudioFile> songs =
            new ArrayList<>();

    private final ArrayList<AudioFile> allSongs =
            new ArrayList<>();

    private MusicPlayerController playerController;

    private String currentMiniPlayerUri;

    private int currentAccentColor =
            Color.rgb(
                    201,
                    196,
                    255
            );

    private int currentBackgroundColor =
            Color.rgb(
                    16,
                    17,
                    26
            );

    private ValueAnimator colorAnimator;

    private boolean receiversRegistered = false;

    private boolean isSearchOpen = false;

    private OnBackPressedCallback searchBackCallback;

    private final BroadcastReceiver playerReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(
                        Context context,
                        Intent intent) {

                    if (!MusicPlayerService.ACTION_STATE_CHANGED
                            .equals(intent.getAction())) {
                        return;
                    }

                    boolean isPlaying =
                            intent.getBooleanExtra(
                                    MusicPlayerService.EXTRA_IS_PLAYING,
                                    false
                            );

                    String uri =
                            intent.getStringExtra(
                                    MusicPlayerService.EXTRA_CURRENT_URI
                            );

                    updateMiniPlayer(
                            uri,
                            isPlaying
                    );
                }
            };

    private final BroadcastReceiver colorReceiver =
            new BroadcastReceiver() {
                @Override
                public void onReceive(
                        Context context,
                        Intent intent) {

                    if (!AlbumColorManager.ACTION_COLORS_CHANGED
                            .equals(intent.getAction())) {
                        return;
                    }

                    int accentColor =
                            intent.getIntExtra(
                                    AlbumColorManager.EXTRA_ACCENT_COLOR,
                                    currentAccentColor
                            );

                    int backgroundColor =
                            intent.getIntExtra(
                                    AlbumColorManager.EXTRA_BACKGROUND_COLOR,
                                    currentBackgroundColor
                            );

                    animateThemeColors(
                            accentColor,
                            backgroundColor
                    );
                }
            };

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        binding =
                FragmentHomeBinding.inflate(
                        inflater,
                        container,
                        false
                );

        View root =
                binding.getRoot();

        playerController =
                new MusicPlayerController(
                        requireContext()
                );

        musicRepository =
                new MusicRepository(
                        requireContext()
                );

        playlistManager =
                new PlaylistManager(
                        requireContext()
                );

        albumColorManager =
                AlbumColorManager.getInstance(
                        requireContext()
                                .getApplicationContext()
                );

        currentAccentColor =
                albumColorManager
                        .getCurrentAccentColor();

        currentBackgroundColor =
                albumColorManager
                        .getCurrentBackgroundColor();

        applyThemeColors(
                currentAccentColor,
                currentBackgroundColor
        );

        audioListAdapter =
                new AudioListAdapter(
                        songs,
                        new AudioListAdapter.OnSongClickListener() {

                            @Override
                            public void onSongClick(
                                    AudioFile song) {

                                showMiniPlayer(
                                        song,
                                        true
                                );

                                playerController.play(
                                        song.getUri()
                                );
                            }

                            @Override
                            public void onFavoriteClick(
                                    AudioFile song) {

                                toggleFavorite(song);
                            }

                            @Override
                            public void onMoreClick(
                                    AudioFile song,
                                    View anchor) {

                                showSongMenu(
                                        song,
                                        anchor
                                );
                            }
                        },
                        playlistManager
                );

        binding.rvHomeMusic.setLayoutManager(
                new LinearLayoutManager(
                        requireContext()
                )
        );

        binding.rvHomeMusic.setAdapter(
                audioListAdapter
        );

        binding.swipeRefresh.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {

                        loadMusic();
                    }
                }
        );

        closeSearchImmediate();

        binding.btnSearch.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (!isSearchOpen) {

                            openSearch();

                            return;
                        }

                        String query =
                                binding.searchEditText
                                        .getText()
                                        .toString();

                        if (!query.trim().isEmpty()) {

                            binding.searchEditText.setText("");

                            binding.searchEditText.requestFocus();

                            InputMethodManager imm =
                                    (InputMethodManager)
                                            requireContext()
                                                    .getSystemService(
                                                            Context.INPUT_METHOD_SERVICE
                                                    );

                            if (imm != null) {

                                imm.showSoftInput(
                                        binding.searchEditText,
                                        InputMethodManager.SHOW_IMPLICIT
                                );
                            }

                        } else {

                            closeSearch();
                        }
                    }
                }
        );

        binding.searchEditText.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count) {

                        String query =
                                s == null
                                        ? ""
                                        : s.toString();

                        filterSongs(
                                query
                        );
                    }

                    @Override
                    public void afterTextChanged(
                            android.text.Editable s) {
                    }
                }
        );

        binding.searchEditText.setOnEditorActionListener(
                (v, actionId, event) -> {

                    if (actionId ==
                            android.view.inputmethod.EditorInfo
                                    .IME_ACTION_SEARCH) {

                        hideKeyboard();

                        return true;
                    }

                    return false;
                }
        );

        searchBackCallback =
                new OnBackPressedCallback(
                        true
                ) {
                    @Override
                    public void handleOnBackPressed() {

                        if (isSearchOpen) {

                            closeSearch();

                        } else {

                            setEnabled(false);

                            requireActivity()
                                    .getOnBackPressedDispatcher()
                                    .onBackPressed();

                            setEnabled(true);
                        }
                    }
                };

        requireActivity()
                .getOnBackPressedDispatcher()
                .addCallback(
                        getViewLifecycleOwner(),
                        searchBackCallback
                );

        binding.miniPlayer.setVisibility(
                View.GONE
        );

        binding.miniPlayer.setOnClickListener(
        new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent =
                        new Intent(
                                requireContext(),
                                PlayerActivity.class
                        );

                startActivity(intent);
            }
        }
);

        binding.miniPlayPause.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        playerController.play();
                    }
                }
        );

        loadMusic();

        return root;
    }

    private void openSearch() {

        if (binding == null ||
                isSearchOpen) {
            return;
        }

        isSearchOpen = true;

        binding.homeTitle.setVisibility(
                View.GONE
        );

        binding.searchIcon.setVisibility(
                View.VISIBLE
        );

        binding.searchEditText.setVisibility(
                View.VISIBLE
        );

        binding.btnSearch.setImageResource(
                R.drawable.ic_close
        );

        binding.btnSearch.setContentDescription(
                "Clear or close search"
        );

        binding.searchIcon.setAlpha(
                0.0f
        );

        binding.searchIcon.setScaleX(
                0.75f
        );

        binding.searchIcon.setScaleY(
                0.75f
        );

        binding.searchEditText.setAlpha(
                0.0f
        );

        binding.searchEditText.setTranslationX(
                18.0f
        );

        binding.btnSearch.setAlpha(
                0.0f
        );

        binding.btnSearch.setScaleX(
                0.75f
        );

        binding.btnSearch.setScaleY(
                0.75f
        );

        binding.searchIcon.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(220)
                .start();

        binding.searchEditText.animate()
                .alpha(1.0f)
                .translationX(0.0f)
                .setDuration(260)
                .setStartDelay(40)
                .start();

        binding.btnSearch.animate()
                .alpha(1.0f)
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(220)
                .setStartDelay(70)
                .start();

        binding.searchEditText.requestFocus();

        binding.searchEditText.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {

                        if (!isAdded() ||
                                binding == null ||
                                !isSearchOpen) {
                            return;
                        }

                        InputMethodManager imm =
                                (InputMethodManager)
                                        requireContext()
                                                .getSystemService(
                                                        Context.INPUT_METHOD_SERVICE
                                                );

                        if (imm != null) {

                            imm.showSoftInput(
                                    binding.searchEditText,
                                    InputMethodManager.SHOW_IMPLICIT
                            );
                        }
                    }
                },
                180
        );
    }

    private void closeSearch() {

        if (binding == null ||
                !isSearchOpen) {
            return;
        }

        isSearchOpen = false;

        hideKeyboard();

        binding.searchEditText.setText("");

        binding.searchIcon.animate().cancel();
        binding.searchEditText.animate().cancel();
        binding.btnSearch.animate().cancel();
        binding.homeTitle.animate().cancel();

        binding.searchIcon.animate()
                .alpha(0.0f)
                .scaleX(0.75f)
                .scaleY(0.75f)
                .setDuration(170)
                .start();

        binding.searchEditText.animate()
                .alpha(0.0f)
                .translationX(18.0f)
                .setDuration(190)
                .start();

        binding.btnSearch.animate()
                .alpha(0.0f)
                .scaleX(0.75f)
                .scaleY(0.75f)
                .setDuration(170)
                .withEndAction(
                        new Runnable() {
                            @Override
                            public void run() {

                                if (binding == null) {
                                    return;
                                }

                                binding.searchIcon
                                        .setVisibility(
                                                View.GONE
                                        );

                                binding.searchEditText
                                        .setVisibility(
                                                View.GONE
                                        );

                                binding.btnSearch
                                        .setImageResource(
                                                R.drawable.ic_search
                                        );

                                binding.btnSearch
                                        .setContentDescription(
                                                "Search"
                                        );

                                binding.btnSearch
                                        .setScaleX(
                                                0.75f
                                        );

                                binding.btnSearch
                                        .setScaleY(
                                                0.75f
                                        );

                                binding.homeTitle
                                        .setAlpha(
                                                0.0f
                                        );

                                binding.homeTitle
                                        .setTranslationX(
                                                -12.0f
                                        );

                                binding.homeTitle
                                        .setVisibility(
                                                View.VISIBLE
                                        );

                                binding.homeTitle
                                        .animate()
                                        .alpha(1.0f)
                                        .translationX(0.0f)
                                        .setDuration(220)
                                        .start();

                                binding.btnSearch
                                        .animate()
                                        .alpha(1.0f)
                                        .scaleX(1.0f)
                                        .scaleY(1.0f)
                                        .setDuration(190)
                                        .start();

                                binding.searchEditText
                                        .setAlpha(
                                                1.0f
                                        );

                                binding.searchEditText
                                        .setTranslationX(
                                                18.0f
                                        );
                            }
                        }
                )
                .start();
    }

    private void showAddToPlaylistDialog(
            final AudioFile song) {

        if (song == null ||
                playlistManager == null) {
            return;
        }

        final ArrayList<String> playlists =
                playlistManager.getPlaylists();

        playlists.add(
                "＋ New Playlist"
        );

        String[] names =
                playlists.toArray(
                        new String[0]
                );

        AlertDialog dialog =
                new AlertDialog.Builder(
                        requireContext()
                )
                .setTitle(
                        "Add to Playlist"
                )
                .setItems(
                        names,
                        (d, which) -> {

                            String selected =
                                    names[which];

                            if ("＋ New Playlist".equals(
                                    selected
                            )) {

                                showCreatePlaylistDialog(
                                        song
                                );

                                return;
                            }

                            String uri =
                                    song.getUri();

                            if (uri == null ||
                                    uri.trim().isEmpty()) {
                                return;
                            }

                            if (playlistManager
                                    .isSongInPlaylist(
                                            selected,
                                            uri
                                    )) {

                                Toast.makeText(
                                        requireContext(),
                                        "Already in "
                                                + selected,
                                        Toast.LENGTH_SHORT
                                ).show();

                                return;
                            }

                            boolean added =
                                    playlistManager
                                            .addSongToPlaylist(
                                                    selected,
                                                    uri
                                            );

                            if (added) {

                                Toast.makeText(
                                        requireContext(),
                                        "Added to "
                                                + selected,
                                        Toast.LENGTH_SHORT
                                ).show();

                            } else {

                                Toast.makeText(
                                        requireContext(),
                                        "Couldn't add song",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                )
                .setNegativeButton(
                        "Cancel",
                        null
                )
                .create();

        dialog.show();
    }

    private void showCreatePlaylistDialog(
            final AudioFile song) {

        EditText input =
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
                android.text.InputType.TYPE_CLASS_TEXT
        );

        int padding =
                Math.round(
                        20
                                * getResources()
                                        .getDisplayMetrics()
                                        .density
                );

        input.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        AlertDialog dialog =
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

                                String uri =
                                        song == null
                                                ? null
                                                : song.getUri();

                                if (uri != null &&
                                        !uri.trim().isEmpty()) {

                                    playlistManager
                                            .addSongToPlaylist(
                                                    name,
                                                    uri
                                            );
                                }

                                dialog.dismiss();

                                Toast.makeText(
                                        requireContext(),
                                        "Playlist created",
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                    );
                }
        );

        dialog.show();
    }

    private void closeSearchImmediate() {

        if (binding == null) {
            return;
        }

        isSearchOpen = false;

        binding.homeTitle.setVisibility(
                View.VISIBLE
        );

        binding.homeTitle.setAlpha(
                1.0f
        );

        binding.homeTitle.setTranslationX(
                0.0f
        );

        binding.searchIcon.setVisibility(
                View.GONE
        );

        binding.searchEditText.setVisibility(
                View.GONE
        );

        binding.btnSearch.setVisibility(
                View.VISIBLE
        );

        binding.btnSearch.setAlpha(
                1.0f
        );

        binding.btnSearch.setScaleX(
                1.0f
        );

        binding.btnSearch.setScaleY(
                1.0f
        );

        binding.btnSearch.setImageResource(
                R.drawable.ic_search
        );

        binding.btnSearch.setContentDescription(
                "Search"
        );

        binding.searchEditText.setAlpha(
                1.0f
        );

        binding.searchEditText.setTranslationX(
                18.0f
        );
    }

    private void filterSongs(
            String query) {

        if (binding == null ||
                audioListAdapter == null) {
            return;
        }

        String normalizedQuery =
                query == null
                        ? ""
                        : query.trim().toLowerCase();

        songs.clear();

        if (normalizedQuery.isEmpty()) {

            songs.addAll(
                    allSongs
            );

            audioListAdapter
                    .notifyDataSetChanged();

            return;
        }

        for (AudioFile song : allSongs) {

            if (song == null) {
                continue;
            }

            String title =
                    song.getTitle();

            String artist =
                    song.getArtist();

            String album =
                    song.getAlbum();

            if (title == null) {
                title = "";
            }

            if (artist == null) {
                artist = "";
            }

            if (album == null) {
                album = "";
            }

            String searchableText =
                    (
                            title
                                    + " "
                                    + artist
                                    + " "
                                    + album
                    ).toLowerCase();

            if (searchableText.contains(
                    normalizedQuery
            )) {

                songs.add(
                        song
                );
            }
        }

        audioListAdapter
                .notifyDataSetChanged();
    }

    private void hideKeyboard() {

        if (binding == null) {
            return;
        }

        InputMethodManager imm =
                (InputMethodManager)
                        requireContext()
                                .getSystemService(
                                        Context.INPUT_METHOD_SERVICE
                                );

        if (imm != null) {

            imm.hideSoftInputFromWindow(
                    binding.searchEditText
                            .getWindowToken(),
                    0
            );
        }

        binding.searchEditText.clearFocus();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (receiversRegistered) {
            return;
        }

        IntentFilter playerFilter =
                new IntentFilter(
                        MusicPlayerService.ACTION_STATE_CHANGED
                );

        ContextCompat.registerReceiver(
                requireContext(),
                playerReceiver,
                playerFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );

        IntentFilter colorFilter =
                new IntentFilter(
                        AlbumColorManager.ACTION_COLORS_CHANGED
                );

        ContextCompat.registerReceiver(
                requireContext(),
                colorReceiver,
                colorFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );

        receiversRegistered = true;

        if (albumColorManager != null) {

            currentAccentColor =
                    albumColorManager
                            .getCurrentAccentColor();

            currentBackgroundColor =
                    albumColorManager
                            .getCurrentBackgroundColor();

            applyThemeColors(
                    currentAccentColor,
                    currentBackgroundColor
            );
        }
    }

    @Override
    public void onStop() {

        if (receiversRegistered) {

            try {

                requireContext()
                        .unregisterReceiver(
                                playerReceiver
                        );

            } catch (Exception ignored) {
            }

            try {

                requireContext()
                        .unregisterReceiver(
                                colorReceiver
                        );

            } catch (Exception ignored) {
            }

            receiversRegistered = false;
        }

        if (colorAnimator != null) {

            colorAnimator.cancel();
            colorAnimator = null;
        }

        super.onStop();
    }

    private void toggleFavorite(
            AudioFile song) {

        if (song == null ||
                playlistManager == null) {
            return;
        }

        String uri =
                song.getUri();

        if (uri == null ||
                uri.trim().isEmpty()) {
            return;
        }

        boolean isFavorite =
                playlistManager.toggleFavorite(
                        uri
                );

        int position =
                songs.indexOf(song);

        if (position >= 0) {

            audioListAdapter.notifyItemChanged(
                    position
            );
        }

        if (isFavorite) {

            Toast.makeText(
                    requireContext(),
                    "Added to Favorites",
                    Toast.LENGTH_SHORT
            ).show();

        } else {

            Toast.makeText(
                    requireContext(),
                    "Removed from Favorites",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void showSongMenu(
        AudioFile song,
        View anchor) {

    if (song == null) {
        return;
    }

    PopupMenu popupMenu =
            new PopupMenu(
                    requireContext(),
                    anchor
            );

    // =========================
    // MENU ITEMS
    // =========================

    popupMenu.getMenu().add(
            "Play"
    );
   popupMenu.getMenu().add("Test Music Identifier");

    popupMenu.getMenu().add(
            "Test AcoustID"
    );

    popupMenu.getMenu().add(
            "Add to Playlist"
    );

    popupMenu.getMenu().add(
            "Song Info"
    );

    popupMenu.getMenu().add(
            "Test Fingerprint"
    );

    popupMenu.getMenu().add(
            "Test MusicBrainz"
    );

    // =========================
    // MENU CLICK LISTENER
    // =========================

    popupMenu.setOnMenuItemClickListener(
            item -> {

                String title =
                        item.getTitle().toString();

                // =========================
                // PLAY
                // =========================

                if ("Play".equals(title)) {

                    showMiniPlayer(
                            song,
                            true
                    );

                    playerController.play(
                            song.getUri()
                    );

                }

                // =========================
                // TEST ACOUSTID
                // =========================

                else if (
                        "Test AcoustID".equals(title)
                ) {

                    AcoustIdTestHelper.test(
                            requireContext(),
                            song
                    );

                }

                // =========================
                // ADD TO PLAYLIST
                // =========================

                else if (
                        "Add to Playlist".equals(title)
                ) {

                    showAddToPlaylistDialog(
                            song
                    );

                }

                // =========================
                // SONG INFO
                // =========================

                else if (
                        "Song Info".equals(title)
                ) {

                    Toast.makeText(
                            requireContext(),
                            song.getTitle(),
                            Toast.LENGTH_SHORT
                    ).show();

                }

                // =========================
                // TEST FINGERPRINT
                // =========================

                else if (
                        "Test Fingerprint".equals(title)
                ) {

                    ChromaprintTestHelper.test(
                            requireContext(),
                            song
                    );

                    Toast.makeText(
                            requireContext(),
                            "Fingerprint test started. Check Logcat.",
                            Toast.LENGTH_SHORT
                    ).show();

                }

                // =========================
                // TEST MUSICBRAINZ
                // =========================

                else if (
                        "Test MusicBrainz".equals(title)
                ) {

                    MusicBrainzTestHelper.test(
                            requireContext(),
                            song
                    );

               } else if ("Test Music Identifier".equals(title)) {

    MusicIdentifierTestHelper.test(
            requireContext(),
            song
    );
    }
                

                return true;
            }
    );

    popupMenu.show();
}

    private void loadMusic() {

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {

                        if (musicRepository == null) {

                            stopRefreshing();

                            return;
                        }

                        final ArrayList<AudioFile> result;

                        try {

                            result =
                                    musicRepository
                                            .getAllSongs();

                        } catch (Exception ignored) {

                            stopRefreshing();

                            return;
                        }

                        if (!isAdded()) {
                            return;
                        }

                        requireActivity()
                                .runOnUiThread(
                                        new Runnable() {
                                            @Override
                                            public void run() {

                                                if (binding == null) {
                                                    return;
                                                }

                                                allSongs.clear();

                                                if (result != null) {

                                                    allSongs.addAll(
                                                            result
                                                    );
                                                }

                                                String query =
                                                        binding.searchEditText
                                                                .getText()
                                                                .toString();

                                                filterSongs(
                                                        query
                                                );

                                                binding.swipeRefresh
                                                        .setRefreshing(
                                                                false
                                                        );
                                            }
                                        }
                                );
                    }
                }
        ).start();
    }

    private void stopRefreshing() {

        if (!isAdded()) {
            return;
        }

        requireActivity()
                .runOnUiThread(
                        new Runnable() {
                            @Override
                            public void run() {

                                if (binding == null) {
                                    return;
                                }

                                binding.swipeRefresh
                                        .setRefreshing(
                                                false
                                        );
                            }
                        }
                );
    }

    private void showMiniPlayer(
            AudioFile song,
            boolean isPlaying) {

        if (binding == null ||
                song == null) {
            return;
        }

        String songUri =
                song.getUri();

        if (songUri == null ||
                songUri.trim().isEmpty()) {
            return;
        }

        currentMiniPlayerUri =
                songUri;

        binding.miniPlayer.setVisibility(
                View.VISIBLE
        );

        String title =
                song.getTitle();

        if (title == null ||
                title.trim().isEmpty()) {

            title = "Unknown song";
        }

        binding.miniTitle.setText(
                title
        );

        String artist =
                song.getArtist();

        if (artist == null ||
                artist.trim().isEmpty()) {

            artist = "Unknown artist";
        }

        binding.miniArtist.setText(
                artist
        );

        binding.miniAlbumArt.setImageResource(
                android.R.drawable.ic_media_play
        );

        loadAlbumArt(
                song,
                songUri
        );

        binding.miniPlayPause.setImageResource(
                isPlaying
                        ? android.R.drawable.ic_media_pause
                        : android.R.drawable.ic_media_play
        );

        applyThemeColors(
                currentAccentColor,
                currentBackgroundColor
        );
    }

    private void loadAlbumArt(
            final AudioFile song,
            final String requestedUri) {

        if (binding == null ||
                song == null ||
                requestedUri == null) {
            return;
        }

        final String albumArtUri =
                song.getAlbumArtUri();

        if (albumArtUri == null ||
                albumArtUri.trim().isEmpty()) {

            loadAlbumArtThumbnail(
                    song,
                    requestedUri
            );

            return;
        }

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {

                        Bitmap bitmap =
                                null;

                        try {

                            Uri uri =
                                    Uri.parse(
                                            albumArtUri
                                    );

                            InputStream inputStream =
                                    requireContext()
                                            .getContentResolver()
                                            .openInputStream(
                                                    uri
                                            );

                            if (inputStream != null) {

                                bitmap =
                                        BitmapFactory
                                                .decodeStream(
                                                        inputStream
                                                );

                                inputStream.close();
                            }

                        } catch (Exception ignored) {
                        }

                        final Bitmap result =
                                bitmap;

                        if (!isAdded()) {
                            return;
                        }

                        requireActivity()
                                .runOnUiThread(
                                        new Runnable() {
                                            @Override
                                            public void run() {

                                                if (binding == null) {
                                                    return;
                                                }

                                                if (!requestedUri.equals(
                                                        currentMiniPlayerUri
                                                )) {
                                                    return;
                                                }

                                                if (result != null) {

                                                    binding.miniAlbumArt
                                                            .setImageBitmap(
                                                                    result
                                                            );

                                                } else {

                                                    loadAlbumArtThumbnail(
                                                            song,
                                                            requestedUri
                                                    );
                                                }
                                            }
                                        }
                                );
                    }
                }
        ).start();
    }

    private void loadAlbumArtThumbnail(
            final AudioFile song,
            final String requestedUri) {

        if (binding == null ||
                song == null ||
                requestedUri == null) {
            return;
        }

        new Thread(
                new Runnable() {
                    @Override
                    public void run() {

                        Bitmap bitmap =
                                null;

                        try {

                            Uri songUri =
                                    Uri.parse(
                                            song.getUri()
                                    );

                            bitmap =
                                    requireContext()
                                            .getContentResolver()
                                            .loadThumbnail(
                                                    songUri,
                                                    new android.util.Size(
                                                            256,
                                                            256
                                                    ),
                                                    null
                                            );

                        } catch (Exception ignored) {
                        }

                        final Bitmap result =
                                bitmap;

                        if (!isAdded()) {
                            return;
                        }

                        requireActivity()
                                .runOnUiThread(
                                        new Runnable() {
                                            @Override
                                            public void run() {

                                                if (binding == null) {
                                                    return;
                                                }

                                                if (!requestedUri.equals(
                                                        currentMiniPlayerUri
                                                )) {
                                                    return;
                                                }

                                                if (result != null) {

                                                    binding.miniAlbumArt
                                                            .setImageBitmap(
                                                                    result
                                                            );

                                                } else {

                                                    binding.miniAlbumArt
                                                            .setImageResource(
                                                                    android.R.drawable
                                                                            .ic_media_play
                                                            );
                                                }
                                            }
                                        }
                                );
                    }
                }
        ).start();
    }

    private void updateMiniPlayer(
            String uri,
            boolean isPlaying) {

        if (binding == null) {
            return;
        }

        if (uri == null ||
                uri.trim().isEmpty()) {

            currentMiniPlayerUri = null;

            binding.miniPlayer.setVisibility(
                    View.GONE
            );

            return;
        }

        AudioFile currentSong =
                null;

        for (AudioFile song : allSongs) {

            if (song == null) {
                continue;
            }

            if (uri.equals(
                    song.getUri()
            )) {

                currentSong =
                        song;

                break;
            }
        }

        if (currentSong == null) {
            return;
        }

        boolean songChanged =
                !uri.equals(
                        currentMiniPlayerUri
                );

        if (songChanged) {

            showMiniPlayer(
                    currentSong,
                    isPlaying
            );

        } else {

            binding.miniPlayPause
                    .setImageResource(
                            isPlaying
                                    ? R.drawable
                                            .ic_pause
                                    : R.drawable
                                            .ic_play
                    );
        }
    }

    private void animateThemeColors(
            final int newAccentColor,
            final int newBackgroundColor) {

        if (binding == null) {
            return;
        }

        if (colorAnimator != null) {

            colorAnimator.cancel();
        }

        final int oldAccentColor =
                currentAccentColor;

        final int oldBackgroundColor =
                currentBackgroundColor;

        colorAnimator =
                ValueAnimator.ofFloat(
                        0.0f,
                        1.0f
                );

        colorAnimator.setDuration(
                450
        );

        colorAnimator.addUpdateListener(
                new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(
                            ValueAnimator animation) {

                        float fraction =
                                (Float)
                                        animation
                                                .getAnimatedValue();

                        int accentColor =
                                (Integer)
                                        new ArgbEvaluator()
                                                .evaluate(
                                                        fraction,
                                                        oldAccentColor,
                                                        newAccentColor
                                                );

                        int backgroundColor =
                                (Integer)
                                        new ArgbEvaluator()
                                                .evaluate(
                                                        fraction,
                                                        oldBackgroundColor,
                                                        newBackgroundColor
                                                );

                        applyThemeColors(
                                accentColor,
                                backgroundColor
                        );
                    }
                }
        );

        colorAnimator.start();
    }

    private void applyThemeColors(
            int accentColor,
            int backgroundColor) {

        if (binding == null) {
            return;
        }

        currentAccentColor =
                accentColor;

        currentBackgroundColor =
                backgroundColor;

        binding.getRoot()
                .setBackgroundColor(
                        backgroundColor
                );

        binding.homeToolbar
                .setBackgroundColor(
                        backgroundColor
                );

        binding.searchContainer
                .setCardBackgroundColor(
                        darkenColor(
                                backgroundColor,
                                0.78f
                        )
                );

        binding.rvHomeMusic
                .setBackgroundColor(
                        Color.TRANSPARENT
                );

        binding.swipeRefresh
                .setBackgroundColor(
                        Color.TRANSPARENT
                );

        binding.swipeRefresh
                .setColorSchemeColors(
                        currentAccentColor
                );

        binding.homeTitle
                .setTextColor(
                        Color.WHITE
                );

        binding.searchEditText
                .setTextColor(
                        Color.WHITE
                );

        binding.searchEditText
                .setHintTextColor(
                        blendColors(
                                currentAccentColor,
                                Color.WHITE,
                                0.30f
                        )
                );

        binding.btnSearch
                .setImageTintList(
                        ColorStateList.valueOf(
                                currentAccentColor
                        )
                );

        binding.searchIcon
                .setImageTintList(
                        ColorStateList.valueOf(
                                currentAccentColor
                        )
                );

        int miniPlayerColor =
                darkenColor(
                        backgroundColor,
                        0.78f
                );

        binding.miniPlayer
                .setCardBackgroundColor(
                        miniPlayerColor
                );

        binding.miniTitle
                .setTextColor(
                        Color.WHITE
                );

        binding.miniArtist
                .setTextColor(
                        blendColors(
                                currentAccentColor,
                                Color.WHITE,
                                0.35f
                        )
                );

        binding.miniPlayPause
                .setImageTintList(
                        ColorStateList.valueOf(
                                currentAccentColor
                        )
                );
    }

    private int darkenColor(
            int color,
            float factor) {

        factor =
                Math.max(
                        0.0f,
                        Math.min(
                                1.0f,
                                factor
                        )
                );

        int red =
                Math.max(
                        0,
                        Math.min(
                                255,
                                Math.round(
                                        Color.red(color)
                                                * factor
                                )
                        )
                );

        int green =
                Math.max(
                        0,
                        Math.min(
                                255,
                                Math.round(
                                        Color.green(color)
                                                * factor
                                )
                        )
                );

        int blue =
                Math.max(
                        0,
                        Math.min(
                                255,
                                Math.round(
                                        Color.blue(color)
                                                * factor
                                )
                        )
                );

        return Color.rgb(
                red,
                green,
                blue
        );
    }

    private int blendColors(
            int color1,
            int color2,
            float ratio) {

        ratio =
                Math.max(
                        0.0f,
                        Math.min(
                                1.0f,
                                ratio
                        )
                );

        int r =
                Math.round(
                        Color.red(color1)
                                * (1.0f - ratio)
                                +
                        Color.red(color2)
                                * ratio
                );

        int g =
                Math.round(
                        Color.green(color1)
                                * (1.0f - ratio)
                                +
                        Color.green(color2)
                                * ratio
                );

        int b =
                Math.round(
                        Color.blue(color1)
                                * (1.0f - ratio)
                                +
                        Color.blue(color2)
                                * ratio
                );

        return Color.rgb(
                r,
                g,
                b
        );
    }

    @Override
    public void onDestroyView() {

        if (colorAnimator != null) {

            colorAnimator.cancel();
            colorAnimator = null;
        }

        hideKeyboard();

        currentMiniPlayerUri = null;

        isSearchOpen = false;

        receiversRegistered = false;

        binding = null;

        super.onDestroyView();
    }
}