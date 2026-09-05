package com.urfavxbf.kanade.ui.youtube;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.urfavxbf.kanade.R;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class YouTubeFragment extends Fragment {

    private EditText searchInput;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(
                R.layout.fragment_youtube,
                container,
                false
        );
    }

    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchInput = view.findViewById(R.id.youtubeSearchInput);
        Button searchButton = view.findViewById(R.id.youtubeSearchButton);

        searchButton.setOnClickListener(v -> openYouTube());

        searchInput.setOnEditorActionListener((v, actionId, event) -> {
            openYouTube();
            return true;
        });
    }

    private void openYouTube() {
        if (!isAdded() || searchInput == null) {
            return;
        }

        String query = searchInput.getText().toString().trim();

        if (query.isEmpty()) {
            searchInput.setError("Enter a song, artist, or YouTube link");
            searchInput.requestFocus();
            return;
        }

        hideKeyboard();

        Uri destination = buildYouTubeUri(query);
        Intent intent = new Intent(Intent.ACTION_VIEW, destination);
        intent.setPackage("com.google.android.youtube");

        try {
            startActivity(intent);
            return;
        } catch (ActivityNotFoundException ignored) {
            intent.setPackage(null);
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(
                    requireContext(),
                    "No app is available to open YouTube",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private Uri buildYouTubeUri(String query) {
        if (isYouTubeUrl(query)) {
            return Uri.parse(query);
        }

        String encodedQuery = URLEncoder.encode(
                query,
                StandardCharsets.UTF_8
        );

        return Uri.parse(
                "https://www.youtube.com/results?search_query="
                        + encodedQuery
        );
    }

    private boolean isYouTubeUrl(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        return lower.startsWith("https://youtube.com/")
                || lower.startsWith("https://www.youtube.com/")
                || lower.startsWith("http://youtube.com/")
                || lower.startsWith("http://www.youtube.com/")
                || lower.startsWith("https://youtu.be/")
                || lower.startsWith("http://youtu.be/");
    }

    private void hideKeyboard() {
        if (searchInput == null || !isAdded()) {
            return;
        }

        InputMethodManager manager =
                (InputMethodManager) requireContext().getSystemService(
                        Context.INPUT_METHOD_SERVICE
                );

        if (manager != null) {
            manager.hideSoftInputFromWindow(
                    searchInput.getWindowToken(),
                    0
            );
        }
    }

    @Override
    public void onDestroyView() {
        if (searchInput != null) {
            searchInput.setOnEditorActionListener(null);
        }
        searchInput = null;
        super.onDestroyView();
    }
}
