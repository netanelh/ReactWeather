package com.natan.vortex.gui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.natan.vortex.VortexClient;
import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.module.client.ClickGui;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.ModeSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.setting.Setting;
import com.natan.vortex.util.Colors;
import com.natan.vortex.util.Keys;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Category-panel ClickGUI.
 * LMB module = toggle, RMB module = expand settings, MMB module = bind key.
 * Drag panel headers to move them, RMB header to collapse, scroll to move a panel, type to search.
 */
public class ClickGuiScreen extends Screen {
    // ---- persistent state (kept between openings and saved to config) ----
    private static final Map<Category, Panel> PANELS = new EnumMap<>(Category.class);
    private static final Set<Module> EXPANDED = new HashSet<>();

    private static final int HEADER_H = 18, MODULE_H = 15, SETTING_H = 13, SLIDER_H = 17;
    private static final int C_PANEL = 0xE6121216, C_HEADER = 0xFF1A1A21, C_SETTINGS = 0xEE0C0C0F;
    private static final int C_TEXT = 0xFFE8E8EE, C_TEXT_DIM = 0xFF9A9AA6, C_HOVER = 0x16FFFFFF;

    private static boolean laidOut;

    static {
        for (Category c : Category.values()) PANELS.put(c, new Panel(0, 0, true));
    }

    /** Spreads the panels across the screen, wrapping to a second row when they don't fit. */
    private void layout() {
        int pw = cfg().panelWidth.i(), gap = 6;
        int perRow = Math.max(1, (width - 12 + gap) / (pw + gap));
        int total = Category.values().length;
        int cols = Math.min(perRow, total);
        int startX = (width - (cols * pw + (cols - 1) * gap)) / 2;
        int i = 0;
        for (Category c : Category.values()) {
            Panel p = PANELS.get(c);
            p.x = startX + (i % cols) * (pw + gap);
            p.y = 30 + (i / cols) * (height / 2);
            p.scroll = 0;
            i++;
        }
        laidOut = true;
    }

    @Override
    protected void init() {
        super.init();
        if (!laidOut) layout();
    }

    private final long openedAt = System.currentTimeMillis();
    private final List<Row> rows = new ArrayList<>();
    private String search = "";

    private Panel dragging;
    private double dragDX, dragDY;
    private NumberSetting slidingSetting;
    private Row slidingRow;
    private Module binding;
    private double mouseX, mouseY;

    public ClickGuiScreen() {
        super(Component.literal("Vortex"));
    }

    private ClickGui cfg() { return VortexClient.modules.clickGui(); }

    // =====================================================================
    // Rendering
    // =====================================================================

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int alpha = (int) (cfg().dim.d() / 100.0 * 255 * openProgress());
        g.fill(0, 0, width, height, alpha << 24);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        this.mouseX = mx;
        this.mouseY = my;
        if (slidingSetting != null) applySlider(slidingRow, mx);
        if (cfg().resetLayout.on()) { cfg().resetLayout.set(false); layout(); }

        float t = openProgress();
        int slide = (int) ((1 - t) * 16);
        int pw = cfg().panelWidth.i();
        rows.clear();
        Row hovered = null;

