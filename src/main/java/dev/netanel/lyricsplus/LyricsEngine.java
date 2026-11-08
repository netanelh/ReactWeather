package dev.netanel.lyricsplus;

import dev.netanel.lyricsplus.backend.LinuxMusicManager;
import dev.netanel.lyricsplus.backend.LyricsFetcher;
import dev.netanel.lyricsplus.backend.LyricsLine;
import dev.netanel.lyricsplus.backend.MusicManager;
import dev.netanel.lyricsplus.backend.WindowsMusicManager;
import dev.netanel.lyricsplus.config.LyricsConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Tracks the current song, loads lyrics off-thread, and answers "what is on screen right now". */
public final class LyricsEngine {

    public enum Status { NO_PLAYER, AD, LOADING, READY, NOT_FOUND }

    /** Immutable view of the current playback moment, built once per frame. */
    public record Snapshot(
            String title, String artist, long positionMs, long durationMs, boolean playing,
            Status status, List<LyricsLine> lines, int index,
            long lineStartMs, long lineEndMs, long msSinceTrackChange, boolean demo) {

        public boolean hasLine() {
            return lines != null && index >= 0 && index < lines.size();
        }

        public String line(int offset) {
            int i = index + offset;
            if (lines == null || i < 0 || i >= lines.size()) {
                return "";
            }
            return lines.get(i).getText();
        }

        /** Before the first lyric line, or on an empty (instrumental) line. */
        public boolean instrumental() {
            if (status != Status.READY || lines == null || lines.isEmpty()) {
                return false;
            }
            return index < 0 || line(0).isBlank();
        }
    }

    private static final List<LyricsLine> DEMO_LINES = List.of(
            new LyricsLine(0, ""),
            new LyricsLine(4000, "Welcome to Lyrics Plus"),
            new LyricsLine(7000, "Press ; or type /lyrics"),
            new LyricsLine(10000, "Pick a color and a style"),
            new LyricsLine(13000, "Let the words float in the world"),
            new LyricsLine(16500, "Or sing along karaoke style"),
            new LyricsLine(20000, ""),
            new LyricsLine(23000, "The music never stops")
    );
    private static final long DEMO_LENGTH = 27000;

