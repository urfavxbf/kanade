package com.urfavxbf.kanade.ui.artist;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ArtistFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        TextView view = new TextView(requireContext());

        view.setText("Artists\n\nPlaceholder");
        view.setTextSize(20);
        view.setPadding(32, 32, 32, 32);

        return view;
    }
}