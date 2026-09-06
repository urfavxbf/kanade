package com.urfavxbf.kanade.ui.youtube;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.urfavxbf.kanade.MusicPlayerController;
import com.urfavxbf.kanade.MusicRepository;
import com.urfavxbf.kanade.PlaybackStatsManager;
import com.urfavxbf.kanade.PlaylistManager;
import com.urfavxbf.kanade.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class YouTubeFragment extends Fragment {

    private EditText searchInput;
    private RecyclerView resultsRecycler;
    private TextView statusText;
    private Button playAllButton;
    private ResultAdapter adapter;
    private ExecutorService executor;
    private Handler mainHandler;
    private AtomicInteger searchGeneration;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_audius, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        searchInput = view.findViewById(R.id.audiusSearchInput);
        resultsRecycler = view.findViewById(R.id.audiusResultsRecycler);
        statusText = view.findViewById(R.id.audiusStatusText);
        playAllButton = view.findViewById(R.id.audiusPlayAllButton);
        mainHandler = new Handler(Looper.getMainLooper());
        searchGeneration = new AtomicInteger();
        executor = Executors.newFixedThreadPool(3, runnable -> {
            Thread thread = new Thread(runnable, "Kanade-YouTube");
            thread.setDaemon(true);
            return thread;
        });
        adapter = new ResultAdapter(this::playTrack, executor, mainHandler);
        resultsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        resultsRecycler.setAdapter(adapter);
        view.findViewById(R.id.audiusSearchButton).setOnClickListener(v -> search());
        playAllButton.setOnClickListener(v -> playAll());
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search();
                return true;
            }
            return false;
        });
        loadDefaultTracks();
    }

    private void search() {
        if (searchInput == null || executor == null) return;
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            loadDefaultTracks();
            return;
        }
        final int generation = searchGeneration.incrementAndGet();
        statusText.setText("Searching YouTube…");
        playAllButton.setVisibility(View.GONE);
        adapter.setItems(new ArrayList<>());
        executor.execute(() -> {
            try {
                ArrayList<YouTubeClient.Track> results = YouTubeClient.searchTracks(query);
                mainHandler.post(() -> {
                    if (!isCurrentSearch(generation)) return;
                    adapter.setItems(results);
                    playAllButton.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
                    statusText.setText(results.isEmpty() ? "No matching YouTube tracks found."
                            : results.size() + " matching YouTube results");
                });
            } catch (Exception e) {
                String message = e.getMessage() == null || e.getMessage().trim().isEmpty()
                        ? "Unknown YouTube error" : e.getMessage();
                mainHandler.post(() -> {
                    if (!isCurrentSearch(generation)) return;
                    playAllButton.setVisibility(View.GONE);
                    statusText.setText("YouTube search failed: " + message);
                });
            }
        });
    }

    private void loadDefaultTracks() {
        if (searchInput == null || executor == null) return;
        final int generation = searchGeneration.incrementAndGet();
        statusText.setText("Loading YouTube recommendations…");
        playAllButton.setVisibility(View.GONE);
        adapter.setItems(new ArrayList<>());
        executor.execute(() -> {
            ArrayList<YouTubeClient.Track> results = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            try {
                android.content.Context context = requireContext().getApplicationContext();
                PlaybackStatsManager stats = new PlaybackStatsManager(context);
                PlaylistManager playlists = new PlaylistManager(context);
                ArrayList<String> seedQueries = new ArrayList<>();

                for (PlaybackStatsManager.SongStat stat : stats.getMostPlayed(8)) {
                    String query = buildQuery(stat.title, stat.artist);
                    if (!query.isEmpty() && !seedQueries.contains(query)) seedQueries.add(query);
                }

                for (String uri : playlists.getPlaylistSongs(PlaylistManager.FAVORITES_PLAYLIST)) {
                    PlaybackStatsManager.SongStat stat = stats.getSongStat(uri);
                    if (stat != null) {
                        String query = buildQuery(stat.title, stat.artist);
                        if (!query.isEmpty() && !seedQueries.contains(query)) seedQueries.add(query);
                    }
                    if (seedQueries.size() >= 12) break;
                }

                if (seedQueries.isEmpty()) {
                    seedQueries.add("popular music");
                    seedQueries.add("top songs 2026");
                }

                for (String query : seedQueries) {
                    try {
                        ArrayList<YouTubeClient.Track> found = YouTubeClient.searchTracks(query);
                        for (YouTubeClient.Track track : found) {
                            if (track == null || track.id.isEmpty() || !seen.add(track.id)) continue;
                            results.add(track);
                            if (results.size() >= 40) break;
                        }
                    } catch (Exception ignored) {
                    }
                    if (results.size() >= 40) break;
                }
            } catch (Exception ignored) {
            }

            mainHandler.post(() -> {
                if (!isCurrentSearch(generation)) return;
                adapter.setItems(results);
                playAllButton.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
                statusText.setText(results.isEmpty()
                        ? "No YouTube recommendations available. Search for music above."
                        : "Most played / liked — YouTube recommendations");
            });
        });
    }

    private String buildQuery(String title, String artist) {
        String cleanTitle = safe(title, "");
        String cleanArtist = safe(artist, "");
        if (cleanTitle.isEmpty()) return cleanArtist;
        if (cleanArtist.isEmpty()) return cleanTitle;
        return cleanTitle + " " + cleanArtist;
    }

    private void playAll() {
        if (adapter == null || executor == null || adapter.items.isEmpty()) return;
        ArrayList<YouTubeClient.Track> tracks = adapter.getItemsCopy();
        final int generation = searchGeneration.get();
        statusText.setText("Starting playlist…");
        playAllButton.setEnabled(false);
        executor.execute(() -> {
            try {
                YouTubeClient.Track first = null;
                String firstStreamUrl = "";
                for (YouTubeClient.Track track : tracks) {
                    if (track == null || track.id.isEmpty()) continue;
                    try {
                        String streamUrl = YouTubeClient.resolveStreamUrl(track.id);
                        if (!streamUrl.isEmpty()) {
                            first = track;
                            firstStreamUrl = streamUrl;
                            break;
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (first == null || firstStreamUrl.isEmpty()) {
                    mainHandler.post(() -> {
                        if (!isCurrentSearch(generation)) return;
                        playAllButton.setEnabled(true);
                        statusText.setText("Play all failed: no playable YouTube songs");
                    });
                    return;
                }

                MusicRepository.registerRemoteSong(
                        firstStreamUrl,
                        first.title,
                        first.artist,
                        first.artworkUrl,
                        first.durationSeconds
                );

                final String playableFirstUrl = firstStreamUrl;
                mainHandler.post(() -> {
                    if (!isCurrentSearch(generation)) return;
                    new MusicPlayerController(requireContext()).play(playableFirstUrl);
                    statusText.setText("Playing YouTube playlist…");
                });

                int queued = 0;
                Set<String> queuedIds = new HashSet<>();
                queuedIds.add(first.id);
                MusicPlayerController controller = new MusicPlayerController(requireContext());
                for (YouTubeClient.Track track : tracks) {
                    if (track == null || track.id.isEmpty() || queuedIds.contains(track.id)) continue;
                    try {
                        String streamUrl = YouTubeClient.resolveStreamUrl(track.id);
                        if (streamUrl.isEmpty()) continue;
                        MusicRepository.registerRemoteSong(
                                streamUrl,
                                track.title,
                                track.artist,
                                track.artworkUrl,
                                track.durationSeconds
                        );
                        controller.addToQueue(streamUrl);
                        queuedIds.add(track.id);
                        queued++;
                    } catch (Exception ignored) {
                    }
                }

                final int queuedCount = queued;
                mainHandler.post(() -> {
                    if (!isCurrentSearch(generation)) return;
                    playAllButton.setEnabled(true);
                    statusText.setText("Playing YouTube playlist • " + (queuedCount + 1) + " songs");
                });
            } catch (Exception e) {
                String message = e.getMessage() == null || e.getMessage().trim().isEmpty()
                        ? "Unable to prepare YouTube playlist" : e.getMessage();
                mainHandler.post(() -> {
                    if (!isCurrentSearch(generation)) return;
                    playAllButton.setEnabled(true);
                    statusText.setText("Play all failed: " + message);
                });
            }
        });
    }

    private void playTrack(YouTubeClient.Track track) {
        if (!isAdded() || track == null || track.id.isEmpty() || executor == null) return;
        statusText.setText("Loading " + track.title + "…");
        executor.execute(() -> {
            try {
                String streamUrl = YouTubeClient.resolveStreamUrl(track.id);
                mainHandler.post(() -> {
                    if (!isAdded() || getView() == null) return;
                    new MusicPlayerController(requireContext()).play(streamUrl);
                    statusText.setText("Playing in Kanade's player");
                });
            } catch (Exception e) {
                String message = e.getMessage() == null || e.getMessage().trim().isEmpty()
                        ? "Unable to resolve YouTube stream" : e.getMessage();
                mainHandler.post(() -> {
                    if (!isAdded() || getView() == null) return;
                    statusText.setText("Playback failed: " + message);
                });
            }
        });
    }

    private boolean isCurrentSearch(int generation) {
        return isAdded() && getView() != null && searchGeneration != null
                && searchGeneration.get() == generation;
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    @Override
    public void onDestroyView() {
        if (searchGeneration != null) searchGeneration.incrementAndGet();
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (mainHandler != null) mainHandler.removeCallbacksAndMessages(null);
        adapter = null;
        resultsRecycler = null;
        searchInput = null;
        statusText = null;
        playAllButton = null;
        super.onDestroyView();
    }

    private static final class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {
        interface Listener { void onTrack(YouTubeClient.Track track); }
        private final ArrayList<YouTubeClient.Track> items = new ArrayList<>();
        private final Listener listener;
        private final ExecutorService imageExecutor;
        private final Handler mainHandler;

        ResultAdapter(Listener listener, ExecutorService imageExecutor, Handler mainHandler) {
            this.listener = listener;
            this.imageExecutor = imageExecutor;
            this.mainHandler = mainHandler;
        }

        void setItems(ArrayList<YouTubeClient.Track> newItems) {
            items.clear();
            if (newItems != null) items.addAll(newItems);
            notifyDataSetChanged();
        }

        ArrayList<YouTubeClient.Track> getItemsCopy() {
            return new ArrayList<>(items);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int padding = Math.round(10 * parent.getResources().getDisplayMetrics().density);
            float density = parent.getResources().getDisplayMetrics().density;
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(padding, padding, padding, padding);
            ImageView artwork = new ImageView(parent.getContext());
            artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
            row.addView(artwork, new LinearLayout.LayoutParams(Math.round(64 * density), Math.round(64 * density)));
            LinearLayout textContainer = new LinearLayout(parent.getContext());
            textContainer.setOrientation(LinearLayout.VERTICAL);
            textContainer.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textParams.leftMargin = Math.round(12 * density);
            row.addView(textContainer, textParams);
            TextView title = new TextView(parent.getContext());
            title.setTextColor(Color.WHITE);
            title.setTextSize(15);
            title.setMaxLines(2);
            title.setEllipsize(TextUtils.TruncateAt.END);
            textContainer.addView(title);
            TextView artist = new TextView(parent.getContext());
            artist.setTextColor(Color.rgb(160, 162, 175));
            artist.setTextSize(12);
            artist.setMaxLines(1);
            artist.setEllipsize(TextUtils.TruncateAt.END);
            textContainer.addView(artist);
            return new ViewHolder(row, artwork, title, artist);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            YouTubeClient.Track track = items.get(position);
            holder.title.setText(track.title);
            holder.artist.setText(track.artist);
            holder.artwork.setImageResource(android.R.drawable.ic_menu_gallery);
            holder.artwork.setTag(null);
            if (!TextUtils.isEmpty(track.artworkUrl)) {
                String artworkUrl = track.artworkUrl;
                holder.artwork.setTag(artworkUrl);
                imageExecutor.execute(() -> {
                    Bitmap bitmap = downloadBitmap(artworkUrl);
                    if (bitmap == null) return;
                    mainHandler.post(() -> {
                        Object tag = holder.artwork.getTag();
                        if (artworkUrl.equals(tag)) holder.artwork.setImageBitmap(bitmap);
                    });
                });
            }
            holder.itemView.setOnClickListener(v -> listener.onTrack(track));
        }

        @Override public int getItemCount() { return items.size(); }

        private static Bitmap downloadBitmap(String url) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(10000);
                connection.setRequestMethod("GET");
                connection.setInstanceFollowRedirects(true);
                try (InputStream inputStream = connection.getInputStream()) {
                    return BitmapFactory.decodeStream(inputStream);
                }
            } catch (Exception ignored) {
                return null;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }

        static final class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView artwork;
            final TextView title;
            final TextView artist;
            ViewHolder(@NonNull View itemView, ImageView artwork, TextView title, TextView artist) {
                super(itemView);
                this.artwork = artwork;
                this.title = title;
                this.artist = artist;
            }
        }
    }
}
