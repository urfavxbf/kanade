package com.urfavxbf.kanade.ui.youtube;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.urfavxbf.kanade.BuildConfig;
import com.urfavxbf.kanade.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class YouTubeFragment extends Fragment {

    private EditText searchInput;
    private RecyclerView resultsRecycler;
    private WebView playerWebView;
    private TextView playerTitle;
    private TextView statusText;
    private ResultAdapter adapter;
    private ExecutorService searchExecutor;
    private Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_youtube, container, false);
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchInput = view.findViewById(R.id.youtubeSearchInput);
        resultsRecycler = view.findViewById(R.id.youtubeResultsRecycler);
        playerWebView = view.findViewById(R.id.youtubePlayerWebView);
        playerTitle = view.findViewById(R.id.youtubePlayerTitle);
        statusText = view.findViewById(R.id.youtubeStatusText);

        mainHandler = new Handler(Looper.getMainLooper());
        searchExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Kanade-YouTube-Search");
            thread.setDaemon(true);
            return thread;
        });

        adapter = new ResultAdapter(this::playVideo);
        resultsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        resultsRecycler.setAdapter(adapter);

        configurePlayer();

        view.findViewById(R.id.youtubeSearchButton).setOnClickListener(
                v -> searchOrPlayDirectLink());

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchOrPlayDirectLink();
                return true;
            }
            return false;
        });
    }

    private void searchOrPlayDirectLink() {
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            searchInput.setError("Enter a song or artist");
            return;
        }

        String videoId = extractVideoId(query);
        if (videoId != null) {
            playVideo(new YouTubeResult(videoId, "YouTube video", ""));
            return;
        }

        searchYouTube(query);
    }

    private void searchYouTube(String query) {
        if (TextUtils.isEmpty(BuildConfig.YOUTUBE_API_KEY)) {
            statusText.setText("YouTube search is not configured. Add YOUTUBE_API_KEY to local.properties.");
            Toast.makeText(requireContext(), "YouTube API key is missing", Toast.LENGTH_SHORT).show();
            return;
        }

        statusText.setText("Searching YouTube…");
        adapter.setItems(new ArrayList<>());

        searchExecutor.execute(() -> {
            ArrayList<YouTubeResult> results = new ArrayList<>();
            HttpURLConnection connection = null;

            try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name());
                String endpoint = "https://www.googleapis.com/youtube/v3/search"
                        + "?part=snippet"
                        + "&type=video"
                        + "&videoCategoryId=10"
                        + "&maxResults=15"
                        + "&q=" + encodedQuery
                        + "&key=" + URLEncoder.encode(
                        BuildConfig.YOUTUBE_API_KEY,
                        StandardCharsets.UTF_8.name());

                connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");

                int responseCode = connection.getResponseCode();
                InputStream stream = responseCode >= 200 && responseCode < 300
                        ? connection.getInputStream()
                        : connection.getErrorStream();

                if (stream == null) {
                    throw new IllegalStateException("YouTube returned no response");
                }

                String response = readResponse(stream);
                if (responseCode < 200 || responseCode >= 300) {
                    JSONObject error = new JSONObject(response);
                    JSONObject errorObject = error.optJSONObject("error");
                    String message = errorObject == null
                            ? "YouTube search failed"
                            : errorObject.optString("message", "YouTube search failed");
                    throw new IllegalStateException(message);
                }

                JSONArray items = new JSONObject(response).optJSONArray("items");
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.optJSONObject(i);
                        if (item == null) {
                            continue;
                        }

                        JSONObject id = item.optJSONObject("id");
                        JSONObject snippet = item.optJSONObject("snippet");
                        if (id == null || snippet == null) {
                            continue;
                        }

                        String videoId = id.optString("videoId", "").trim();
                        String title = snippet.optString("title", "").trim();
                        String channel = snippet.optString("channelTitle", "").trim();

                        if (!videoId.isEmpty() && !title.isEmpty()) {
                            results.add(new YouTubeResult(videoId, title, channel));
                        }
                    }
                }

                mainHandler.post(() -> {
                    if (!isAdded() || getView() == null) {
                        return;
                    }
                    adapter.setItems(results);
                    statusText.setText(results.isEmpty()
                            ? "No YouTube music results found."
                            : results.size() + " results");
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (!isAdded() || getView() == null) {
                        return;
                    }
                    statusText.setText("YouTube search failed: " + e.getMessage());
                });
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configurePlayer() {
        WebSettings settings = playerWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);

        playerWebView.setBackgroundColor(Color.BLACK);
        playerWebView.setWebViewClient(new WebViewClient());
    }

    private void playVideo(YouTubeResult result) {
        if (!isAdded() || result == null || result.videoId.isEmpty()) {
            return;
        }

        playerTitle.setText(result.title);
        playerWebView.setVisibility(View.VISIBLE);
        playerTitle.setVisibility(View.VISIBLE);

        String url = "https://www.youtube.com/embed/" + result.videoId
                + "?playsinline=1&rel=0&controls=1";
        playerWebView.loadUrl(url);

        statusText.setText(result.channel.isEmpty()
                ? "Now playing on YouTube"
                : result.channel);
    }

    private String extractVideoId(String value) {
        try {
            Uri uri = Uri.parse(value);
            String host = uri.getHost();
            if (host == null) {
                return null;
            }

            if (host.equalsIgnoreCase("youtu.be")) {
                String id = uri.getPath();
                return validVideoId(id == null ? null : id.replace("/", ""));
            }

            if (host.equalsIgnoreCase("youtube.com")
                    || host.equalsIgnoreCase("www.youtube.com")
                    || host.equalsIgnoreCase("m.youtube.com")) {
                String id = uri.getQueryParameter("v");
                if (id != null) {
                    return validVideoId(id);
                }

                String path = uri.getPath();
                if (path != null && path.startsWith("/shorts/")) {
                    return validVideoId(path.substring("/shorts/".length()));
                }
                if (path != null && path.startsWith("/embed/")) {
                    return validVideoId(path.substring("/embed/".length()));
                }
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String validVideoId(String value) {
        if (value == null || value.length() != 11) {
            return null;
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_')) {
                return null;
            }
        }
        return value;
    }

    private String readResponse(InputStream inputStream) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    @Override
    public void onDestroyView() {
        if (playerWebView != null) {
            playerWebView.stopLoading();
            playerWebView.loadUrl("about:blank");
            playerWebView.destroy();
        }
        if (searchExecutor != null) {
            searchExecutor.shutdownNow();
        }
        if (mainHandler != null) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        super.onDestroyView();
    }

    private static final class YouTubeResult {
        final String videoId;
        final String title;
        final String channel;

        YouTubeResult(String videoId, String title, String channel) {
            this.videoId = videoId;
            this.title = title;
            this.channel = channel;
        }
    }

    private static final class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {
        interface Listener {
            void onResult(YouTubeResult result);
        }

        private final ArrayList<YouTubeResult> items = new ArrayList<>();
        private final Listener listener;

        ResultAdapter(Listener listener) {
            this.listener = listener;
        }

        void setItems(ArrayList<YouTubeResult> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayoutCompat row = new LinearLayoutCompat(parent.getContext());
            row.setOrientation(LinearLayoutCompat.VERTICAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int padding = Math.round(14 * parent.getResources().getDisplayMetrics().density);
            row.setPadding(padding, padding, padding, padding);

            TextView title = new TextView(parent.getContext());
            title.setTextColor(Color.WHITE);
            title.setTextSize(15);
            title.setMaxLines(2);
            title.setEllipsize(TextUtils.TruncateAt.END);
            row.addView(title, new LinearLayoutCompat.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            TextView channel = new TextView(parent.getContext());
            channel.setTextColor(Color.rgb(155, 157, 170));
            channel.setTextSize(12);
            LinearLayoutCompat.LayoutParams channelParams = new LinearLayoutCompat.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            channelParams.topMargin = Math.round(4 * parent.getResources().getDisplayMetrics().density);
            row.addView(channel, channelParams);

            return new ViewHolder(row, title, channel);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            YouTubeResult result = items.get(position);
            holder.title.setText(result.title);
            holder.channel.setText(result.channel);
            holder.itemView.setOnClickListener(v -> listener.onResult(result));
            holder.itemView.setContentDescription("Play " + result.title);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static final class ViewHolder extends RecyclerView.ViewHolder {
            final TextView title;
            final TextView channel;

            ViewHolder(View itemView, TextView title, TextView channel) {
                super(itemView);
                this.title = title;
                this.channel = channel;
            }
        }
    }

    private static final class LinearLayoutCompat extends android.widget.LinearLayout {
        LinearLayoutCompat(android.content.Context context) {
            super(context);
        }
    }
}
