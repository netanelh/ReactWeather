package dev.netanel.lyricsplus.gui;

import dev.netanel.lyricsplus.LyricsPlusClient;
import dev.netanel.lyricsplus.config.LyricsConfig;
import dev.netanel.lyricsplus.config.LyricsConfig.DisplayMode;
import dev.netanel.lyricsplus.render.LineFormatter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

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
 * A custom-drawn settings panel (no vanilla buttons): a dark card docked to the right,
 * tabs on top, scrollable rows. No background blur, so lyrics stay visible for live preview.
 */
public final class LyricsConfigScreen extends Screen {

    // ---- palette
    private static final int BG = 0xF0121216;
    private static final int CARD = 0xFF1C1C22;
    private static final int CARD_HOVER = 0xFF26262E;
    private static final int TEXT = 0xFFF2F2F5;
    private static final int MUTED = 0xFF8C8C99;
    private static final int TRACK = 0xFF34343D;

    private static final int PANEL_W = 260;
    private static final int HEADER_H = 30;
    private static final int TABS_H = 22;
    private static final int FOOTER_H = 30;
    private static final int PAD = 10;

    private static final String[] TABS = {"display", "style", "colors", "world", "more"};

    private final Screen parent;
    private final LyricsConfig cfg = LyricsConfig.get();
    private int tab;
    private double scroll;
    private int contentHeight;
    private Row dragging;
    private final List<Row> rows = new ArrayList<>();

    // panel geometry (recomputed each frame)
    private int px, py, pw, ph, contentTop, contentBottom;

