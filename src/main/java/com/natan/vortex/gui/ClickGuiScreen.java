package com.natan.vortex.gui;

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
import java.util.List;

/**
 * One window: categories on the left, a grid of module cards in the middle,
 * and the selected module's settings on the right. Fits 100+ modules on any screen.
 *
 * LMB card = toggle, RMB card = open settings, MMB card = bind. Type to search all modules.
 */
public class ClickGuiScreen extends Screen {
    // persistent between openings / saved in config
    private static Category selectedCategory = Category.COMBAT;
    private static Module selectedModule;
    private static int winX = Integer.MIN_VALUE, winY;
    private static int listScroll, settingsScroll;

    private static final int TITLE_H = 22, SIDEBAR_W = 92, FOOTER_H = 14, CARD_H = 22, ROW_H = 14, SLIDER_H = 18;
    private static final int C_BG = 0xF00E0E12, C_SIDEBAR = 0xF0141419, C_TITLE = 0xFF17171D, C_CARD = 0xFF1A1A21, C_CARD_HOVER = 0xFF22222B;
    private static final int C_TEXT = 0xFFE8E8EE, C_DIM = 0xFF8C8C98, C_FAINT = 0xFF55555F;

    private final long openedAt = System.currentTimeMillis();
    private final List<Hit> hits = new ArrayList<>();
    private String search = "";
    private boolean draggingWindow;
    private double dragDX, dragDY;
    private NumberSetting sliding;
    private int slideX1, slideX2;
    private Module binding;
    private int listContentH, settingsContentH;

    public ClickGuiScreen() { super(Component.literal(VortexClient.NAME)); }

    private ClickGui cfg() { return VortexClient.modules.clickGui(); }
    private int acc(int i) { return cfg().accent(i); }

    // ---------------------------------------------------------------- geometry
    private int winW() { return Math.min(width - 20, 560); }
    private int winH() { return Math.min(height - 20, 320); }

    private void clampWindow() {
        if (winX == Integer.MIN_VALUE) { winX = (width - winW()) / 2; winY = (height - winH()) / 2; }
        winX = Math.max(0, Math.min(winX, width - winW()));
        winY = Math.max(0, Math.min(winY, height - winH()));
    }

    // ---------------------------------------------------------------- render
    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int alpha = (int) (cfg().dim.d() / 100.0 * 255 * fade());
        g.fill(0, 0, width, height, alpha << 24);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        clampWindow();
        if (sliding != null) applySlider(mx);
        if (cfg().resetLayout.on()) { cfg().resetLayout.set(false); winX = Integer.MIN_VALUE; clampWindow(); }
        hits.clear();

        int x = winX, y = winY + (int) ((1 - fade()) * 10), w = winW(), h = winH();
        String hover = null;

