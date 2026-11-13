package com.natan.vortex.hud;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Esp;
import com.natan.vortex.util.Colors;
import com.natan.vortex.util.Project;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** On-screen ESP: 2D/3D entity boxes, tracers and name/health tags drawn on the GUI layer (Sodium-safe). */
final class EspRender {
    private EspRender() {}

    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    static void render(GuiGraphicsExtractor g, Minecraft mc, float pt) {
        Esp esp = VortexClient.modules.get(Esp.class);
        if (!esp.isEnabled() || mc.level == null || mc.player == null) return;
        if (!esp.box.on() && !esp.tracer.on() && !esp.nametag.on()) return;
        int w = g.guiWidth(), h = g.guiHeight();
        boolean box3d = esp.boxMode.is("3D");
        int drawn = 0;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (drawn >= 300) break;
            Integer col = esp.colorFor(e);
            if (col == null || e == mc.player) continue;
            // interpolate to the frame position so boxes don't lag behind moving entities
            Vec3 lerp = e.getPosition(pt);
            AABB bb = e.getBoundingBox().move(lerp.x - e.getX(), lerp.y - e.getY(), lerp.z - e.getZ());
            Vec3 c = bb.getCenter();
            drawn++;

            if (esp.tracer.on()) {
                float[] s = Project.toScreen(c, w, h);
                if (s != null) Box3D.screenLine(g, new float[]{w / 2f, h}, s, col, 1);
            }
            if (esp.box.on()) {
                if (box3d) Box3D.box(g, bb, col, w, h);
                else box2d(g, bb, col, w, h);
            }
            if (esp.nametag.on()) tag(g, mc, esp, e, new Vec3(c.x, bb.maxY + 0.25, c.z), w, h);
        }
    }

    private static void box2d(GuiGraphicsExtractor g, AABB bb, int col, int w, int h) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (int i = 0; i < 8; i++) {
            float[] s = Project.toScreen(new Vec3((i & 1) == 0 ? bb.minX : bb.maxX, (i & 2) == 0 ? bb.minY : bb.maxY, (i & 4) == 0 ? bb.minZ : bb.maxZ), w, h);
            if (s == null) return; // partly behind the camera: skip, avoids huge broken rectangles
            minX = Math.min(minX, s[0]); maxX = Math.max(maxX, s[0]);
            minY = Math.min(minY, s[1]); maxY = Math.max(maxY, s[1]);
        }
        if (maxX < 0 || minX > w || maxY < 0 || minY > h) return;
        int x1 = (int) minX, y1 = (int) minY, x2 = (int) maxX, y2 = (int) maxY;
        g.fill(x1, y1, x2 + 1, y1 + 1, col); g.fill(x1, y2, x2 + 1, y2 + 1, col);
        g.fill(x1, y1, x1 + 1, y2 + 1, col); g.fill(x2, y1, x2 + 1, y2 + 1, col);
    }

    private static void tag(GuiGraphicsExtractor g, Minecraft mc, Esp esp, Entity e, Vec3 anchor, int w, int h) {
        float[] s = Project.toScreen(anchor, w, h);
        if (s == null || s[0] < -100 || s[0] > w + 100 || s[1] < -40 || s[1] > h + 40) return;
        String name = e.getName().getString();
        String hp = "";
        int hpCol = 0xFFFFFFFF;
        if (e instanceof LivingEntity le) {
            float max = Math.max(1, le.getMaxHealth());
            float cur = le.getHealth() + le.getAbsorptionAmount();
            hp = " " + (int) Math.ceil(cur);
            hpCol = Colors.lerp(0xFFEF4444, 0xFF22C55E, Math.max(0, Math.min(1, le.getHealth() / max)));
        }
        String dist = " " + (int) e.distanceTo(mc.player) + "m";
        var f = mc.font;
        int tw = f.width(name) + f.width(hp) + f.width(dist);
        int x = (int) s[0] - tw / 2, y = (int) s[1] - 10;
        g.fill(x - 2, y - 1, x + tw + 2, y + 9, 0x90101014);
        g.text(f, name, x, y, 0xFFFFFFFF, true);
        g.text(f, hp, x + f.width(name), y, hpCol, true);
        g.text(f, dist, x + f.width(name) + f.width(hp), y, 0xFFAAAAAA, true);

        if (esp.tagItems.on() && e instanceof Player p) {
            ItemStack[] items = new ItemStack[]{p.getMainHandItem(), p.getItemBySlot(ARMOR[0]), p.getItemBySlot(ARMOR[1]), p.getItemBySlot(ARMOR[2]), p.getItemBySlot(ARMOR[3]), p.getOffhandItem()};
            int n = 0;
            for (ItemStack st : items) if (!st.isEmpty()) n++;
            if (n == 0) return;
            int ix = (int) s[0] - n * 8, iy = y - 18;
            for (ItemStack st : items) {
                if (st.isEmpty()) continue;
                g.item(st, ix, iy);
                g.itemDecorations(f, st, ix, iy);
                ix += 16;
            }
        }
    }
}
