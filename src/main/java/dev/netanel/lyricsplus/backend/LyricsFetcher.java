package dev.netanel.lyricsplus.backend;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/** Fetches synced lyrics from LRCLIB: exact match first, then a search fallback. */
public final class LyricsFetcher {

    private static final String USER_AGENT = "LyricsPlus/1.0 (Minecraft Fabric mod)";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();
    private final LyricsManager parser = new LyricsManager();

    public List<LyricsLine> fetch(String artist, String title, String album, long durationSec) {
        String synced = null;
        try {
            synced = fetchExact(artist, title, album, durationSec);
        } catch (Exception e) {
            System.err.println("[Lyrics Plus] exact lookup failed: " + e.getMessage());
        }
        if (synced == null) {
            try {
                synced = search(artist, cleanTitle(title));
            } catch (Exception e) {
                System.err.println("[Lyrics Plus] search failed: " + e.getMessage());
            }
        }
        return parser.parseLyrics(synced);
    }

    private String fetchExact(String artist, String title, String album, long durationSec) throws Exception {
        StringBuilder url = new StringBuilder("https://lrclib.net/api/get?artist_name=")
                .append(enc(artist))
                .append("&track_name=").append(enc(title));
        if (album != null && !album.isBlank()) {
            url.append("&album_name=").append(enc(album));
        }
        if (durationSec > 0) {
            url.append("&duration=").append(durationSec);
        }
        String body = get(url.toString());
        if (body == null) {
            return null;
        }
        return syncedFrom(JsonParser.parseString(body).getAsJsonObject());
    }

    private String search(String artist, String title) throws Exception {
        String body = get("https://lrclib.net/api/search?artist_name=" + enc(artist) + "&track_name=" + enc(title));
        if (body == null) {
            return null;
        }
        JsonElement root = JsonParser.parseString(body);
        if (!root.isJsonArray()) {
            return null;
        }
        JsonArray results = root.getAsJsonArray();
        for (JsonElement el : results) {
            if (el.isJsonObject()) {
                String synced = syncedFrom(el.getAsJsonObject());
                if (synced != null) {
                    return synced;
                }
            }
        }
        return null;
    }

    private String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode() == 200 ? response.body() : null;
    }

    private static String syncedFrom(JsonObject json) {
        if (!json.has("syncedLyrics") || json.get("syncedLyrics").isJsonNull()) {
            return null;
        }
        String s = json.get("syncedLyrics").getAsString();
        return s.isBlank() ? null : s;
    }

    /** "Song (feat. X) - Remastered 2011" -> "Song" */
    static String cleanTitle(String title) {
        String t = title.replaceAll("(?i)\\s*[(\\[](feat\\.?|ft\\.?|with)[^)\\]]*[)\\]]", "");
        t = t.replaceAll("(?i)\\s+-\\s+.*(remaster|version|live|edit|mix).*$", "");
        return t.trim().isEmpty() ? title : t.trim();
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
