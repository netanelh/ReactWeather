package dev.netanel.lyricsplus.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** All user settings. Saved to config/lyricsplus.json. */
public final class LyricsConfig {

    public enum DisplayMode { SIDE, SUBTITLE, CENTER, WORLD, SKY }

    public enum RevealMode { LINE, WORD, KARAOKE }

    public enum SidePosition { TOP_LEFT, TOP_RIGHT, MIDDLE_LEFT, MIDDLE_RIGHT }

    public enum WorldStyle { WORDS, SCATTER, LINE }

    public enum ScatterFrom { CENTER, SIDES, BELOW, ABOVE, PLAYER }

    /** Lyric font styles. id = font json under assets/lyricsplus/font, null = vanilla. */
    public enum FontStyle {
        MINECRAFT(null, true), ROUND("round", true), BOLD("bold", true), HANDWRITTEN("handwritten", true),
        ELEGANT("elegant", true), SCRIPT("script", false), COMIC("comic", false), TALL("tall", false);

        public final String id;
        public final boolean hebrew;

        FontStyle(String id, boolean hebrew) {
            this.id = id;
            this.hebrew = hebrew;
        }
    }

    public enum PanelPosition { CENTER, LEFT, RIGHT }

    public enum WordAnimation { POP, RISE, DROP, SPIN, NONE }

    public enum WordColors { HIGHLIGHT, SINGLE, RAINBOW, RANDOM }

    /** Quick-pick swatches shown in the panel. */
    public static final int[] SWATCHES = {
            0xFFFFFF, 0xFFE066, 0xFFAA00, 0xFF8C42, 0xFF5555, 0xFF77C8,
            0xB57BFF, 0x7C83FF, 0x55AAFF, 0x55FFFF, 0x55FF77, 0x1ED760, 0xAAAAAA, 0x222222
    };

    public DisplayMode displayMode = DisplayMode.SIDE;
    public RevealMode revealMode = RevealMode.KARAOKE;
    public SidePosition sidePosition = SidePosition.TOP_LEFT;
    public int textColor = 0xFFFFFF;
    public boolean textRainbow = false;
    public int highlightColor = 0x1ED760;
    public boolean highlightRainbow = false;
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
    public WorldStyle worldStyle = WorldStyle.WORDS;
    public boolean worldFollow = true;
    public float wordScale = 1.4f;
    public float currentWordScale = 1.3f;
    public float wordSpacing = 1.0f;
    public float worldHeight = 0.0f;
    public WordAnimation wordAnimation = WordAnimation.POP;
    public WordColors wordColors = WordColors.HIGHLIGHT;
    public float scatterSpread = 1.5f;
    public ScatterFrom scatterFrom = ScatterFrom.CENTER;
    public float scatterTilt = 15f;
    public float scatterSizeVariation = 0.3f;
    public FontStyle font = FontStyle.ROUND;
    public float skyDistance = 26f;
    public float skyHeight = 9f;
    public boolean albumArt = true;
    public boolean albumColors = false;
    public PanelPosition panelPosition = PanelPosition.CENTER;

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
                String json = Files.readString(p, StandardCharsets.UTF_8);
                if (json.contains("\"textColor\": \"")) { // v1.0 stored colors as names; start fresh
                    return new LyricsConfig();
                }
                LyricsConfig c = GSON.fromJson(json, LyricsConfig.class);
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
        if (worldStyle == null) worldStyle = WorldStyle.WORDS;
        if (wordAnimation == null) wordAnimation = WordAnimation.POP;
        if (wordColors == null) wordColors = WordColors.HIGHLIGHT;
        if (scatterFrom == null) scatterFrom = ScatterFrom.CENTER;
        if (font == null) font = FontStyle.ROUND;
        if (panelPosition == null) panelPosition = PanelPosition.CENTER;
        skyDistance = Math.max(10f, Math.min(60f, skyDistance));
        skyHeight = Math.max(0f, Math.min(30f, skyHeight));
        scatterSpread = Math.max(0.3f, Math.min(3f, scatterSpread));
        scatterTilt = Math.max(0f, Math.min(45f, scatterTilt));
        scatterSizeVariation = Math.max(0f, Math.min(0.8f, scatterSizeVariation));
        textColor &= 0xFFFFFF;
        highlightColor &= 0xFFFFFF;
        wordScale = Math.max(0.5f, Math.min(4f, wordScale));
        currentWordScale = Math.max(1f, Math.min(2f, currentWordScale));
        wordSpacing = Math.max(0.3f, Math.min(3f, wordSpacing));
        worldHeight = Math.max(-1.5f, Math.min(1.5f, worldHeight));
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
