package com.urfavxbf.kanade.ui.youtube;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.urfavxbf.kanade.MusicPlayerController;
import com.urfavxbf.kanade.MusicRepository;
import com.urfavxbf.kanade.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class YouTubeFragment extends Fragment {

    private static final String DEFAULT_CACHE_PREFS = "youtube_default_cache";
    private static final String DEFAULT_CACHE_KEY = "tracks";
    private static final String DEFAULT_CACHE_TIME_KEY = "cached_at";
    private static final long DEFAULT_CACHE_TTL = 6L * 60L * 60L * 1000L;

    private EditText searchInput;
    private RecyclerView resultsRecycler;
    private TextView statusText;
    private Button playAllButton;
    private ResultAdapter adapter;
    private ExecutorService executor;
    private Handler mainHandler;
    private AtomicInteger searchGeneration;
    private AlertDialog loadingDialog;
    private ProgressBar loadingProgress;
    private TextView loadingMessage;
    private boolean loadingOperation;

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
        loadingOperation = false;
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
        if (searchInput == null || executor == null || loadingOperation) return;
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            loadDefaultTracks();
            return;
        }
        final int generation = searchGeneration.incrementAndGet();
        loadingOperation = true;
        showLoadingDialog("Searching YouTube", "Connecting to YouTube…", false, 0);
        statusText.setText("Searching YouTube…");
        playAllButton.setVisibility(View.GONE);
        playAllButton.setEnabled(false);
        adapter.setItems(new ArrayList<>());
        executor.execute(() -> {
            try {
                updateLoadingMessage("Searching for \"" + query + "\"…");
                ArrayList<YouTubeClient.Track> results = YouTubeClient.searchTracks(query);
                mainHandler.post(() -> {
                    if (!isCurrentSearch(generation)) return;
                    hideLoadingDialog();
                    loadingOperation = false;
                    adapter.setItems(results);
                    playAllButton.setVisibility(results.isEmpty() ? View.GONE : View.VISIBLE);
                    playAllButton.setEnabled(!results.isEmpty());
                    statusText.setText(results.isEmpty() ? "No matching YouTube tracks found."
                            : results.size() + " matching YouTube results");
                });
            } catch (Exception e) {
                String message = e.getMessage() == null || e.getMessage().trim().isEmpty()
                        ? "Unknown YouTube error" : e.getMessage();
                mainHandler.post(() -> {
                    if (!isCurrentSearch(generation)) return;
                    hideLoadingDialog();
                    loadingOperation = false;
                    playAllButton.setVisibility(View.GONE);
                    playAllButton.setEnabled(false);
                    statusText.setText("YouTube search failed: " + message);
                });
            }
        });
    }

    private void loadDefaultTracks() {
        if (searchInput == null || executor == null || loadingOperation) return;
        final int generation = searchGeneration.incrementAndGet();
        ArrayList<YouTubeClient.Track> cached = loadDefaultCache();
        if (!cached.isEmpty()) {
            adapter.setItems(cached);
            playAllButton.setVisibility(View.VISIBLE);
            playAllButton.setEnabled(true);
            statusText.setText("YouTube music recommendations");
        } else {
            statusText.setText("Loading YouTube music…");
            playAllButton.setVisibility(View.GONE);
            playAllButton.setEnabled(false);
            adapter.setItems(new ArrayList<>());
        }

        executor.execute(() -> {
            ArrayList<YouTubeClient.Track> results = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            String[] defaultQueries = {
                    "popular music",
                    "trending songs",
                    "new music 2026",
                    "top songs 2026"
            };

            for (String query : defaultQueries) {
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

            if (!results.isEmpty()) saveDefaultCache(results);

            mainHandler.post(() -> {
                if (!isCurrentSearch(generation)) return;
                if (!results.isEmpty()) {
                    adapter.setItems(results);
                    playAllButton.setVisibility(View.VISIBLE);
                    playAllButton.setEnabled(true);
                    statusText.setText("YouTube music recommendations");
                } else if (cached.isEmpty()) {
                    adapter.setItems(results);
                    playAllButton.setVisibility(View.GONE);
                    playAllButton.setEnabled(false);
                    statusText.setText("No YouTube recommendations available. Search for music above.");
                }
            });
        });
    }

    private ArrayList<YouTubeClient.Track> loadDefaultCache() {
        ArrayList<YouTubeClient.Track> tracks = new ArrayList<>();
        if (!isAdded()) return tracks;
        try {
            Context context = requireContext().getApplicationContext();
            android.content.SharedPreferences preferences = context.getSharedPreferences(
                    DEFAULT_CACHE_PREFS, Context.MODE_PRIVATE);
            String json = preferences.getString(DEFAULT_CACHE_KEY, "");
            long cachedAt = preferences.getLong(DEFAULT_CACHE_TIME_KEY, 0L);
            if (json.isEmpty() || cachedAt <= 0L || System.currentTimeMillis() - cachedAt > DEFAULT_CACHE_TTL) {
                return tracks;
            }

            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;
                String id = object.optString("id", "");
                if (id.isEmpty()) continue;
                tracks.add(new YouTubeClient.Track(
                        id,
                        object.optString("title", "Unknown title"),
                        object.optString("artist", "Unknown artist"),
                        object.optString("artworkUrl", ""),
                        object.optString("url", ""),
                        object.optInt("durationSeconds", 0)
                ));
            }
        } catch (Exception ignored) {
            tracks.clear();
        }
        return tracks;
    }

    private void saveDefaultCache(ArrayList<YouTubeClient.Track> tracks) {
        if (!isAdded() || tracks == null || tracks.isEmpty()) return;
        try {
            JSONArray array = new JSONArray();
            int count = Math.min(40, tracks.size());
            for (int i = 0; i < count; i++) {
                YouTubeClient.Track track = tracks.get(i);
                if (track == null || track.id.isEmpty()) continue;
                JSONObject object = new JSONObject();
                object.put("id", track.id);
                object.put("title", track.title);
                object.put("artist", track.artist);
                object.put("artworkUrl", track.artworkUrl);
                object.put("url", track.url);
                object.put("durationSeconds", track.durationSeconds);
                array.put(object);
            }
            requireContext().getApplicationContext()
                    .getSharedPreferences(DEFAULT_CACHE_PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putString(DEFAULT_CACHE_KEY, array.toString())
                    .putLong(DEFAULT_CACHE_TIME_KEY, System.currentTimeMillis())
                    .apply();
        } catch (Exception ignored) {
        }
    }

    private void playAll() {
        if (adapter == null || executor == null || adapter.items.isEmpty() || loadingOperation) return;
        ArrayList<YouTubeClient.Track> tracks = adapter.getItemsCopy();
        final int generation = searchGeneration.get();
        loadingOperation = true;
        playAllButton.setEnabled(false);
        showLoadingDialog("Preparing playlist", "Starting YouTube playlist…", true, tracks.size());
        statusText.setText("Preparing YouTube playlist…");
        executor.execute(() -> {
            try {
                ArrayList<String> playableUris = new ArrayList<>();
                Set<String> queuedIds = new HashSet<>();
                int total = tracks.size();
                int processed = 0;

                for (YouTubeClient.Track track : tracks) {
                    if (track == null || track.id.isEmpty() || queuedIds.contains(track.id)) {
                        processed++;
                        continue;
                    }
                    try {
                        updateLoadingProgress(processed, total,
                                "Resolving song " + (processed + 1) + " of " + total + "…");
                        String streamUrl = YouTubeClient.resolveStreamUrl(track.id);
                        if (streamUrl.isEmpty()) {
                            processed++;
                            continue;
                        }
                        MusicRepository.registerRemoteSong(
                                streamUrl,
                                track.title,
                                track.artist,
                                track.artworkUrl,
                                track.durationSeconds
                        );
                        playableUris.add(streamUrl);
                        queuedIds.add(track.id);
                    } catch (Exception ignored) {
                    }
                    processed++;
                    updateLoadingProgress(processed, total,
                            "Prepared " + processed + " of " + total + " songs");
                }

                if (playableUris.isEmpty()) {
                    mainHandler.post(() -> {
                        if (!isCurrentSearch(generation)) return;
                        hideLoadingDialog();
                        loadingOperation = false;
                        playAllButton.setEnabled(true);
                        statusText.setText("Play all failed: no playable YouTube songs");
                    });
                    return;
                }

                mainHandler.post(() -> updateLoadingMessage(
                        "Building queue with " + playableUris.size() + " playable songs…"));
                MusicPlayerController controller = new MusicPlayerController(requireContext());
                controller.clearQueue();
                for (int i = 0; i < playableUris.size(); i++) {
                    controller.addToQueue(playableUris.get(i));
                    final int queueProgress = i + 1;
                    updateLoadingProgress(queueProgress, playableUris.size(),
                            "Adding song " + queueProgress + " of " + playableUris.size() + " to queue…");
                }
                controller.playQueueItem(0);

                final int count = playableUris.size();
                mainHandler.post(() -> {
                    if (!isCurrentSearch(generation)) return;
                    hideLoadingDialog();
                    loadingOperation = false;
                    playAllButton.setEnabled(true);
                    statusText.setText("Playing YouTube playlist • " + count + " songs");
                });
            } catch (Exception e) {
                String message = e.getMessage() == null || e.getMessage().trim().isEmpty()
                        ? "Unable to prepare YouTube playlist" : e.getMessage();
                mainHandler.post(() -> {
                    if (!isCurrentSearch(generation)) return;
                    hideLoadingDialog();
                    loadingOperation = false;
                    playAllButton.setEnabled(true);
                    statusText.setText("Play all failed: " + message);
                });
            }
        });
    }

    private void playTrack(YouTubeClient.Track track) {
        if (!isAdded() || track == null || track.id.isEmpty() || executor == null || loadingOperation) return;
        loadingOperation = true;
        showLoadingDialog("Loading song", "Resolving YouTube stream…", false, 0);
        statusText.setText("Loading " + track.title + "…");
        executor.execute(() -> {
            try {
                updateLoadingMessage("Resolving audio stream for \"" + track.title + "\"…");
                String streamUrl = YouTubeClient.resolveStreamUrl(track.id);
                mainHandler.post(() -> {
                    if (!isAdded() || getView() == null) return;
                    hideLoadingDialog();
                    loadingOperation = false;
                    new MusicPlayerController(requireContext()).play(streamUrl);
                    statusText.setText("Playing in Kanade's player");
                });
            } catch (Exception e) {
                String message = e.getMessage() == null || e.getMessage().trim().isEmpty()
                        ? "Unable to resolve YouTube stream" : e.getMessage();
                mainHandler.post(() -> {
                    if (!isAdded() || getView() == null) return;
                    hideLoadingDialog();
                    loadingOperation = false;
                    statusText.setText("Playback failed: " + message);
                });
            }
        });
    }

    private void showLoadingDialog(String title, String message, boolean determinate, int max) {
        if (!isAdded() || getActivity() == null) return;
        mainHandler.post(() -> {
            if (!isAdded() || getActivity() == null) return;
            if (loadingDialog == null) {
                LinearLayout layout = new LinearLayout(requireContext());
                layout.setOrientation(LinearLayout.VERTICAL);
                layout.setPadding(48, 12, 48, 8);

                loadingMessage = new TextView(requireContext());
                loadingMessage.setTextSize(14);
                loadingMessage.setGravity(Gravity.START);
                layout.addView(loadingMessage, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                loadingProgress = new ProgressBar(requireContext(), null,
                        android.R.attr.progressBarStyleHorizontal);
                LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                progressParams.topMargin = 18;
                layout.addView(loadingProgress, progressParams);

                loadingDialog = new AlertDialog.Builder(requireContext())
                        .setTitle(title)
                        .setView(layout)
                        .setCancelable(false)
                        .create();
            } else {
                loadingDialog.setTitle(title);
            }

            loadingMessage.setText(message);
            loadingProgress.setIndeterminate(!determinate);
            if (determinate) {
                loadingProgress.setMax(Math.max(1, max));
                loadingProgress.setProgress(0);
            }
            if (!loadingDialog.isShowing()) loadingDialog.show();
        });
    }

    private void updateLoadingMessage(String message) {
        if (mainHandler == null) return;
        mainHandler.post(() -> {
            if (loadingDialog != null && loadingDialog.isShowing() && loadingMessage != null) {
                loadingMessage.setText(message);
            }
        });
    }

    private void updateLoadingProgress(int progress, int max, String message) {
        if (mainHandler == null) return;
        mainHandler.post(() -> {
            if (loadingDialog == null || !loadingDialog.isShowing() || loadingProgress == null) return;
            loadingProgress.setIndeterminate(false);
            loadingProgress.setMax(Math.max(1, max));
            loadingProgress.setProgress(Math.min(Math.max(0, progress), Math.max(1, max)));
            if (loadingMessage != null) loadingMessage.setText(message);
        });
    }

    private void hideLoadingDialog() {
        if (mainHandler == null) return;
        mainHandler.post(() -> {
            if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
        });
    }

    private boolean isCurrentSearch(int generation) {
        return isAdded() && getView() != null && searchGeneration != null
                && searchGeneration.get() == generation;
    }

    @Override
    public void onDestroyView() {
        if (searchGeneration != null) searchGeneration.incrementAndGet();
        loadingOperation = false;
        if (loadingDialog != null && loadingDialog.isShowing()) loadingDialog.dismiss();
        loadingDialog = null;
        loadingProgress = null;
        loadingMessage = null;
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audio, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            YouTubeClient.Track track = items.get(position);
            holder.title.setText(TextUtils.isEmpty(track.title) ? "Unknown title" : track.title);
            holder.artist.setText(TextUtils.isEmpty(track.artist) ? "Unknown artist" : track.artist);
            holder.album.setText("YouTube");
            holder.favorite.setVisibility(View.GONE);
            holder.more.setVisibility(View.GONE);
            holder.albumArt.setImageResource(android.R.drawable.ic_media_play);
            holder.albumArt.setTag(null);

            if (!TextUtils.isEmpty(track.artworkUrl)) {
                String artworkUrl = track.artworkUrl;
                holder.albumArt.setTag(artworkUrl);
                imageExecutor.execute(() -> {
                    Bitmap bitmap = downloadBitmap(artworkUrl);
                    if (bitmap == null) return;
                    mainHandler.post(() -> {
                        Object tag = holder.albumArt.getTag();
                        if (artworkUrl.equals(tag)) holder.albumArt.setImageBitmap(bitmap);
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
            final ImageView albumArt;
            final ImageView more;
            final android.widget.ImageButton favorite;
            final TextView title;
            final TextView artist;
            final TextView album;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                albumArt = itemView.findViewById(R.id.imgAlbumArt);
                title = itemView.findViewById(R.id.txtTitle);
                artist = itemView.findViewById(R.id.txtArtist);
                album = itemView.findViewById(R.id.txtAlbum);
                more = itemView.findViewById(R.id.btnMore);
                favorite = itemView.findViewById(R.id.btnFave);
            }
        }
    }
}
