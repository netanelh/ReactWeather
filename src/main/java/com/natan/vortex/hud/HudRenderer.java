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
        if (VortexClient.modules.get(com.natan.vortex.module.pathing.NavSettings.class).showPath.on()) PathOverlay.render(g);
        Hud hud = VortexClient.modules.get(Hud.class);
        ClickGui gui = VortexClient.modules.clickGui();
        Font font = mc.font;
        int w = g.guiWidth(), h = g.guiHeight();
        int bg = hud.background.on() ? 0x90101014 : 0;

        // ESP / Xray boxes / Tracers / HUD widgets each check their own module, so they work even with "HUD" off
        float pt = delta.getGameTimeDeltaPartialTick(false);
        EspRender.render(g, mc, pt);
        BlockEspRender.render(g, mc);
        HudMore.render(g, mc, gui);
        HudExtras.render(g, mc, gui);
        VortexClient.modules.get(com.natan.vortex.module.render.LogoutSpots.class).renderMarkers(g);
        if (!hud.isEnabled()) return;

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

    private static void drawLine(GuiGraphicsExtractor g, Font font, String a, String b, int x, int y, int bg, int accent) {
        int tw = font.width(a + b);
        if (bg != 0) g.fill(x, y - 1, x + tw + 4, y + 10, bg);
        g.text(font, a, x + 2, y + 1, accent, true);
        g.text(font, b, x + 2 + font.width(a), y + 1, 0xFFDDDDDD, true);
    }
}
