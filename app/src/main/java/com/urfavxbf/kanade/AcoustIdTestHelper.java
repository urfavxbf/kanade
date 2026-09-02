package com.urfavxbf.kanade;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AcoustIdTestHelper {

    private AcoustIdTestHelper() {
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
                    "AcoustID Test",
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
                "AcoustID Test",
                "Generating fingerprint...\n\n"
                        + "Song:\n"
                        + safe(
                                audioFile.getTitle()
                        )
                        + "\n\n"
                        + "Please wait..."
        );

        executor.execute(() -> {

            ChromaprintFingerprintGenerator.Result fingerprintResult;

            try {

                ChromaprintFingerprintGenerator generator =
                        new ChromaprintFingerprintGenerator(
                                appContext
                        );

                fingerprintResult =
                        generator.generate(
                                testSong
                        );

            } catch (Exception e) {

                fingerprintResult =
                        ChromaprintFingerprintGenerator.Result.error(
                                "Chromaprint crashed: "
                                        + safe(
                                                e.getMessage()
                                        )
                        );
            }

            final ChromaprintFingerprintGenerator.Result finalFingerprintResult =
                    fingerprintResult;

            if (!finalFingerprintResult.isSuccess()) {

                mainHandler.post(() -> {

                    showResult(
                            dialogContext,
                            "AcoustID Test",
                            "CHROMAPRINT FAILED\n\n"
                                    + "Song:\n"
                                    + safe(
                                            testSong.getTitle()
                                    )
                                    + "\n\nError:\n"
                                    + safe(
                                            finalFingerprintResult
                                                    .getError()
                                    )
                    );

                    executor.shutdown();
                });

                return;
            }

            mainHandler.post(() -> {

                showResult(
                        dialogContext,
                        "AcoustID Test",
                        "Fingerprint generated successfully.\n\n"
                                + "Duration: "
                                + finalFingerprintResult
                                        .getDurationSeconds()
                                + " seconds\n\n"
                                + "Sample Rate: "
                                + finalFingerprintResult
                                        .getSampleRate()
                                + " Hz\n\n"
                                + "Channels: "
                                + finalFingerprintResult
                                        .getChannels()
                                + "\n\n"
                                + "Now querying AcoustID..."
                );
            });

            AcoustIdClient.AcoustIdResult acoustIdResult;

            try {

                AcoustIdClient client =
                        new AcoustIdClient();

                acoustIdResult =
                        client.lookup(
                                finalFingerprintResult
                                        .getFingerprint(),
                                finalFingerprintResult
                                        .getDurationSeconds()
                        );

            } catch (Exception e) {

                acoustIdResult =
                        new AcoustIdClient.AcoustIdResult(
                                false,
                                "AcoustID crashed: "
                                        + safe(
                                                e.getMessage()
                                        )
                        );
            }

            final AcoustIdClient.AcoustIdResult finalAcoustIdResult =
                    acoustIdResult;

            mainHandler.post(() -> {

                try {

                    showAcoustIdResult(
                            dialogContext,
                            testSong,
                            finalFingerprintResult,
                            finalAcoustIdResult
                    );

                } finally {

                    executor.shutdown();
                }
            });
        });
    }

    private static void showAcoustIdResult(
            Context context,
            AudioFile audioFile,
            ChromaprintFingerprintGenerator.Result fingerprintResult,
            AcoustIdClient.AcoustIdResult acoustIdResult) {

        if (acoustIdResult == null) {

            showResult(
                    context,
                    "AcoustID Test",
                    "FAILED\n\nAcoustID result is null."
            );

            return;
        }

        StringBuilder message =
                new StringBuilder();

        message.append(
                "ACOUSTID LOOKUP RESULT\n\n"
        );

        message.append(
                "LOCAL SONG\n"
        );

        message.append(
                "Title: "
        );

        message.append(
                safe(
                        audioFile.getTitle()
                )
        );

        message.append(
                "\nArtist: "
        );

        message.append(
                safe(
                        audioFile.getArtist()
                )
        );

        message.append(
                "\nAlbum: "
        );

        message.append(
                safe(
                        audioFile.getAlbum()
                )
        );

        message.append(
                "\n\n"
        );

        message.append(
                "CHROMAPRINT\n"
        );

        message.append(
                "Status: SUCCESS"
        );

        message.append(
                "\nDuration: "
        );

        message.append(
                fingerprintResult
                        .getDurationSeconds()
        );

        message.append(
                " seconds"
        );

        message.append(
                "\nSample Rate: "
        );

        message.append(
                fingerprintResult
                        .getSampleRate()
        );

        message.append(
                " Hz"
        );

        message.append(
                "\nChannels: "
        );

        message.append(
                fingerprintResult
                        .getChannels()
        );

        message.append(
                "\n\n"
        );

        message.append(
                "ACOUSTID\n"
        );

        message.append(
                "Status: "
        );

        message.append(
                acoustIdResult.isSuccess()
                        ? "SUCCESS"
                        : "FAILED"
        );

        message.append(
                "\nMessage: "
        );

        message.append(
                safe(
                        acoustIdResult.getMessage()
                )
        );

        if (!acoustIdResult.isSuccess()) {

            appendRawResponse(
                    message,
                    acoustIdResult
            );

            showResult(
                    context,
                    "AcoustID Test",
                    message.toString()
            );

            return;
        }

        ArrayList<AcoustIdClient.AcoustIdMatch> matches =
                acoustIdResult.getMatches();

        if (matches == null ||
                matches.isEmpty()) {

            message.append(
                    "\n\nNo matches found."
            );

            appendRawResponse(
                    message,
                    acoustIdResult
            );

            showResult(
                    context,
                    "AcoustID Test",
                    message.toString()
            );

            return;
        }

        message.append(
                "\n\nMATCHES FOUND: "
        );

        message.append(
                matches.size()
        );

        message.append(
                "\n\n"
        );

        for (int i = 0;
                i < matches.size();
                i++) {

            AcoustIdClient.AcoustIdMatch match =
                    matches.get(i);

            message.append(
                    "========== MATCH "
            );

            message.append(
                    i + 1
            );

            message.append(
                    " ==========\n"
            );

            message.append(
                    "Score: "
            );

            message.append(
                    match.getScore()
            );

            message.append(
                    "\n\n"
            );

            message.append(
                    "AcoustID Track ID:\n"
            );

            message.append(
                    safe(
                            match.getAcoustId()
                    )
            );

            message.append(
                    "\n\n"
            );

            message.append(
                    "MusicBrainz Recording ID:\n"
            );

            message.append(
                    safe(
                            match.getRecordingId()
                    )
            );

            message.append(
                    "\n\n"
            );

            message.append(
                    "Title:\n"
            );

            message.append(
                    safe(
                            match.getTitle()
                    )
            );

            message.append(
                    "\n\n"
            );

            message.append(
                    "Artist:\n"
            );

            message.append(
                    safe(
                            match.getArtist()
                    )
            );

            message.append(
                    "\n\n"
            );

            message.append(
                    "Album:\n"
            );

            message.append(
                    safe(
                            match.getAlbum()
                    )
            );

            message.append(
                    "\n\n"
            );

            message.append(
                    "Duration:\n"
            );

            message.append(
                    match.getDuration()
            );

            message.append(
                    " seconds\n\n"
            );
        }

        /*
         * IMPORTANT:
         *
         * Show the exact JSON returned by AcoustID.
         * This is what we need to diagnose why
         * recordings[] metadata is missing.
         */
        appendRawResponse(
                message,
                acoustIdResult
        );

        showResult(
                context,
                "AcoustID Test",
                message.toString()
        );
    }

    private static void appendRawResponse(
            StringBuilder message,
            AcoustIdClient.AcoustIdResult result) {

        if (result == null) {
            return;
        }

        String rawResponse =
                result.getRawResponse();

        message.append(
                "\n\n"
        );

        message.append(
                "========== RAW ACOUSTID RESPONSE ==========\n\n"
        );

        if (rawResponse == null ||
                rawResponse.trim().isEmpty()) {

            message.append(
                    "No raw response available."
            );

            return;
        }

        message.append(
                rawResponse
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