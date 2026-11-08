package dev.netanel.lyricsplus.render;

import dev.netanel.lyricsplus.LyricsEngine.Snapshot;
import dev.netanel.lyricsplus.config.LyricsConfig;
import dev.netanel.lyricsplus.config.LyricsConfig.RevealMode;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/**
 * Turns a lyric line into one styled Component. The whole line is always a single
 * Component (one styled segment per word), so Minecraft's bidi reordering keeps
 * Hebrew/Arabic lines in the right order.
 */
public final class LineFormatter {

    /** A color choice: a fixed RGB, or an animated rainbow. */
    public record Paint(int rgb, boolean rainbow) {
        public static final Paint GRAY = new Paint(0xAAAAAA, false);

        public static Paint text(LyricsConfig cfg) {
            return new Paint(cfg.textColor, cfg.textRainbow);
        }

        public static Paint highlight(LyricsConfig cfg) {
            return new Paint(cfg.highlightColor, cfg.highlightRainbow);
        }

        public int at(int index) {
            return rainbow ? rainbowRgb(index) : rgb;
        }
    }

    private LineFormatter() {
    }

    /** Words of the current line and the time (ms) each one starts being sung. */
    public record WordTiming(String[] words, long[] starts, long singEndMs) {
        /** Index of the last word that has started, or -1. */
        public int reached(long positionMs) {
            int r = -1;
            for (int i = 0; i < starts.length; i++) {
                if (starts[i] <= positionMs) r = i;
            }
            return r;
        }
    }

    public static WordTiming timing(Snapshot s) {
        String text = s.line(0).trim();
        String[] words = text.isEmpty() ? new String[0] : text.split("\\s+");
        long lineLen = Math.max(1, s.lineEndMs() - s.lineStartMs());
        long singLen = Math.min(lineLen, Math.max(700, text.length() * 75L));
        int total = 0;
        for (String w : words) total += w.length() + 1;
        long[] starts = new long[words.length];
        int chars = 0;
        for (int i = 0; i < words.length; i++) {
            starts[i] = s.lineStartMs() + (total == 0 ? 0 : singLen * chars / total);
            chars += words[i].length() + 1;
        }
        return new WordTiming(words, starts, s.lineStartMs() + singLen);
    }

    /** Current line, styled according to the reveal mode. */
    public static Component current(Snapshot s, LyricsConfig cfg) {
        String text = s.line(0);
        if (text.isBlank()) {
            return Component.empty();
        }
        if (cfg.revealMode == RevealMode.LINE) {
            return colorize(text, Paint.text(cfg), 0);
        }

        WordTiming t = timing(s);
        int reached = Math.max(0, t.reached(s.positionMs()));
        MutableComponent out = Component.empty();
        for (int i = 0; i < t.words().length; i++) {
            boolean started = i <= reached;
            if (cfg.revealMode == RevealMode.WORD && !started) {
                break;
            }
            Paint paint = cfg.revealMode == RevealMode.KARAOKE && started ? Paint.highlight(cfg) : Paint.text(cfg);
            if (i > 0) {
                out.append(Component.literal(" "));
            }
            out.append(Component.literal(t.words()[i]).withStyle(style(paint.at(i))));
        }
        return out;
    }

    /** A plain context line (previous / next), always in the text color. */
    public static Component plain(String text, LyricsConfig cfg) {
        return colorize(text, Paint.text(cfg), 0);
    }

    public static Component colorize(String text, Paint paint, int seed) {
        if (!paint.rainbow()) {
            return Component.literal(text).withStyle(style(paint.rgb()));
        }
        MutableComponent out = Component.empty();
        String[] words = text.split(" ", -1);
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                out.append(Component.literal(" "));
            }
            out.append(Component.literal(words[i]).withStyle(style(paint.at(seed + i))));
        }
        return out;
    }

    public static Style style(int rgb) {
        return Style.EMPTY.withColor(TextColor.fromRgb(rgb & 0xFFFFFF));
    }

    public static int rainbowRgb(int index) {
        float hue = ((System.currentTimeMillis() % 4000L) / 4000f + index * 0.08f) % 1f;
        return hsv(hue, 0.65f, 1f);
    }

    public static int hsv(float h, float s, float v) {
        h = ((h % 1f) + 1f) % 1f;
        int i = (int) (h * 6) % 6;
        float f = h * 6 - (int) (h * 6);
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    /** True if the text is written right-to-left (Hebrew, Arabic...). */
    public static boolean isRtl(String text) {
        for (int i = 0; i < text.length(); i++) {
            byte d = Character.getDirectionality(text.charAt(i));
            if (d == Character.DIRECTIONALITY_RIGHT_TO_LEFT || d == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC) {
                return true;
            }
            if (d == Character.DIRECTIONALITY_LEFT_TO_RIGHT) {
                return false;
            }
        }
        return false;
    }

    public static String time(long ms) {
        long sec = Math.max(0, ms) / 1000;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }
}
