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
import android.util.LruCache;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
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
        configurePlayer();

        audioOnlyCheck.setOnCheckedChangeListener((buttonView, checked) -> {
            isAudioOnly = checked;
            updatePlayerMode();
        });

        miniPlayPause.setOnClickListener(v -> toggleYouTubePlayback());
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
                String endpoint = "https://www.googleapis.com/youtube/v3/search"
                        + "?part=snippet&type=video&videoCategoryId=10"
                        + "&videoEmbeddable=true&videoSyndicated=true&maxResults=15"
                        + "&q=" + encodedQuery + "&key="
                        + URLEncoder.encode(BuildConfig.YOUTUBE_API_KEY, StandardCharsets.UTF_8.name());
                connection = (HttpURLConnection) new URL(endpoint).openConnection();
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/json");
                int responseCode = connection.getResponseCode();
                InputStream stream = responseCode >= 200 && responseCode < 300
                        ? connection.getInputStream() : connection.getErrorStream();
                if (stream == null) {
                    throw new IllegalStateException("YouTube returned no response");
                }
                String response = readResponse(stream);
                if (responseCode < 200 || responseCode >= 300) {
                    JSONObject error = new JSONObject(response);
                    JSONObject errorObject = error.optJSONObject("error");
                    String message = errorObject == null ? "YouTube search failed"
                            : errorObject.optString("message", "YouTube search failed");
                    throw new IllegalStateException(message);
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
                        String thumbnailUrl = "";
                        JSONObject thumbnails = snippet.optJSONObject("thumbnails");
                        if (thumbnails != null) {
                            JSONObject medium = thumbnails.optJSONObject("medium");
                            JSONObject high = thumbnails.optJSONObject("high");
                            JSONObject selected = medium != null ? medium : high;
                            if (selected != null) thumbnailUrl = selected.optString("url", "").trim();
                        }
                        if (!videoId.isEmpty() && !title.isEmpty()) {
                            results.add(new YouTubeResult(videoId, title, channel, thumbnailUrl));
                        }
                    }
                }
                final ArrayList<YouTubeResult> finalResults = results;
                mainHandler.post(() -> {
                    if (!isAdded() || getView() == null) return;
                    adapter.setItems(finalResults);
                    statusText.setText(finalResults.isEmpty()
                            ? "No YouTube music results found." : finalResults.size() + " results");
                });
            } catch (Exception e) {
                String message = e.getMessage();
                if (TextUtils.isEmpty(message)) message = "Unknown YouTube error";
                final String finalMessage = message;
                mainHandler.post(() -> {
                    if (!isAdded() || getView() == null) return;
                    statusText.setText("YouTube search failed: " + finalMessage);
                });
            } finally {
                if (connection != null) connection.disconnect();
            }
        });
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void configurePlayer() {
        WebSettings settings = playerWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        playerWebView.setBackgroundColor(Color.BLACK);
        playerWebView.addJavascriptInterface(new YouTubeBridge(), "KanadePlayer");
        playerWebView.setWebViewClient(new WebViewClient());
    }

    private void playVideo(YouTubeResult result) {
        if (!isAdded() || result == null || result.videoId.isEmpty()) return;

        currentVideoId = result.videoId;
        isYouTubePlaying = false;
        playerTitle.setText(result.title);
        playerFrame.setVisibility(View.VISIBLE);
        playerTitle.setVisibility(View.VISIBLE);
        miniTitle.setText(result.title);
        miniArtist.setText(TextUtils.isEmpty(result.channel) ? "YouTube" : result.channel);
        miniPlayer.setVisibility(View.GONE);
        miniPlayPause.setImageResource(R.drawable.ic_pause);
        statusText.setText(result.channel.isEmpty() ? "Opening YouTube player…" : result.channel);

        if (!result.thumbnailUrl.isEmpty()) {
            loadMiniThumbnail(result.thumbnailUrl);
        } else {
            miniThumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        String escapedVideoId = TextUtils.htmlEncode(result.videoId);
        String playerHtml = "<!doctype html><html><head>"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
                + "<style>html,body,#player{margin:0;padding:0;background:#000;width:100%;height:100%;overflow:hidden;}"
                + "iframe{border:0;width:100%;height:100%;display:block;}</style></head><body>"
                + "<div id=\"player\"></div>"
                + "<script>"
                + "var player;"
                + "function onYouTubeIframeAPIReady(){"
                + "player=new YT.Player('player',{height:'100%',width:'100%',videoId:'" + escapedVideoId + "',"
                + "playerVars:{playsinline:1,rel:0,controls:1,enablejsapi:1,origin:'https://com.urfavxbf.kanade'},"
                + "events:{onReady:onPlayerReady,onStateChange:onPlayerStateChange,onError:onPlayerError}});"
                + "}"
                + "function onPlayerReady(){KanadePlayer.ready();}"
                + "function onPlayerStateChange(e){KanadePlayer.state(e.data);}"
                + "function onPlayerError(e){KanadePlayer.error(e.data);}"
                + "function playYT(){if(player)player.playVideo();}"
                + "function pauseYT(){if(player)player.pauseVideo();}"
                + "</script>"
                + "<script src=\"https://www.youtube.com/iframe_api\"></script>"
                + "</body></html>";

        playerWebView.setAlpha(1f);
        playerWebView.loadDataWithBaseURL("https://com.urfavxbf.kanade/", playerHtml, "text/html",
                "UTF-8", "https://com.urfavxbf.kanade/");
        if (isAudioOnly) updatePlayerMode();
    }

    private void updatePlayerMode() {
        if (playerFrame == null || playerWebView == null) return;
        if (isAudioOnly && !currentVideoId.isEmpty()) {
            playerWebView.setAlpha(0f);
            miniPlayer.setVisibility(View.VISIBLE);
            statusText.setText("Audio only • YouTube playback");
        } else {
            showVideoPlayer();
        }
    }

    private void showVideoPlayer() {
        isAudioOnly = false;
        if (audioOnlyCheck != null && audioOnlyCheck.isChecked()) {
            audioOnlyCheck.setOnCheckedChangeListener(null);
            audioOnlyCheck.setChecked(false);
            audioOnlyCheck.setOnCheckedChangeListener((buttonView, checked) -> {
                isAudioOnly = checked;
                updatePlayerMode();
            });
        }
        playerWebView.setAlpha(1f);
        miniPlayer.setVisibility(View.GONE);
        if (!currentVideoId.isEmpty()) statusText.setText("YouTube video");
    }

    private void toggleYouTubePlayback() {
        if (playerWebView == null || currentVideoId.isEmpty()) return;
        if (isYouTubePlaying) {
            playerWebView.evaluateJavascript("pauseYT();", null);
        } else {
            playerWebView.evaluateJavascript("playYT();", null);
        }
    }

    private void loadMiniThumbnail(String thumbnailUrl) {
        if (searchExecutor == null || mainHandler == null) return;
        searchExecutor.execute(() -> {
            Bitmap bitmap = loadThumbnail(thumbnailUrl);
            if (bitmap == null) return;
            mainHandler.post(() -> {
                if (miniThumbnail != null && isAdded()) miniThumbnail.setImageBitmap(bitmap);
            });
        });
    }

    private Bitmap loadThumbnail(String thumbnailUrl) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(thumbnailUrl).openConnection();
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

    private String extractVideoId(String value) {
        try {
            Uri uri = Uri.parse(value);
            String host = uri.getHost();
            if (host == null) return null;
            if (host.equalsIgnoreCase("youtu.be")) {
                String id = uri.getPath();
                return validVideoId(id == null ? null : id.replace("/", ""));
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
            playerWebView.stopLoading();
            playerWebView.loadUrl("about:blank");
            playerWebView.destroy();
            playerWebView = null;
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

    private final class YouTubeBridge {
        @JavascriptInterface
        public void ready() {
            postToMain(() -> {
                if (playerWebView == null) return;
                playerWebView.evaluateJavascript("playYT();", null);
            });
        }

        @JavascriptInterface
        public void state(int state) {
            postToMain(() -> {
                isYouTubePlaying = state == 1 || state == 3;
                updateMiniPlayPauseIcon();
                if (state == 1) statusText.setText(isAudioOnly ? "Audio only • Playing" : "Playing on YouTube");
                if (state == 2) statusText.setText(isAudioOnly ? "Audio only • Paused" : "Paused");
                if (state == 0) isYouTubePlaying = false;
            });
        }

        @JavascriptInterface
        public void error(int errorCode) {
            postToMain(() -> statusText.setText("YouTube player error: " + errorCode));
        }

        private void postToMain(Runnable runnable) {
            if (mainHandler != null) mainHandler.post(runnable);
        }
    }

    private void updateMiniPlayPauseIcon() {
        if (miniPlayPause == null) return;
        miniPlayPause.setImageResource(isYouTubePlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        miniPlayPause.setContentDescription(isYouTubePlaying ? "Pause YouTube playback" : "Play YouTube playback");
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
        private final LruCache<String, Bitmap> thumbnailCache;

        ResultAdapter(Listener listener, ExecutorService imageExecutor, Handler mainHandler) {
            this.listener = listener;
            this.imageExecutor = imageExecutor;
            this.mainHandler = mainHandler;
            int cacheSize = Math.max(4, (int) (Runtime.getRuntime().maxMemory() / 1024 / 8));
            thumbnailCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override protected int sizeOf(@NonNull String key, @NonNull Bitmap value) {
                    return Math.max(1, value.getByteCount() / 1024);
                }
            };
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
            textContainer.addView(title, new LinearLayoutCompat.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView channel = new TextView(parent.getContext());
            channel.setTextColor(Color.rgb(155, 157, 170));
            channel.setTextSize(12);
            channel.setMaxLines(1);
            channel.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayoutCompat.LayoutParams channelParams = new LinearLayoutCompat.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            channelParams.topMargin = Math.round(4 * parent.getResources().getDisplayMetrics().density);
            textContainer.addView(channel, channelParams);
            return new ViewHolder(row, thumbnail, title, channel);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            YouTubeResult result = items.get(position);
            holder.title.setText(result.title);
            holder.channel.setText(result.channel);
            holder.itemView.setOnClickListener(v -> listener.onResult(result));
            holder.itemView.setContentDescription("Play " + result.title);
            holder.thumbnail.setTag(result.thumbnailUrl);
            holder.thumbnail.setImageResource(android.R.drawable.ic_menu_gallery);
            if (result.thumbnailUrl.isEmpty()) return;
            Bitmap cached = thumbnailCache.get(result.thumbnailUrl);
            if (cached != null) {
                holder.thumbnail.setImageBitmap(cached);
                return;
            }
            imageExecutor.execute(() -> {
                Bitmap bitmap = loadThumbnail(result.thumbnailUrl);
                if (bitmap == null) return;
                thumbnailCache.put(result.thumbnailUrl, bitmap);
                mainHandler.post(() -> {
                    Object tag = holder.thumbnail.getTag();
                    if (result.thumbnailUrl.equals(tag)) holder.thumbnail.setImageBitmap(bitmap);
                });
            });
        }

        private Bitmap loadThumbnail(String thumbnailUrl) {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(thumbnailUrl).openConnection();
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
