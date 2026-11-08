package dev.netanel.lyricsplus.render;

import dev.netanel.lyricsplus.LyricsEngine.Snapshot;
import dev.netanel.lyricsplus.config.LyricsConfig;
import dev.netanel.lyricsplus.config.LyricsConfig.ColorPreset;
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

    private LineFormatter() {
    }

    /** Current line, styled according to the reveal mode. */
    public static Component current(Snapshot s, LyricsConfig cfg) {
        String text = s.line(0);
        String[] words = text.trim().split("\\s+");
        if (text.isBlank() || words.length == 0) {
            return Component.empty();
        }

        if (cfg.revealMode == RevealMode.LINE) {
            return colorize(text, cfg.textColor, 0);
        }

        // Spread the words over the time the line is actually being sung.
        long lineLen = Math.max(1, s.lineEndMs() - s.lineStartMs());
        long singLen = Math.min(lineLen, Math.max(700, text.length() * 75L));
        double progress = Math.clamp((s.positionMs() - s.lineStartMs()) / (double) singLen, 0.0, 1.0);

        int totalChars = 0;
        for (String w : words) {
            totalChars += w.length() + 1;
        }
        double reachedChars = progress * totalChars;

        MutableComponent out = Component.empty();
        int charsSoFar = 0;
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            boolean started = charsSoFar <= reachedChars || i == 0;
            charsSoFar += w.length() + 1;
            if (cfg.revealMode == RevealMode.WORD && !started) {
                break;
            }
            ColorPreset color = cfg.revealMode == RevealMode.KARAOKE
                    ? (started ? cfg.highlightColor : cfg.textColor)
                    : cfg.textColor;
            if (i > 0) {
                out.append(Component.literal(" "));
            }
            out.append(Component.literal(w).withStyle(styleFor(color, i)));
        }
        return out;
    }

    /** A plain context line (previous / next), always in the text color. */
    public static Component plain(String text, LyricsConfig cfg) {
        return colorize(text, cfg.textColor, 0);
    }

    public static Component colorize(String text, ColorPreset color, int seed) {
        if (color != ColorPreset.RAINBOW) {
            return Component.literal(text).withStyle(styleFor(color, seed));
        }
        MutableComponent out = Component.empty();
        String[] words = text.split(" ", -1);
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                out.append(Component.literal(" "));
            }
            out.append(Component.literal(words[i]).withStyle(styleFor(color, seed + i)));
        }
        return out;
    }

    public static Style styleFor(ColorPreset color, int index) {
        return Style.EMPTY.withColor(TextColor.fromRgb(rgb(color, index)));
    }

    public static int rgb(ColorPreset color, int index) {
        if (color != ColorPreset.RAINBOW) {
            return color.rgb;
        }
        float hue = ((System.currentTimeMillis() % 4000L) / 4000f + index * 0.08f) % 1f;
        return hsv(hue, 0.65f, 1f);
    }

    private static int hsv(float h, float s, float v) {
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

    public static String time(long ms) {
        long sec = Math.max(0, ms) / 1000;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }
}
