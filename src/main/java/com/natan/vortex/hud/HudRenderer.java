package com.natan.vortex.hud;

import com.natan.vortex.VortexClient;
import com.natan.vortex.mixin.MinecraftAccessor;
import com.natan.vortex.module.Module;
import com.natan.vortex.module.client.ClickGui;
import com.natan.vortex.module.client.Hud;
import com.natan.vortex.util.Move;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class HudRenderer {
    private HudRenderer() {}

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || VortexClient.modules == null) return;
        Hud hud = VortexClient.modules.get(Hud.class);
        if (!hud.isEnabled()) return;
        ClickGui gui = VortexClient.modules.clickGui();
        Font font = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();
        int bg = hud.background.on() ? 0x90101014 : 0;

        if (hud.watermark.on()) {
            String name = VortexClient.NAME, ver = " " + VortexClient.VERSION;
            int tw = font.width(name + ver);
            if (bg != 0) g.fill(2, 2, 8 + tw, 15, bg);
            g.fill(2, 2, 3, 15, gui.accent(0));
            g.text(font, name, 5, 5, gui.accent(0), true);
            g.text(font, ver, 5 + font.width(name), 5, 0xFFAAAAAA, true);
        }

        if (hud.moduleList.on()) {
            List<Module> active = new ArrayList<>();
            for (Module m : VortexClient.modules.all()) if (m.isEnabled() && m.showInHud) active.add(m);
            if (hud.sort.is("ABC")) active.sort(Comparator.comparing(m -> m.name));
            else active.sort(Comparator.comparingInt((Module m) -> font.width(label(m, hud))).reversed());
            int y = 2;
            for (int i = 0; i < active.size(); i++) {
                Module m = active.get(i);
                String name = m.name;
                String info = hud.showInfo.on() ? m.info() : null;
                int tw = font.width(label(m, hud));
                int x = w - tw - 5;
                if (bg != 0) g.fill(x - 3, y, w - 2, y + 11, bg);
                g.fill(w - 2, y, w - 1, y + 11, gui.accent(i));
                g.text(font, name, x, y + 2, gui.accent(i), true);
                if (info != null) g.text(font, " " + info, x + font.width(name), y + 2, 0xFFAAAAAA, true);
                y += 11;
            }
        }

        int by = h - 11;
        boolean chatOpen = mc.gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen;
        if (chatOpen) by -= 14;
        if (hud.coords.on()) {
            double x = mc.player.getX(), yy = mc.player.getY(), z = mc.player.getZ();
            boolean nether = mc.level.dimension() == net.minecraft.world.level.Level.NETHER;
            double f = nether ? 8 : 0.125;
            String main = String.format("XYZ %.1f %.1f %.1f", x, yy, z);
            String other = String.format(" [%.0f %.0f]", x * f, z * f);
            drawLine(g, font, main, other, 2, by, bg, gui.accent(0));
            by -= 11;
        }
        if (hud.speed.on()) {
            drawLine(g, font, "Speed ", String.format("%.1f b/s", Move.horizontalSpeed() * 20), 2, by, bg, gui.accent(0));
            by -= 11;
        }
        if (hud.fps.on()) {
            drawLine(g, font, "FPS ", Integer.toString(MinecraftAccessor.vortex$getFps()), 2, by, bg, gui.accent(0));
        }

        HudExtras.render(g, mc, gui);

        if (hud.armor.on() && !mc.player.isCreative()) {
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            int ax = w / 2 + 10, ay = h - 56;
            if (mc.player.isUnderWater()) ay -= 10;
            for (EquipmentSlot s : slots) {
                ItemStack st = mc.player.getItemBySlot(s);
                if (!st.isEmpty()) {
                    g.item(st, ax, ay);
                    g.itemDecorations(font, st, ax, ay);
                }
                ax += 18;
            }
        }
    }

    private static String label(Module m, Hud hud) {
        String info = hud.showInfo.on() ? m.info() : null;
        return info == null ? m.name : m.name + " " + info;
    }

    private static void drawLine(GuiGraphicsExtractor g, Font font, String a, String b, int x, int y, int bg, int accent) {
        int tw = font.width(a + b);
        if (bg != 0) g.fill(x, y - 1, x + tw + 4, y + 10, bg);
        g.text(font, a, x + 2, y + 1, accent, true);
        g.text(font, b, x + 2 + font.width(a), y + 1, 0xFFDDDDDD, true);
    }
}