        for (Category c : Category.values()) {
            Panel p = PANELS.get(c);
            int x = p.x, y = p.y - slide;
            List<Module> mods = filtered(c);

            // header
            clipTop = Integer.MIN_VALUE; clipBottom = Integer.MAX_VALUE;
            Row header = row(RowType.HEADER, x, y, pw, HEADER_H, null, null, c);
            rows.add(header);
            g.fill(x, y, x + pw, y + HEADER_H, C_HEADER);
            g.fill(x, y + HEADER_H - 1, x + pw, y + HEADER_H, cfg().accent(c.ordinal() * 3));
            g.text(font, c.display, x + 6, y + 5, C_TEXT, true);
            int active = 0;
            for (Module m : mods) if (m.isEnabled()) active++;
            String count = active + "/" + mods.size() + (p.open ? "" : " +");
            g.text(font, count, x + pw - 6 - font.width(count), y + 5, active > 0 ? cfg().accent(c.ordinal() * 3) : C_TEXT_DIM, false);
            if (header.contains(mx, my)) hovered = header;
            y += HEADER_H;
            if (!p.open) continue;

            int bodyTop = y;
            int maxView = Math.max(80, height - bodyTop - 26);
            p.scroll = Math.max(0, Math.min(p.scroll, Math.max(0, p.contentH - maxView)));
            clipTop = bodyTop; clipBottom = bodyTop + maxView;
            g.enableScissor(x, bodyTop, x + pw, bodyTop + maxView);
            y = bodyTop - p.scroll;
            for (int mi = 0; mi < mods.size(); mi++) {
                Module m = mods.get(mi);
                Row row = row(RowType.MODULE, x, y, pw, MODULE_H, m, null, c);
                rows.add(row);
                boolean on = m.isEnabled();
                int accent = cfg().accent(mi + c.ordinal() * 3);
                g.fill(x, y, x + pw, y + MODULE_H, C_PANEL);
                if (on) {
                    g.fill(x, y, x + pw, y + MODULE_H, Colors.withAlpha(accent, 0x40));
                    g.fill(x, y, x + 2, y + MODULE_H, accent);
                }
                if (row.contains(mx, my)) { g.fill(x, y, x + pw, y + MODULE_H, C_HOVER); hovered = row; }
                g.text(font, m.name, x + 6, y + 4, on ? C_TEXT : C_TEXT_DIM, on);
                if (binding == m) {
                    String dots = (System.currentTimeMillis() / 300 % 2 == 0) ? "[...]" : "[.. ]";
                    g.text(font, dots, x + pw - 6 - font.width(dots), y + 4, accent, false);
                } else {
                    String arrow = EXPANDED.contains(m) ? "-" : "+";
                    g.text(font, arrow, x + pw - 9, y + 4, C_TEXT_DIM, false);
                    if (m.key != Keys.NONE && !EXPANDED.contains(m)) {
                        String tag = Keys.name(m.key);
                        g.text(font, tag, x + pw - 14 - font.width(tag), y + 4, 0xFF5E5E6A, false);
                    }
                }
                y += MODULE_H;

                if (EXPANDED.contains(m)) {
                    int setTop = y;
                    // bind row first
                    Row bind = row(RowType.BIND, x, y, pw, SETTING_H, m, null, c);
                    rows.add(bind);
                    g.fill(x, y, x + pw, y + SETTING_H, C_SETTINGS);
                    if (bind.contains(mx, my)) { g.fill(x, y, x + pw, y + SETTING_H, C_HOVER); hovered = bind; }
                    g.text(font, "Bind", x + 10, y + 3, C_TEXT_DIM, false);
                    String keyName = binding == m ? "press a key" : Keys.name(m.key);
                    g.text(font, keyName, x + pw - 6 - font.width(keyName), y + 3, binding == m ? accent : C_TEXT, false);
                    y += SETTING_H;

                    if (m.category != Category.CLIENT) {
                        Row vis = row(RowType.HUD_VISIBLE, x, y, pw, SETTING_H, m, null, c);
                        rows.add(vis);
                        g.fill(x, y, x + pw, y + SETTING_H, C_SETTINGS);
                        if (vis.contains(mx, my)) { g.fill(x, y, x + pw, y + SETTING_H, C_HOVER); hovered = vis; }
                        g.text(font, "Show in HUD", x + 10, y + 3, C_TEXT_DIM, false);
                        drawSwitch(g, x + pw - 22, y + 3, m.showInHud, accent);
                        y += SETTING_H;
                    }

                    for (Setting<?> s : m.settings) {
                        if (!s.isVisible()) continue;
                        int h = s instanceof NumberSetting ? SLIDER_H : SETTING_H;
                        Row sr = row(RowType.SETTING, x, y, pw, h, m, s, c);
                        rows.add(sr);
                        g.fill(x, y, x + pw, y + h, C_SETTINGS);
                        if (sr.contains(mx, my)) { g.fill(x, y, x + pw, y + h, C_HOVER); hovered = sr; }
                        drawSetting(g, s, x, y, pw, accent);
                        y += h;
                    }
                    // accent guide line along the expanded settings
                    g.fill(x + 4, setTop + 2, x + 5, y - 2, Colors.withAlpha(accent, 0x90));
                }
            }
            if (mods.isEmpty() && !search.isEmpty()) {
                g.fill(x, y, x + pw, y + MODULE_H, C_PANEL);
                g.text(font, "no results", x + 6, y + 4, 0xFF5A5A66, false);
                y += MODULE_H;
            }
            p.contentH = y - (bodyTop - p.scroll);
            g.disableScissor();
            clipTop = Integer.MIN_VALUE; clipBottom = Integer.MAX_VALUE;
            int visibleEnd = Math.min(y, bodyTop + maxView);
            p.viewH = visibleEnd - bodyTop;
            if (p.contentH > maxView) { // scrollbar
                int barH = Math.max(12, maxView * maxView / p.contentH);
                int barY = bodyTop + (int) ((long) (maxView - barH) * p.scroll / Math.max(1, p.contentH - maxView));
                g.fill(x + pw - 2, barY, x + pw, barY + barH, Colors.withAlpha(cfg().accent(c.ordinal() * 3), 0xB0));
            }
            // bottom cap
            g.fill(x, visibleEnd, x + pw, visibleEnd + 2, C_HEADER);
            if (visibleEnd > bodyTop) g.fill(x, bodyTop, x + 1, visibleEnd, 0x30FFFFFF);
        }

