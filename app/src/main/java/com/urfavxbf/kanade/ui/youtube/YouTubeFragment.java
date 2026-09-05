package com.urfavxbf.kanade.ui.youtube;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
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
    private CheckBox audioOnlyCheck;
    private View playerFrame;
    private View miniPlayer;
    private ImageView miniThumbnail;
    private TextView miniTitle;
    private TextView miniArtist;
    private ImageButton miniPlayPause;
    private ImageButton miniExpand;
    private ResultAdapter adapter;
    private ExecutorService searchExecutor;
    private Handler mainHandler;
    private boolean isYouTubePlaying;
    private boolean isAudioOnly;
    private String currentVideoId = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_youtube, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        searchInput = view.findViewById(R.id.youtubeSearchInput);
        resultsRecycler = view.findViewById(R.id.youtubeResultsRecycler);
        playerWebView = view.findViewById(R.id.youtubePlayerWebView);
        playerTitle = view.findViewById(R.id.youtubePlayerTitle);
        statusText = view.findViewById(R.id.youtubeStatusText);
        audioOnlyCheck = view.findViewById(R.id.youtubeAudioOnlyCheck);
        playerFrame = view.findViewById(R.id.youtubePlayerFrame);
        miniPlayer = view.findViewById(R.id.youtubeMiniPlayer);
        miniThumbnail = view.findViewById(R.id.youtubeMiniThumbnail);
        miniTitle = view.findViewById(R.id.youtubeMiniTitle);
        miniArtist = view.findViewById(R.id.youtubeMiniArtist);
        miniPlayPause = view.findViewById(R.id.youtubeMiniPlayPause);
        miniExpand = view.findViewById(R.id.youtubeMiniExpand);

        mainHandler = new Handler(Looper.getMainLooper());
        searchExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Kanade-YouTube-Search");
            thread.setDaemon(true);
            return thread;
        });
        adapter = new ResultAdapter(this::playVideo, searchExecutor, mainHandler);
        resultsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        resultsRecycler.setAdapter(adapter);

        View globalMiniRoot = requireActivity().findViewById(R.id.bottomPart);
        View globalMini = globalMiniRoot == null ? null : globalMiniRoot.findViewById(R.id.miniPlayerRoot);
        ImageView globalThumb = globalMiniRoot == null ? null : globalMiniRoot.findViewById(R.id.miniAlbumArt);
        TextView globalTitle = globalMiniRoot == null ? null : globalMiniRoot.findViewById(R.id.miniTitle);
        TextView globalArtist = globalMiniRoot == null ? null : globalMiniRoot.findViewById(R.id.miniArtist);
        ImageButton globalPlayPause = globalMiniRoot == null ? null : globalMiniRoot.findViewById(R.id.miniPlayPause);
        if (globalMini != null && globalThumb != null && globalTitle != null && globalArtist != null
                && globalPlayPause != null) {
            YouTubePlaybackManager.setMiniViews(globalMini, globalThumb, globalTitle, globalArtist, globalPlayPause);
            android.content.Context appContext = requireContext().getApplicationContext();
            globalPlayPause.setOnClickListener(v -> {
                if (YouTubePlaybackManager.isActive()) {
                    YouTubePlaybackManager.toggle();
                } else {
                    android.content.Intent intent = new android.content.Intent(appContext,
                            com.urfavxbf.kanade.MusicPlayerService.class);
                    intent.setAction(com.urfavxbf.kanade.MusicPlayerService.ACTION_PLAY);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        androidx.core.content.ContextCompat.startForegroundService(appContext, intent);
                    } else {
                        appContext.startService(intent);
                    }
                }
            });
        }

        audioOnlyCheck.setOnCheckedChangeListener((buttonView, checked) -> {
            isAudioOnly = checked;
            YouTubePlaybackManager.setAudioOnly(checked);
            if (checked) {
                miniPlayer.setVisibility(View.VISIBLE);
                statusText.setText("Audio only • YouTube playback");
            } else {
                miniPlayer.setVisibility(View.GONE);
                statusText.setText("YouTube video");
            }
        });

        miniPlayPause.setOnClickListener(v -> YouTubePlaybackManager.toggle());
        miniExpand.setOnClickListener(v -> showVideoPlayer());
        miniPlayer.setOnClickListener(v -> showVideoPlayer());

        view.findViewById(R.id.youtubeSearchButton).setOnClickListener(v -> searchOrPlayDirectLink());
        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchOrPlayDirectLink();
                return true;
            }
            return false;
        });

        if (YouTubePlaybackManager.isActive()) {
            playerFrame.setVisibility(View.VISIBLE);
            YouTubePlaybackManager.attachTo(playerFrame);
            isAudioOnly = false;
            miniPlayer.setVisibility(View.GONE);
        }
    }

    private void searchOrPlayDirectLink() {
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            searchInput.setError("Enter a song or artist");
            return;
        }
        String videoId = extractVideoId(query);
        if (videoId != null) {
            playVideo(new YouTubeResult(videoId, "YouTube video", "", ""));
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
                String endpoint = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video"
                        + "&videoCategoryId=10&videoEmbeddable=true&videoSyndicated=true&maxResults=15&q="
                        + encodedQuery + "&key="
                        + URLEncoder.encode(BuildConfig.YOUTUBE_API_KEY, StandardCharsets.UTF_8.name());
                connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                int responseCode = connection.getResponseCode();
                InputStream stream = responseCode >= 200 && responseCode < 300
                        ? connection.getInputStream() : connection.getErrorStream();
                if (stream == null) throw new IllegalStateException("YouTube returned no response");
                String response = readResponse(stream);
                if (responseCode < 200 || responseCode >= 300) {
                    JSONObject error = new JSONObject(response).optJSONObject("error");
                    throw new IllegalStateException(error == null ? "YouTube search failed"
                            : error.optString("message", "YouTube search failed"));
                }
                JSONArray items = new JSONObject(response).optJSONArray("items");
                if (items != null) {
                    for (int i = 0; i < items.length(); i++) {
                        JSONObject item = items.optJSONObject(i);
                        if (item == null) continue;
                        JSONObject id = item.optJSONObject("id");
                        JSONObject snippet = item.optJSONObject("snippet");
                        if (id == null || snippet == null) continue;
                        String videoId = id.optString("videoId", "").trim();
                        String title = snippet.optString("title", "").trim();
                        String channel = snippet.optString("channelTitle", "").trim();
                        String thumbnail = "";
                        JSONObject thumbnails = snippet.optJSONObject("thumbnails");
                        if (thumbnails != null) {
                            JSONObject medium = thumbnails.optJSONObject("medium");
                            JSONObject high = thumbnails.optJSONObject("high");
                            JSONObject selected = medium != null ? medium : high;
                            if (selected != null) thumbnail = selected.optString("url", "").trim();
                        }
                        if (!videoId.isEmpty() && !title.isEmpty()) {
                            results.add(new YouTubeResult(videoId, title, channel, thumbnail));
                        }
                    }
                }
                ArrayList<YouTubeResult> finalResults = results;
                mainHandler.post(() -> {
                    if (!isAdded() || getView() == null) return;
                    adapter.setItems(finalResults);
                    statusText.setText(finalResults.isEmpty() ? "No YouTube music results found."
                            : finalResults.size() + " results");
                });
            } catch (Exception e) {
                String message = TextUtils.isEmpty(e.getMessage()) ? "Unknown YouTube error" : e.getMessage();
                mainHandler.post(() -> {
                    if (!isAdded() || getView() == null) return;
                    statusText.setText("YouTube search failed: " + message);
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    private void playVideo(YouTubeResult result) {
        if (!isAdded() || result == null || result.videoId.isEmpty()) return;
        currentVideoId = result.videoId;
        playerTitle.setText(result.title);
        playerFrame.setVisibility(View.VISIBLE);
        playerTitle.setVisibility(View.VISIBLE);
        statusText.setText(result.channel.isEmpty() ? "Opening YouTube player…" : result.channel);
        isAudioOnly = audioOnlyCheck.isChecked();
        YouTubePlaybackManager.play(requireContext(), result.videoId, result.title, result.channel,
                result.thumbnailUrl, isAudioOnly);
        YouTubePlaybackManager.attachTo(playerFrame);
        if (isAudioOnly) {
            miniPlayer.setVisibility(View.VISIBLE);
            statusText.setText("Audio only • YouTube playback");
        } else {
            miniPlayer.setVisibility(View.GONE);
        }
    }

    private void showVideoPlayer() {
        isAudioOnly = false;
        audioOnlyCheck.setOnCheckedChangeListener(null);
        audioOnlyCheck.setChecked(false);
        audioOnlyCheck.setOnCheckedChangeListener((buttonView, checked) -> {
            isAudioOnly = checked;
            YouTubePlaybackManager.setAudioOnly(checked);
            miniPlayer.setVisibility(checked ? View.VISIBLE : View.GONE);
        });
        YouTubePlaybackManager.showVideo();
        YouTubePlaybackManager.attachTo(playerFrame);
        playerWebView.setVisibility(View.GONE);
        miniPlayer.setVisibility(View.GONE);
        if (!currentVideoId.isEmpty()) statusText.setText("YouTube video");
    }

    private String extractVideoId(String value) {
        try {
            Uri uri = Uri.parse(value);
            String host = uri.getHost();
            if (host == null) return null;
            if (host.equalsIgnoreCase("youtu.be")) {
                String path = uri.getPath();
                return validVideoId(path == null ? null : path.replace("/", ""));
            }
            if (host.equalsIgnoreCase("youtube.com") || host.equalsIgnoreCase("www.youtube.com")
                    || host.equalsIgnoreCase("m.youtube.com") || host.equalsIgnoreCase("music.youtube.com")) {
                String id = uri.getQueryParameter("v");
                if (id != null) return validVideoId(id);
                String path = uri.getPath();
                if (path != null && path.startsWith("/shorts/")) return validVideoId(path.substring(8));
                if (path != null && path.startsWith("/embed/")) return validVideoId(path.substring(7));
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private String validVideoId(String value) {
        if (value == null || value.length() != 11) return null;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '-' || c == '_')) return null;
        }
        return value;
    }

    private String readResponse(InputStream inputStream) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) builder.append(line);
        }
        return builder.toString();
    }

    @Override
    public void onDestroyView() {
        if (playerWebView != null) {
            View parent = (View) playerWebView.getParent();
            if (parent instanceof ViewGroup) ((ViewGroup) parent).removeView(playerWebView);
            playerWebView.setVisibility(View.GONE);
        }
        if (YouTubePlaybackManager.isActive()) {
            View globalHost = requireActivity().findViewById(R.id.youtubeGlobalPlayerHost);
            YouTubePlaybackManager.moveToGlobalHost(globalHost);
        }
        if (searchExecutor != null) {
            searchExecutor.shutdownNow();
            searchExecutor = null;
        }
        if (mainHandler != null) mainHandler.removeCallbacksAndMessages(null);
        adapter = null;
        resultsRecycler = null;
        searchInput = null;
        playerTitle = null;
        statusText = null;
        audioOnlyCheck = null;
        playerFrame = null;
        miniPlayer = null;
        miniThumbnail = null;
        miniTitle = null;
        miniArtist = null;
        miniPlayPause = null;
        miniExpand = null;
        super.onDestroyView();
    }

    private static final class YouTubeResult {
        final String videoId;
        final String title;
        final String channel;
        final String thumbnailUrl;
        YouTubeResult(String videoId, String title, String channel, String thumbnailUrl) {
            this.videoId = videoId;
            this.title = title;
            this.channel = channel;
            this.thumbnailUrl = thumbnailUrl;
        }
    }

    private static final class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.ViewHolder> {
        interface Listener { void onResult(YouTubeResult result); }
        private final ArrayList<YouTubeResult> items = new ArrayList<>();
        private final Listener listener;
        private final ExecutorService imageExecutor;
        private final Handler mainHandler;

        ResultAdapter(Listener listener, ExecutorService imageExecutor, Handler mainHandler) {
            this.listener = listener;
            this.imageExecutor = imageExecutor;
            this.mainHandler = mainHandler;
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
            row.setOrientation(LinearLayoutCompat.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            int padding = Math.round(10 * parent.getResources().getDisplayMetrics().density);
            row.setPadding(padding, padding, padding, padding);
            ImageView thumbnail = new ImageView(parent.getContext());
            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            row.addView(thumbnail, new LinearLayoutCompat.LayoutParams(
                    Math.round(120 * parent.getResources().getDisplayMetrics().density),
                    Math.round(68 * parent.getResources().getDisplayMetrics().density)));
            LinearLayoutCompat textContainer = new LinearLayoutCompat(parent.getContext());
            textContainer.setOrientation(LinearLayoutCompat.VERTICAL);
            textContainer.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayoutCompat.LayoutParams textParams = new LinearLayoutCompat.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            textParams.leftMargin = Math.round(12 * parent.getResources().getDisplayMetrics().density);
            row.addView(textContainer, textParams);
            TextView title = new TextView(parent.getContext());
            title.setTextColor(Color.WHITE);
            title.setTextSize(15);
            title.setMaxLines(2);
            title.setEllipsize(TextUtils.TruncateAt.END);
            textContainer.addView(title);
            TextView channel = new TextView(parent.getContext());
            channel.setTextColor(Color.rgb(155, 157, 170));
            channel.setTextSize(12);
            channel.setMaxLines(1);
            channel.setEllipsize(TextUtils.TruncateAt.END);
            textContainer.addView(channel);
            return new ViewHolder(row, thumbnail, title, channel);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            YouTubeResult result = items.get(position);
            holder.title.setText(result.title);
            holder.channel.setText(result.channel);
            holder.itemView.setOnClickListener(v -> listener.onResult(result));
            holder.thumbnail.setTag(result.thumbnailUrl);
            holder.thumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
            if (result.thumbnailUrl.isEmpty()) return;
            imageExecutor.execute(() -> {
                Bitmap bitmap = loadThumbnail(result.thumbnailUrl);
                if (bitmap == null) return;
                mainHandler.post(() -> {
                    Object tag = holder.thumbnail.getTag();
                    if (result.thumbnailUrl.equals(tag)) holder.thumbnail.setImageBitmap(bitmap);
                });
            });
        }

        private Bitmap loadThumbnail(String url) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(8000);
                connection.setReadTimeout(10000);
                connection.setRequestMethod("GET");
                try (InputStream inputStream = connection.getInputStream()) {
                    return BitmapFactory.decodeStream(inputStream);
                }
            } catch (Exception ignored) {
                return null;
            } finally {
                if (connection != null) connection.disconnect();
            }
        }

        @Override public int getItemCount() { return items.size(); }

        static final class ViewHolder extends RecyclerView.ViewHolder {
            final ImageView thumbnail;
            final TextView title;
            final TextView channel;
            ViewHolder(View itemView, ImageView thumbnail, TextView title, TextView channel) {
                super(itemView);
                this.thumbnail = thumbnail;
                this.title = title;
                this.channel = channel;
            }
        }
    }

    private static final class LinearLayoutCompat extends android.widget.LinearLayout {
        LinearLayoutCompat(android.content.Context context) { super(context); }
    }
}
