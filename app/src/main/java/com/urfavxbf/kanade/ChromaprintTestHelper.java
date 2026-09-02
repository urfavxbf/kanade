package com.urfavxbf.kanade;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChromaprintTestHelper {

    private ChromaprintTestHelper() {
        // Utility class
    }

    public static void test(
            Context context,
            AudioFile audioFile) {

        if (context == null) {
            return;
        }

        if (audioFile == null) {

            showResult(
                    context,
                    "Chromaprint Test",
                    "FAILED\n\nAudioFile is null."
            );

            return;
        }

        final Context appContext =
                context.getApplicationContext();

        final Context dialogContext =
                context;

        final AudioFile testSong =
                audioFile;

        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        Handler mainHandler =
                new Handler(
                        Looper.getMainLooper()
                );

        showResult(
                context,
                "Chromaprint Test",
                "Generating fingerprint...\n\n"
                        + "Song: "
                        + safe(
                                audioFile.getTitle()
                        )
                        + "\n\nPlease wait..."
        );

        executor.execute(() -> {

            ChromaprintFingerprintGenerator.Result generatedResult;

            try {

                ChromaprintFingerprintGenerator generator =
                        new ChromaprintFingerprintGenerator(
                                appContext
                        );

                generatedResult =
                        generator.generate(
                                testSong
                        );

            } catch (Exception e) {

                generatedResult =
                        ChromaprintFingerprintGenerator.Result.error(
                                "Test crashed: "
                                        + safe(
                                                e.getMessage()
                                        )
                        );
            }

            final ChromaprintFingerprintGenerator.Result finalResult =
                    generatedResult;

            mainHandler.post(() -> {

                try {

                    showFingerprintResult(
                            dialogContext,
                            testSong,
                            finalResult
                    );

                } finally {

                    executor.shutdown();
                }
            });
        });
    }

    private static void showFingerprintResult(
            Context context,
            AudioFile audioFile,
            ChromaprintFingerprintGenerator.Result result) {

        if (result == null) {

            showResult(
                    context,
                    "Chromaprint Test",
                    "FAILED\n\nResult is null."
            );

            return;
        }

        if (!result.isSuccess()) {

            StringBuilder message =
                    new StringBuilder();

            message.append(
                    "FINGERPRINT GENERATION FAILED\n\n"
            );

            message.append(
                    "Song:\n"
            );

            message.append(
                    safe(
                            audioFile.getTitle()
                    )
            );

            message.append(
                    "\n\nArtist:\n"
            );

            message.append(
                    safe(
                            audioFile.getArtist()
                    )
            );

            message.append(
                    "\n\nError:\n"
            );

            message.append(
                    safe(
                            result.getError()
                    )
            );

            showResult(
                    context,
                    "Chromaprint Test",
                    message.toString()
            );

            return;
        }

        String fingerprint =
                result.getFingerprint();

        int previewLength =
                Math.min(
                        fingerprint.length(),
                        150
                );

        String preview =
                fingerprint.substring(
                        0,
                        previewLength
                );

        StringBuilder message =
                new StringBuilder();

        message.append(
                "FINGERPRINT GENERATION SUCCESS\n\n"
        );

        message.append(
                "Song:\n"
        );

        message.append(
                safe(
                        audioFile.getTitle()
                )
        );

        message.append(
                "\n\nArtist:\n"
        );

        message.append(
                safe(
                        audioFile.getArtist()
                )
        );

        message.append(
                "\n\nDuration:\n"
        );

        message.append(
                result.getDurationSeconds()
        );

        message.append(
                " seconds"
        );

        message.append(
                "\n\nSample Rate:\n"
        );

        message.append(
                result.getSampleRate()
        );

        message.append(
                " Hz"
        );

        message.append(
                "\n\nChannels:\n"
        );

        message.append(
                result.getChannels()
        );

        message.append(
                "\n\nFingerprint Length:\n"
        );

        message.append(
                fingerprint.length()
        );

        message.append(
                "\n\nFingerprint Preview:\n"
        );

        message.append(
                preview
        );

        if (fingerprint.length() > 150) {

            message.append(
                    "..."
            );
        }

        showResult(
                context,
                "Chromaprint Test",
                message.toString()
        );
    }

    private static void showResult(
            Context context,
            String title,
            String message) {

        if (context == null) {
            return;
        }

        TextView textView =
                new TextView(context);

        int padding =
                dp(
                        context,
                        20
                );

        textView.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        textView.setText(
                message
        );

        textView.setTextSize(
                14
        );

        textView.setTextIsSelectable(
                true
        );

        ScrollView scrollView =
                new ScrollView(context);

        scrollView.addView(
                textView,
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(scrollView)
                .setPositiveButton(
                        "OK",
                        null
                )
                .show();
    }

    private static int dp(
            Context context,
            int value) {

        float density =
                context
                        .getResources()
                        .getDisplayMetrics()
                        .density;

        return (int) (
                value * density + 0.5f
        );
    }

    private static String safe(
            String value) {

        if (value == null ||
                value.trim().isEmpty()) {

            return "Unknown";
        }

        return value;
    }
}