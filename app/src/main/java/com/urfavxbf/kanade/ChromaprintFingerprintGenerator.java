package com.urfavxbf.kanade;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.lalilu.fpcalc.Fpcalc;
import com.lalilu.fpcalc.FpcalcParams;
import com.lalilu.fpcalc.FpcalcResult;

public class ChromaprintFingerprintGenerator {

    private final Context context;

    public ChromaprintFingerprintGenerator(Context context) {
        this.context = context.getApplicationContext();
    }

    public Result generate(AudioFile audioFile) {

        if (audioFile == null) {
            return Result.error(
                    "AudioFile is null"
            );
        }

        String uriString = audioFile.getUri();

        if (uriString == null ||
                uriString.trim().isEmpty()) {

            return Result.error(
                    "Audio URI is empty"
            );
        }

        ParcelFileDescriptor pfd = null;
        AssetFileDescriptor afd = null;

        try {

            Uri uri = Uri.parse(uriString);

            /*
             * Open the audio through ContentResolver.
             *
             * ParcelFileDescriptor gives us the raw
             * integer file descriptor required by fpcalc.
             */
            pfd = context
                    .getContentResolver()
                    .openFileDescriptor(
                            uri,
                            "r"
                    );

            if (pfd == null) {

                return Result.error(
                        "Could not open audio file"
                );
            }

            int fd = pfd.getFd();

            if (fd < 0) {

                return Result.error(
                        "Invalid file descriptor"
                );
            }

            /*
             * Chromaprint parameters.
             *
             * targetFd       = Android file descriptor
             * targetFilePath = null
             * max duration   = 120 seconds
             * raw            = false
             * signed         = false
             * algorithm      = 2
             */
            FpcalcParams params =
                    new FpcalcParams(
                            fd,
                            null,
                            120,
                            false,
                            false,
                            2
                    );

            FpcalcResult result =
                    Fpcalc.INSTANCE.calc(
                            params
                    );

            if (result == null) {

                return Result.error(
                        "Fpcalc returned null"
                );
            }

            return parseResult(result);

        } catch (Exception e) {

            return Result.error(
                    "Fingerprint generation failed: "
                            + e.getMessage()
            );

        } finally {

            if (pfd != null) {

                try {
                    pfd.close();
                } catch (Exception ignored) {
                }
            }

            if (afd != null) {

                try {
                    afd.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private Result parseResult(
            FpcalcResult result) {

        String errorMessage =
                result.getErrorMessage();

        if (errorMessage != null &&
                !errorMessage.trim().isEmpty()) {

            return Result.error(
                    errorMessage
            );
        }

        String fingerprint =
                result.getFingerprint();

        if (fingerprint == null ||
                fingerprint.trim().isEmpty()) {

            return Result.error(
                    "Empty Chromaprint fingerprint"
            );
        }

        return new Result(
                fingerprint,
                result.getSourceDurationMs(),
                result.getSourceSampleRate(),
                result.getSourceChannels(),
                null
        );
    }

    public static class Result {

        private final String fingerprint;
        private final long durationMs;
        private final int sampleRate;
        private final int channels;
        private final String error;

        private Result(
                String fingerprint,
                long durationMs,
                int sampleRate,
                int channels,
                String error) {

            this.fingerprint = fingerprint;
            this.durationMs = durationMs;
            this.sampleRate = sampleRate;
            this.channels = channels;
            this.error = error;
        }

        public static Result error(
                String message) {

            return new Result(
                    null,
                    0,
                    0,
                    0,
                    message
            );
        }

        public boolean isSuccess() {

            return fingerprint != null &&
                    !fingerprint.trim().isEmpty() &&
                    error == null;
        }

        public String getFingerprint() {
            return fingerprint;
        }

        public long getDurationMs() {
            return durationMs;
        }

        public long getDurationSeconds() {
            return durationMs / 1000L;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public int getChannels() {
            return channels;
        }

        public String getError() {
            return error;
        }
    }
}