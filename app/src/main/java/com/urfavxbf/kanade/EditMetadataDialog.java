package com.urfavxbf.kanade;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Production metadata editor.
 *
 * The editor changes the in-memory AudioFile and stores the result in
 * MetadataOverrideManager. The original audio file is never modified.
 */
public class EditMetadataDialog {

    public interface OnMetadataSavedListener {
        void onMetadataSaved(AudioFile song);
    }

    private final Context context;
    private final AudioFile song;
    private final MetadataOverrideManager overrideManager;
    private final OnMetadataSavedListener listener;

    private AlertDialog dialog;

    private EditText titleInput;
    private EditText artistInput;
    private EditText albumInput;
    private EditText albumArtistInput;
    private EditText genreInput;
    private EditText composerInput;
    private EditText yearInput;
    private EditText trackInput;
    private EditText discInput;

    public EditMetadataDialog(
            Context context,
            AudioFile song,
            MetadataOverrideManager overrideManager,
            OnMetadataSavedListener listener) {

        this.context = context;
        this.song = song;
        this.overrideManager = overrideManager;
        this.listener = listener;
    }

    public void show() {
        if (song == null || overrideManager == null) {
            return;
        }

        LinearLayout content = createContent();

        dialog = new AlertDialog.Builder(context)
                .setTitle("Edit metadata")
                .setView(content)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", null)
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> saveManualMetadata());
        });

        dialog.show();
    }

    private LinearLayout createContent() {
        LinearLayout outer = new LinearLayout(context);
        outer.setOrientation(LinearLayout.VERTICAL);

        int horizontal = dp(24);
        int vertical = dp(8);
        outer.setPadding(horizontal, dp(4), horizontal, dp(4));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);

        LinearLayout fields = new LinearLayout(context);
        fields.setOrientation(LinearLayout.VERTICAL);

        titleInput = addField(fields, "TITLE", song.getTitle());
        artistInput = addField(fields, "ARTIST", song.getArtist());
        albumInput = addField(fields, "ALBUM", song.getAlbum());
        albumArtistInput = addField(fields, "ALBUM ARTIST", song.getAlbumArtist());
        genreInput = addField(fields, "GENRE", song.getGenre());
        composerInput = addField(fields, "COMPOSER", song.getComposer());
        yearInput = addField(fields, "YEAR", song.getYear());
        trackInput = addField(fields, "TRACK", song.getTrackNumber());
        discInput = addField(fields, "DISC", song.getDiscNumber());

        scrollView.addView(fields);
        outer.addView(
                scrollView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(390)
                )
        );

        TextView onlineButton = new TextView(context);
        onlineButton.setText("✦  UPDATE METADATA ONLINE");
        onlineButton.setTextSize(14);
        onlineButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        onlineButton.setGravity(android.view.Gravity.CENTER);
        onlineButton.setPadding(
                dp(12),
                dp(14),
                dp(12),
                dp(14)
        );
        onlineButton.setClickable(true);
        onlineButton.setFocusable(true);
        onlineButton.setOnClickListener(v -> updateMetadataOnline());

        LinearLayout.LayoutParams onlineParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        onlineParams.topMargin = vertical;
        outer.addView(onlineButton, onlineParams);

        return outer;
    }

    private EditText addField(
            LinearLayout parent,
            String label,
            String value) {

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(11);
        labelView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        labelView.setAlpha(0.70f);

        parent.addView(
                labelView,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        EditText input = new EditText(context);
        input.setSingleLine(true);
        input.setText(value == null ? "" : value);
        input.setTextSize(15);
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        if ("YEAR".equals(label) ||
                "TRACK".equals(label) ||
                "DISC".equals(label)) {
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
        }

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
        params.bottomMargin = dp(7);
        parent.addView(input, params);

        return input;
    }

    private void saveManualMetadata() {
        applyInputsToSong();

        if (!overrideManager.save(song)) {
            Toast.makeText(
                    context,
                    "Couldn't save metadata",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        if (listener != null) {
            listener.onMetadataSaved(song);
        }

        Toast.makeText(
                context,
                "Metadata saved",
                Toast.LENGTH_SHORT
        ).show();

        if (dialog != null) {
            dialog.dismiss();
        }
    }

    private void applyInputsToSong() {
        song.setTitle(valueOf(titleInput));
        song.setArtist(valueOf(artistInput));
        song.setAlbum(valueOf(albumInput));
        song.setAlbumArtist(valueOf(albumArtistInput));
        song.setGenre(valueOf(genreInput));
        song.setComposer(valueOf(composerInput));
        song.setYear(valueOf(yearInput));
        song.setTrackNumber(valueOf(trackInput));
        song.setDiscNumber(valueOf(discInput));
    }

    private String valueOf(EditText input) {
        if (input == null) {
            return null;
        }

        String value = input.getText()
                .toString()
                .trim();

        return value.isEmpty() ? null : value;
    }

    private void updateMetadataOnline() {
        if (song.getUri() == null || song.getUri().trim().isEmpty()) {
            Toast.makeText(
                    context,
                    "This song has no valid URI",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        AlertDialog progressDialog = new AlertDialog.Builder(context)
                .setTitle("Updating metadata")
                .setMessage("Creating fingerprint and searching AcoustID / MusicBrainz…")
                .setView(new ProgressBar(context))
                .setNegativeButton("Cancel", null)
                .create();

        progressDialog.show();

        Thread worker = new Thread(() -> {
            MusicIdentifier.Result result;

            try {
                MusicIdentifier identifier =
                        new MusicIdentifier(context.getApplicationContext());

                result = identifier.identify(song);

            } catch (Throwable error) {
                result = MusicIdentifier.Result.error(
                        safeError(error)
                );
            }

            final MusicIdentifier.Result finalResult = result;

            android.os.Handler mainHandler =
                    new android.os.Handler(
                            context.getMainLooper()
                    );

            mainHandler.post(() -> {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                showCandidates(finalResult);
            });
        });

        worker.start();
    }

    private void showCandidates(MusicIdentifier.Result result) {
        if (result == null || !result.isSuccess()) {
            showError(
                    result == null
                            ? "Metadata identification failed"
                            : safe(result.getError())
            );
            return;
        }

        ArrayList<MusicMetadataCandidate> candidates =
                result.getCandidates();

        if (candidates == null || candidates.isEmpty()) {
            showError("No matching metadata was found");
            return;
        }

        int count = Math.min(candidates.size(), 8);
        String[] labels = new String[count];

        for (int i = 0; i < count; i++) {
            labels[i] = buildCandidateLabel(candidates.get(i));
        }

        AlertDialog candidateDialog = new AlertDialog.Builder(context)
                .setTitle("Online metadata")
                .setSingleChoiceItems(
                        labels,
                        0,
                        null
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("SELECT", null)
                .create();

        candidateDialog.setOnShowListener(d -> {
            candidateDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setOnClickListener(v -> {
                        int selected = candidateDialog
                                .getListView()
                                .getCheckedItemPosition();

                        if (selected < 0 || selected >= count) {
                            return;
                        }

                        candidateDialog.dismiss();
                        showCandidateConfirmation(
                                candidates.get(selected)
                        );
                    });
        });

        candidateDialog.show();
    }

    private String buildCandidateLabel(
            MusicMetadataCandidate candidate) {

        if (candidate == null) {
            return "Unknown match";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(candidate.getSafeTitle());
        builder.append("\n");
        builder.append(candidate.getSafeArtist());

        String album = candidate.getSafeAlbum();
        if (!isUnknown(album)) {
            builder.append("\n");
            builder.append(album);
        }

        builder.append("\nMatch ");
        builder.append(
                String.format(
                        Locale.US,
                        "%.0f%%",
                        candidate.getMatchScore()
                )
        );

        return builder.toString();
    }

    private void showCandidateConfirmation(
            MusicMetadataCandidate candidate) {

        if (candidate == null) {
            return;
        }

        StringBuilder message = new StringBuilder();
        appendComparison(message, "Title", song.getTitle(), candidate.getTitle());
        appendComparison(message, "Artist", song.getArtist(), candidate.getArtist());
        appendComparison(message, "Album", song.getAlbum(), candidate.getAlbum());
        appendComparison(message, "Album Artist", song.getAlbumArtist(), candidate.getAlbumArtist());
        appendComparison(message, "Genre", song.getGenre(), candidate.getGenre());
        appendComparison(message, "Composer", song.getComposer(), candidate.getComposer());
        appendComparison(message, "Year", song.getYear(), candidate.getYear());
        appendComparison(message, "Track", song.getTrackNumber(), candidate.getTrackNumber());
        appendComparison(message, "Disc", song.getDiscNumber(), candidate.getDiscNumber());

        message.append("\nSource: ")
                .append(safe(candidate.getSource()));

        message.append("\nMatch: ")
                .append(
                        String.format(
                                Locale.US,
                                "%.0f%%",
                                candidate.getMatchScore()
                        )
                );

        new AlertDialog.Builder(context)
                .setTitle("Confirm metadata")
                .setMessage(message.toString())
                .setNegativeButton("BACK", null)
                .setPositiveButton(
                        "APPLY",
                        (dialogInterface, which) -> applyCandidate(candidate)
                )
                .show();
    }

    private void appendComparison(
            StringBuilder message,
            String label,
            String current,
            String incoming) {

        String oldValue = display(current);
        String newValue = display(incoming);

        message.append(label)
                .append(": ")
                .append(oldValue)
                .append(" → ")
                .append(newValue)
                .append("\n");
    }

    private void applyCandidate(
            MusicMetadataCandidate candidate) {

        if (hasValue(candidate.getTitle())) {
            song.setTitle(candidate.getTitle().trim());
        }
        if (hasValue(candidate.getArtist())) {
            song.setArtist(candidate.getArtist().trim());
        }
        if (hasValue(candidate.getAlbum())) {
            song.setAlbum(candidate.getAlbum().trim());
        }
        if (hasValue(candidate.getAlbumArtist())) {
            song.setAlbumArtist(candidate.getAlbumArtist().trim());
        }
        if (hasValue(candidate.getGenre())) {
            song.setGenre(candidate.getGenre().trim());
        }
        if (hasValue(candidate.getComposer())) {
            song.setComposer(candidate.getComposer().trim());
        }
        if (hasValue(candidate.getYear())) {
            song.setYear(candidate.getYear().trim());
        }
        if (hasValue(candidate.getTrackNumber())) {
            song.setTrackNumber(candidate.getTrackNumber().trim());
        }
        if (hasValue(candidate.getDiscNumber())) {
            song.setDiscNumber(candidate.getDiscNumber().trim());
        }

        if (!overrideManager.save(song)) {
            showError("Couldn't save the selected metadata");
            return;
        }

        updateEditorFields();

        if (listener != null) {
            listener.onMetadataSaved(song);
        }

        Toast.makeText(
                context,
                "Online metadata applied",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void updateEditorFields() {
        if (titleInput != null) titleInput.setText(displayForInput(song.getTitle()));
        if (artistInput != null) artistInput.setText(displayForInput(song.getArtist()));
        if (albumInput != null) albumInput.setText(displayForInput(song.getAlbum()));
        if (albumArtistInput != null) albumArtistInput.setText(displayForInput(song.getAlbumArtist()));
        if (genreInput != null) genreInput.setText(displayForInput(song.getGenre()));
        if (composerInput != null) composerInput.setText(displayForInput(song.getComposer()));
        if (yearInput != null) yearInput.setText(displayForInput(song.getYear()));
        if (trackInput != null) trackInput.setText(displayForInput(song.getTrackNumber()));
        if (discInput != null) discInput.setText(displayForInput(song.getDiscNumber()));
    }

    private String displayForInput(String value) {
        return value == null ? "" : value;
    }

    private void showError(String message) {
        new AlertDialog.Builder(context)
                .setTitle("Metadata update")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    private String safeError(Throwable error) {
        if (error == null) {
            return "Unknown error";
        }

        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? error.getClass().getSimpleName()
                : message;
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty()
                ? "Unknown"
                : value.trim();
    }

    private String display(String value) {
        return safe(value);
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isUnknown(String value) {
        return value == null || value.trim().isEmpty()
                || "Unknown".equalsIgnoreCase(value.trim());
    }

    private int dp(int value) {
        return Math.round(
                value * context.getResources()
                        .getDisplayMetrics().density
        );
    }
}