        drawSearch(g);
        drawFooter(g, hovered);
    }

    private void drawSetting(GuiGraphicsExtractor g, Setting<?> s, int x, int y, int pw, int accent) {
        if (s instanceof BoolSetting b) {
            g.text(font, s.name, x + 10, y + 3, C_TEXT_DIM, false);
            drawSwitch(g, x + pw - 22, y + 3, b.on(), accent);
        } else if (s instanceof NumberSetting n) {
            g.text(font, s.name, x + 10, y + 2, C_TEXT_DIM, false);
            String v = n.display();
            g.text(font, v, x + pw - 6 - font.width(v), y + 2, C_TEXT, false);
            int bx1 = x + 10, bx2 = x + pw - 6, by = y + SLIDER_H - 4;
            double frac = (n.d() - n.min) / (n.max - n.min);
            int fill = bx1 + (int) Math.round((bx2 - bx1) * frac);
            g.fill(bx1, by, bx2, by + 2, 0xFF2A2A33);
            g.fill(bx1, by, fill, by + 2, accent);
            g.fill(fill - 1, by - 1, fill + 2, by + 3, 0xFFFFFFFF);
        } else if (s instanceof ModeSetting mo) {
            g.text(font, s.name, x + 10, y + 3, C_TEXT_DIM, false);
            String v = mo.get();
            int vx = x + pw - 6 - font.width(v);
            int col = mo.name.endsWith("Color") || mo.name.equals("Accent") ? Colors.named(v, 0) : accent;
            g.text(font, v, vx, y + 3, col, false);
        }
    }

    private void drawSwitch(GuiGraphicsExtractor g, int x, int y, boolean on, int accent) {
        g.fill(x, y, x + 16, y + 7, on ? accent : 0xFF33333D);
        int kx = on ? x + 9 : x + 1;
        g.fill(kx, y + 1, kx + 6, y + 6, 0xFFF4F4F8);
    }

    private void drawSearch(GuiGraphicsExtractor g) {
        int w = 170, x = (width - w) / 2, y = 8;
        g.fill(x, y, x + w, y + 16, 0xEE121216);
        g.fill(x, y + 15, x + w, y + 16, search.isEmpty() ? 0xFF33333D : cfg().accent(0));
        boolean caret = System.currentTimeMillis() / 500 % 2 == 0;
        if (search.isEmpty()) g.text(font, "Type to search modules...", x + 6, y + 4, 0xFF5A5A66, false);
        else g.text(font, search + (caret ? "_" : ""), x + 6, y + 4, C_TEXT, false);
        String brand = VortexClient.NAME + " " + VortexClient.VERSION;
        g.text(font, brand, width - font.width(brand) - 6, 6, cfg().accent(0), true);
    }

    private void drawFooter(GuiGraphicsExtractor g, Row hovered) {
        String text = null;
        if (cfg().descriptions.on() && hovered != null) {
            if (hovered.type == RowType.SETTING) text = hovered.setting.description;
            else if (hovered.type == RowType.MODULE) text = hovered.module.description;
            else if (hovered.type == RowType.BIND) text = "Click, then press a key. Esc/Backspace clears.";
            else if (hovered.type == RowType.HUD_VISIBLE) text = "Show this module in the HUD list while active";
            else if (hovered.type == RowType.HEADER) text = "Drag to move, right-click to collapse";
        }
        if (text == null) text = "LMB toggle  |  RMB settings  |  MMB bind  |  scroll to move panels";
        int tw = font.width(text);
        int x = (width - tw) / 2, y = height - 16;
        g.fill(x - 6, y - 3, x + tw + 6, y + 11, 0xDD0C0C0F);
        g.text(font, text, x, y, hovered != null ? C_TEXT : 0xFF7A7A86, false);
    }

    private float openProgress() {
        float t = Math.min(1f, (System.currentTimeMillis() - openedAt) / 180f);
        return 1 - (1 - t) * (1 - t); // ease-out
    }

    private List<Module> filtered(Category c) {
        List<Module> out = new ArrayList<>();
        String q = search.toLowerCase();
        for (Module m : VortexClient.modules.byCategory(c))
            if (q.isEmpty() || m.name.toLowerCase().contains(q) || m.description.toLowerCase().contains(q)) out.add(m);
        return out;
    }

    // =====================================================================
    // Input
    // =====================================================================

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int button = click.button();
        // iterate top-down: last drawn rows are on top
        for (int i = rows.size() - 1; i >= 0; i--) {
            Row r = rows.get(i);
            if (!r.contains(mx, my)) continue;
            onRowClick(r, button, mx, my);
            return true;
        }
        binding = null;
        return super.mouseClicked(click, doubled);
    }

    private void onRowClick(Row r, int button, double mx, double my) {
        switch (r.type) {
            case HEADER -> {
                Panel p = PANELS.get(r.category);
                if (button == 0) { dragging = p; dragDX = mx - p.x; dragDY = my - p.y; }
                else if (button == 1) p.open = !p.open;
            }
            case MODULE -> {
                if (button == 0) {
                    if (r.module == cfg()) EXPANDED.add(r.module); else r.module.toggle();
                } else if (button == 1) {
                    if (!EXPANDED.remove(r.module)) EXPANDED.add(r.module);
                } else if (button == 2) {
                    binding = r.module;
                }
            }
            case BIND -> binding = binding == r.module ? null : r.module;
            case HUD_VISIBLE -> r.module.showInHud = !r.module.showInHud;
            case SETTING -> {
                Setting<?> s = r.setting;
                if (s instanceof BoolSetting b) b.toggle();
                else if (s instanceof ModeSetting m) m.cycle(button == 1 ? -1 : 1);
                else if (s instanceof NumberSetting n) {
                    if (button == 1) { n.reset(); return; }
                    slidingSetting = n; slidingRow = r;
                    applySlider(r, mx);
                }
            }
        }
    }

    private void applySlider(Row r, double mx) {
        NumberSetting n = (NumberSetting) r.setting;
        double x1 = r.x + 10, x2 = r.x + r.w - 6;
        double frac = Math.max(0, Math.min(1, (mx - x1) / (x2 - x1)));
        n.set(n.min + (n.max - n.min) * frac);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (dragging != null) {
            dragging.x = (int) (mx - dragDX);
            dragging.y = (int) (my - dragDY);
        }
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        dragging = null;
        slidingSetting = null;
        slidingRow = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        int pw = cfg().panelWidth.i();
        for (Panel p : PANELS.values()) {
            int top = p.y + HEADER_H;
            if (p.open && mx >= p.x && mx <= p.x + pw && my >= top && my <= top + Math.max(p.viewH, 20)) {
                p.scroll = Math.max(0, p.scroll - (int) (vertical * 18));
                return true;
            }
        }
        // not over a panel: scroll every panel together
        for (Panel p : PANELS.values()) p.y += (int) (vertical * 14);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        int key = input.key();
        if (binding != null) {
            binding.key = (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_BACKSPACE || key == GLFW.GLFW_KEY_DELETE) ? Keys.NONE : key;
            binding = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            return true;
        }
        if (key == cfg().key && search.isEmpty()) { onClose(); return true; }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char c = (char) event.codepoint();
        if (binding == null && (Character.isLetterOrDigit(c) || c == ' ') && search.length() < 24) {
            search += c;
            return true;
        }
        return super.charTyped(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void removed() {
        VortexClient.config.save();
        super.removed();
    }

    // =====================================================================
    // Persistence
    // =====================================================================

    public static JsonObject savePanels() {
        JsonObject o = new JsonObject();
        PANELS.forEach((c, p) -> {
            JsonObject j = new JsonObject();
            j.addProperty("x", p.x); j.addProperty("y", p.y); j.addProperty("open", p.open);
            o.add(c.name(), j);
        });
        return o;
    }

    public static void loadPanels(JsonObject o) {
        for (Category c : Category.values()) {
            JsonElement e = o.get(c.name());
            if (e == null || !e.isJsonObject()) continue;
            JsonObject j = e.getAsJsonObject();
            Panel p = PANELS.get(c);
            if (j.has("x")) p.x = j.get("x").getAsInt();
            if (j.has("y")) p.y = j.get("y").getAsInt();
            if (j.has("open")) p.open = j.get("open").getAsBoolean();
            laidOut = true;
        }
    }

    // =====================================================================

    private static final class Panel {
        int x, y, scroll, contentH, viewH; boolean open;
        Panel(int x, int y, boolean open) { this.x = x; this.y = y; this.open = open; }
    }

    private enum RowType { HEADER, MODULE, BIND, HUD_VISIBLE, SETTING }

    private record Row(RowType type, int x, int y, int w, int h, Module module, Setting<?> setting, Category category, int clipTop, int clipBottom) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h && my >= clipTop && my < clipBottom;
        }
    }

    private int clipTop = Integer.MIN_VALUE, clipBottom = Integer.MAX_VALUE;

    private Row row(RowType type, int x, int y, int w, int h, Module m, Setting<?> s, Category c) {
        return new Row(type, x, y, w, h, m, s, c, clipTop, clipBottom);
    }
}