        // frame + title bar
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, Colors.withAlpha(acc(0), 0x60));
        g.fill(x, y, x + w, y + h, C_BG);
        g.fill(x, y, x + w, y + TITLE_H, C_TITLE);
        g.fill(x, y + TITLE_H - 1, x + w, y + TITLE_H, acc(0));
        g.text(font, VortexClient.NAME, x + 8, y + 7, acc(0), true);
        int enabled = 0, total = VortexClient.modules.all().size();
        for (Module m : VortexClient.modules.all()) if (m.isEnabled() && m.category != Category.CLIENT) enabled++;
        g.text(font, enabled + " on · " + total + " modules", x + 8 + font.width(VortexClient.NAME) + 8, y + 7, C_FAINT, false);
        hits.add(new Hit(HitType.TITLE, x, y, w - 170, TITLE_H, null, null, null));

        // search box (top right)
        int sw = 150, sx = x + w - sw - 6, sy = y + 4;
        g.fill(sx, sy, sx + sw, sy + 14, 0xFF0C0C10);
        g.fill(sx, sy + 13, sx + sw, sy + 14, search.isEmpty() ? 0xFF2C2C35 : acc(0));
        boolean caret = System.currentTimeMillis() / 500 % 2 == 0;
        if (search.isEmpty()) g.text(font, "type to search...", sx + 4, sy + 3, C_FAINT, false);
        else g.text(font, search + (caret ? "_" : ""), sx + 4, sy + 3, C_TEXT, false);

        int bodyY = y + TITLE_H, bodyH = h - TITLE_H - FOOTER_H;

        // sidebar
        g.fill(x, bodyY, x + SIDEBAR_W, bodyY + bodyH, C_SIDEBAR);
        int cy = bodyY + 6;
        for (Category c : Category.values()) {
            List<Module> mods = VortexClient.modules.byCategory(c);
            int on = 0;
            for (Module m : mods) if (m.isEnabled()) on++;
            boolean sel = search.isEmpty() && c == selectedCategory;
            boolean hov = mx >= x && mx < x + SIDEBAR_W && my >= cy && my < cy + 18;
            if (sel) { g.fill(x, cy, x + SIDEBAR_W, cy + 18, Colors.withAlpha(acc(c.ordinal()), 0x35)); g.fill(x, cy, x + 2, cy + 18, acc(c.ordinal())); }
            else if (hov) g.fill(x, cy, x + SIDEBAR_W, cy + 18, 0x14FFFFFF);
            g.text(font, c.display, x + 9, cy + 5, sel ? C_TEXT : C_DIM, sel);
            String cnt = on > 0 ? on + "/" + mods.size() : String.valueOf(mods.size());
            g.text(font, cnt, x + SIDEBAR_W - 6 - font.width(cnt), cy + 5, on > 0 ? acc(c.ordinal()) : C_FAINT, false);
            hits.add(new Hit(HitType.CATEGORY, x, cy, SIDEBAR_W, 18, c, null, null));
            if (hov) hover = "Show " + c.display + " modules";
            cy += 19;
        }

        // content split
        boolean showSettings = selectedModule != null;
        int contentX = x + SIDEBAR_W, contentW = w - SIDEBAR_W;
        int settingsW = showSettings ? Math.max(170, contentW * 42 / 100) : 0;
        int listW = contentW - settingsW;

        String h1 = drawModuleList(g, mx, my, contentX, bodyY, listW, bodyH);
        if (h1 != null) hover = h1;
        if (showSettings) {
            String h2 = drawSettings(g, mx, my, contentX + listW, bodyY, settingsW, bodyH);
            if (h2 != null) hover = h2;
        }

        // footer
        int fy = y + h - FOOTER_H;
        g.fill(x, fy, x + w, y + h, C_TITLE);
        boolean showHover = hover != null && cfg().descriptions.on();
        String foot = showHover ? hover : "LMB toggle  ·  RMB settings  ·  MMB bind  ·  drag the title bar to move";
        g.text(font, trim(foot, w - 12), x + 6, fy + 3, showHover ? C_TEXT : C_FAINT, false);
    }

    private String drawModuleList(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, int h) {
        String hover = null;
        List<Module> mods = visibleModules();
        int pad = 6, cols = w >= 330 ? 3 : w >= 210 ? 2 : 1;
        int cardW = (w - pad * (cols + 1)) / cols;
        int rows = (mods.size() + cols - 1) / cols;
        listContentH = pad + rows * (CARD_H + 4);
        listScroll = Math.max(0, Math.min(listScroll, Math.max(0, listContentH - h)));

        g.enableScissor(x, y, x + w, y + h);
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            int cx = x + pad + (i % cols) * (cardW + pad), cyy = y + pad + (i / cols) * (CARD_H + 4) - listScroll;
            if (cyy + CARD_H < y || cyy > y + h) continue;
            boolean on = m.isEnabled(), sel = m == selectedModule;
            boolean hov = mx >= cx && mx < cx + cardW && my >= cyy && my < cyy + CARD_H && my >= y && my < y + h;
            int a = acc(i);
            g.fill(cx, cyy, cx + cardW, cyy + CARD_H, hov ? C_CARD_HOVER : C_CARD);
            if (on) { g.fill(cx, cyy, cx + cardW, cyy + CARD_H, Colors.withAlpha(a, 0x38)); g.fill(cx, cyy, cx + 2, cyy + CARD_H, a); }
            if (sel) { g.fill(cx, cyy + CARD_H - 1, cx + cardW, cyy + CARD_H, a); }
            String label = binding == m ? "press a key..." : m.name;
            g.text(font, trim(label, cardW - 26), cx + 7, cyy + 7, on ? C_TEXT : C_DIM, on);
            if (m.key != Keys.NONE && binding != m) {
                String k = Keys.name(m.key);
                g.text(font, k, cx + cardW - 5 - font.width(k), cyy + 7, C_FAINT, false);
            } else if (!m.settings.isEmpty()) {
                g.text(font, "...", cx + cardW - 13, cyy + 7, C_FAINT, false);
            }
            hits.add(new Hit(HitType.MODULE, cx, cyy, cardW, CARD_H, null, m, null).clip(y, y + h));
            if (hov) hover = search.isEmpty() ? m.name + ": " + m.description : m.name + " [" + m.category.display + "]: " + m.description;
        }
        if (mods.isEmpty()) g.text(font, "No modules match \"" + search + "\"", x + 10, y + 10, C_FAINT, false);
        g.disableScissor();
        scrollbar(g, x + w - 3, y, h, listContentH, listScroll, acc(0));
        return hover;
    }

    private String drawSettings(GuiGraphicsExtractor g, int mx, int my, int x, int y, int w, int h) {
        Module m = selectedModule;
        String hover = null;
        int a = acc(m.category.ordinal());
        g.fill(x, y, x + 1, y + h, 0x30FFFFFF);
        g.fill(x + 1, y, x + w, y + h, 0x60000000);

        // header (not scrolled)
        g.text(font, trim(m.name, m.settings.isEmpty() ? w - 56 : w - 72), x + 8, y + 6, C_TEXT, true);
        if (!m.settings.isEmpty()) {
            boolean rh = mx >= x + w - 62 && mx < x + w - 48 && my >= y + 3 && my < y + 17;
            g.text(font, "R", x + w - 58, y + 6, rh ? C_TEXT : C_DIM, false);
            hits.add(new Hit(HitType.RESET, x + w - 62, y + 3, 14, 14, null, m, null));
            if (rh) hover = "Double-click: reset all settings of " + m.name;
        }
        drawSwitch(g, x + w - 26, y + 6, m.isEnabled(), a);
        hits.add(new Hit(HitType.TOGGLE, x + w - 30, y + 3, 26, 14, null, m, null));
        hits.add(new Hit(HitType.CLOSE_SETTINGS, x + w - 46, y + 3, 14, 14, null, null, null));
        g.text(font, "x", x + w - 42, y + 6, C_DIM, false);
        g.text(font, trim(m.description, w - 16), x + 8, y + 18, C_FAINT, false);

        int top = y + 32, viewH = h - 32;
        g.enableScissor(x + 1, top, x + w, top + viewH);
        int ry = top - settingsScroll;

        // bind + hud rows
        g.text(font, "Bind", x + 10, ry + 3, C_DIM, false);
        String bindText = binding == m ? "press a key" : Keys.name(m.key);
        g.text(font, bindText, x + w - 8 - font.width(bindText), ry + 3, binding == m ? a : C_TEXT, false);
        hits.add(new Hit(HitType.BIND, x + 1, ry, w - 1, ROW_H, null, m, null).clip(top, top + viewH));
        ry += ROW_H;
        if (m.category != Category.CLIENT) {
            g.text(font, "Toggle Messages", x + 10, ry + 3, C_DIM, false);
            drawSwitch(g, x + w - 26, ry + 3, m.showInHud, a);
            hits.add(new Hit(HitType.HUD_VISIBLE, x + 1, ry, w - 1, ROW_H, null, m, null).clip(top, top + viewH));
            ry += ROW_H;
        }
        g.fill(x + 8, ry + 2, x + w - 8, ry + 3, 0x20FFFFFF);
        ry += 6;

        String lastGroup = null;
        for (Setting<?> s : m.settings) {
            if (!s.isVisible()) continue;
            // group header with a master switch that toggles every setting in the group at once
            if (s.group != null && !s.group.equals(lastGroup)) {
                boolean allOn = groupAllOn(m, s.group);
                boolean ghov = mx >= x && mx < x + w && my >= ry && my < ry + ROW_H && my >= top && my < top + viewH;
                if (ghov) { g.fill(x + 1, ry, x + w, ry + ROW_H, 0x18FFFFFF); hover = s.group + ": turn every option below on or off"; }
                g.text(font, "§l" + s.group, x + 8, ry + 3, C_TEXT, false);
                drawSwitch(g, x + w - 26, ry + 3, allOn, a);
                hits.add(new Hit(HitType.GROUP, x + 1, ry, w - 1, ROW_H, null, m, null).withGroup(s.group).clip(top, top + viewH));
                ry += ROW_H;
                lastGroup = s.group;
            } else if (s.group == null) {
                lastGroup = null;
            }
            int indent = s.group != null ? 8 : 0;
            int rh = s instanceof NumberSetting ? SLIDER_H : ROW_H;
            boolean hov = mx >= x && mx < x + w && my >= ry && my < ry + rh && my >= top && my < top + viewH;
            if (hov) { g.fill(x + 1, ry, x + w, ry + rh, 0x12FFFFFF); hover = s.name + ": " + s.description; }
            if (s instanceof BoolSetting b) {
                g.text(font, trim(s.name, w - 44 - indent), x + 10 + indent, ry + 3, C_DIM, false);
                drawSwitch(g, x + w - 26, ry + 3, b.on(), a);
            } else if (s instanceof ModeSetting mo) {
                g.text(font, trim(s.name, w / 2), x + 10, ry + 3, C_DIM, false);
                String v = "< " + mo.get() + " >";
                int col = mo.name.endsWith("Color") || mo.name.equals("Accent") ? Colors.named(mo.get(), 0) : a;
                g.text(font, v, x + w - 8 - font.width(v), ry + 3, col, false);
            } else if (s instanceof NumberSetting n) {
                g.text(font, trim(s.name, w - 50), x + 10, ry + 2, C_DIM, false);
                String v = n.display();
                g.text(font, v, x + w - 8 - font.width(v), ry + 2, C_TEXT, false);
                int bx1 = x + 10, bx2 = x + w - 8, by = ry + SLIDER_H - 5;
                double frac = (n.d() - n.min) / (n.max - n.min);
                int fill = bx1 + (int) Math.round((bx2 - bx1) * frac);
                g.fill(bx1, by, bx2, by + 2, 0xFF2A2A33);
                g.fill(bx1, by, fill, by + 2, a);
                g.fill(fill - 1, by - 1, fill + 2, by + 3, 0xFFFFFFFF);
            }
            hits.add(new Hit(HitType.SETTING, x + 1, ry, w - 1, rh, null, m, s).clip(top, top + viewH));
            ry += rh;
        }
        settingsContentH = ry + settingsScroll - top;
        settingsScroll = Math.max(0, Math.min(settingsScroll, Math.max(0, settingsContentH - viewH)));
        g.disableScissor();
        scrollbar(g, x + w - 3, top, viewH, settingsContentH, settingsScroll, a);
        return hover;
    }

    private void scrollbar(GuiGraphicsExtractor g, int x, int y, int viewH, int contentH, int scroll, int color) {
        if (contentH <= viewH) return;
        int barH = Math.max(14, viewH * viewH / contentH);
        int barY = y + (int) ((long) (viewH - barH) * scroll / Math.max(1, contentH - viewH));
        g.fill(x, barY, x + 2, barY + barH, Colors.withAlpha(color, 0xB0));
    }

    private void drawSwitch(GuiGraphicsExtractor g, int x, int y, boolean on, int accent) {
        g.fill(x, y, x + 18, y + 8, on ? accent : 0xFF33333D);
        int kx = on ? x + 11 : x + 1;
        g.fill(kx, y + 1, kx + 6, y + 7, 0xFFF4F4F8);
    }

    private String trim(String s, int maxW) {
        if (font.width(s) <= maxW) return s;
        while (s.length() > 1 && font.width(s + "..") > maxW) s = s.substring(0, s.length() - 1);
        return s + "..";
    }

    private List<Module> visibleModules() {
        if (!search.isEmpty()) {
            List<Module> out = new ArrayList<>();
            String q = search.toLowerCase();
            for (Module m : VortexClient.modules.all())
                if (m.name.toLowerCase().contains(q) || m.description.toLowerCase().contains(q)
                    || m.settings.stream().anyMatch(st -> st.name.toLowerCase().contains(q))) out.add(m);
            return out;
        }
        return VortexClient.modules.byCategory(selectedCategory);
    }

    private float fade() {
        float t = Math.min(1f, (System.currentTimeMillis() - openedAt) / 160f);
        return 1 - (1 - t) * (1 - t);
    }

    // ---------------------------------------------------------------- input
    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int button = click.button();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.contains(mx, my)) continue;
            switch (h.type) {
                case TITLE -> { if (button == 0) { draggingWindow = true; dragDX = mx - winX; dragDY = my - winY; } }
                case CATEGORY -> { selectedCategory = h.category; search = ""; listScroll = 0; }
                case MODULE -> {
                    if (button == 0) { if (h.module == cfg()) select(h.module); else h.module.toggle(); }
                    else if (button == 1) select(selectedModule == h.module ? null : h.module);
                    else if (button == 2) binding = h.module;
                }
                case TOGGLE -> h.module.toggle();
                case CLOSE_SETTINGS -> selectedModule = null;
                case RESET -> { if (doubled && button == 0) h.module.settings.forEach(Setting::reset); }
                case BIND -> binding = binding == h.module ? null : h.module;
                case HUD_VISIBLE -> h.module.showInHud = !h.module.showInHud;
                case GROUP -> {
                    boolean allOn = groupAllOn(h.module, h.group);
                    for (Setting<?> st : h.module.settings)
                        if (st instanceof BoolSetting bb && h.group.equals(st.group)) bb.set(!allOn);
                }
                case SETTING -> {
                    Setting<?> s = h.setting;
                    if (s instanceof BoolSetting b) b.toggle();
                    else if (s instanceof ModeSetting m) m.cycle(button == 1 ? -1 : 1);
                    else if (s instanceof NumberSetting n) {
                        if (button == 1) n.reset();
                        else { sliding = n; slideX1 = h.x + 9; slideX2 = h.x + h.w - 8; applySlider(mx); }
                    }
                }
            }
            return true;
        }
        binding = null;
        return super.mouseClicked(click, doubled);
    }

    private static boolean groupAllOn(Module m, String group) {
        boolean any = false;
        for (Setting<?> st : m.settings)
            if (st instanceof BoolSetting b && group.equals(st.group) && st.isVisible()) { any = true; if (!b.on()) return false; }
        return any;
    }

    private void select(Module m) { selectedModule = m; settingsScroll = 0; }

    private void applySlider(double mx) {
        double frac = Math.max(0, Math.min(1, (mx - slideX1) / (double) Math.max(1, slideX2 - slideX1)));
        sliding.set(sliding.min + (sliding.max - sliding.min) * frac);
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (draggingWindow) { winX = (int) (mx - dragDX); winY = (int) (my - dragDY); }
        super.mouseMoved(mx, my);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        draggingWindow = false;
        sliding = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        // Ctrl/Shift + wheel over a number setting nudges its value
        if (vertical != 0 && (Keys.isDown(GLFW.GLFW_KEY_LEFT_CONTROL) || Keys.isDown(GLFW.GLFW_KEY_RIGHT_CONTROL)
            || Keys.isDown(GLFW.GLFW_KEY_LEFT_SHIFT) || Keys.isDown(GLFW.GLFW_KEY_RIGHT_SHIFT))) {
            for (Hit h : hits) {
                if (h.type == HitType.SETTING && h.contains(mx, my) && h.setting instanceof NumberSetting n) {
                    double st = n.step > 0 ? n.step : (n.max - n.min) / 100;
                    n.set(n.d() + Math.signum(vertical) * st);
                    return true;
                }
            }
        }
        int step = (int) (vertical * 20);
        int contentX = winX + SIDEBAR_W, contentW = winW() - SIDEBAR_W;
        int settingsW = selectedModule != null ? Math.max(170, contentW * 42 / 100) : 0;
        if (selectedModule != null && mx >= contentX + contentW - settingsW) settingsScroll = Math.max(0, settingsScroll - step);
        else listScroll = Math.max(0, listScroll - step);
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
        if (key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) { search = search.substring(0, search.length() - 1); listScroll = 0; return true; }
        if (key == GLFW.GLFW_KEY_ESCAPE && !search.isEmpty()) { search = ""; return true; }
        if (key == cfg().key && search.isEmpty()) { onClose(); return true; }
        char c = 0;
        if (key >= GLFW.GLFW_KEY_A && key <= GLFW.GLFW_KEY_Z) c = (char) ('a' + (key - GLFW.GLFW_KEY_A));
        else if (key >= GLFW.GLFW_KEY_0 && key <= GLFW.GLFW_KEY_9) c = (char) ('0' + (key - GLFW.GLFW_KEY_0));
        else if (key >= GLFW.GLFW_KEY_KP_0 && key <= GLFW.GLFW_KEY_KP_9) c = (char) ('0' + (key - GLFW.GLFW_KEY_KP_0));
        else if (key == GLFW.GLFW_KEY_SPACE && !search.isEmpty()) c = ' ';
        if (c != 0 && search.length() < 24) { search += c; listScroll = 0; return true; }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        return true; // typing is handled in keyPressed (char events don't reach screens reliably with some input mods)
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void removed() {
        VortexClient.config.save();
        super.removed();
    }

    // ---------------------------------------------------------------- persistence
    public static JsonObject savePanels() {
        JsonObject o = new JsonObject();
        o.addProperty("category", selectedCategory.name());
        if (winX != Integer.MIN_VALUE) { o.addProperty("x", winX); o.addProperty("y", winY); }
        return o;
    }

    public static void loadPanels(JsonObject o) {
        try {
            if (o.has("category")) selectedCategory = Category.valueOf(o.get("category").getAsString());
            if (o.has("x")) { winX = o.get("x").getAsInt(); winY = o.get("y").getAsInt(); }
        } catch (Exception ignored) {}
    }

    // ---------------------------------------------------------------- hit testing
    private enum HitType { TITLE, CATEGORY, MODULE, TOGGLE, CLOSE_SETTINGS, RESET, BIND, HUD_VISIBLE, SETTING, GROUP }

    private static final class Hit {
        final HitType type; final int x, y, w, h; final Category category; final Module module; final Setting<?> setting;
        int clipTop = Integer.MIN_VALUE, clipBottom = Integer.MAX_VALUE;
        String group;

        Hit(HitType type, int x, int y, int w, int h, Category c, Module m, Setting<?> s) {
            this.type = type; this.x = x; this.y = y; this.w = w; this.h = h; this.category = c; this.module = m; this.setting = s;
        }

        Hit clip(int top, int bottom) { clipTop = top; clipBottom = bottom; return this; }
        Hit withGroup(String g) { this.group = g; return this; }

        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h && my >= clipTop && my < clipBottom;
        }
    }
}
