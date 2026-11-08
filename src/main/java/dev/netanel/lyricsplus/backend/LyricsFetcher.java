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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds lyrics on LRCLIB, trying hard: exact match, field search, free-text search and
 * YouTube-style "Artist - Song (Official Video)" cleanup. Synced lyrics win; if a song only
 * has plain lyrics, they are spread over the song with estimated timing.
 */
public final class LyricsFetcher {

    /** failed = nothing could be checked (network down, rate limited): worth retrying later. */
    public record Result(List<LyricsLine> lines, boolean estimated, boolean failed) {
        public static final Result NONE = new Result(List.of(), false, false);
        public static final Result FAILED = new Result(List.of(), false, true);
    }

    // per-fetch bookkeeping (fetch() only runs on the single fetch thread)
    private int answered;
    private boolean transientError;

    private static final String USER_AGENT = "LyricsPlus/2.0 (Minecraft Fabric mod)";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();
    private final LyricsManager parser = new LyricsManager();

    public Result fetch(String artist, String title, String album, long durationSec) {
        answered = 0;
        transientError = false;
        String plainFallback = null;

        // 1. exact match
        try {
            JsonObject exact = getObject(exactUrl(artist, title, album, durationSec));
            if (exact != null) {
                String synced = field(exact, "syncedLyrics");
                if (synced != null) {
                    return synced(synced);
                }
                plainFallback = field(exact, "plainLyrics");
            }
        } catch (Exception e) {
            System.err.println("[Lyrics Plus] exact lookup failed: " + e.getMessage());
        }

        // 2. searches, best guesses first
        for (String url : searchUrls(artist, title)) {
            try {
                JsonArray results = getArray(url);
                if (results == null) {
                    continue;
                }
                JsonObject bestSynced = null;
                long bestDiff = Long.MAX_VALUE;
                for (JsonElement el : results) {
                    if (!el.isJsonObject()) continue;
                    JsonObject o = el.getAsJsonObject();
                    if (field(o, "syncedLyrics") != null) {
                        long diff = durationSec > 0 && o.has("duration") && !o.get("duration").isJsonNull()
                                ? Math.abs(Math.round(o.get("duration").getAsDouble()) - durationSec) : 0;
                        if (diff < bestDiff) {
                            bestDiff = diff;
                            bestSynced = o;
                        }
                    } else if (plainFallback == null) {
                        plainFallback = field(o, "plainLyrics");
                    }
                }
                // Accept a synced version only if its length roughly matches the song.
                if (bestSynced != null && (durationSec <= 0 || bestDiff <= 8)) {
                    return synced(field(bestSynced, "syncedLyrics"));
                }
                if (bestSynced != null && plainFallback == null) {
                    plainFallback = stripTimestamps(field(bestSynced, "syncedLyrics"));
                }
            } catch (Exception e) {
                System.err.println("[Lyrics Plus] search failed: " + e.getMessage());
            }
        }

        // 3. plain lyrics with estimated timing
        if (plainFallback != null) {
            List<LyricsLine> est = estimate(plainFallback, durationSec);
            if (!est.isEmpty()) {
                return new Result(est, true, false);
            }
        }
        return transientError && answered == 0 ? Result.FAILED : Result.NONE;
    }

    private Result synced(String lrc) {
        List<LyricsLine> lines = parser.parseLyrics(lrc);
        return lines.isEmpty() ? Result.NONE : new Result(lines, false, false);
    }

    // ------------------------------------------------------------------ query building

    private static String exactUrl(String artist, String title, String album, long durationSec) {
        StringBuilder url = new StringBuilder("https://lrclib.net/api/get?artist_name=").append(enc(artist))
                .append("&track_name=").append(enc(title));
        if (album != null && !album.isBlank()) url.append("&album_name=").append(enc(album));
        if (durationSec > 0) url.append("&duration=").append(durationSec);
        return url.toString();
    }

    private static List<String> searchUrls(String artist, String title) {
        Set<String> urls = new LinkedHashSet<>();
        String cleanArtist = cleanArtist(artist);
        String cleanTitle = cleanTitle(title);

        // YouTube style: "Artist - Song (Official Video)"
        String ytArtist = null;
        String ytTitle = null;
        int dash = indexOfDash(cleanTitle);
        if (dash > 0) {
            ytArtist = cleanTitle.substring(0, dash).trim();
            ytTitle = cleanTitle.substring(dash + 3).trim();
        }

        urls.add(fieldSearch(cleanArtist, cleanTitle));
        if (ytArtist != null && !ytTitle.isEmpty()) {
            urls.add(fieldSearch(ytArtist, ytTitle));
            urls.add(textSearch(ytArtist + " " + ytTitle));
        }
        urls.add(textSearch(cleanArtist + " " + cleanTitle));
        urls.add(textSearch(ytTitle != null && !ytTitle.isEmpty() ? ytTitle : cleanTitle));
        return new ArrayList<>(urls);
    }

