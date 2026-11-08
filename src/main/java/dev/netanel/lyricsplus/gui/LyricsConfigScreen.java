package dev.netanel.lyricsplus.gui;

import dev.netanel.lyricsplus.LyricsEngine.Snapshot;
import dev.netanel.lyricsplus.LyricsEngine.Status;
import dev.netanel.lyricsplus.LyricsPlusClient;
import dev.netanel.lyricsplus.backend.AlbumArt;
import dev.netanel.lyricsplus.backend.MediaKeys;
import dev.netanel.lyricsplus.config.LyricsConfig;
import dev.netanel.lyricsplus.config.LyricsConfig.DisplayMode;
import dev.netanel.lyricsplus.config.LyricsConfig.FontStyle;
import dev.netanel.lyricsplus.render.LineFormatter;
import dev.netanel.lyricsplus.render.LineFormatter.Paint;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * The settings panel: a dark card with a sidebar of pages, a now-playing card with
 * playback controls, described rows and live preview (no background blur).
 */
public final class LyricsConfigScreen extends Screen {

    // ---- palette
    private static final int BG = 0xF2141418;
    private static final int SIDEBAR = 0xF20D0D11;
    private static final int CARD = 0xFF1D1D24;
    private static final int CARD_HOVER = 0xFF262630;
    private static final int TEXT = 0xFFF2F2F5;
    private static final int MUTED = 0xFF8A8A98;
    private static final int TRACK = 0xFF34343F;

    private static final int SIDEBAR_W = 114;
    private static final int PAD = 10;

    private static final String[] PAGES = {"now", "lyrics", "look", "world", "hud", "settings"};
    private static final String[] ICONS = {"♫", "✎", "✦", "☁", "▣", "⚙"};

    private final Screen parent;
    private final LyricsConfig cfg = LyricsConfig.get();
    private static int page; // remembered while the game runs
    private double scroll;
    private Row dragging;
    private boolean rebuildPending;
    private final List<Row> rows = new ArrayList<>();

    private int px, py, pw, ph, cx, cw, contentTop, contentBottom;