    public LyricsConfigScreen(Screen parent) {
        super(Component.translatable("lyricsplus.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        buildRows();
    }

    private int accent() {
        return 0xFF000000 | LineFormatter.Paint.highlight(cfg).at(0);
    }

    // =================================================================== rows per tab

    private void buildRows() {
        rows.clear();
        scroll = 0;
        switch (TABS[tab]) {
            case "display" -> {
                rows.add(new ChoiceRow("mode", () -> "lyricsplus.mode." + low(cfg.displayMode),
                        d -> cfg.displayMode = cycle(cfg.displayMode, d)));
                rows.add(new ChoiceRow("position", () -> "lyricsplus.position." + low(cfg.sidePosition),
                        d -> cfg.sidePosition = cycle(cfg.sidePosition, d)));
                rows.add(new SliderRow("scale", 0.5, 3.0, 0.1, () -> cfg.scale, v -> cfg.scale = (float) v,
                        v -> String.format(Locale.ROOT, "%.1fx", v)));
                rows.add(new SliderRow("offset", -3000, 3000, 50, () -> cfg.offsetMs, v -> cfg.offsetMs = (int) Math.round(v),
                        v -> String.format(Locale.ROOT, "%+.2fs", v / 1000.0)));
                rows.add(new ToggleRow("enabled", () -> cfg.enabled, v -> cfg.enabled = v));
                rows.add(new HintRow("lyricsplus.hint.keys"));
            }
            case "style" -> {
                rows.add(new ChoiceRow("reveal", () -> "lyricsplus.reveal." + low(cfg.revealMode),
                        d -> cfg.revealMode = cycle(cfg.revealMode, d)));
                rows.add(new ToggleRow("context_lines", () -> cfg.showContextLines, v -> cfg.showContextLines = v));
                rows.add(new ToggleRow("animations", () -> cfg.animations, v -> cfg.animations = v));
                rows.add(new ToggleRow("shadow", () -> cfg.shadow, v -> cfg.shadow = v));
                rows.add(new ToggleRow("background", () -> cfg.background, v -> cfg.background = v));
                rows.add(new SliderRow("bg_opacity", 0, 1, 0.05, () -> cfg.backgroundOpacity, v -> cfg.backgroundOpacity = (float) v,
                        v -> Math.round(v * 100) + "%"));
            }
            case "colors" -> {
                rows.add(new ColorRow("text_color", () -> cfg.textColor, c -> cfg.textColor = c,
                        () -> cfg.textRainbow, v -> cfg.textRainbow = v));
                rows.add(new ColorRow("highlight_color", () -> cfg.highlightColor, c -> cfg.highlightColor = c,
                        () -> cfg.highlightRainbow, v -> cfg.highlightRainbow = v));
            }
            case "world" -> {
                if (cfg.displayMode != DisplayMode.WORLD) {
                    rows.add(new ActionRow("lyricsplus.hint.use_world", () -> {
                        cfg.displayMode = DisplayMode.WORLD;
                        buildRows();
                    }));
                }
                rows.add(new ChoiceRow("world_style", () -> "lyricsplus.world_style." + low(cfg.worldStyle),
                        d -> cfg.worldStyle = cycle(cfg.worldStyle, d)));
                rows.add(new ToggleRow("world_follow", () -> cfg.worldFollow, v -> cfg.worldFollow = v));
                if (cfg.worldStyle == LyricsConfig.WorldStyle.SCATTER) {
                    rows.add(new ChoiceRow("scatter_from", () -> "lyricsplus.scatter_from." + low(cfg.scatterFrom),
                            d -> cfg.scatterFrom = cycle(cfg.scatterFrom, d)));
                    rows.add(new SliderRow("scatter_spread", 0.3, 3.0, 0.1, () -> cfg.scatterSpread,
                            v -> cfg.scatterSpread = (float) v, v -> String.format(Locale.ROOT, "%.1fx", v)));
                    rows.add(new SliderRow("scatter_tilt", 0, 45, 1, () -> cfg.scatterTilt,
                            v -> cfg.scatterTilt = (float) v, v -> Math.round(v) + "°"));
                    rows.add(new SliderRow("scatter_size", 0, 0.8, 0.05, () -> cfg.scatterSizeVariation,
                            v -> cfg.scatterSizeVariation = (float) v, v -> "±" + Math.round(v * 100) + "%"));
                }
                rows.add(new ChoiceRow("word_animation", () -> "lyricsplus.word_animation." + low(cfg.wordAnimation),
                        d -> cfg.wordAnimation = cycle(cfg.wordAnimation, d)));
                rows.add(new ChoiceRow("word_colors", () -> "lyricsplus.word_colors." + low(cfg.wordColors),
                        d -> cfg.wordColors = cycle(cfg.wordColors, d)));
                rows.add(new SliderRow("word_scale", 0.5, 4.0, 0.1, () -> cfg.wordScale, v -> cfg.wordScale = (float) v,
                        v -> String.format(Locale.ROOT, "%.1fx", v)));
                rows.add(new SliderRow("current_word_scale", 1.0, 2.0, 0.05, () -> cfg.currentWordScale,
                        v -> cfg.currentWordScale = (float) v, v -> String.format(Locale.ROOT, "%.2fx", v)));
                rows.add(new SliderRow("word_spacing", 0.3, 3.0, 0.1, () -> cfg.wordSpacing, v -> cfg.wordSpacing = (float) v,
                        v -> String.format(Locale.ROOT, "%.1fx", v)));
                rows.add(new SliderRow("world_distance", 2, 8, 0.5, () -> cfg.worldDistance, v -> cfg.worldDistance = (float) v,
                        v -> String.format(Locale.ROOT, "%.1f", v)));
                rows.add(new SliderRow("world_height", -1.5, 1.5, 0.1, () -> cfg.worldHeight, v -> cfg.worldHeight = (float) v,
                        v -> String.format(Locale.ROOT, "%+.1f", v)));
            }
            default -> {
                rows.add(new ToggleRow("title_card", () -> cfg.titleCard, v -> cfg.titleCard = v));
                rows.add(new ToggleRow("now_playing", () -> cfg.nowPlaying, v -> cfg.nowPlaying = v));
                rows.add(new ToggleRow("progress_bar", () -> cfg.progressBar, v -> cfg.progressBar = v));
                rows.add(new ToggleRow("instrumental", () -> cfg.instrumentalDots, v -> cfg.instrumentalDots = v));
                rows.add(new ToggleRow("demo", () -> LyricsPlusClient.engine().isDemo(),
                        v -> LyricsPlusClient.engine().setDemo(v)));
            }
        }
    }

    // =================================================================== rendering

    private void layoutPanel() {
        pw = Math.min(PANEL_W, this.width - 20);
        ph = this.height - 20;
        px = this.width - pw - 10;
        py = 10;
        contentTop = py + HEADER_H + TABS_H + 6;
        contentBottom = py + ph - FOOTER_H;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        layoutPanel();
        int accent = accent();

        // card + accent strip
        g.fill(px, py, px + pw, py + ph, BG);
        g.fill(px, py, px + pw, py + 2, accent);
        g.outline(px, py, pw, ph, 0x30FFFFFF);

        // header
        g.text(this.font, Component.literal("♪"), px + PAD, py + 11, accent, false);
        g.text(this.font, this.title, px + PAD + 10, py + 11, TEXT, false);
        boolean closeHover = inside(mouseX, mouseY, px + pw - 22, py + 6, 16, 16);
        g.text(this.font, "✕", px + pw - 17, py + 10, closeHover ? TEXT : MUTED, false);

        // tabs
        int tabY = py + HEADER_H;
        int tabW = (pw - PAD * 2) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            int tx = px + PAD + i * tabW;
            boolean active = i == tab;
            boolean hover = inside(mouseX, mouseY, tx, tabY, tabW, TABS_H);
            if (active) {
                g.fill(tx + 1, tabY, tx + tabW - 1, tabY + TABS_H, CARD_HOVER);
                g.fill(tx + 1, tabY + TABS_H - 2, tx + tabW - 1, tabY + TABS_H, accent);
            } else if (hover) {
                g.fill(tx + 1, tabY, tx + tabW - 1, tabY + TABS_H, CARD);
            }
            Component label = Component.translatable("lyricsplus.tab." + TABS[i]);
            g.centeredText(this.font, label, tx + tabW / 2, tabY + 7, active ? TEXT : MUTED);
        }

        // rows (scrollable)
        int x = px + PAD;
        int w = pw - PAD * 2;
        contentHeight = 0;
        for (Row r : rows) contentHeight += r.height() + 4;
        double maxScroll = Math.max(0, contentHeight - (contentBottom - contentTop));
        scroll = Math.clamp(scroll, 0, maxScroll);

        g.enableScissor(px, contentTop, px + pw, contentBottom);
        int y = contentTop - (int) scroll;
        for (Row r : rows) {
            r.x = x;
            r.y = y;
            r.w = w;
            boolean hover = mouseY >= contentTop && mouseY < contentBottom && inside(mouseX, mouseY, x, y, w, r.height());
            r.render(g, hover, mouseX, mouseY);
            y += r.height() + 4;
        }
        g.disableScissor();

        // scrollbar
        if (maxScroll > 0) {
            int trackH = contentBottom - contentTop;
            int barH = Math.max(16, (int) (trackH * (trackH / (double) contentHeight)));
            int barY = contentTop + (int) ((trackH - barH) * (scroll / maxScroll));
            g.fill(px + pw - 4, barY, px + pw - 2, barY + barH, 0x60FFFFFF);
        }

        // footer: done button
        int fy = py + ph - FOOTER_H + 6;
        boolean doneHover = inside(mouseX, mouseY, px + PAD, fy, pw - PAD * 2, 18);
        g.fill(px + PAD, fy, px + pw - PAD, fy + 18, doneHover ? accent : CARD_HOVER);
        g.centeredText(this.font, Component.translatable("lyricsplus.done"), px + pw / 2, fy + 5,
                doneHover ? 0xFF101010 : TEXT);

        super.extractRenderState(g, mouseX, mouseY, delta);
    }

    /** No blur / dirt background: keep the game and the lyrics visible for live preview. */
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
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
        layoutPanel();
        double mx = event.x();
        double my = event.y();
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        if (inside(mx, my, px + pw - 22, py + 6, 16, 16)) {
            onClose();
            return true;
        }
        int tabY = py + HEADER_H;
        int tabW = (pw - PAD * 2) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            if (inside(mx, my, px + PAD + i * tabW, tabY, tabW, TABS_H)) {
                tab = i;
                buildRows();
                return true;
            }
        }
        if (inside(mx, my, px + PAD, py + ph - FOOTER_H + 6, pw - PAD * 2, 18)) {
            onClose();
            return true;
        }
        if (my >= contentTop && my < contentBottom) {
            for (Row r : rows) {
                if (inside(mx, my, r.x, r.y, r.w, r.height())) {
                    if (r.click(mx, my)) {
                        dragging = r;
                        cfg.save();
                    }
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
            scroll -= sy * 18;
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    // =================================================================== row widgets

    private abstract class Row {
        int x, y, w;
        final String key;

        Row(String key) {
            this.key = key;
        }

        int height() {
            return 22;
        }

        Component label() {
            return Component.translatable("lyricsplus.label." + key);
        }

        void card(GuiGraphicsExtractor g, boolean hover) {
            g.fill(x, y, x + w, y + height(), hover ? CARD_HOVER : CARD);
        }

        abstract void render(GuiGraphicsExtractor g, boolean hover, int mx, int my);

        /** @return true if the row wants drag events until the mouse is released */
        boolean click(double mx, double my) {
            return false;
        }

        void drag(double mx, double my) {
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
            g.text(font, label(), x + 8, y + 7, TEXT, false);
            boolean on = get.getAsBoolean();
            int sx = x + w - 30;
            int sy = y + 6;
            g.fill(sx, sy, sx + 22, sy + 10, on ? accent() : TRACK);
            int kx = on ? sx + 13 : sx + 1;
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

        ChoiceRow(String key, Supplier<String> valueKey, IntConsumer step) {
            super(key);
            this.valueKey = valueKey;
            this.step = step;
        }

        @Override
        void render(GuiGraphicsExtractor g, boolean hover, int mx, int my) {
            card(g, hover);
            g.text(font, label(), x + 8, y + 7, TEXT, false);
            int bw = 104;
            int bx = x + w - bw - 4;
            g.fill(bx, y + 3, bx + bw, y + 19, 0xFF15151A);
            boolean leftHover = hover && mx < bx + 14 && mx >= bx;
            boolean rightHover = hover && mx >= bx + bw - 14;
            g.text(font, "‹", bx + 5, y + 7, leftHover ? accent() : MUTED, false);
            g.text(font, "›", bx + bw - 9, y + 7, rightHover ? accent() : MUTED, false);
            g.centeredText(font, Component.translatable(valueKey.get()), bx + bw / 2, y + 7, TEXT);
        }

        @Override
        boolean click(double mx, double my) {
            int bx = x + w - 104 - 4;
            step.accept(mx < bx + 52 && mx >= bx ? -1 : 1);
            if (key.equals("mode") || key.equals("world_style")) {
                buildRowsLater();
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
        int height() {
            return 28;
        }

        @Override
        void render(GuiGraphicsExtractor g, boolean hover, int mx, int my) {
            card(g, hover || dragging == this);
            g.text(font, label(), x + 8, y + 5, TEXT, false);
            String v = format.apply(get.getAsDouble());
            g.text(font, v, x + w - 8 - font.width(v), y + 5, MUTED, false);
            int tx = x + 8;
            int tw = w - 16;
            int ty = y + 19;
            double p = (get.getAsDouble() - min) / (max - min);
            int fillX = tx + (int) Math.round(tw * Math.clamp(p, 0, 1));
            g.fill(tx, ty, tx + tw, ty + 2, TRACK);
            g.fill(tx, ty, fillX, ty + 2, accent());
            g.fill(fillX - 2, ty - 3, fillX + 3, ty + 5, 0xFFFFFFFF);
        }

        @Override
        boolean click(double mx, double my) {
            drag(mx, my);
            return true;
        }

        @Override
        void drag(double mx, double my) {
            double p = Math.clamp((mx - (x + 8)) / (double) (w - 16), 0, 1);
            double v = min + p * (max - min);
            set.accept(Math.round(v / stepSize) * stepSize);
        }
    }

    private final class ColorRow extends Row {
        private final IntSupplier get;
        private final IntConsumer set;
        private final BooleanSupplier rainbow;
        private final Consumer<Boolean> setRainbow;
        private int dragBar = -1; // 0 = hue, 1 = shade

        ColorRow(String key, IntSupplier get, IntConsumer set, BooleanSupplier rainbow, Consumer<Boolean> setRainbow) {
            super(key);
            this.get = get;
            this.set = set;
            this.rainbow = rainbow;
            this.setRainbow = setRainbow;
        }

        @Override
        int height() {
            return 88;
        }

        private int swatchSize() {
            return (w - 16 - 6 * 3) / 7;
        }

        @Override
        void render(GuiGraphicsExtractor g, boolean hover, int mx, int my) {
            card(g, false);
            g.text(font, label(), x + 8, y + 6, TEXT, false);

            // current color preview + rainbow chip
            int cur = rainbow.getAsBoolean() ? LineFormatter.rainbowRgb(0) : get.getAsInt();
            g.fill(x + w - 22, y + 4, x + w - 8, y + 14, 0xFF000000 | cur);
            g.outline(x + w - 22, y + 4, 14, 10, 0x60FFFFFF);
            Component rb = Component.translatable("lyricsplus.color.rainbow");
            int rbw = font.width(rb) + 8;
            int rbx = x + w - 28 - rbw;
            g.fill(rbx, y + 3, rbx + rbw, y + 15, rainbow.getAsBoolean() ? accent() : TRACK);
            g.text(font, LineFormatter.colorize(rb.getString(), new LineFormatter.Paint(0, true), 0), rbx + 4, y + 5, 0xFFFFFFFF, false);

            // swatches: 2 rows of 7
            int s = swatchSize();
            for (int i = 0; i < LyricsConfig.SWATCHES.length; i++) {
                int sx = x + 8 + (i % 7) * (s + 3);
                int sy = y + 20 + (i / 7) * 13;
                int c = LyricsConfig.SWATCHES[i];
                g.fill(sx, sy, sx + s, sy + 10, 0xFF000000 | c);
                if (!rainbow.getAsBoolean() && c == get.getAsInt()) {
                    g.outline(sx - 1, sy - 1, s + 2, 12, 0xFFFFFFFF);
                }
            }

            // hue bar
            int bx = x + 8;
            int bw = w - 16;
            int hy = y + 50;
            for (int i = 0; i < bw; i++) {
                g.fill(bx + i, hy, bx + i + 1, hy + 9, 0xFF000000 | LineFormatter.hsv(i / (float) bw, 0.8f, 1f));
            }
            float[] hsv = toHsv(get.getAsInt());
            int hk = bx + Math.round(hsv[0] * bw);
            g.fill(hk - 1, hy - 2, hk + 2, hy + 11, 0xFFFFFFFF);

            // shade bar: dark -> color -> white
            int sy2 = y + 66;
            for (int i = 0; i < bw; i++) {
                g.fill(bx + i, sy2, bx + i + 1, sy2 + 9, 0xFF000000 | shade(hsv[0], i / (float) bw));
            }
            g.text(font, Component.translatable("lyricsplus.hint.color"), bx, y + 78, MUTED, false);
        }

        @Override
        boolean click(double mx, double my) {
            Component rb = Component.translatable("lyricsplus.color.rainbow");
            int rbw = font.width(rb) + 8;
            int rbx = x + w - 28 - rbw;
            if (inside(mx, my, rbx, y + 3, rbw, 12)) {
                setRainbow.accept(!rainbow.getAsBoolean());
                return false;
            }
            int s = swatchSize();
            for (int i = 0; i < LyricsConfig.SWATCHES.length; i++) {
                int sx = x + 8 + (i % 7) * (s + 3);
                int sy = y + 20 + (i / 7) * 13;
                if (inside(mx, my, sx, sy, s, 10)) {
                    set.accept(LyricsConfig.SWATCHES[i]);
                    setRainbow.accept(false);
                    return false;
                }
            }
            if (my >= y + 47 && my < y + 62) {
                dragBar = 0;
            } else if (my >= y + 63 && my < y + 78) {
                dragBar = 1;
            } else {
                return false;
            }
            drag(mx, my);
            return true;
        }

        @Override
        void drag(double mx, double my) {
            float p = (float) Math.clamp((mx - (x + 8)) / (double) (w - 16), 0, 1);
            setRainbow.accept(false);
            if (dragBar == 0) {
                set.accept(LineFormatter.hsv(p, 0.8f, 1f));
            } else if (dragBar == 1) {
                set.accept(shade(toHsv(get.getAsInt())[0], p));
            }
        }

        /** t in [0,1]: 0 = black, 0.5 = full color, 1 = white. */
        private int shade(float hue, float t) {
            if (t < 0.5f) {
                return LineFormatter.hsv(hue, 0.8f, t * 2f);
            }
            return LineFormatter.hsv(hue, 0.8f * (1f - (t - 0.5f) * 2f), 1f);
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
            return 14;
        }

        @Override
        void render(GuiGraphicsExtractor g, boolean hover, int mx, int my) {
            g.text(font, Component.translatable(key), x + 2, y + 3, MUTED, false);
        }
    }

    // =================================================================== helpers

    private boolean rebuildPending;

    private void buildRowsLater() {
        rebuildPending = true;
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