    private final ExecutorService fetchThread = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "LyricsPlus-Fetch");
        t.setDaemon(true);
        return t;
    });
    private final LyricsFetcher fetcher = new LyricsFetcher();
    private final Map<String, List<LyricsLine>> cache = new HashMap<>();
    private final boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");

    private MusicManager music;
    private long nextConnectAttempt;

    private volatile String trackKey = "";
    private volatile String title = "";
    private volatile String artist = "";
    private volatile List<LyricsLine> lines;
    private volatile Status status = Status.NO_PLAYER;
    private volatile long trackChangedAt = System.currentTimeMillis();
    private volatile int generation;

    private volatile boolean adPlaying;
    private boolean demo;
    private long demoStart;

    public void setDemo(boolean on) {
        if (on == demo) {
            return;
        }
        demo = on;
        demoStart = System.currentTimeMillis();
        trackChangedAt = demoStart;
        if (!on) {
            trackKey = ""; // force a reload of the real song
        }
    }

    public boolean isDemo() {
        return demo;
    }

    /** Called every client tick. Cheap: reads cached values from the media thread. */
    public void tick() {
        if (demo) {
            long now = System.currentTimeMillis();
            if (now - demoStart >= DEMO_LENGTH) {
                demoStart = now;
                trackChangedAt = now;
            }
            return;
        }
        ensurePlayer();
        if (music == null) {
            status = Status.NO_PLAYER;
            return;
        }

        String a;
        String t;
        try {
            a = music.getArtist();
            t = music.getTitle();
        } catch (Exception e) {
            music = windows ? music : null; // Linux: Spotify probably closed, reconnect later
            status = Status.NO_PLAYER;
            trackKey = "";
            return;
        }
        if (a == null || t == null || t.isBlank()) {
            return;
        }
        // Ads: hide everything until music comes back. The song before the ad keeps its key,
        // so if it simply resumes we don't replay the title card.
        adPlaying = isAd(a, t);
        if (adPlaying) {
            return;
        }

        String key = a + " - " + t;
        if (!key.equals(trackKey)) {
            onTrackChanged(key, a, t);
        }
    }

    static boolean isAd(String artist, String title) {
        String t = title.trim().toLowerCase(java.util.Locale.ROOT);
        String a = artist.trim().toLowerCase(java.util.Locale.ROOT);
        if (t.equals("advertisement") || t.equals("spotify") || t.equals("spotify free")
                || t.startsWith("advertisement") || t.equals("ad") || t.startsWith("ad ·") || t.startsWith("ad:")) {
            return true;
        }
        if (a.equals("spotify") || a.equals("advertisement")) {
            return true;
        }
        // Spotify ads come with no artist; real songs always have one.
        return a.isEmpty();
    }

    private void ensurePlayer() {
        if (music != null || System.currentTimeMillis() < nextConnectAttempt) {
            return;
        }
        nextConnectAttempt = System.currentTimeMillis() + 5000;
        try {
            music = windows ? new WindowsMusicManager() : new LinuxMusicManager();
        } catch (Throwable e) {
            music = null;
        }
    }

    private void onTrackChanged(String key, String a, String t) {
        trackKey = key;
        artist = a;
        title = t;
        trackChangedAt = System.currentTimeMillis();
        int gen = ++generation;
        dev.netanel.lyricsplus.backend.AlbumArt.request(a, t);

        List<LyricsLine> cached;
        synchronized (cache) {
            cached = cache.get(key);
        }
        if (cached != null) {
            lines = cached;
            status = cached.isEmpty() ? Status.NOT_FOUND : Status.READY;
            return;
        }

        lines = null;
        status = Status.LOADING;
        String album = safe(music::getAlbum);
        long durationSec = music.getDuration();

        fetchThread.submit(() -> {
            List<LyricsLine> result = fetcher.fetch(a, t, album, durationSec);
            synchronized (cache) {
                cache.put(key, result);
            }
            if (gen == generation) { // ignore results for a song we already skipped
                lines = result;
                status = result.isEmpty() ? Status.NOT_FOUND : Status.READY;
            }
        });
    }

    public Snapshot snapshot() {
        LyricsConfig cfg = LyricsConfig.get();
        long now = System.currentTimeMillis();

        if (demo) {
            long pos = now - demoStart;
            return build("Lyrics Plus Demo", "Netanel", pos, DEMO_LENGTH, true,
                    Status.READY, DEMO_LINES, now - trackChangedAt, true);
        }
        if (music == null) {
            return build("", "", 0, 0, false, Status.NO_PLAYER, null, now - trackChangedAt, false);
        }
        if (adPlaying) {
            return build("", "", 0, 0, false, Status.AD, null, now - trackChangedAt, false);
        }
        long pos = music.getPosition() + cfg.offsetMs;
        long durMs = music.getDuration() * 1000L;
        return build(title, artist, pos, durMs, music.isPlaying(), status, lines,
                now - trackChangedAt, false);
    }

    private static Snapshot build(String title, String artist, long pos, long dur, boolean playing,
                                  Status status, List<LyricsLine> lines, long sinceChange, boolean demo) {
        int index = -1;
        long start = 0;
        long end = 0;
        if (lines != null && !lines.isEmpty()) {
            for (int i = 0; i < lines.size(); i++) {
                if (lines.get(i).getTimeStamp() > pos) {
                    break;
                }
                index = i;
            }
            if (index >= 0) {
                start = lines.get(index).getTimeStamp();
                end = index + 1 < lines.size()
                        ? lines.get(index + 1).getTimeStamp()
                        : start + 6000;
            } else {
                end = lines.getFirst().getTimeStamp();
            }
        }
        return new Snapshot(title, artist, Math.max(0, pos), dur, playing, status, lines, index,
                start, end, sinceChange, demo);
    }

    private static String safe(java.util.function.Supplier<String> s) {
        try {
            String v = s.get();
            return v == null ? "" : v;
        } catch (Exception e) {
            return "";
        }
    }
}
