package com.urfavxbf.kanade.ui.youtube;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

final class YouTubeDownloader extends Downloader {

    private static final String USER_AGENT =
            "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 "
                    + "Chrome/140.0 Mobile Safari/537.36";

    @Override
    public Response execute(Request request) throws IOException, ReCaptchaException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(request.url()).openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(20000);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestMethod(request.httpMethod());
            connection.setRequestProperty("User-Agent", USER_AGENT);
            connection.setRequestProperty("Accept-Encoding", "identity");

            Map<String, List<String>> headers = request.headers();
            if (headers != null) {
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    if (entry.getKey() == null || entry.getValue() == null) {
                        continue;
                    }
                    for (String value : entry.getValue()) {
                        if (value != null) {
                            connection.addRequestProperty(entry.getKey(), value);
                        }
                    }
                }
            }

            byte[] data = request.dataToSend();
            if (data != null && data.length > 0) {
                connection.setDoOutput(true);
                connection.setFixedLengthStreamingMode(data.length);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(data);
                }
            }

            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();
            InputStream input = responseCode >= 400
                    ? connection.getErrorStream()
                    : connection.getInputStream();
            String body = input == null ? "" : readBody(input);

            return new Response(
                    responseCode,
                    responseMessage,
                    normalizeHeaders(connection.getHeaderFields()),
                    body,
                    connection.getURL().toString()
            );
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readBody(InputStream input) throws IOException {
        try (InputStream stream = input;
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = stream.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toString("UTF-8");
        }
    }

    private static Map<String, List<String>> normalizeHeaders(
            Map<String, List<String>> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            List<String> values = entry.getValue() == null
                    ? Collections.emptyList()
                    : new ArrayList<>(entry.getValue());
            result.put(entry.getKey(), values);
        }
        return result;
    }
}
