package dev.netanel.lyricsplus.render;

import dev.netanel.lyricsplus.LyricsEngine.Snapshot;
import dev.netanel.lyricsplus.LyricsEngine.Status;
import dev.netanel.lyricsplus.LyricsPlusClient;
import dev.netanel.lyricsplus.config.LyricsConfig;
import dev.netanel.lyricsplus.config.LyricsConfig.SidePosition;
import dev.netanel.lyricsplus.render.LineFormatter.Paint;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Draws every 2D part of the mod: lyrics, title card, progress bar, status. */
public final class HudRenderer {

    private static final int MARGIN = 10;
    private static final int PAD = 4;

    private HudRenderer() {
    }

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        LyricsConfig cfg = LyricsConfig.get();
        if (!cfg.enabled) {
            return;
        }
        Snapshot s = LyricsPlusClient.engine().snapshot();
        if (s.status() == Status.NO_PLAYER || s.status() == Status.AD || s.title().isBlank()) {
            return;
        }
        Font font = Minecraft.getInstance().font;

        boolean card = cfg.titleCard && s.msSinceTrackChange() < 4500;
        if (card) {
            drawTitleCard(g, font, s, cfg);
        }

        Component current = currentComponent(s, cfg);
        switch (cfg.displayMode) {
            case SIDE -> drawSide(g, font, s, cfg, current);
            case SUBTITLE -> drawSubtitle(g, font, s, cfg, current);
            case CENTER -> {
                if (!card) {
                    drawCenter(g, font, s, cfg, current);
                }
            }
            case WORLD -> drawWorldCompanion(g, font, s, cfg);
        }
    }

    /** What to show as the "current" line, including status and instrumental states. */
    static Component currentComponent(Snapshot s, LyricsConfig cfg) {
        return switch (s.status()) {
            case LOADING -> Component.translatable("lyricsplus.status.loading").withStyle(LineFormatter.style(0xAAAAAA));
            case NOT_FOUND -> s.msSinceTrackChange() < 6000
                    ? Component.translatable("lyricsplus.status.not_found").withStyle(LineFormatter.style(0xAAAAAA))
                    : Component.empty();
            case READY -> {
                if (s.instrumental()) {
                    yield cfg.instrumentalDots ? dots(s, cfg) : Component.empty();
                }
                yield LineFormatter.current(s, cfg);
            }
            default -> Component.empty();
        };
    }

    static Component dots(Snapshot s, LyricsConfig cfg) {
        int n = 1 + (int) ((s.positionMs() / 500) % 3);
        return LineFormatter.colorize("♪ ".repeat(n).trim(), Paint.highlight(cfg), 0);
    }

    // ------------------------------------------------------------------ modes

    private static void drawSide(GuiGraphicsExtractor g, Font font, Snapshot s, LyricsConfig cfg, Component current) {
        float sc = cfg.scale;
        int lineH = Math.round((font.lineHeight + 3) * sc);
        boolean context = cfg.showContextLines && s.status() == Status.READY && !s.instrumental();

        List<Component> rows = new ArrayList<>();
        List<Integer> alphas = new ArrayList<>();
        if (context && !s.line(-1).isBlank()) {
            rows.add(LineFormatter.plain(s.line(-1), cfg));
            alphas.add(0x80);
        }
        int currentRow = rows.size();
        rows.add(current);
        alphas.add(0xFF);
        if (context && !s.line(1).isBlank()) {
            rows.add(LineFormatter.plain(s.line(1), cfg));
            alphas.add(0x80);
        }

        int maxW = 0;
        for (Component c : rows) {
            maxW = Math.max(maxW, Math.round(font.width(c) * sc));
        }
        if (cfg.nowPlaying) {
            maxW = Math.max(maxW, Math.round(font.width(nowPlaying(s)) * 0.75f));
        }
        if (maxW == 0) {
            return;
        }
        maxW = Math.max(maxW, 90);

        int extra = (cfg.nowPlaying ? 10 : 0) + (cfg.progressBar ? 6 : 0);
        int boxH = rows.size() * lineH + extra;
        boolean right = cfg.sidePosition == SidePosition.TOP_RIGHT || cfg.sidePosition == SidePosition.MIDDLE_RIGHT;
        boolean middle = cfg.sidePosition == SidePosition.MIDDLE_LEFT || cfg.sidePosition == SidePosition.MIDDLE_RIGHT;
        int x = right ? g.guiWidth() - MARGIN - maxW : MARGIN;
        int y = middle ? g.guiHeight() / 2 - boxH / 2 : MARGIN;

        background(g, cfg, x - PAD, y - PAD, x + maxW + PAD, y + boxH + PAD - 2);

        int cy = y;
        if (cfg.nowPlaying) {
            Component np = nowPlaying(s);
            int w = Math.round(font.width(np) * 0.75f);
            drawScaled(g, font, np, right ? x + maxW - w : x, cy, 0.75f, 0xFFFFFFFF, cfg.shadow);
            cy += 10;
        }

        long intoLine = s.positionMs() - s.lineStartMs();
        float anim = cfg.animations ? Math.clamp(intoLine / 180f, 0f, 1f) : 1f;
        for (int i = 0; i < rows.size(); i++) {
            Component c = rows.get(i);
            int w = Math.round(font.width(c) * sc);
            int rx = right ? x + maxW - w : x;
            int slide = Math.round((1f - anim) * lineH * 0.5f);
            int alpha = i == currentRow ? Math.max(0x20, Math.round(0xFF * anim)) : alphas.get(i);
            drawScaled(g, font, c, rx, cy + slide, sc, (alpha << 24) | 0xFFFFFF, cfg.shadow);
            cy += lineH;
        }

        if (cfg.progressBar) {
            progressBar(g, font, s, cfg, x, cy + 1, maxW, right);
        }
    }

    private static void drawSubtitle(GuiGraphicsExtractor g, Font font, Snapshot s, LyricsConfig cfg, Component current) {
        float sc = cfg.scale * 1.25f;
        int cx = g.guiWidth() / 2;
        int y = g.guiHeight() - 72 - Math.round(font.lineHeight * sc);

        long intoLine = s.positionMs() - s.lineStartMs();
        float anim = cfg.animations ? Math.clamp(intoLine / 200f, 0f, 1f) : 1f;
        int alpha = Math.max(0x20, Math.round(0xFF * anim));

        int w = Math.round(font.width(current) * sc);
        if (w > 0) {
            background(g, cfg, cx - w / 2 - PAD, y - PAD, cx + w / 2 + PAD, y + Math.round(font.lineHeight * sc) + PAD - 1);
            drawScaled(g, font, current, cx - w / 2, y, sc, (alpha << 24) | 0xFFFFFF, cfg.shadow);
        }

        if (cfg.showContextLines && s.status() == Status.READY && !s.line(1).isBlank()) {
            Component next = LineFormatter.plain(s.line(1), cfg);
            float ns = cfg.scale * 0.85f;
            int nw = Math.round(font.width(next) * ns);
            drawScaled(g, font, next, cx - nw / 2, y + Math.round(font.lineHeight * sc) + 6, ns, 0x80FFFFFF, cfg.shadow);
        }
        if (cfg.progressBar && s.durationMs() > 0) {
            int bw = 160;
            progressBar(g, font, s, cfg, cx - bw / 2, y - 14, bw, false);
        }
    }

    private static void drawCenter(GuiGraphicsExtractor g, Font font, Snapshot s, LyricsConfig cfg, Component current) {
        long intoLine = s.positionMs() - s.lineStartMs();
        float pop = cfg.animations ? 0.8f + 0.2f * Math.clamp(intoLine / 160f, 0f, 1f) : 1f;
        float sc = cfg.scale * 2.0f * pop;
        int cx = g.guiWidth() / 2;
        int y = Math.round(g.guiHeight() * 0.30f);

        int w = Math.round(font.width(current) * sc);
        int h = Math.round(font.lineHeight * sc);
        if (w > 0) {
            background(g, cfg, cx - w / 2 - PAD * 2, y - PAD, cx + w / 2 + PAD * 2, y + h + PAD);
            drawScaled(g, font, current, cx - w / 2, y, sc, 0xFFFFFFFF, cfg.shadow);
        }
        if (cfg.showContextLines && s.status() == Status.READY && !s.line(1).isBlank()) {
            Component next = LineFormatter.plain(s.line(1), cfg);
            float ns = cfg.scale * 1.1f;
            int nw = Math.round(font.width(next) * ns);
            drawScaled(g, font, next, cx - nw / 2, y + h + 8, ns, 0x70FFFFFF, cfg.shadow);
        }
        if (cfg.progressBar && s.durationMs() > 0) {
            int bw = 200;
            progressBar(g, font, s, cfg, cx - bw / 2, y - 16, bw, false);
        }
    }

    /** In world mode the lyrics are 3D; the HUD only keeps a small "now playing" corner. */
    private static void drawWorldCompanion(GuiGraphicsExtractor g, Font font, Snapshot s, LyricsConfig cfg) {
        if (!cfg.nowPlaying && !cfg.progressBar) {
            return;
        }
        int w = 140;
        int x = MARGIN;
        int y = MARGIN;
        background(g, cfg, x - PAD, y - PAD, x + w + PAD, y + 16 + PAD);
        if (cfg.nowPlaying) {
            drawScaled(g, font, nowPlaying(s), x, y, 0.75f, 0xFFFFFFFF, cfg.shadow);
        }
        if (cfg.progressBar) {
            progressBar(g, font, s, cfg, x, y + 10, w, false);
        }
    }

    // ------------------------------------------------------------- title card

    private static void drawTitleCard(GuiGraphicsExtractor g, Font font, Snapshot s, LyricsConfig cfg) {
        long t = s.msSinceTrackChange();
        float a = 1f;
        if (cfg.animations) {
            if (t < 400) a = t / 400f;
            else if (t > 3700) a = Math.max(0f, (4500 - t) / 800f);
        }
        int alpha = Math.round(a * 255);
        if (alpha < 8) {
            return;
        }

        int cx = g.guiWidth() / 2;
        int y = Math.round(g.guiHeight() * 0.22f);
        float rise = cfg.animations ? (1f - Math.min(1f, t / 400f)) * 10f : 0f;
        y += Math.round(rise);

        Component label = Component.translatable("lyricsplus.card.now_playing")
                .withStyle(LineFormatter.style(Paint.highlight(cfg).at(0)));
        Component title = LineFormatter.colorize(s.title(), Paint.text(cfg), 0);
        Component artist = Component.literal(s.artist()).withStyle(LineFormatter.style(0xAAAAAA));

        float ts = 2.4f;
        int titleW = Math.round(font.width(title) * ts);
        int maxW = Math.max(titleW, Math.round(font.width(artist) * 1.3f));
        int boxH = 8 + Math.round(font.lineHeight * ts) + 4 + Math.round(font.lineHeight * 1.3f);

        if (cfg.background) {
            int bgA = Math.round(cfg.backgroundOpacity * a * 255);
            g.fill(cx - maxW / 2 - 12, y - 6, cx + maxW / 2 + 12, y + boxH + 6, bgA << 24);
            int accent = (alpha << 24) | Paint.highlight(cfg).at(0);
            g.fill(cx - maxW / 2 - 12, y + boxH + 5, cx + maxW / 2 + 12, y + boxH + 6, accent);
        }

        int argb = (alpha << 24) | 0xFFFFFF;
        int lw = Math.round(font.width(label) * 0.75f);
        drawScaled(g, font, label, cx - lw / 2, y, 0.75f, argb, cfg.shadow);
        drawScaled(g, font, title, cx - titleW / 2, y + 8, ts, argb, true);
        int aw = Math.round(font.width(artist) * 1.3f);
        drawScaled(g, font, artist, cx - aw / 2, y + 12 + Math.round(font.lineHeight * ts), 1.3f, argb, cfg.shadow);
    }

    // ---------------------------------------------------------------- helpers

    private static Component nowPlaying(Snapshot s) {
        String prefix = s.playing() ? "▶ " : "⏸ ";
        return Component.literal(prefix + s.title() + " — " + s.artist());
    }

    private static void progressBar(GuiGraphicsExtractor g, Font font, Snapshot s, LyricsConfig cfg,
                                    int x, int y, int w, boolean alignRight) {
        if (s.durationMs() <= 0) {
            return;
        }
        String times = LineFormatter.time(s.positionMs()) + " / " + LineFormatter.time(s.durationMs());
        int tw = Math.round(font.width(times) * 0.5f) + 4;
        int barW = Math.max(20, w - tw);
        int bx = alignRight ? x + tw : x;
        float p = Math.clamp(s.positionMs() / (float) s.durationMs(), 0f, 1f);
        g.fill(bx, y + 1, bx + barW, y + 3, 0x55FFFFFF);
        g.fill(bx, y + 1, bx + Math.round(barW * p), y + 3, 0xFF000000 | Paint.highlight(cfg).at(0));
        int textX = alignRight ? x : bx + barW + 4;
        drawScaled(g, font, Component.literal(times), textX, y, 0.5f, 0xCCFFFFFF, false);
    }

    private static void background(GuiGraphicsExtractor g, LyricsConfig cfg, int x1, int y1, int x2, int y2) {
        if (!cfg.background || cfg.backgroundOpacity <= 0.01f) {
            return;
        }
        int a = Math.round(cfg.backgroundOpacity * 255);
        g.fill(x1, y1, x2, y2, a << 24);
    }

    static void drawScaled(GuiGraphicsExtractor g, Font font, Component c, int x, int y, float scale, int argb, boolean shadow) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(scale, scale);
        g.text(font, c, 0, 0, argb, shadow);
        g.pose().popMatrix();
    }
}
