package com.urfavxbf.kanade;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class DebugActivity extends Activity {

    public static final String EXTRA_ERROR =
            "debug_error";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scrollView =
                new ScrollView(this);

        scrollView.setBackgroundColor(
                Color.rgb(16, 17, 26)
        );

        LinearLayout container =
                new LinearLayout(this);

        container.setOrientation(
                LinearLayout.VERTICAL
        );

        container.setPadding(
                dp(16),
                dp(16),
                dp(16),
                dp(16)
        );

        TextView title =
                new TextView(this);

        title.setText(
                "Kanade Debug"
        );

        title.setTextColor(
                Color.WHITE
        );

        title.setTextSize(
                22
        );

        title.setGravity(
                Gravity.CENTER_VERTICAL
        );

        title.setPadding(
                0,
                0,
                0,
                dp(16)
        );

        container.addView(
                title,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        TextView errorText =
                new TextView(this);

        errorText.setTextColor(
                Color.rgb(255, 120, 120)
        );

        errorText.setTextSize(
                13
        );

        errorText.setTextIsSelectable(
                true
        );

        errorText.setGravity(
                Gravity.TOP
        );

        String error =
                getIntent().getStringExtra(
                        EXTRA_ERROR
                );

        if (error == null ||
                error.trim().isEmpty()) {

            error =
                    "No crash information available.";
        }

        errorText.setText(
                error
        );

        container.addView(
                errorText,
                new LinearLayout.LayoutParams(
                        -1,
                        -2
                )
        );

        scrollView.addView(
                container
        );

        setContentView(
                scrollView
        );
    }

    private int dp(int value) {

        return Math.round(
                value *
                        getResources()
                                .getDisplayMetrics()
                                .density
        );
    }
}