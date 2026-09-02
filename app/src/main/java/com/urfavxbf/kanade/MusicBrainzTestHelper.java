package com.urfavxbf.kanade;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.util.ArrayList;

public class MusicBrainzTestHelper {

    public static void test(
            Context context,
            AudioFile song) {

        if (context == null ||
                song == null) {

            return;
        }

        Toast.makeText(
                context,
                "Searching MusicBrainz...",
                Toast.LENGTH_SHORT
        ).show();

        new Thread(
                new Runnable() {

                    @Override
                    public void run() {

                        MusicBrainzClient client =
                                new MusicBrainzClient();

                        ArrayList<
                                MusicBrainzClient
                                        .MusicBrainzRecording
                                > results =
                                client.searchRecordings(
                                        song.getTitle(),
                                        song.getArtist(),
                                        song.getDuration() / 1000L
                                );

                        StringBuilder output =
                                new StringBuilder();

                        output.append(
                                "MUSICBRAINZ SEARCH RESULT\n\n"
                        );

                        output.append(
                                "LOCAL SONG\n"
                        );

                        output.append(
                                "Title: "
                        );

                        output.append(
                                safe(
                                        song.getTitle()
                                )
                        );

                        output.append(
                                "\nArtist: "
                        );

                        output.append(
                                safe(
                                        song.getArtist()
                                )
                        );

                        output.append(
                                "\nAlbum: "
                        );

                        output.append(
                                safe(
                                        song.getAlbum()
                                )
                        );

                        output.append(
                                "\nDuration: "
                        );

                        output.append(
                                song.getDuration()
                                        / 1000L
                        );

                        output.append(
                                " seconds\n\n"
                        );

                        output.append(
                                "RESULTS FOUND: "
                        );

                        output.append(
                                results.size()
                        );

                        output.append(
                                "\n\n"
                        );

                        for (int i = 0;
                                i < results.size();
                                i++) {

                            MusicBrainzClient
                                    .MusicBrainzRecording
                                    recording =
                                    results.get(i);

                            output.append(
                                    "========== RESULT "
                            );

                            output.append(
                                    i + 1
                            );

                            output.append(
                                    " ==========\n"
                            );

                            output.append(
                                    "Match Score: "
                            );

                            output.append(
                                    recording
                                            .getMatchScore()
                            );

                            output.append(
                                    "\n"
                            );

                            output.append(
                                    "MusicBrainz Recording ID: "
                            );

                            output.append(
                                    safe(
                                            recording.getId()
                                    )
                            );

                            output.append(
                                    "\nTitle: "
                            );

                            output.append(
                                    safe(
                                            recording.getTitle()
                                    )
                            );

                            output.append(
                                    "\nArtist: "
                            );

                            output.append(
                                    safe(
                                            recording.getArtist()
                                    )
                            );

                            output.append(
                                    "\nAlbum: "
                            );

                            output.append(
                                    safe(
                                            recording.getAlbum()
                                    )
                            );

                            output.append(
                                    "\nRelease Group: "
                            );

                            output.append(
                                    safe(
                                            recording
                                                    .getReleaseGroup()
                                    )
                            );

                            output.append(
                                    "\nDuration: "
                            );

                            output.append(
                                    recording
                                            .getDurationSeconds()
                            );

                            output.append(
                                    " seconds\n"
                            );

                            output.append(
                                    "MusicBrainz Score: "
                            );

                            output.append(
                                    recording
                                            .getScore()
                            );

                            output.append(
                                    "\nDisambiguation: "
                            );

                            output.append(
                                    safe(
                                            recording
                                                    .getDisambiguation()
                                    )
                            );

                            output.append(
                                    "\n\n"
                            );
                        }

                        String resultText =
                                output.toString();

                        new Handler(
                                Looper.getMainLooper()
                        ).post(
                                new Runnable() {

                                    @Override
                                    public void run() {

                                        new android.app.AlertDialog
                                                .Builder(
                                                        context
                                                )
                                                .setTitle(
                                                        "MusicBrainz Search"
                                                )
                                                .setMessage(
                                                        resultText
                                                )
                                                .setPositiveButton(
                                                        "OK",
                                                        null
                                                )
                                                .show();
                                    }
                                }
                        );
                    }
                }
        ).start();
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