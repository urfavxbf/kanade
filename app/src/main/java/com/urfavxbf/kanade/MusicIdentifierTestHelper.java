package com.urfavxbf.kanade;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class MusicIdentifierTestHelper {

    private MusicIdentifierTestHelper() {
    }

    public static void test(
            Context context,
            AudioFile song) {

        if (context == null) {
            return;
        }

        if (song == null) {

            Toast.makeText(
                    context,
                    "Song is null.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        Context appContext =
                context.getApplicationContext();

        Toast.makeText(
                context,
                "Identifying song...",
                Toast.LENGTH_SHORT
        ).show();

        new Thread(
                new Runnable() {

                    @Override
                    public void run() {

                        MusicIdentifier.Result result;

                        try {

                            MusicIdentifier identifier =
                                    new MusicIdentifier(
                                            appContext
                                    );

                            result =
                                    identifier.identify(
                                            song
                                    );

                        } catch (Exception e) {

                            showErrorDialog(
                                    context,
                                    "Identification failed:\n\n"
                                            + safe(
                                            e.getMessage()
                                    )
                            );

                            return;
                        }

                        showResultDialog(
                                context,
                                song,
                                result
                        );
                    }
                }
        ).start();
    }

    private static void showResultDialog(
            Context context,
            AudioFile song,
            MusicIdentifier.Result result) {

        new Handler(
                Looper.getMainLooper()
        ).post(
                new Runnable() {

                    @Override
                    public void run() {

                        if (context == null ||
                                result == null) {

                            return;
                        }

                        StringBuilder text =
                                new StringBuilder();

                        text.append(
                                "MUSIC IDENTIFICATION\n"
                        );

                        text.append(
                                "━━━━━━━━━━━━━━━━━━━━\n\n"
                        );

                        /*
                         * LOCAL SONG
                         */
                        text.append(
                                "LOCAL SONG\n\n"
                        );

                        text.append(
                                "Title: "
                        );

                        text.append(
                                safe(
                                        song.getTitle()
                                )
                        );

                        text.append("\n");

                        text.append(
                                "Artist: "
                        );

                        text.append(
                                safe(
                                        song.getArtist()
                                )
                        );

                        text.append("\n");

                        text.append(
                                "Album: "
                        );

                        text.append(
                                safe(
                                        song.getAlbum()
                                )
                        );

                        text.append("\n");

                        text.append(
                                "Duration: "
                        );

                        text.append(
                                song.getDuration()
                                        / 1000L
                        );

                        text.append(
                                " seconds\n\n"
                        );

                        /*
                         * RESULT
                         */
                        text.append(
                                "IDENTIFICATION\n\n"
                        );

                        if (!result.isSuccess()) {

                            text.append(
                                    "Status: FAILED\n\n"
                            );

                            text.append(
                                    "Error:\n"
                            );

                            text.append(
                                    safe(
                                            result.getError()
                                    )
                            );

                            showDialog(
                                    context,
                                    text.toString()
                            );

                            return;
                        }

                        text.append(
                                "Status: SUCCESS\n"
                        );

                        text.append(
                                "Duration: "
                        );

                        text.append(
                                result
                                        .getDurationSeconds()
                        );

                        text.append(
                                " seconds\n"
                        );

                        text.append(
                                "Fingerprint Length: "
                        );

                        text.append(
                                safeLength(
                                        result
                                                .getFingerprint()
                                )
                        );

                        text.append(
                                "\n\n"
                        );

                        /*
                         * CANDIDATES
                         */
                        ArrayList<
                                MusicMetadataCandidate>
                                candidates =
                                result.getCandidates();

                        if (candidates == null ||
                                candidates.isEmpty()) {

                            text.append(
                                    "CANDIDATES FOUND: 0\n\n"
                            );

                            text.append(
                                    "No matching metadata "
                                            + "was found."
                            );

                        } else {

                            text.append(
                                    "CANDIDATES FOUND: "
                            );

                            text.append(
                                    candidates.size()
                            );

                            text.append(
                                    "\n\n"
                            );

                            for (
                                    int i = 0;
                                    i < candidates.size();
                                    i++
                            ) {

                                MusicMetadataCandidate
                                        candidate =
                                        candidates.get(i);

                                if (candidate == null) {
                                    continue;
                                }

                                text.append(
                                        "━━━━━━━━━━━━━━━━━━━━\n"
                                );

                                text.append(
                                        "MATCH "
                                );

                                text.append(
                                        i + 1
                                );

                                if (i == 0) {

                                    text.append(
                                            "  ★ BEST MATCH"
                                    );
                                }

                                text.append(
                                        "\n"
                                );

                                text.append(
                                        "━━━━━━━━━━━━━━━━━━━━\n\n"
                                );

                                text.append(
                                        "Match Score: "
                                );

                                text.append(
                                        formatScore(
                                                candidate
                                                        .getMatchScore()
                                        )
                                );

                                text.append(
                                        "\n"
                                );

                                text.append(
                                        "Source: "
                                );

                                text.append(
                                        safe(
                                                candidate
                                                        .getSource()
                                        )
                                );

                                text.append(
                                        "\n\n"
                                );

                                text.append(
                                        "Title: "
                                );

                                text.append(
                                        candidate
                                                .getSafeTitle()
                                );

                                text.append(
                                        "\n"
                                );

                                text.append(
                                        "Artist: "
                                );

                                text.append(
                                        candidate
                                                .getSafeArtist()
                                );

                                text.append(
                                        "\n"
                                );

                                text.append(
                                        "Album: "
                                );

                                text.append(
                                        candidate
                                                .getSafeAlbum()
                                );

                                text.append(
                                        "\n"
                                );

                                text.append(
                                        "Album Artist: "
                                );

                                text.append(
                                        safe(
                                                candidate
                                                        .getAlbumArtist()
                                        )
                                );

                                text.append(
                                        "\n"
                                );

                                text.append(
                                        "Release Group: "
                                );

                                text.append(
                                        safe(
                                                candidate
                                                        .getReleaseGroup()
                                        )
                                );

                                text.append(
                                        "\n"
                                );

                                text.append(
                                        "Genre: "
                                );

                                text.append(
                                        safe(
                                                candidate
                                                        .getGenre()
                                        )
                                );

                                text.append(
                                        "\n"
                                );

                                text.append(
                                        "Composer: "
                                );

                                text.append(
                                        safe(
                                                candidate
                                                        .getComposer()
                                        )
                                );

                                text.append(
                                        "\n"
                                );

                                text.append(
                                        "Year: "
                                );

                                text.append(
                                        safe(
                                                candidate
                                                        .getYear()
                                        )
                                );

                                text.append(
                                        "\n"
                                );

                                text.append(
                                        "Track: "
                                );

                                text.append(
                                        safe(
                                                candidate
                                                        .getTrackNumber()
                                        )
                                );

                                text.append(
                                        "\n"
                                );

                                text.append(
                                        "Disc: "
                                );

                                text.append(
                                        safe(
                                                candidate
                                                        .getDiscNumber()
                                        )
                                );

                                text.append(
                                        "\n"
                                );

                                text.append(
                                        "Duration: "
                                );

                                text.append(
                                        candidate
                                                .getDurationSeconds()
                                );

                                text.append(
                                        " seconds\n\n"
                                );

                                /*
                                 * IDs
                                 */
                                text.append(
                                        "AcoustID: "
                                );

                                text.append(
                                        safe(
                                                candidate
                                                        .getAcoustId()
                                        )
                                );

                                text.append(
                                        "\n"
                                );

                                text.append(
                                        "MusicBrainz Recording ID: "
                                );

                                text.append(
                                        safe(
                                                candidate
                                                        .getRecordingId()
                                        )
                                );

                                text.append(
                                        "\n"
                                );

                                text.append(
                                        "MusicBrainz Score: "
                                );

                                text.append(
                                        candidate
                                                .getMusicBrainzScore()
                                );

                                text.append(
                                        "\n"
                                );

                                text.append(
                                        "Disambiguation: "
                                );

                                text.append(
                                        safe(
                                                candidate
                                                        .getDisambiguation()
                                        )
                                );

                                text.append(
                                        "\n"
                                );
                            }
                        }

                        showDialog(
                                context,
                                text.toString()
                        );
                    }
                }
        );
    }

    private static void showErrorDialog(
            Context context,
            String message) {

        new Handler(
                Looper.getMainLooper()
        ).post(
                new Runnable() {

                    @Override
                    public void run() {

                        showDialog(
                                context,
                                message
                        );
                    }
                }
        );
    }

    private static void showDialog(
            Context context,
            String message) {

        if (context == null) {
            return;
        }

        TextView textView =
                new TextView(
                        context
                );

        textView.setText(
                message
        );

        textView.setTextSize(
                14
        );

        textView.setGravity(
                Gravity.START
        );

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

        ScrollView scrollView =
                new ScrollView(
                        context
                );

        scrollView.addView(
                textView
        );

        new AlertDialog.Builder(
                context
        )
                .setTitle(
                        "Music Identifier"
                )
                .setView(
                        scrollView
                )
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
                value * density
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

    private static int safeLength(
            String value) {

        if (value == null) {
            return 0;
        }

        return value.length();
    }

    private static String formatScore(
            double score) {

        return String.format(
                Locale.US,
                "%.2f",
                score
        );
    }
}