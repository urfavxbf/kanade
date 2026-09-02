package com.urfavxbf.kanade;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MusicIdentifier {

    private final Context context;

    private final ChromaprintFingerprintGenerator
            fingerprintGenerator;

    private final AcoustIdClient
            acoustIdClient;

    private final MusicBrainzClient
            musicBrainzClient;

    public MusicIdentifier(
            Context context) {

        this.context =
                context.getApplicationContext();

        fingerprintGenerator =
                new ChromaprintFingerprintGenerator(
                        this.context
                );

        acoustIdClient =
                new AcoustIdClient();

        musicBrainzClient =
                new MusicBrainzClient();
    }

    public Result identify(
            AudioFile audioFile) {

        if (audioFile == null) {

            return Result.error(
                    "AudioFile is null"
            );
        }

        ArrayList<MusicMetadataCandidate>
                candidates =
                new ArrayList<>();

        /*
         * Keep track of the strongest AcoustID
         * match. This can be attached to the
         * MusicBrainz fallback result later.
         */
        String bestAcoustId = null;

        double bestAcoustIdScore = -1.0;

        /*
         * ------------------------------------------------
         * STEP 1
         * Generate Chromaprint fingerprint
         * ------------------------------------------------
         */

        ChromaprintFingerprintGenerator.Result
                fingerprintResult =
                fingerprintGenerator.generate(
                        audioFile
                );

        if (fingerprintResult == null ||
                !fingerprintResult.isSuccess()) {

            String error =
                    fingerprintResult != null
                            ? fingerprintResult.getError()
                            : "Fingerprint generation failed";

            return Result.error(
                    "Chromaprint failed: "
                            + safe(error)
            );
        }

        String fingerprint =
                fingerprintResult.getFingerprint();

        long durationSeconds =
                fingerprintResult
                        .getDurationSeconds();

        if (durationSeconds <= 0 &&
                audioFile.getDuration() > 0) {

            durationSeconds =
                    audioFile.getDuration()
                            / 1000L;
        }

        /*
         * ------------------------------------------------
         * STEP 2
         * AcoustID lookup
         * ------------------------------------------------
         */

        AcoustIdClient.AcoustIdResult
                acoustIdResult =
                acoustIdClient.lookup(
                        fingerprint,
                        durationSeconds
                );

        if (acoustIdResult != null &&
                acoustIdResult.isSuccess()) {

            ArrayList<
                    AcoustIdClient.AcoustIdMatch>
                    matches =
                    acoustIdResult.getMatches();

            if (matches != null) {

                for (
                        AcoustIdClient.AcoustIdMatch
                                match
                        : matches) {

                    if (match == null) {

                        continue;
                    }

                    /*
                     * Save strongest AcoustID match.
                     */
                    String currentAcoustId =
                            clean(
                                    match.getAcoustId()
                            );

                    double currentScore =
                            match.getScore();

                    if (currentAcoustId != null &&
                            currentScore >
                                    bestAcoustIdScore) {

                        bestAcoustId =
                                currentAcoustId;

                        bestAcoustIdScore =
                                currentScore;
                    }

                    /*
                     * ------------------------------------------------
                     * Try AcoustID -> MusicBrainz
                     * ------------------------------------------------
                     */

                    String recordingId =
                            clean(
                                    match.getRecordingId()
                            );

                    boolean addedMusicBrainzCandidate =
                            false;

                    if (recordingId != null) {

                        MusicBrainzClient
                                .MusicBrainzRecording
                                recording =
                                musicBrainzClient
                                        .getRecording(
                                                recordingId
                                        );

                        if (recording != null) {

                            MusicMetadataCandidate
                                    candidate =
                                    createFromMusicBrainz(
                                            recording
                                    );

                            if (candidate != null) {

                                candidate.setAcoustId(
                                        currentAcoustId
                                );

                                candidate.setMatchScore(
                                        combineScores(
                                                match.getScore(),
                                                recording
                                                        .getMatchScore()
                                        )
                                );

                                candidate.setSource(
                                        "AcoustID + MusicBrainz"
                                );

                                candidates.add(
                                        candidate
                                );

                                addedMusicBrainzCandidate =
                                        true;
                            }
                        }
                    }

                    /*
                     * ------------------------------------------------
                     * AcoustID-only candidate
                     * ------------------------------------------------
                     *
                     * IMPORTANT:
                     *
                     * Do NOT add a completely empty AcoustID
                     * result.
                     *
                     * Otherwise:
                     *
                     * candidates.isEmpty()
                     *
                     * becomes false and the MusicBrainz
                     * fallback never executes.
                     */

                    if (!addedMusicBrainzCandidate) {

                        MusicMetadataCandidate
                                acoustIdCandidate =
                                createFromAcoustId(
                                        match
                                );

                        if (isUsableAcoustIdCandidate(
                                acoustIdCandidate
                        )) {

                            candidates.add(
                                    acoustIdCandidate
                            );
                        }
                    }
                }
            }
        }

        /*
         * ------------------------------------------------
         * STEP 3
         * MusicBrainz fallback
         * ------------------------------------------------
         *
         * If AcoustID gave us nothing useful,
         * search MusicBrainz directly using the
         * local metadata.
         */

        if (candidates.isEmpty()) {

            ArrayList<
                    MusicBrainzClient
                            .MusicBrainzRecording>
                    recordings =
                    musicBrainzClient
                            .searchRecordings(
                                    audioFile.getTitle(),
                                    audioFile.getArtist(),
                                    durationSeconds
                            );

            if (recordings != null) {

                for (
                        MusicBrainzClient
                                .MusicBrainzRecording
                                recording
                        : recordings) {

                    if (recording == null) {

                        continue;
                    }

                    MusicMetadataCandidate
                            candidate =
                            createFromMusicBrainz(
                                    recording
                            );

                    if (candidate != null) {

                        candidates.add(
                                candidate
                        );
                    }
                }
            }

            /*
             * Sort first so the strongest MusicBrainz
             * result is the one that receives the
             * strongest AcoustID reference.
             */
            sortCandidates(
                    candidates
            );

            /*
             * The AcoustID result did identify the
             * fingerprint, but it had no linked
             * MusicBrainz recording.
             *
             * Preserve that AcoustID on the BEST
             * MusicBrainz candidate.
             */
            if (!candidates.isEmpty() &&
                    bestAcoustId != null) {

                MusicMetadataCandidate bestCandidate =
                        candidates.get(0);

                bestCandidate.setAcoustId(
                        bestAcoustId
                );

                bestCandidate.setSource(
                        "AcoustID + MusicBrainz"
                );
            }
        }

        /*
         * Final sorting.
         */
        sortCandidates(
                candidates
        );

        return Result.success(
                fingerprint,
                durationSeconds,
                candidates
        );
    }

    /*
     * ------------------------------------------------
     * Create candidate from AcoustID
     * ------------------------------------------------
     */

    private MusicMetadataCandidate
            createFromAcoustId(
                    AcoustIdClient.AcoustIdMatch match) {

        if (match == null) {

            return null;
        }

        MusicMetadataCandidate candidate =
                new MusicMetadataCandidate();

        candidate.setAcoustId(
                clean(
                        match.getAcoustId()
                )
        );

        candidate.setRecordingId(
                clean(
                        match.getRecordingId()
                )
        );

        candidate.setTitle(
                clean(
                        match.getTitle()
                )
        );

        candidate.setArtist(
                clean(
                        match.getArtist()
                )
        );

        candidate.setAlbum(
                clean(
                        match.getAlbum()
                )
        );

        long duration =
                match.getDuration();

        if (duration > 0) {

            candidate.setDurationMs(
                    duration * 1000L
            );
        }

        candidate.setMatchScore(
                normalizeAcoustIdScore(
                        match.getScore()
                )
        );

        candidate.setSource(
                "AcoustID"
        );

        return candidate;
    }

    /*
     * ------------------------------------------------
     * Create candidate from MusicBrainz
     * ------------------------------------------------
     */

    private MusicMetadataCandidate
            createFromMusicBrainz(
                    MusicBrainzClient
                            .MusicBrainzRecording
                            recording) {

        if (recording == null) {

            return null;
        }

        MusicMetadataCandidate candidate =
                new MusicMetadataCandidate();

        candidate.setRecordingId(
                clean(
                        recording.getId()
                )
        );

        candidate.setTitle(
                clean(
                        recording.getTitle()
                )
        );

        candidate.setArtist(
                clean(
                        recording.getArtist()
                )
        );

        candidate.setAlbum(
                clean(
                        recording.getAlbum()
                )
        );

        candidate.setReleaseGroup(
                clean(
                        recording.getReleaseGroup()
                )
        );

        candidate.setDisambiguation(
                clean(
                        recording.getDisambiguation()
                )
        );

        candidate.setDurationMs(
                recording.getDurationMs()
        );

        candidate.setMusicBrainzScore(
                recording.getScore()
        );

        candidate.setMatchScore(
                recording.getMatchScore()
        );

        candidate.setSource(
                "MusicBrainz"
        );

        return candidate;
    }

    /*
     * ------------------------------------------------
     * Determines whether an AcoustID candidate
     * actually contains useful information.
     * ------------------------------------------------
     */

    private boolean isUsableAcoustIdCandidate(
            MusicMetadataCandidate candidate) {

        if (candidate == null) {

            return false;
        }

        /*
         * A linked MusicBrainz ID is useful even if
         * the AcoustID response itself has no metadata.
         */
        if (!isEmpty(
                candidate.getRecordingId()
        )) {

            return true;
        }

        /*
         * Otherwise require actual metadata.
         */
        if (!isEmpty(
                candidate.getTitle()
        )) {

            return true;
        }

        if (!isEmpty(
                candidate.getArtist()
        )) {

            return true;
        }

        if (!isEmpty(
                candidate.getAlbum()
        )) {

            return true;
        }

        if (candidate.getDurationMs() > 0) {

            return true;
        }

        /*
         * Completely empty AcoustID result.
         *
         * DO NOT let this block MusicBrainz
         * fallback.
         */
        return false;
    }

    /*
     * ------------------------------------------------
     * Normalize AcoustID score
     * ------------------------------------------------
     */

    private double normalizeAcoustIdScore(
            double score) {

        if (score <= 1.0) {

            return score * 100.0;
        }

        return Math.min(
                100.0,
                score
        );
    }

    /*
     * ------------------------------------------------
     * Combine AcoustID + MusicBrainz scores
     * ------------------------------------------------
     */

    private double combineScores(
            double acoustIdScore,
            double musicBrainzScore) {

        double acoust =
                normalizeAcoustIdScore(
                        acoustIdScore
                );

        return (
                acoust * 0.60
        ) + (
                musicBrainzScore * 0.40
        );
    }

    /*
     * ------------------------------------------------
     * Sort candidates by strongest match
     * ------------------------------------------------
     */

    private void sortCandidates(
            ArrayList<MusicMetadataCandidate>
                    candidates) {

        Collections.sort(
                candidates,
                new Comparator<
                        MusicMetadataCandidate>() {

                    @Override
                    public int compare(
                            MusicMetadataCandidate a,
                            MusicMetadataCandidate b) {

                        return Double.compare(
                                b.getMatchScore(),
                                a.getMatchScore()
                        );
                    }
                }
        );
    }

    /*
     * ------------------------------------------------
     * Result
     * ------------------------------------------------
     */

    public static class Result {

        private final boolean success;
        private final String error;
        private final String fingerprint;
        private final long durationSeconds;

        private final ArrayList<
                MusicMetadataCandidate>
                candidates;

        private Result(
                boolean success,
                String error,
                String fingerprint,
                long durationSeconds,
                ArrayList<
                        MusicMetadataCandidate>
                        candidates) {

            this.success = success;
            this.error = error;
            this.fingerprint = fingerprint;
            this.durationSeconds = durationSeconds;
            this.candidates = candidates;
        }

        public static Result success(
                String fingerprint,
                long durationSeconds,
                ArrayList<
                        MusicMetadataCandidate>
                        candidates) {

            return new Result(
                    true,
                    null,
                    fingerprint,
                    durationSeconds,
                    candidates
            );
        }

        public static Result error(
                String error) {

            return new Result(
                    false,
                    error,
                    null,
                    0,
                    new ArrayList<
                            MusicMetadataCandidate>()
            );
        }

        public boolean isSuccess() {

            return success;
        }

        public String getError() {

            return error;
        }

        public String getFingerprint() {

            return fingerprint;
        }

        public long getDurationSeconds() {

            return durationSeconds;
        }

        public ArrayList<
                MusicMetadataCandidate>
                getCandidates() {

            return candidates;
        }
    }

    /*
     * ------------------------------------------------
     * String helpers
     * ------------------------------------------------
     */

    private String clean(
            String value) {

        if (value == null) {

            return null;
        }

        String cleaned =
                value.trim();

        return cleaned.isEmpty()
                ? null
                : cleaned;
    }

    private boolean isEmpty(
            String value) {

        return value == null ||
                value.trim().isEmpty();
    }

    private String safe(
            String value) {

        return value == null ||
                value.trim().isEmpty()
                ? "Unknown error"
                : value;
    }
}