package dev.netanel.lyricsplus.gui;

import dev.netanel.lyricsplus.LyricsPlusClient;
import dev.netanel.lyricsplus.config.LyricsConfig;
import dev.netanel.lyricsplus.config.LyricsConfig.ColorPreset;
import dev.netanel.lyricsplus.render.LineFormatter;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;

import java.util.Locale;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.Supplier;

/**
 * The settings panel. It draws no blurred background, so the lyrics stay visible
 * behind it and every change is previewed live.
 */
public final class LyricsConfigScreen extends Screen {

    private static final int COL_W = 160;
    private static final int GAP = 6;
    private static final int ROW = 24;

    private final Screen parent;
    private final LyricsConfig cfg = LyricsConfig.get();
    private int panelLeft;
    private int panelRight;
    private int panelBottom;

    public LyricsConfigScreen(Screen parent) {
        super(Component.translatable("lyricsplus.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = this.width / 2 - COL_W - GAP / 2;
        int right = this.width / 2 + GAP / 2;
        int y = 36;
        panelLeft = left - 10;
        panelRight = right + COL_W + 10;

        // ---- left column: how it looks
        int ly = y;
        cycle(left, ly, () -> opt("mode", "lyricsplus.mode." + low(cfg.displayMode)),
                () -> cfg.displayMode = LyricsConfig.next(cfg.displayMode));
        ly += ROW;
        cycle(left, ly, () -> opt("reveal", "lyricsplus.reveal." + low(cfg.revealMode)),
                () -> cfg.revealMode = LyricsConfig.next(cfg.revealMode));
        ly += ROW;
        cycle(left, ly, () -> color("text_color", cfg.textColor),
                () -> cfg.textColor = LyricsConfig.next(cfg.textColor));
        ly += ROW;
        cycle(left, ly, () -> color("highlight_color", cfg.highlightColor),
                () -> cfg.highlightColor = LyricsConfig.next(cfg.highlightColor));
        ly += ROW;
        cycle(left, ly, () -> opt("position", "lyricsplus.position." + low(cfg.sidePosition)),
                () -> cfg.sidePosition = LyricsConfig.next(cfg.sidePosition));
        ly += ROW;
        addRenderableWidget(new Slider(left, ly, 0.5, 3.0, 0.1, cfg.scale,
                v -> Component.translatable("lyricsplus.option.scale", String.format(Locale.ROOT, "%.1fx", v)),
                v -> cfg.scale = (float) v));
        ly += ROW;
        addRenderableWidget(new Slider(left, ly, -3000, 3000, 50, cfg.offsetMs,
                v -> Component.translatable("lyricsplus.option.offset", String.format(Locale.ROOT, "%+.2fs", v / 1000.0)),
                v -> cfg.offsetMs = (int) Math.round(v)));
        ly += ROW;
        addRenderableWidget(new Slider(left, ly, 2, 8, 0.5, cfg.worldDistance,
                v -> Component.translatable("lyricsplus.option.world_distance", String.format(Locale.ROOT, "%.1f", v)),
                v -> cfg.worldDistance = (float) v));
        ly += ROW;

        // ---- right column: what to show
        int ry = y;
        toggle(right, ry, "title_card", () -> cfg.titleCard, v -> cfg.titleCard = v);
        ry += ROW;
        toggle(right, ry, "context_lines", () -> cfg.showContextLines, v -> cfg.showContextLines = v);
        ry += ROW;
        toggle(right, ry, "progress_bar", () -> cfg.progressBar, v -> cfg.progressBar = v);
        ry += ROW;
        toggle(right, ry, "now_playing", () -> cfg.nowPlaying, v -> cfg.nowPlaying = v);
        ry += ROW;
        toggle(right, ry, "instrumental", () -> cfg.instrumentalDots, v -> cfg.instrumentalDots = v);
        ry += ROW;
        toggle(right, ry, "animations", () -> cfg.animations, v -> cfg.animations = v);
        ry += ROW;
        toggle(right, ry, "shadow", () -> cfg.shadow, v -> cfg.shadow = v);
        ry += ROW;
        toggle(right, ry, "background", () -> cfg.background, v -> cfg.background = v);
        ry += ROW;
        addRenderableWidget(new Slider(right, ry, 0, 1, 0.05, cfg.backgroundOpacity,
                v -> Component.translatable("lyricsplus.option.bg_opacity", Math.round(v * 100) + "%"),
                v -> cfg.backgroundOpacity = (float) v));
        ry += ROW;

        // ---- bottom row
        int by = Math.max(ly, ry) + 8;
        toggle(left, by, "enabled", () -> cfg.enabled, v -> cfg.enabled = v);
        addRenderableWidget(Button.builder(demoLabel(), b -> {
            LyricsPlusClient.engine().setDemo(!LyricsPlusClient.engine().isDemo());
            b.setMessage(demoLabel());
        }).bounds(right, by, COL_W, 20).build());
        by += ROW;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(this.width / 2 - 100, by, 200, 20).build());
        panelBottom = by + 28;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        g.fill(panelLeft, 10, panelRight, panelBottom, 0xC0101010);
        g.fill(panelLeft, 10, panelRight, 11, 0xFF000000 | LineFormatter.rgb(cfg.highlightColor, 0));
        g.centeredText(this.font, this.title, this.width / 2, 18, 0xFFFFFFFF);
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
        this.minecraft.setScreen(parent);
    }

    // ---------------------------------------------------------------- helpers

    private void cycle(int x, int y, Supplier<Component> label, Runnable action) {
        addRenderableWidget(Button.builder(label.get(), b -> {
            action.run();
            b.setMessage(label.get());
            cfg.save();
        }).bounds(x, y, COL_W, 20).build());
    }

    private void toggle(int x, int y, String key, Supplier<Boolean> get, java.util.function.Consumer<Boolean> set) {
        Supplier<Component> label = () -> Component.translatable("lyricsplus.option." + key,
                get.get() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
        cycle(x, y, label, () -> set.accept(!get.get()));
    }

    private static Component opt(String key, String valueKey) {
        return Component.translatable("lyricsplus.option." + key, Component.translatable(valueKey));
    }

    private static Component color(String key, ColorPreset c) {
        Component name = LineFormatter.colorize(
                Component.translatable("lyricsplus.color." + low(c)).getString(), c, 0);
        return Component.translatable("lyricsplus.option." + key, name);
    }

    private static Component demoLabel() {
        return Component.translatable("lyricsplus.option.demo",
                LyricsPlusClient.engine().isDemo() ? CommonComponents.OPTION_ON : CommonComponents.OPTION_OFF);
    }

    private static String low(Enum<?> e) {
        return e.name().toLowerCase(Locale.ROOT);
    }

    private final class Slider extends AbstractSliderButton {
        private final double min;
        private final double max;
        private final double step;
        private final DoubleFunction<Component> label;
        private final DoubleConsumer apply;

        Slider(int x, int y, double min, double max, double step, double current,
               DoubleFunction<Component> label, DoubleConsumer apply) {
            super(x, y, COL_W, 20, Component.empty(), (current - min) / (max - min));
            this.min = min;
            this.max = max;
            this.step = step;
            this.label = label;
            this.apply = apply;
            updateMessage();
        }

        private double real() {
            double v = min + this.value * (max - min);
            return Math.round(v / step) * step;
        }

        @Override
        protected void updateMessage() {
            if (label != null) {
                setMessage(label.apply(real()));
            }
        }

        @Override
        protected void applyValue() {
            apply.accept(real());
        }
    }
}
