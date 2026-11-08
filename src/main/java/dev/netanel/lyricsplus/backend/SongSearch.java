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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Finds songs by name (or a word from the name) using two free, key-less catalogs:
 * iTunes (big catalog, popularity order) and LRCLIB (lyrics database, many Hebrew songs).
 * Also opens a result in Spotify or YouTube.
 */
public final class SongSearch {

    public record Hit(String title, String artist, boolean hasLyrics) {
    }

    private static final HttpClient CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build();
    private static final String UA = "LyricsPlus/2.4 (Minecraft Fabric mod)";
    private static final int MAX = 8;

    private SongSearch() {
    }

    public static List<Hit> search(String query) {
        Map<String, Hit> hits = new LinkedHashMap<>();
        String q = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);

        // LRCLIB first: these are guaranteed to have lyrics in the mod.
        try {
            JsonElement root = JsonParser.parseString(get("https://lrclib.net/api/search?q=" + q));
            if (root.isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray()) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    String title = str(o, "trackName");
                    String artist = str(o, "artistName");
                    boolean synced = o.has("syncedLyrics") && !o.get("syncedLyrics").isJsonNull();
                    add(hits, title, artist, synced);
                    if (hits.size() >= MAX / 2) break;
                }
            }
        } catch (Exception e) {
            System.err.println("[Lyrics Plus] song search (lrclib) failed: " + e.getMessage());
        }

        // iTunes: bigger catalog, ordered by popularity.
        try {
            JsonObject root = JsonParser.parseString(
                    get("https://itunes.apple.com/search?entity=song&limit=10&term=" + q)).getAsJsonObject();
            JsonArray results = root.getAsJsonArray("results");
            if (results != null) {
                for (JsonElement el : results) {
                    if (hits.size() >= MAX) break;
                    JsonObject o = el.getAsJsonObject();
                    add(hits, str(o, "trackName"), str(o, "artistName"), false);
                }
            }
        } catch (Exception e) {
            System.err.println("[Lyrics Plus] song search (itunes) failed: " + e.getMessage());
        }
        return new ArrayList<>(hits.values());
    }

    private static void add(Map<String, Hit> hits, String title, String artist, boolean lyrics) {
        if (title.isBlank()) return;
        String key = (title + "|" + artist).toLowerCase(Locale.ROOT);
        Hit old = hits.get(key);
        if (old == null || (!old.hasLyrics() && lyrics)) {
            hits.put(key, new Hit(title, artist, lyrics || (old != null && old.hasLyrics())));
        }
    }

    public static void openSpotify(Hit h) {
        open("spotify:search:" + URLEncoder.encode(h.artist() + " " + h.title(), StandardCharsets.UTF_8).replace("+", "%20"));
    }

    public static void openYouTube(Hit h) {
        open("https://www.youtube.com/results?search_query="
                + URLEncoder.encode(h.artist() + " " + h.title(), StandardCharsets.UTF_8));
    }

    private static void open(String uri) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (os.contains("win")) {
                new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", uri).start();
            } else if (os.contains("mac")) {
                new ProcessBuilder("open", uri).start();
            } else {
                new ProcessBuilder("xdg-open", uri).start();
            }
        } catch (Exception e) {
            System.err.println("[Lyrics Plus] could not open " + uri + ": " + e.getMessage());
        }
    }

    private static String get(String url) throws Exception {
        HttpResponse<String> r = CLIENT.send(HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", UA).timeout(Duration.ofSeconds(8)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) throw new IllegalStateException("HTTP " + r.statusCode());
        return r.body();
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }
}
