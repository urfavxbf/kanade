package com.urfavxbf.kanade.ui.artist;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.urfavxbf.kanade.AudioFile;
import com.urfavxbf.kanade.MusicPlayerService;
import com.urfavxbf.kanade.MusicRepository;
import com.urfavxbf.kanade.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArtistFragment extends Fragment {

    private static final int BACKGROUND = Color.rgb(16, 17, 26);
    private static final int CARD = Color.rgb(24, 26, 36);
    private static final int PRIMARY = Color.WHITE;
    private static final int SECONDARY = Color.rgb(150, 152, 166);
    private static final int ACCENT = Color.rgb(201, 196, 255);

    private MusicRepository musicRepository;
    private ExecutorService executor;
    private RecyclerView recyclerView;
    private TextView titleView;
    private TextView subtitleView;
    private ImageButton backButton;
    private ArtistAdapter artistAdapter;
    private ArrayList<AudioFile> allSongs = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        Context context = requireContext();
        musicRepository = new MusicRepository(context.getApplicationContext());
        executor = Executors.newSingleThreadExecutor();

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        root.addView(createToolbar(context), new LinearLayout.LayoutParams(-1, dp(76)));

        recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setClipToPadding(false);
        recyclerView.setPadding(dp(8), 0, dp(8), dp(24));
        root.addView(recyclerView, new LinearLayout.LayoutParams(-1, 0, 1f));

        loadArtists();
        return root;
    }

    private View createToolbar(Context context) {
        LinearLayout toolbar = new LinearLayout(context);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setPadding(dp(8), dp(8), dp(12), dp(8));

        backButton = new ImageButton(context);
        backButton.setImageResource(R.drawable.ic_back);
        backButton.setColorFilter(PRIMARY);
        backButton.setBackgroundResource(android.R.drawable.btn_default);
        backButton.setContentDescription("Back");
        backButton.setVisibility(View.GONE);
        backButton.setOnClickListener(v -> loadArtists());
        toolbar.addView(backButton, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout text = new LinearLayout(context);
        text.setOrientation(LinearLayout.VERTICAL);
        text.setGravity(Gravity.CENTER_VERTICAL);

        titleView = new TextView(context);
        titleView.setText("Artists");
        titleView.setTextColor(PRIMARY);
        titleView.setTextSize(21);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);

        subtitleView = new TextView(context);
        subtitleView.setText("Your music, by artist");
        subtitleView.setTextColor(SECONDARY);
        subtitleView.setTextSize(12);
        subtitleView.setMaxLines(1);

        text.addView(titleView);
        text.addView(subtitleView);
        toolbar.addView(text, new LinearLayout.LayoutParams(0, -1, 1f));
        return toolbar;
    }

    private void loadArtists() {
        if (executor == null || recyclerView == null) {
            return;
        }
        titleView.setText("Artists");
        subtitleView.setText("Loading artists…");
        backButton.setVisibility(View.GONE);

        executor.execute(() -> {
            ArrayList<AudioFile> songs = musicRepository.getAllSongs();
            ArrayList<ArtistItem> artists = buildArtists(songs);
            if (!isAdded()) {
                return;
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded() || recyclerView == null) {
                    return;
                }
                allSongs = songs;
                subtitleView.setText(artists.size() + (artists.size() == 1 ? " artist" : " artists"));
                artistAdapter = new ArtistAdapter(requireContext(), artists, this::showArtistDetail);
                recyclerView.setAdapter(artistAdapter);
            });
        });
    }

    private ArrayList<ArtistItem> buildArtists(List<AudioFile> songs) {
        Map<String, ArtistItem> map = new LinkedHashMap<>();
        for (AudioFile song : songs) {
            if (song == null) {
                continue;
            }
            String artist = clean(song.getArtist(), "Unknown artist");
            String key = artist.toLowerCase(Locale.ROOT);
            ArtistItem item = map.get(key);
            if (item == null) {
                item = new ArtistItem(artist);
                map.put(key, item);
            }
            item.songCount++;
            item.albums.add(clean(song.getAlbum(), "Unknown album"));
        }
        ArrayList<ArtistItem> result = new ArrayList<>(map.values());
        Collections.sort(result, Comparator.comparing(item -> item.name.toLowerCase(Locale.ROOT)));
        return result;
    }

    private void showArtistDetail(String artist) {
        ArrayList<AudioFile> songs = new ArrayList<>();
        Set<String> albums = new LinkedHashSet<>();
        long totalDuration = 0L;

        for (AudioFile song : allSongs) {
            if (song == null || !artist.equalsIgnoreCase(clean(song.getArtist(), "Unknown artist"))) {
                continue;
            }
            songs.add(song);
            albums.add(clean(song.getAlbum(), "Unknown album"));
            totalDuration += Math.max(0L, song.getDuration());
        }

        Collections.sort(songs, (left, right) -> {
            int album = clean(left.getAlbum(), "Unknown album")
                    .compareToIgnoreCase(clean(right.getAlbum(), "Unknown album"));
            if (album != 0) {
                return album;
            }
            return clean(left.getTitle(), "Unknown song")
                    .compareToIgnoreCase(clean(right.getTitle(), "Unknown song"));
        });

        titleView.setText(artist);
        subtitleView.setText(songs.size() + (songs.size() == 1 ? " song" : " songs")
                + " • " + albums.size() + (albums.size() == 1 ? " album" : " albums")
                + " • " + formatDuration(totalDuration));
        backButton.setVisibility(View.VISIBLE);

        LinearLayout detailHeader = createDetailHeader(requireContext(), artist, songs.size(), albums.size());
        recyclerView.setAdapter(new HeaderSongAdapter(requireContext(), detailHeader, songs));
    }

    private LinearLayout createDetailHeader(Context context, String artist, int songs, int albums) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(dp(4), dp(4), dp(4), dp(14));

        LinearLayout hero = new LinearLayout(context);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setPadding(dp(18), dp(18), dp(18), dp(18));
        hero.setBackground(round(CARD, 24));

        TextView avatar = new TextView(context);
        avatar.setText(initials(artist));
        avatar.setTextColor(BACKGROUND);
        avatar.setTextSize(28);
        avatar.setTypeface(null, android.graphics.Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);
        avatar.setBackground(round(ACCENT, 100));
        hero.addView(avatar, new LinearLayout.LayoutParams(dp(72), dp(72)));

        LinearLayout info = new LinearLayout(context);
        info.setOrientation(LinearLayout.VERTICAL);
        info.setPadding(dp(16), 0, 0, 0);

        TextView name = new TextView(context);
        name.setText(artist);
        name.setTextColor(PRIMARY);
        name.setTextSize(22);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);

        TextView stats = new TextView(context);
        stats.setText(songs + " songs  •  " + albums + (albums == 1 ? " album" : " albums"));
        stats.setTextColor(SECONDARY);
        stats.setTextSize(12);
        stats.setPadding(0, dp(5), 0, 0);

        info.addView(name);
        info.addView(stats);
        hero.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));
        header.addView(hero, new LinearLayout.LayoutParams(-1, -2));

        TextView section = new TextView(context);
        section.setText("Songs");
        section.setTextColor(PRIMARY);
        section.setTextSize(18);
        section.setTypeface(null, android.graphics.Typeface.BOLD);
        section.setPadding(dp(8), dp(18), dp(8), dp(8));
        header.addView(section);
        return header;
    }

    private static String clean(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static String initials(String name) {
        String clean = clean(name, "?");
        String[] parts = clean.split("\\s+");
        if (parts.length == 1) {
            return clean.substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase(Locale.ROOT);
    }

    private static String formatDuration(long milliseconds) {
        long minutes = milliseconds / 60000L;
        if (minutes < 60L) {
            return minutes + " min";
        }
        return (minutes / 60L) + "h " + (minutes % 60L) + "m";
    }

    private GradientDrawable round(int color, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp((int) radiusDp));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * requireContext().getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        recyclerView = null;
        super.onDestroyView();
    }

    private static final class ArtistItem {
        final String name;
        final Set<String> albums = new LinkedHashSet<>();
        int songCount;

        ArtistItem(String name) {
            this.name = name;
        }
    }

    private static final class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ViewHolder> {
        interface Listener {
            void onArtistClick(String artist);
        }

        private final Context context;
        private final ArrayList<ArtistItem> artists;
        private final Listener listener;

        ArtistAdapter(Context context, ArrayList<ArtistItem> artists, Listener listener) {
            this.context = context;
            this.artists = artists;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(parent, 12), dp(parent, 8), dp(parent, 12), dp(parent, 8));
            row.setBackground(round(CARD, parent, 20));

            TextView avatar = new TextView(parent.getContext());
            avatar.setGravity(Gravity.CENTER);
            avatar.setTextColor(BACKGROUND);
            avatar.setTextSize(16);
            avatar.setTypeface(null, android.graphics.Typeface.BOLD);
            avatar.setBackground(round(ACCENT, parent, 100));
            row.addView(avatar, new LinearLayout.LayoutParams(dp(parent, 52), dp(parent, 52)));

            LinearLayout text = new LinearLayout(parent.getContext());
            text.setOrientation(LinearLayout.VERTICAL);
            text.setPadding(dp(parent, 14), 0, dp(parent, 8), 0);

            TextView name = new TextView(parent.getContext());
            name.setTextColor(PRIMARY);
            name.setTextSize(16);
            name.setTypeface(null, android.graphics.Typeface.BOLD);
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);

            TextView meta = new TextView(parent.getContext());
            meta.setTextColor(SECONDARY);
            meta.setTextSize(12);
            meta.setPadding(0, dp(parent, 3), 0, 0);

            text.addView(name);
            text.addView(meta);
            row.addView(text, new LinearLayout.LayoutParams(0, -2, 1f));

            TextView arrow = new TextView(parent.getContext());
            arrow.setText("›");
            arrow.setTextColor(SECONDARY);
            arrow.setTextSize(28);
            row.addView(arrow, new LinearLayout.LayoutParams(dp(parent, 28), dp(parent, 52)));

            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, -2);
            params.setMargins(0, dp(parent, 4), 0, dp(parent, 4));
            row.setLayoutParams(params);
            return new ViewHolder(row, avatar, name, meta);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            ArtistItem item = artists.get(position);
            holder.avatar.setText(initials(item.name));
            holder.name.setText(item.name);
            holder.meta.setText(item.songCount + (item.songCount == 1 ? " song" : " songs")
                    + "  •  " + item.albums.size() + (item.albums.size() == 1 ? " album" : " albums"));
            holder.itemView.setContentDescription(item.name + ", " + item.songCount + " songs");
            holder.itemView.setOnClickListener(v -> listener.onArtistClick(item.name));
        }

        @Override
        public int getItemCount() {
            return artists.size();
        }

        static final class ViewHolder extends RecyclerView.ViewHolder {
            final TextView avatar;
            final TextView name;
            final TextView meta;

            ViewHolder(View itemView, TextView avatar, TextView name, TextView meta) {
                super(itemView);
                this.avatar = avatar;
                this.name = name;
                this.meta = meta;
            }
        }

        private static int dp(View view, int value) {
            return Math.round(value * view.getResources().getDisplayMetrics().density);
        }

        private static GradientDrawable round(int color, View view, float radiusDp) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(color);
            drawable.setCornerRadius(radiusDp * view.getResources().getDisplayMetrics().density);
            return drawable;
        }

        private static String initials(String name) {
            String value = name == null || name.trim().isEmpty() ? "?" : name.trim();
            String[] parts = value.split("\\s+");
            if (parts.length == 1) {
                return value.substring(0, 1).toUpperCase(Locale.ROOT);
            }
            return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                    .toUpperCase(Locale.ROOT);
        }
    }

    private static final class HeaderSongAdapter extends RecyclerView.Adapter<HeaderSongAdapter.ViewHolder> {
        private final Context context;
        private final View header;
        private final ArrayList<AudioFile> songs;

        HeaderSongAdapter(Context context, View header, ArrayList<AudioFile> songs) {
            this.context = context;
            this.header = header;
            this.songs = songs;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? 0 : 1;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 0) {
                return new ViewHolder(header);
            }

            LinearLayout row = new LinearLayout(parent.getContext());
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(parent, 12), dp(parent, 7), dp(parent, 12), dp(parent, 7));
            row.setBackground(round(CARD, parent, 18));

            TextView number = new TextView(parent.getContext());
            number.setTextColor(SECONDARY);
            number.setTextSize(12);
            row.addView(number, new LinearLayout.LayoutParams(dp(parent, 34), dp(parent, 50)));

            LinearLayout text = new LinearLayout(parent.getContext());
            text.setOrientation(LinearLayout.VERTICAL);
            TextView title = new TextView(parent.getContext());
            title.setTextColor(PRIMARY);
            title.setTextSize(14);
            title.setMaxLines(1);
            title.setEllipsize(android.text.TextUtils.TruncateAt.END);
            TextView album = new TextView(parent.getContext());
            album.setTextColor(SECONDARY);
            album.setTextSize(11);
            album.setMaxLines(1);
            album.setEllipsize(android.text.TextUtils.TruncateAt.END);
            text.addView(title);
            text.addView(album);
            row.addView(text, new LinearLayout.LayoutParams(0, -2, 1f));

            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(-1, -2);
            params.setMargins(0, dp(parent, 3), 0, dp(parent, 3));
            row.setLayoutParams(params);
            return new ViewHolder(row, number, title, album);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            if (position == 0) {
                return;
            }
            AudioFile song = songs.get(position - 1);
            holder.number.setText(String.valueOf(position));
            holder.title.setText(clean(song.getTitle(), "Unknown song"));
            holder.album.setText(clean(song.getAlbum(), "Unknown album"));
            holder.itemView.setContentDescription(song.getTitle() + ", " + song.getAlbum());
            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, MusicPlayerService.class);
                intent.setAction(MusicPlayerService.ACTION_PLAY);
                intent.putExtra(MusicPlayerService.EXTRA_SONG_URI, song.getUri());
                context.startService(intent);
            });
        }

        @Override
        public int getItemCount() {
            return songs.size() + 1;
        }

        static final class ViewHolder extends RecyclerView.ViewHolder {
            TextView number;
            TextView title;
            TextView album;

            ViewHolder(View itemView) {
                super(itemView);
            }

            ViewHolder(View itemView, TextView number, TextView title, TextView album) {
                super(itemView);
                this.number = number;
                this.title = title;
                this.album = album;
            }
        }

        private static int dp(View view, int value) {
            return Math.round(value * view.getResources().getDisplayMetrics().density);
        }

        private static GradientDrawable round(int color, View view, float radiusDp) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setColor(color);
            drawable.setCornerRadius(radiusDp * view.getResources().getDisplayMetrics().density);
            return drawable;
        }

        private static String clean(String value, String fallback) {
            return value == null || value.trim().isEmpty() ? fallback : value.trim();
        }
    }
}
