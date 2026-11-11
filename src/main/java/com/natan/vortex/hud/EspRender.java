package com.natan.vortex.hud;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Esp;
import com.natan.vortex.util.Project;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** On-screen ESP: projects entity boxes, tracers and name/health tags onto the GUI layer. */
final class EspRender {
    private EspRender() {}

    static void render(GuiGraphicsExtractor g, Minecraft mc) {
        Esp esp = VortexClient.modules.get(Esp.class);
        if (!esp.isEnabled() || mc.level == null || mc.player == null) return;
        if (!esp.box.on() && !esp.tracer.on() && !esp.nametag.on()) return;
        int w = g.guiWidth(), h = g.guiHeight();
        for (Entity e : mc.level.entitiesForRendering()) {
            Integer col = esp.colorFor(e);
            if (col == null || e == mc.player) continue;
            AABB bb = e.getBoundingBox();
            // project the 8 corners, keep the screen bounding box
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            boolean any = false;
            for (int i = 0; i < 8; i++) {
                Vec3 corner = new Vec3((i & 1) == 0 ? bb.minX : bb.maxX, (i & 2) == 0 ? bb.minY : bb.maxY, (i & 4) == 0 ? bb.minZ : bb.maxZ);
                float[] s = Project.toScreen(corner, w, h);
                if (s == null) continue;
                any = true;
                minX = Math.min(minX, s[0]); maxX = Math.max(maxX, s[0]);
                minY = Math.min(minY, s[1]); maxY = Math.max(maxY, s[1]);
            }
            if (!any || maxX < 0 || minX > w || maxY < 0 || minY > h) continue;
            int x1 = (int) minX, y1 = (int) minY, x2 = (int) maxX, y2 = (int) maxY;

            if (esp.tracer.on()) line(g, w / 2, h, (x1 + x2) / 2, y2, col);
            if (esp.box.on()) {
                hLine(g, x1, x2, y1, col); hLine(g, x1, x2, y2, col);
                vLine(g, x1, y1, y2, col); vLine(g, x2, y1, y2, col);
            }
            if (esp.nametag.on()) {
                String name = e.getName().getString();
                if (e instanceof LivingEntity le) name += " " + (int) Math.ceil(le.getHealth());
                name += " " + (int) e.distanceTo(mc.player) + "m";
                int tw = mc.font.width(name);
                g.text(mc.font, name, (x1 + x2) / 2 - tw / 2, y1 - 10, 0xFFFFFFFF, true);
            }
        }
    }

    private static void hLine(GuiGraphicsExtractor g, int x1, int x2, int y, int c) { g.fill(Math.min(x1, x2), y, Math.max(x1, x2) + 1, y + 1, c); }
    private static void vLine(GuiGraphicsExtractor g, int x, int y1, int y2, int c) { g.fill(x, Math.min(y1, y2), x + 1, Math.max(y1, y2) + 1, c); }

    /** Diagonal line via small steps (GuiGraphics has no line primitive). */
    private static void line(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int c) {
        int dx = Math.abs(x2 - x1), dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        if (steps == 0) return;
        for (int i = 0; i <= steps; i += 2) {
            int x = x1 + (x2 - x1) * i / steps, y = y1 + (y2 - y1) * i / steps;
            g.fill(x, y, x + 1, y + 1, c);
        }
    }
}