    public LyricsConfigScreen(Screen parent) {
        super(Component.translatable("lyricsplus.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        buildRows();
    }

    private int accent() {
        return 0xFF000000 | Paint.highlight(cfg).at(0);
    }

    private static boolean worldMode(LyricsConfig c) {
        return c.displayMode == DisplayMode.WORLD || c.displayMode == DisplayMode.SKY;
    }

    // =================================================================== pages

    private void buildRows() {
        rows.clear();
        switch (PAGES[page]) {
            case "now" -> {
                rows.add(new NowPlayingRow());
                rows.add(new HintRow("lyricsplus.hint.keys"));
                rows.add(new SectionRow("presets"));
                rows.add(new PresetRow());
                rows.add(new OffsetRow());
                rows.add(new SectionRow("quick"));
                rows.add(new ToggleRow("enabled", () -> cfg.enabled, v -> cfg.enabled = v).desc());
                rows.add(new ToggleRow("demo", () -> LyricsPlusClient.engine().isDemo(),
                        v -> LyricsPlusClient.engine().setDemo(v)).desc());
            }
            case "lyrics" -> {
                rows.add(new SectionRow("lyrics"));
                rows.add(new ChoiceRow("where", () -> "lyricsplus.mode." + low(cfg.displayMode),
                        d -> cfg.displayMode = cycle(cfg.displayMode, d), true)
                        .descKey(() -> "lyricsplus.desc.where." + low(cfg.displayMode)));
                if (worldMode(cfg)) {
                    rows.add(new ChoiceRow("show", () -> "lyricsplus.world_style." + low(cfg.worldStyle),
                            d -> cfg.worldStyle = cycle(cfg.worldStyle, d), true)
                            .descKey(() -> "lyricsplus.desc.show." + low(cfg.worldStyle)));
                    rows.add(new ToggleRow("world_follow", () -> cfg.worldFollow, v -> cfg.worldFollow = v).desc());
                    rows.add(new ToggleRow("hide_paused", () -> cfg.hideWhenPaused, v -> cfg.hideWhenPaused = v).desc());
                } else {
                    rows.add(new ChoiceRow("show", () -> "lyricsplus.reveal." + low(cfg.revealMode),
                            d -> cfg.revealMode = cycle(cfg.revealMode, d), false)
                            .descKey(() -> "lyricsplus.desc.reveal." + low(cfg.revealMode)));
                    rows.add(new ToggleRow("context_lines", () -> cfg.showContextLines, v -> cfg.showContextLines = v).desc());
                }
                rows.add(new ToggleRow("title_card", () -> cfg.titleCard, v -> cfg.titleCard = v).desc());
                rows.add(new SectionRow("timing"));
                rows.add(new SliderRow("offset", -3000, 3000, 50, () -> cfg.offsetMs, v -> cfg.offsetMs = (int) Math.round(v),
                        v -> String.format(Locale.ROOT, "%+.2fs", v / 1000.0)).desc());
            }
            case "look" -> {
                rows.add(new SectionRow("font"));
                rows.add(new FontTilesRow());
                rows.add(new SectionRow("colors"));
                rows.add(new ColorRow("text_color", () -> cfg.textColor, c -> cfg.textColor = c,
                        () -> cfg.textRainbow, v -> cfg.textRainbow = v));
                rows.add(new ColorRow("highlight_color", () -> cfg.highlightColor, c -> cfg.highlightColor = c,
                        () -> cfg.highlightRainbow, v -> cfg.highlightRainbow = v));
                rows.add(new ToggleRow("album_colors", () -> cfg.albumColors, v -> cfg.albumColors = v).desc());
                rows.add(new SectionRow("text"));
                rows.add(new SliderRow("scale", 0.5, 3.0, 0.1, () -> cfg.scale, v -> cfg.scale = (float) v,
                        v -> String.format(Locale.ROOT, "%.1fx", v)).desc());
                rows.add(new ToggleRow("shadow", () -> cfg.shadow, v -> cfg.shadow = v));
                rows.add(new ToggleRow("background", () -> cfg.background, v -> cfg.background = v));
                rows.add(new SliderRow("bg_opacity", 0, 1, 0.05, () -> cfg.backgroundOpacity, v -> cfg.backgroundOpacity = (float) v,
                        v -> Math.round(v * 100) + "%"));
                rows.add(new ToggleRow("animations", () -> cfg.animations, v -> cfg.animations = v));
            }
            case "world" -> {
                if (!worldMode(cfg)) {
                    rows.add(new ActionRow("lyricsplus.hint.use_world", () -> {
                        cfg.displayMode = DisplayMode.WORLD;
                        rebuildPending = true;
                    }));
                    rows.add(new ActionRow("lyricsplus.hint.use_sky", () -> {
                        cfg.displayMode = DisplayMode.SKY;
                        rebuildPending = true;
                    }));
                }
                rows.add(new SectionRow("words"));
                rows.add(new ChoiceRow("show", () -> "lyricsplus.world_style." + low(cfg.worldStyle),
                        d -> cfg.worldStyle = cycle(cfg.worldStyle, d), true));
                rows.add(new ChoiceRow("word_animation", () -> "lyricsplus.word_animation." + low(cfg.wordAnimation),
                        d -> cfg.wordAnimation = cycle(cfg.wordAnimation, d), false));
                rows.add(new ChoiceRow("word_colors", () -> "lyricsplus.word_colors." + low(cfg.wordColors),
                        d -> cfg.wordColors = cycle(cfg.wordColors, d), false));
                rows.add(new SliderRow("word_scale", 0.5, 4.0, 0.1, () -> cfg.wordScale, v -> cfg.wordScale = (float) v,
                        v -> String.format(Locale.ROOT, "%.1fx", v)));
                rows.add(new SliderRow("current_word_scale", 1.0, 2.0, 0.05, () -> cfg.currentWordScale,
                        v -> cfg.currentWordScale = (float) v, v -> String.format(Locale.ROOT, "%.2fx", v)).desc());
                rows.add(new SliderRow("word_spacing", 0.3, 3.0, 0.1, () -> cfg.wordSpacing, v -> cfg.wordSpacing = (float) v,
                        v -> String.format(Locale.ROOT, "%.1fx", v)));
                if (cfg.worldStyle == LyricsConfig.WorldStyle.SCATTER) {
                    rows.add(new SectionRow("scatter"));
                    rows.add(new ChoiceRow("scatter_from", () -> "lyricsplus.scatter_from." + low(cfg.scatterFrom),
                            d -> cfg.scatterFrom = cycle(cfg.scatterFrom, d), false));
                    rows.add(new SliderRow("scatter_spread", 0.3, 3.0, 0.1, () -> cfg.scatterSpread,
                            v -> cfg.scatterSpread = (float) v, v -> String.format(Locale.ROOT, "%.1fx", v)));
                    rows.add(new SliderRow("scatter_tilt", 0, 45, 1, () -> cfg.scatterTilt,
                            v -> cfg.scatterTilt = (float) v, v -> Math.round(v) + "°"));
                    rows.add(new SliderRow("scatter_size", 0, 0.8, 0.05, () -> cfg.scatterSizeVariation,
                            v -> cfg.scatterSizeVariation = (float) v, v -> "±" + Math.round(v * 100) + "%"));
                }
                rows.add(new SectionRow("position"));
                rows.add(new ToggleRow("see_through", () -> cfg.seeThrough, v -> cfg.seeThrough = v).desc());
                if (cfg.displayMode == DisplayMode.SKY) {
                    rows.add(new SliderRow("sky_distance", 10, 60, 1, () -> cfg.skyDistance, v -> cfg.skyDistance = (float) v,
                            v -> Math.round(v) + "m"));
                    rows.add(new SliderRow("sky_height", 0, 30, 1, () -> cfg.skyHeight, v -> cfg.skyHeight = (float) v,
                            v -> Math.round(v) + "m"));
                } else {
                    rows.add(new SliderRow("world_distance", 2, 8, 0.5, () -> cfg.worldDistance, v -> cfg.worldDistance = (float) v,
                            v -> String.format(Locale.ROOT, "%.1fm", v)));
                    rows.add(new SliderRow("world_height", -1.5, 1.5, 0.1, () -> cfg.worldHeight, v -> cfg.worldHeight = (float) v,
                            v -> String.format(Locale.ROOT, "%+.1fm", v)));
                }
            }
            case "hud" -> {
                rows.add(new SectionRow("hud"));
                rows.add(new ChoiceRow("position", () -> "lyricsplus.position." + low(cfg.sidePosition),
                        d -> cfg.sidePosition = cycle(cfg.sidePosition, d), false).desc());
                rows.add(new ToggleRow("now_playing", () -> cfg.nowPlaying, v -> cfg.nowPlaying = v).desc());
                rows.add(new ToggleRow("album_art", () -> cfg.albumArt, v -> cfg.albumArt = v).desc());
                rows.add(new ToggleRow("progress_bar", () -> cfg.progressBar, v -> cfg.progressBar = v));
                rows.add(new ToggleRow("instrumental", () -> cfg.instrumentalDots, v -> cfg.instrumentalDots = v).desc());
            }
            default -> {
                rows.add(new SectionRow("panel"));
                rows.add(new ChoiceRow("panel_position", () -> "lyricsplus.panel_position." + low(cfg.panelPosition),
                        d -> cfg.panelPosition = cycle(cfg.panelPosition, d), false).desc());
                rows.add(new SectionRow("keys"));
                rows.add(new HintRow("lyricsplus.hint.keys"));
                rows.add(new HintRow("lyricsplus.hint.rebind"));
                rows.add(new SectionRow("reset"));
                rows.add(new ActionRow("lyricsplus.action.reset", () -> {
                    LyricsConfig fresh = new LyricsConfig();
                    copyInto(fresh, cfg);
                    cfg.save();
                    rebuildPending = true;
                }));
                rows.add(new HintRow("lyricsplus.hint.credits"));
            }
        }
    }

    private static void copyInto(LyricsConfig from, LyricsConfig to) {
        for (var f : LyricsConfig.class.getFields()) {
            if (java.lang.reflect.Modifier.isStatic(f.getModifiers())) continue;
            try {
                f.set(to, f.get(from));
            } catch (IllegalAccessException ignored) {
            }
        }
    }

    // =================================================================== rendering

    private void layout() {
        pw = Math.min(480, this.width - 16);
        ph = Math.min(300, this.height - 16);
        px = switch (cfg.panelPosition) {
            case LEFT -> 8;
            case RIGHT -> this.width - pw - 8;
            case CENTER -> (this.width - pw) / 2;
        };
        py = (this.height - ph) / 2;
        cx = px + SIDEBAR_W + PAD;
        cw = pw - SIDEBAR_W - PAD * 2;
        contentTop = py + 40;
        contentBottom = py + ph - 8;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        layout();
        int accent = accent();

        // frame
        g.fill(px, py, px + pw, py + ph, BG);
        g.fill(px, py, px + SIDEBAR_W, py + ph, SIDEBAR);
        g.outline(px, py, pw, ph, 0x28FFFFFF);
        g.fill(px + SIDEBAR_W, py, px + SIDEBAR_W + 1, py + ph, 0x18FFFFFF);

        // sidebar header
        g.fill(px + 8, py + 9, px + 24, py + 25, accent);
        g.text(font, Component.literal("♪"), px + 13, py + 13, 0xFF101010, false);
        g.text(font, Component.literal("Lyrics Plus"), px + 29, py + 9, TEXT, false);
        small(g, Component.translatable("lyricsplus.tagline"), px + 29, py + 20, MUTED);

        // sidebar pages
        int iy = py + 36;
        for (int i = 0; i < PAGES.length; i++) {
            boolean active = i == page;
            boolean hover = inside(mouseX, mouseY, px + 6, iy, SIDEBAR_W - 12, 18);
            if (active) {
                g.fill(px + 6, iy, px + SIDEBAR_W - 6, iy + 18, (accent & 0xFFFFFF) | 0x38000000);
                g.fill(px + 6, iy, px + 8, iy + 18, accent);
            } else if (hover) {
                g.fill(px + 6, iy, px + SIDEBAR_W - 6, iy + 18, CARD);
            }
            g.text(font, Component.literal(ICONS[i]), px + 13, iy + 5, active ? accent : MUTED, false);
            g.text(font, Component.translatable("lyricsplus.page." + PAGES[i]), px + 26, iy + 5, active ? TEXT : MUTED, false);
            iy += 21;
        }

        // sidebar footer: done
        int dy = py + ph - 26;
        boolean doneHover = inside(mouseX, mouseY, px + 8, dy, SIDEBAR_W - 16, 18);
        g.fill(px + 8, dy, px + SIDEBAR_W - 8, dy + 18, doneHover ? accent : CARD_HOVER);
        g.centeredText(font, Component.translatable("lyricsplus.done"), px + SIDEBAR_W / 2, dy + 5, doneHover ? 0xFF101010 : TEXT);
        small(g, Component.literal("v2.2"), px + 10, dy - 11, 0xFF55555F);

        // content header
        g.pose().pushMatrix();
        g.pose().translate(cx, py + 10);
        g.pose().scale(1.25f, 1.25f);
        g.text(font, Component.translatable("lyricsplus.page." + PAGES[page]), 0, 0, TEXT, false);
        g.pose().popMatrix();
        small(g, Component.translatable("lyricsplus.page." + PAGES[page] + ".sub"), cx, py + 24, MUTED);
        boolean closeHover = inside(mouseX, mouseY, px + pw - 20, py + 6, 14, 14);
        g.text(font, "✕", px + pw - 16, py + 9, closeHover ? TEXT : MUTED, false);

        // rows
        int contentH = 0;
        for (Row r : rows) contentH += r.height() + 4;
        double maxScroll = Math.max(0, contentH - (contentBottom - contentTop));
        scroll = Math.clamp(scroll, 0, maxScroll);
        g.enableScissor(cx - 2, contentTop, cx + cw + 4, contentBottom);
        int y = contentTop - (int) scroll;
        for (Row r : rows) {
            r.x = cx;
            r.y = y;
            r.w = cw;
            boolean hover = mouseY >= contentTop && mouseY < contentBottom && inside(mouseX, mouseY, cx, y, cw, r.height());
            r.render(g, hover, mouseX, mouseY);
            y += r.height() + 4;
        }
        g.disableScissor();
        if (maxScroll > 0) {
            int trackH = contentBottom - contentTop;
            int barH = Math.max(16, (int) (trackH * (trackH / (double) contentH)));
            int barY = contentTop + (int) ((trackH - barH) * (scroll / maxScroll));
            g.fill(px + pw - 4, barY, px + pw - 2, barY + barH, 0x50FFFFFF);
        }

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (rebuildPending) {
            rebuildPending = false;
            double keep = scroll;
            buildRows();
            scroll = keep;
        }
    }

    @Override
    public void onClose() {
        cfg.save();
        LyricsPlusClient.engine().setDemo(false);
        this.minecraft.gui.setScreen(parent);
    }

    // =================================================================== input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        layout();
        double mx = event.x();
        double my = event.y();
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        if (inside(mx, my, px + pw - 20, py + 6, 14, 14) || inside(mx, my, px + 8, py + ph - 26, SIDEBAR_W - 16, 18)) {
            onClose();
            return true;
        }
        int iy = py + 36;
        for (int i = 0; i < PAGES.length; i++) {
            if (inside(mx, my, px + 6, iy, SIDEBAR_W - 12, 18)) {
                page = i;
                scroll = 0;
                buildRows();
                return true;
            }
            iy += 21;
        }
        if (my >= contentTop && my < contentBottom) {
            for (Row r : rows) {
                if (inside(mx, my, r.x, r.y, r.w, r.height())) {
                    if (r.click(mx, my)) {
                        dragging = r;
                    }
                    cfg.save();
                    return true;
                }
            }
        }
        return inside(mx, my, px, py, pw, ph) || super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) {
            dragging.drag(event.x(), event.y());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragging != null) {
            dragging = null;
            cfg.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (inside(mx, my, px, py, pw, ph)) {
            scroll -= sy * 20;
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    // =================================================================== rows

    private abstract class Row {
        int x, y, w;
        final String key;
        Supplier<String> descKey;

        Row(String key) {
            this.key = key;
        }

        @SuppressWarnings("unchecked")
        <T extends Row> T desc() {
            this.descKey = () -> "lyricsplus.desc." + key;
            return (T) this;
        }

        @SuppressWarnings("unchecked")
        <T extends Row> T descKey(Supplier<String> k) {
            this.descKey = k;
            return (T) this;
        }

        int baseHeight() {
            return 22;
        }

        int height() {
            return baseHeight() + (descKey != null ? 9 : 0);
        }

        Component label() {
            return Component.translatable("lyricsplus.label." + key);
        }

        void card(GuiGraphicsExtractor g, boolean hover) {
            g.fill(x, y, x + w, y + height(), hover ? CARD_HOVER : CARD);
        }

        /** Title (and gray description below it), vertically centered in the control strip. */
        void title(GuiGraphicsExtractor g) {
            if (descKey == null) {
                g.text(font, label(), x + 8, y + 7, TEXT, false);
            } else {
                g.text(font, label(), x + 8, y + 5, TEXT, false);
                small(g, Component.translatable(descKey.get()), x + 8, y + 17, MUTED);
            }
        }

        /** Y of the right-hand control, centered on the row. */
        int controlY() {
            return y + height() / 2;
        }

        abstract void render(GuiGraphicsExtractor g, boolean hover, int mx, int my);

        boolean click(double mx, double my) {
            return false;
        }

        void drag(double mx, double my) {
        }
    }

    private final class SectionRow extends Row {
        SectionRow(String key) {
            super(key);
        }

        @Override
        int height() {
            return 12;
        }

        @Override
        void render(GuiGraphicsExtractor g, boolean hover, int mx, int my) {
            Component t = Component.literal("◆ ").append(Component.translatable("lyricsplus.section." + key));
            small(g, t, x + 1, y + 4, accent());
            int tw = Math.round(font.width(t) * 0.75f) + 6;
            g.fill(x + tw, y + 7, x + w, y + 8, 0x18FFFFFF);
        }
    }

    private final class ToggleRow extends Row {
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;

        ToggleRow(String key, BooleanSupplier get, Consumer<Boolean> set) {
            super(key);
            this.get = get;
            this.set = set;
        }

        @Override
        void render(GuiGraphicsExtractor g, boolean hover, int mx, int my) {
            card(g, hover);
            title(g);
            boolean on = get.getAsBoolean();
            int sx = x + w - 32;
            int sy = controlY() - 5;
            g.fill(sx, sy, sx + 24, sy + 10, on ? accent() : TRACK);
            int kx = on ? sx + 15 : sx + 1;
            g.fill(kx, sy + 1, kx + 8, sy + 9, 0xFFFFFFFF);
        }

        @Override
        boolean click(double mx, double my) {
            set.accept(!get.getAsBoolean());
            return false;
        }
    }

    private final class ChoiceRow extends Row {
        private final Supplier<String> valueKey;
        private final IntConsumer step;
        private final boolean rebuild;

        ChoiceRow(String key, Supplier<String> valueKey, IntConsumer step, boolean rebuild) {
            super(key);
            this.valueKey = valueKey;
            this.step = step;
            this.rebuild = rebuild;
        }

        @Override
        void render(GuiGraphicsExtractor g, boolean hover, int mx, int my) {
            card(g, hover);
            title(g);
            int bw = 110;
            int bx = x + w - bw - 5;
            int by = controlY() - 8;
            g.fill(bx, by, bx + bw, by + 16, 0xFF15151B);
            boolean leftHover = hover && mx >= bx && mx < bx + bw / 2;
            g.text(font, "←", bx + 4, by + 4, leftHover ? accent() : MUTED, false);
            g.text(font, "→", bx + bw - 10, by + 4, hover && !leftHover && mx >= bx ? accent() : MUTED, false);
            g.centeredText(font, Component.translatable(valueKey.get()), bx + bw / 2, by + 4, TEXT);
        }

        @Override
        boolean click(double mx, double my) {
            int bx = x + w - 110 - 5;
            step.accept(mx >= bx && mx < bx + 55 ? -1 : 1);
            if (rebuild) {
                rebuildPending = true;
            }
            return false;
        }
    }

    private final class SliderRow extends Row {
        private final double min, max, stepSize;
        private final DoubleSupplier get;
        private final DoubleConsumer set;
        private final DoubleFunction<String> format;

        SliderRow(String key, double min, double max, double step, DoubleSupplier get, DoubleConsumer set,
                  DoubleFunction<String> format) {
            super(key);
            this.min = min;
            this.max = max;
            this.stepSize = step;
            this.get = get;
            this.set = set;
            this.format = format;
        }

        @Override
        int baseHeight() {
            return 30;
        }

        @Override
        void render(GuiGraphicsExtractor g, boolean hover, int mx, int my) {
            card(g, hover || dragging == this);
            if (descKey == null) {
                g.text(font, label(), x + 8, y + 6, TEXT, false);
            } else {
                g.text(font, label(), x + 8, y + 5, TEXT, false);
                small(g, Component.translatable(descKey.get()), x + 8, y + 16, MUTED);
            }
            String v = format.apply(get.getAsDouble());
            g.text(font, v, x + w - 8 - font.width(v), y + 6, accent(), false);
            int tx = x + 8;
            int tw = w - 16;
            int ty = y + height() - 8;
            double p = Math.clamp((get.getAsDouble() - min) / (max - min), 0, 1);
            int fillX = tx + (int) Math.round(tw * p);
            g.fill(tx, ty, tx + tw, ty + 2, TRACK);
            g.fill(tx, ty, fillX, ty + 2, accent());
            g.fill(fillX - 3, ty - 3, fillX + 3, ty + 5, 0xFFFFFFFF);
        }

        @Override
        boolean click(double mx, double my) {
            drag(mx, my);
            return true;
        }

        @Override
        void drag(double mx, double my) {
            double p = Math.clamp((mx - (x + 8)) / (double) (w - 16), 0, 1);
            set.accept(Math.round((min + p * (max - min)) / stepSize) * stepSize);
        }
    }

    private final class ColorRow extends Row {
        private final IntSupplier get;
        private final IntConsumer set;
        private final BooleanSupplier rainbow;
        private final Consumer<Boolean> setRainbow;
        private int dragBar = -1;
        private float hue = -1f; // remembered so black/white/gray don't snap the shade bar to red

        private float hue() {
            if (hue < 0) {
                float[] hsv = toHsv(get.getAsInt());
                hue = hsv[1] < 0.05f ? 0.75f : hsv[0];
            }
            return hue;
        }

        ColorRow(String key, IntSupplier get, IntConsumer set, BooleanSupplier rainbow, Consumer<Boolean> setRainbow) {
            super(key);
            this.get = get;
            this.set = set;
            this.rainbow = rainbow;
            this.setRainbow = setRainbow;
        }

        @Override
        int baseHeight() {
            return 78;
        }

        private int swatch() {
            return (w - 16 - 13 * 3) / 14;
        }

        @Override
        void render(GuiGraphicsExtractor g, boolean hover, int mx, int my) {
            card(g, false);
            g.text(font, label(), x + 8, y + 6, TEXT, false);
            int cur = rainbow.getAsBoolean() ? LineFormatter.rainbowRgb(0) : get.getAsInt();
            g.fill(x + w - 22, y + 5, x + w - 8, y + 15, 0xFF000000 | cur);
            g.outline(x + w - 22, y + 5, 14, 10, 0x60FFFFFF);
            Component rb = Component.translatable("lyricsplus.color.rainbow");
            int rbw = font.width(rb) + 8;
            int rbx = x + w - 28 - rbw;
            g.fill(rbx, y + 4, rbx + rbw, y + 16, rainbow.getAsBoolean() ? accent() : TRACK);
            g.text(font, LineFormatter.colorize(rb.getString(), new Paint(0, true), 0).copy().withStyle(Style.EMPTY),
                    rbx + 4, y + 6, 0xFFFFFFFF, false);

            int s = swatch();
            for (int i = 0; i < LyricsConfig.SWATCHES.length; i++) {
                int sx = x + 8 + i * (s + 3);
                int c = LyricsConfig.SWATCHES[i];
                g.fill(sx, y + 22, sx + s, y + 32, 0xFF000000 | c);
                if (!rainbow.getAsBoolean() && c == get.getAsInt()) {
                    g.outline(sx - 1, y + 21, s + 2, 12, 0xFFFFFFFF);
                }
            }
            int bx = x + 8;
            int bw = w - 16;
            for (int i = 0; i < bw; i++) {
                g.fill(bx + i, y + 40, bx + i + 1, y + 49, 0xFF000000 | LineFormatter.hsv(i / (float) bw, 0.8f, 1f));
            }
            float h = hue();
            int hk = bx + Math.round(h * bw);
            g.fill(hk - 1, y + 38, hk + 2, y + 51, 0xFFFFFFFF);
            for (int i = 0; i < bw; i++) {
                g.fill(bx + i, y + 56, bx + i + 1, y + 65, 0xFF000000 | shade(h, i / (float) bw));
            }
            small(g, Component.translatable("lyricsplus.hint.color"), bx, y + 68, MUTED);
        }

        @Override
        boolean click(double mx, double my) {
            Component rb = Component.translatable("lyricsplus.color.rainbow");
            int rbw = font.width(rb) + 8;
            if (inside(mx, my, x + w - 28 - rbw, y + 4, rbw, 12)) {
                setRainbow.accept(!rainbow.getAsBoolean());
                return false;
            }
            int s = swatch();
            for (int i = 0; i < LyricsConfig.SWATCHES.length; i++) {
                if (inside(mx, my, x + 8 + i * (s + 3), y + 22, s, 10)) {
                    set.accept(LyricsConfig.SWATCHES[i]);
                    float[] hsv = toHsv(LyricsConfig.SWATCHES[i]);
                    if (hsv[1] >= 0.05f) hue = hsv[0];
                    setRainbow.accept(false);
                    return false;
                }
            }
            if (my >= y + 37 && my < y + 52) dragBar = 0;
            else if (my >= y + 53 && my < y + 68) dragBar = 1;
            else return false;
            drag(mx, my);
            return true;
        }

        @Override
        void drag(double mx, double my) {
            float p = (float) Math.clamp((mx - (x + 8)) / (double) (w - 16), 0, 1);
            setRainbow.accept(false);
            if (dragBar == 0) {
                hue = p;
                set.accept(LineFormatter.hsv(p, 0.8f, 1f));
            } else if (dragBar == 1) {
                set.accept(shade(hue(), p));
            }
        }

        private int shade(float hue, float t) {
            if (t < 0.5f) return LineFormatter.hsv(hue, 0.8f, t * 2f);
            return LineFormatter.hsv(hue, 0.8f * (1f - (t - 0.5f) * 2f), 1f);
        }
    }

    /** Font style picker: tiles that preview each font. */
    private final class FontTilesRow extends Row {
        private static final int COLS = 4;
        private static final int TILE_H = 26;

        FontTilesRow() {
            super("font");
        }

        @Override
        int height() {
            int rowsN = (FontStyle.values().length + COLS - 1) / COLS;
            return rowsN * (TILE_H + 4) - 4;
        }

        private int tileW() {
            return (w - (COLS - 1) * 4) / COLS;
        }

        @Override
        void render(GuiGraphicsExtractor g, boolean hover, int mx, int my) {
            FontStyle[] all = FontStyle.values();
            int tw = tileW();
            for (int i = 0; i < all.length; i++) {
                int tx = x + (i % COLS) * (tw + 4);
                int ty = y + (i / COLS) * (TILE_H + 4);
                boolean sel = cfg.font == all[i];
                boolean th = inside(mx, my, tx, ty, tw, TILE_H);
                g.fill(tx, ty, tx + tw, ty + TILE_H, th ? CARD_HOVER : CARD);
                if (sel) {
                    g.outline(tx, ty, tw, TILE_H, accent());
                }
                Style st = LineFormatter.withFont(Style.EMPTY.withColor(TextColor.fromRgb(sel ? accent() & 0xFFFFFF : 0xF2F2F5)), all[i]);
                Component name = Component.translatable("lyricsplus.font." + low(all[i])).copy().withStyle(st);
                g.centeredText(font, name, tx + tw / 2, ty + 6, 0xFFFFFFFF);
                Component sample = Component.literal(hebrew(all[i]) ? "Aa · אב" : "Aa").withStyle(
                        LineFormatter.withFont(Style.EMPTY.withColor(TextColor.fromRgb(0x8A8A98)), all[i]));
                g.pose().pushMatrix();
                g.pose().translate(tx + tw / 2f, ty + 16);
                g.pose().scale(0.7f, 0.7f);
                g.centeredText(font, sample, 0, 0, 0xFFFFFFFF);
                g.pose().popMatrix();
            }
        }

        @Override
        boolean click(double mx, double my) {
            FontStyle[] all = FontStyle.values();
            int tw = tileW();
            for (int i = 0; i < all.length; i++) {
                if (inside(mx, my, x + (i % COLS) * (tw + 4), y + (i / COLS) * (TILE_H + 4), tw, TILE_H)) {
                    cfg.font = all[i];
                }
            }
            return false;
        }

        private boolean hebrew(FontStyle f) {
            return f.hebrew;
        }
    }

    /** Cover, title, artist, lyrics status, time, progress and playback buttons. */
    private final class NowPlayingRow extends Row {
        NowPlayingRow() {
            super("now");
        }

        @Override
        int height() {
            return 60;
        }

        private int btnX(int i) {
            return x + w - 8 - (3 - i) * 20;
        }

        @Override
        void render(GuiGraphicsExtractor g, boolean hover, int mx, int my) {
            Snapshot s = LyricsPlusClient.engine().snapshot();
            card(g, false);
            int art = 44;
            int ax = x + 8, ay = y + 8;
            if (AlbumArt.ready() && s.status() != Status.NO_PLAYER) {
                g.blit(AlbumArt.TEXTURE, ax, ay, ax + art, ay + art, 0f, 1f, 0f, 1f);
            } else {
                g.fill(ax, ay, ax + art, ay + art, 0xFF2A2A34);
                g.centeredText(font, Component.literal("♪"), ax + art / 2, ay + art / 2 - 4, accent());
            }
            int tx = ax + art + 10;
            int textW = btnX(0) - tx - 6;
            boolean none = s.status() == Status.NO_PLAYER || s.title().isBlank();
            g.enableScissor(tx, y, tx + textW, y + height());
            g.text(font, none ? Component.translatable("lyricsplus.now.nothing") : Component.literal(s.title()), tx, y + 9, TEXT, false);
            small(g, Component.literal(none ? "" : s.artist()), tx, y + 21, MUTED);
            g.disableScissor();

            // status badge + time
            String badgeKey;
            int badgeColor;
            switch (s.status()) {
                case READY -> {
                    badgeKey = s.estimated() ? "estimated" : "synced";
                    badgeColor = s.estimated() ? 0xFF4A90E2 : 0xFF1DB954;
                }
                case LOADING -> { badgeKey = "loading"; badgeColor = 0xFFE0A030; }
                case NOT_FOUND -> { badgeKey = "no_lyrics"; badgeColor = 0xFF6A6A76; }
                case AD -> { badgeKey = "ad"; badgeColor = 0xFFB05050; }
                default -> { badgeKey = "idle"; badgeColor = 0xFF4A4A56; }
            }
            Component badge = Component.literal("• ").append(Component.translatable("lyricsplus.now." + badgeKey));
            int bw = Math.round(font.width(badge) * 0.75f) + 8;
            g.fill(tx, y + 31, tx + bw, y + 41, (badgeColor & 0xFFFFFF) | 0x40000000);
            small(g, badge, tx + 4, y + 33, badgeColor);
            if (s.durationMs() > 0) {
                small(g, Component.literal(LineFormatter.time(s.positionMs()) + " / " + LineFormatter.time(s.durationMs())),
                        tx + bw + 6, y + 33, MUTED);
            }

            // progress
            int barX = tx, barW = x + w - 8 - tx;
            g.fill(barX, y + 48, barX + barW, y + 50, TRACK);
            if (s.durationMs() > 0) {
                float p = Math.clamp(s.positionMs() / (float) s.durationMs(), 0f, 1f);
                g.fill(barX, y + 48, barX + Math.round(barW * p), y + 50, accent());
            }

            // controls
            String[] icons = {"«", s.playing() ? "❚❚" : "▶", "»"};
            for (int i = 0; i < 3; i++) {
                int bx = btnX(i);
                boolean bh = inside(mx, my, bx, y + 8, 18, 18);
                g.fill(bx, y + 8, bx + 18, y + 26, i == 1 ? (bh ? 0xFFFFFFFF : accent()) : (bh ? CARD_HOVER : 0xFF15151B));
                g.centeredText(font, Component.literal(icons[i]), bx + 9, y + 13, i == 1 ? 0xFF101010 : TEXT);
            }
        }

        @Override
        boolean click(double mx, double my) {
            for (int i = 0; i < 3; i++) {
                if (inside(mx, my, btnX(i), y + 8, 18, 18)) {
                    switch (i) {
                        case 0 -> MediaKeys.previous();
                        case 1 -> MediaKeys.playPause();
                        default -> MediaKeys.next();
                    }
                }
            }
            return false;
        }
    }

    /** Three one-click looks. */
    private final class PresetRow extends Row {
        PresetRow() {
            super("presets");
        }

        @Override
        int height() {
            return 30;
        }

        private int tileW() {
            return (w - 8) / 3;
        }

        @Override
        void render(GuiGraphicsExtractor g, boolean hover, int mx, int my) {
            LyricsConfig.Preset[] all = LyricsConfig.Preset.values();
            int tw = tileW();
            for (int i = 0; i < all.length; i++) {
                int tx = x + i * (tw + 4);
                boolean th = inside(mx, my, tx, y, tw, height());
                g.fill(tx, y, tx + tw, y + height(), th ? CARD_HOVER : CARD);
                g.fill(tx, y, tx + 2, y + height(), accent());
                g.centeredText(font, Component.translatable("lyricsplus.preset." + low(all[i])), tx + tw / 2, y + 6, TEXT);
                g.pose().pushMatrix();
                g.pose().translate(tx + tw / 2f, y + 18);
                g.pose().scale(0.7f, 0.7f);
                g.centeredText(font, Component.translatable("lyricsplus.preset." + low(all[i]) + ".sub"), 0, 0, MUTED);
                g.pose().popMatrix();
            }
        }

        @Override
        boolean click(double mx, double my) {
            LyricsConfig.Preset[] all = LyricsConfig.Preset.values();
            int tw = tileW();
            for (int i = 0; i < all.length; i++) {
                if (inside(mx, my, x + i * (tw + 4), y, tw, height())) {
                    cfg.apply(all[i]);
                    cfg.save();
                }
            }
            return false;
        }
    }

    /** Lyrics too early / late: buttons instead of hidden keys. */
    private final class OffsetRow extends Row {
        OffsetRow() {
            super("offset");
        }

        private int btnX(int i) {
            return x + w - 6 - (3 - i) * 40;
        }

        @Override
        void render(GuiGraphicsExtractor g, boolean hover, int mx, int my) {
            card(g, hover);
            g.text(font, label(), x + 8, y + 7, TEXT, false);
            String v = String.format(Locale.ROOT, "%+.1fs", cfg.offsetMs / 1000.0);
            g.text(font, v, btnX(0) - 8 - font.width(v), y + 7, accent(), false);
            String[] labels = {"-0.1", "0", "+0.1"};
            for (int i = 0; i < 3; i++) {
                int bx = btnX(i);
                boolean bh = inside(mx, my, bx, y + 3, 36, 16);
                g.fill(bx, y + 3, bx + 36, y + 19, bh ? accent() : 0xFF15151B);
                g.centeredText(font, Component.literal(labels[i]), bx + 18, y + 7, bh ? 0xFF101010 : TEXT);
            }
        }

        @Override
        boolean click(double mx, double my) {
            for (int i = 0; i < 3; i++) {
                if (inside(mx, my, btnX(i), y + 3, 36, 16)) {
                    cfg.offsetMs = switch (i) {
                        case 0 -> Math.max(-5000, cfg.offsetMs - 100);
                        case 1 -> 0;
                        default -> Math.min(5000, cfg.offsetMs + 100);
                    };
                }
            }
            return false;
        }
    }

    private final class ActionRow extends Row {
        private final Runnable action;

        ActionRow(String key, Runnable action) {
            super(key);
            this.action = action;
        }

        @Override
        void render(GuiGraphicsExtractor g, boolean hover, int mx, int my) {
            g.fill(x, y, x + w, y + height(), hover ? accent() : CARD_HOVER);
            g.centeredText(font, Component.translatable(key), x + w / 2, y + 7, hover ? 0xFF101010 : TEXT);
        }

        @Override
        boolean click(double mx, double my) {
            action.run();
            return false;
        }
    }

    private final class HintRow extends Row {
        HintRow(String key) {
            super(key);
        }

        @Override
        int height() {
            return 12;
        }

        @Override
        void render(GuiGraphicsExtractor g, boolean hover, int mx, int my) {
            small(g, Component.translatable(key), x + 2, y + 3, MUTED);
        }
    }

    // =================================================================== helpers

    private void small(GuiGraphicsExtractor g, Component c, int x, int y, int color) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(0.75f, 0.75f);
        g.text(font, c, 0, 0, color, false);
        g.pose().popMatrix();
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static <E extends Enum<E>> E cycle(E value, int dir) {
        E[] all = value.getDeclaringClass().getEnumConstants();
        return all[Math.floorMod(value.ordinal() + dir, all.length)];
    }

    private static String low(Enum<?> e) {
        return e.name().toLowerCase(Locale.ROOT);
    }

    private static float[] toHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f, g = ((rgb >> 8) & 0xFF) / 255f, b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h;
        if (d == 0) h = 0;
        else if (max == r) h = ((g - b) / d) % 6;
        else if (max == g) h = (b - r) / d + 2;
        else h = (r - g) / d + 4;
        h /= 6f;
        if (h < 0) h += 1;
        return new float[]{h, max == 0 ? 0 : d / max, max};
    }
}
