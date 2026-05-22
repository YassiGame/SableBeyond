package me.yassigame.sable_beyond.utils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

public class MCLogApi {
    private static final Gson GSON = new Gson();

    private final HttpClient client;

    public MCLogApi() {
        client = HttpClient.newHttpClient();
    }

    public String post(final String text) {
        Map<String, String> payload = Map.of(
                "content", text,
                "source", "SableBeyond"
        );

        final String body = GSON.toJson(payload);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.mclo.gs/1/log"))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            return "Error uploading mod list: " + e.getMessage();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "Error uploading mod list: interrupted";
        }

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return "Error uploading mod list: HTTP " + response.statusCode();
        }

        final McLogResponse mcLogResponse;
        try {
            mcLogResponse = GSON.fromJson(response.body(), McLogResponse.class);
        } catch (JsonSyntaxException exception) {
            return "Error uploading mod list: invalid response";
        }

        if (mcLogResponse == null || mcLogResponse.url() == null || mcLogResponse.url().isBlank()) {
            return "Error uploading mod list: missing url";
        }

        return mcLogResponse.url();
    }

    private record McLogResponse(String url) {
    }
}
