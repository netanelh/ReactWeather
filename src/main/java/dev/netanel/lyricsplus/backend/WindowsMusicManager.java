package dev.netanel.lyricsplus.backend;

/**
 * Polls the Windows media session on its own thread. Everything about the current track is
 * published as ONE immutable State, so readers never see a new artist with an old title.
 */
public final class WindowsMusicManager extends MusicManager implements AutoCloseable {

    private record State(String artist, String title, String album, long durationMs,
                         long positionMs, long capturedNanos, boolean playing) {
        static final State EMPTY = new State("", "", "", 0, 0, System.nanoTime(), false);
    }

    private volatile State state = State.EMPTY;
    private volatile boolean running = true;
    private long lastErrorLog;
    private final Thread worker;

    public WindowsMusicManager() {
        worker = new Thread(this::runWorker, "LyricsPlus-Windows-Media");
        worker.setDaemon(true);
        worker.start();
    }

    private void runWorker() {
        // The session must be opened, used and closed on this same thread.
        while (running) {
            try (WindowsMediaSession media = WindowsMediaSession.open()) {
                int fails = 0;
                while (running) {
                    media.beginPoll();
                    try {
                        if (!media.refreshSession()) {
                            state = State.EMPTY; // player closed: nothing is playing
                            fails = 0;
                        } else {
                            String[] tad = media.getTitleArtistAlbum();
                            boolean playing = media.isPlaying();
                            long duration = media.getDurationMs();
                            long position = media.getPositionMs(playing);
                            state = new State(tad[1], tad[0], tad[2], duration, position, System.nanoTime(), playing);
                            fails = 0;
                        }
                    } catch (Throwable e) {
                        fails++;
                        // Keep the last snapshot if one read fails. Log at most once a minute.
                        long nowMs = System.currentTimeMillis();
                        if (nowMs - lastErrorLog > 60_000) {
                            lastErrorLog = nowMs;
                            System.err.println("[Lyrics Plus] Windows media read failed: " + e.getMessage());
                        }
                    } finally {
                        media.endPoll();
                    }
                    if (fails >= 12) {
                        // ~3 s of failed reads (e.g. Windows media service restarted): stop showing
                        // the frozen song and reopen the session.
                        state = State.EMPTY;
                        break;
                    }
                    Thread.sleep(250);
                }
            } catch (InterruptedException e) {
                if (!running) {
                    break;
                }
            } catch (Throwable e) {
                if (running) {
                    state = State.EMPTY;
                    System.err.println("[Lyrics Plus] Windows media initialization failed: " + e.getMessage());
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException interrupted) {
                        if (!running) {
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public String getArtist() {
        return state.artist();
    }

    @Override
    public String getTitle() {
        return state.title();
    }

    @Override
    public String getAlbum() {
        return state.album();
    }

    @Override
    public long getDuration() {
        return state.durationMs() / 1000;
    }

    @Override
    public long getPosition() {
        State s = state;
        long current = s.positionMs();
        if (s.playing()) {
            current += Math.max(0L, (System.nanoTime() - s.capturedNanos()) / 1_000_000L);
        }
        if (s.durationMs() > 0) {
            current = Math.min(current, s.durationMs());
        }
        return Math.max(0L, current);
    }

    @Override
    public boolean isPlaying() {
        return state.playing();
    }

    @Override
    public void close() {
        running = false;
        worker.interrupt();
    }
}
