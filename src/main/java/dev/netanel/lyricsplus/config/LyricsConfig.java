package dev.netanel.lyricsplus.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** All user settings. Saved to config/lyricsplus.json. */
public final class LyricsConfig {

    public enum DisplayMode { SIDE, SUBTITLE, CENTER, WORLD }

    public enum RevealMode { LINE, WORD, KARAOKE }

    public enum SidePosition { TOP_LEFT, TOP_RIGHT, MIDDLE_LEFT, MIDDLE_RIGHT }

    public enum ColorPreset {
        WHITE(0xFFFFFF), YELLOW(0xFFE066), GOLD(0xFFAA00), ORANGE(0xFF8C42), RED(0xFF5555),
        PINK(0xFF77C8), PURPLE(0xB57BFF), BLUE(0x55AAFF), CYAN(0x55FFFF), GREEN(0x55FF77),
        SPOTIFY(0x1ED760), GRAY(0xAAAAAA), RAINBOW(-1);

        public final int rgb;

        ColorPreset(int rgb) {
            this.rgb = rgb;
        }
    }

    public DisplayMode displayMode = DisplayMode.SIDE;
    public RevealMode revealMode = RevealMode.KARAOKE;
    public SidePosition sidePosition = SidePosition.TOP_LEFT;
    public ColorPreset textColor = ColorPreset.WHITE;
    public ColorPreset highlightColor = ColorPreset.SPOTIFY;
    public float scale = 1.0f;
    public int offsetMs = 0;
    public boolean enabled = true;
    public boolean titleCard = true;
    public boolean showContextLines = true;
    public boolean background = true;
    public float backgroundOpacity = 0.45f;
    public boolean shadow = true;
    public boolean progressBar = true;
    public boolean nowPlaying = true;
    public boolean animations = true;
    public boolean instrumentalDots = true;
    public float worldDistance = 3.5f;

    // ---- persistence ----

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static LyricsConfig instance;

    public static LyricsConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("lyricsplus.json");
    }

    private static LyricsConfig load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                LyricsConfig c = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), LyricsConfig.class);
                if (c != null) {
                    c.sanitize();
                    return c;
                }
            }
        } catch (Exception e) {
            System.err.println("[Lyrics Plus] Could not read config, using defaults: " + e.getMessage());
        }
        return new LyricsConfig();
    }

    public void save() {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("[Lyrics Plus] Could not save config: " + e.getMessage());
        }
    }

    private void sanitize() {
        if (displayMode == null) displayMode = DisplayMode.SIDE;
        if (revealMode == null) revealMode = RevealMode.KARAOKE;
        if (sidePosition == null) sidePosition = SidePosition.TOP_LEFT;
        if (textColor == null) textColor = ColorPreset.WHITE;
        if (highlightColor == null) highlightColor = ColorPreset.SPOTIFY;
        scale = Math.max(0.5f, Math.min(3.0f, scale));
        offsetMs = Math.max(-5000, Math.min(5000, offsetMs));
        backgroundOpacity = Math.max(0f, Math.min(1f, backgroundOpacity));
        worldDistance = Math.max(2f, Math.min(8f, worldDistance));
    }

    public static <E extends Enum<E>> E next(E value) {
        E[] all = value.getDeclaringClass().getEnumConstants();
        return all[(value.ordinal() + 1) % all.length];
    }
}
