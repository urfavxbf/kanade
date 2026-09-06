package com.urfavxbf.kanade.ui.audius;

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
import com.urfavxbf.kanade.R;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class AudiusFragment extends Fragment {

    private EditText searchInput;
    private RecyclerView resultsRecycler;
    private TextView statusText;
    private ResultAdapter adapter;
    private ExecutorService executor;
    private Handler mainHandler;
    private AtomicInteger searchGeneration;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_audius, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchInput = view.findViewById(R.id.audiusSearchInput);
        resultsRecycler = view.findViewById(R.id.audiusResultsRecycler);
        statusText = view.findViewById(R.id.audiusStatusText);

        mainHandler = new Handler(Looper.getMainLooper());
        searchGeneration = new AtomicInteger();
        executor = Executors.newFixedThreadPool(3, runnable -> {
            Thread thread = new Thread(runnable, "Kanade-Audius");
            thread.setDaemon(true);
            return thread;
        });

        adapter = new ResultAdapter(this::playTrack, executor, mainHandler);
        resultsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        resultsRecycler.setAdapter(adapter);

        view.findViewById(R.id.audiusSearchButton).setOnClickListener(v -> search());
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                search();
                return true;
            }
            return false;
        });

        statusText.setText("Search Audius music");
    }

    private void search() {
        if (searchInput == null || executor == null) {
            return;
        }

        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            searchInput.setError("Enter a song or artist");
            return;
        }

        final int generation = searchGeneration.incrementAndGet();
        statusText.setText("Searching Audius…");
        adapter.setItems(new ArrayList<>());

        executor.execute(() -> {
            try {
                ArrayList<AudiusClient.Track> results = AudiusClient.searchTracks(query);
                mainHandler.post(() -> {
                    if (!isCurrentSearch(generation)) {
                        return;
                    }
                    adapter.setItems(results);
                    statusText.setText(results.isEmpty()
                            ? "No matching Audius tracks found."
                            : results.size() + " matching results");
                });
            } catch (Exception e) {
                String message = e.getMessage() == null || e.getMessage().trim().isEmpty()
                        ? "Unknown Audius error"
                        : e.getMessage();
                mainHandler.post(() -> {
                    if (!isCurrentSearch(generation)) {
                        return;
                    }
                    statusText.setText("Audius search failed: " + message);
                });
            }
        });
    }

    private boolean isCurrentSearch(int generation) {
        return isAdded()
                && getView() != null
                && searchGeneration != null
                && searchGeneration.get() == generation;
    }

    private void playTrack(AudiusClient.Track track) {
        if (!isAdded() || track == null || track.id.isEmpty() || executor == null) {
            return;
        }

        statusText.setText("Loading " + track.title + "…");

        executor.execute(() -> {
            try {
                String streamUrl = AudiusClient.resolveStreamUrl(track.id);
                mainHandler.post(() -> {
                    if (!isAdded() || getView() == null) {
                        return;
                    }
                    new MusicPlayerController(requireContext()).play(streamUrl);
                    statusText.setText("Playing in Kanade's player");
                });
            } catch (Exception e) {
                String message = e.getMessage() == null || e.getMessage().trim().isEmpty()
                        ? "Unable to resolve Audius stream"
                        : e.getMessage();
                mainHandler.post(() -> {
                    if (!isAdded() || getView() == null) {
                        return;
                    }
                    statusText.setText("Playback failed: " + message);
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        if (searchGeneration != null) {
            searchGeneration.incrementAndGet();
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        adapter = null;
        resultsRecycler = null;
        searchInput = null;
        statusText = null;
        super.onDestroyView();
    }

    private static final class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {

        interface Listener {
            void onTrack(AudiusClient.Track track);
        }

        private final ArrayList<AudiusClient.Track> items = new ArrayList<>();
        private final Listener listener;
        private final ExecutorService imageExecutor;
        private final Handler mainHandler;

        ResultAdapter(
                Listener listener,
                ExecutorService imageExecutor,
                Handler mainHandler) {
            this.listener = listener;
            this.imageExecutor = imageExecutor;
            this.mainHandler = mainHandler;
        }

        void setItems(ArrayList<AudiusClient.Track> newItems) {
            items.clear();
            if (newItems != null) {
                items.addAll(newItems);
            }
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(
                @NonNull ViewGroup parent,
                int viewType) {
            int padding = Math.round(10 * parent.getResources().getDisplayMetrics().density);
            float density = parent.getResources().getDisplayMetrics().density;

            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(padding, padding, padding, padding);

            ImageView artwork = new ImageView(parent.getContext());
            artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);
            row.addView(artwork, new LinearLayout.LayoutParams(
                    Math.round(64 * density), Math.round(64 * density)));

            LinearLayout textContainer = new LinearLayout(parent.getContext());
            textContainer.setOrientation(LinearLayout.VERTICAL);
            textContainer.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
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
        public void onBindViewHolder(
                @NonNull ViewHolder holder,
                int position) {
            AudiusClient.Track track = items.get(position);
            holder.title.setText(track.title);
            holder.artist.setText(track.artist);
            holder.artwork.setImageResource(android.R.drawable.ic_menu_gallery);
            holder.artwork.setTag(null);

            if (!TextUtils.isEmpty(track.artworkUrl)) {
                String artworkUrl = track.artworkUrl;
                holder.artwork.setTag(artworkUrl);
                imageExecutor.execute(() -> {
                    Bitmap bitmap = downloadBitmap(artworkUrl);
                    if (bitmap == null) {
                        return;
                    }
                    mainHandler.post(() -> {
                        Object tag = holder.artwork.getTag();
                        if (artworkUrl.equals(tag)) {
                            holder.artwork.setImageBitmap(bitmap);
                        }
                    });
                });
            }

            holder.itemView.setOnClickListener(v -> listener.onTrack(track));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

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
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        static final class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView artwork;
            final TextView title;
            final TextView artist;

            ViewHolder(
                    @NonNull View itemView,
                    ImageView artwork,
                    TextView title,
                    TextView artist) {
                super(itemView);
                this.artwork = artwork;
                this.title = title;
                this.artist = artist;
            }
        }
    }
}