    private static String fieldSearch(String artist, String title) {
        return "https://lrclib.net/api/search?artist_name=" + enc(artist) + "&track_name=" + enc(title);
    }

    private static String textSearch(String q) {
        return "https://lrclib.net/api/search?q=" + enc(q.trim());
    }

    private static int indexOfDash(String s) {
        for (String d : new String[]{" - ", " – ", " — ", " | "}) {
            int i = s.indexOf(d);
            if (i > 0) return i;
        }
        return -1;
    }

    /** "Song (feat. X) [Official Music Video] - Remastered 2011 | HD" -> "Song" */
    static String cleanTitle(String title) {
        String t = title;
        t = t.replaceAll("(?i)[(\\[](feat\\.?|ft\\.?|with|prod\\.?)[^)\\]]*[)\\]]", "");
        t = t.replaceAll("(?i)[(\\[][^)\\]]*(official|video|audio|lyric|lyrics|visualizer|clip|hd|4k|hq|live|remaster|mv|m/v|קליפ|רשמי)[^)\\]]*[)\\]]", "");
        t = t.replaceAll("(?i)\\s+-\\s+[^-]*(remaster|version|edit|mix)[^-]*$", "");
        t = t.replaceAll("(?i)\\b(official (music )?video|official audio|lyric video|lyrics|hd|4k)\\b", "");
        t = t.replaceAll("(?i)\\s+(feat\\.?|ft\\.?)\\s+.*$", "");
        t = t.replaceAll("\\s{2,}", " ").replaceAll("[\\s|\\-–—]+$", "").trim();
        return t.isEmpty() ? title : t;
    }

    /** "ArtistVEVO", "Artist - Topic", "Artist Official" -> "Artist" */
    static String cleanArtist(String artist) {
        String a = artist.replaceAll("(?i)vevo$", "").replaceAll("(?i)\\s*-\\s*topic$", "")
                .replaceAll("(?i)\\s+official$", "").trim();
        int comma = a.indexOf(", ");
        if (comma > 0) a = a.substring(0, comma); // "A, B" -> "A"
        return a.isEmpty() ? artist : a;
    }

    // ------------------------------------------------------------------ estimated timing

    /** Spread plain lines over the song, longer lines get more time, blank lines are pauses. */
    static List<LyricsLine> estimate(String plain, long durationSec) {
        String[] raw = plain.split("\\R");
        List<String> lines = new ArrayList<>();
        for (String l : raw) {
            String t = l.trim();
            if (t.startsWith("[") && t.endsWith("]")) continue; // [Chorus] etc.
            if (t.isEmpty() && (lines.isEmpty() || lines.getLast().isEmpty())) continue;
            lines.add(t);
        }
        while (!lines.isEmpty() && lines.getLast().isEmpty()) lines.removeLast();
        if (lines.isEmpty()) return List.of();

        long durMs = (durationSec > 0 ? durationSec : 210) * 1000L;
        long start = Math.max(6000, durMs * 8 / 100);
        long end = durMs - Math.max(6000, durMs * 6 / 100);
        if (end <= start) end = start + 1000L * lines.size();

        double total = 0;
        double[] w = new double[lines.size()];
        for (int i = 0; i < w.length; i++) {
            w[i] = lines.get(i).isEmpty() ? 18 : lines.get(i).length() + 10;
            total += w[i];
        }
        List<LyricsLine> out = new ArrayList<>();
        out.add(new LyricsLine(0, ""));
        double acc = 0;
        for (int i = 0; i < w.length; i++) {
            long ts = start + Math.round((end - start) * (acc / total));
            out.add(new LyricsLine(ts, lines.get(i)));
            acc += w[i];
        }
        return out;
    }

    private static String stripTimestamps(String lrc) {
        return lrc == null ? null : lrc.replaceAll("(?m)^\\s*\\[[0-9:.]+\\]\\s*", "");
    }

    // ------------------------------------------------------------------ http

    private JsonObject getObject(String url) throws Exception {
        String body = get(url);
        if (body == null) return null;
        JsonElement el = JsonParser.parseString(body);
        return el.isJsonObject() ? el.getAsJsonObject() : null;
    }

    private JsonArray getArray(String url) throws Exception {
        String body = get(url);
        if (body == null) return null;
        JsonElement el = JsonParser.parseString(body);
        return el.isJsonArray() ? el.getAsJsonArray() : null;
    }

    private String get(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", USER_AGENT)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        HttpResponse<String> response;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            transientError = true;
            throw e;
        }
        int code = response.statusCode();
        if (code == 200 || code == 404) {
            answered++;
        } else if (code == 429 || code >= 500) {
            transientError = true;
        }
        return code == 200 ? response.body() : null;
    }

    private static String field(JsonObject json, String name) {
        if (!json.has(name) || json.get(name).isJsonNull()) return null;
        String s = json.get(name).getAsString();
        return s.isBlank() ? null : s;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }
}
