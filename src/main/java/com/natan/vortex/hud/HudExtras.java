package com.natan.vortex.hud;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.client.ClickGui;
import com.natan.vortex.module.combat.KillAura;
import com.natan.vortex.module.pathing.NavSettings;
import com.natan.vortex.module.render.DangerAlert;
import com.natan.vortex.module.render.Radar;
import com.natan.vortex.module.render.TargetHud;
import com.natan.vortex.nav.Navigator;
import com.natan.vortex.util.Colors;
import com.natan.vortex.util.Targets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

import java.util.ArrayList;
import java.util.List;

/** Radar, target info, danger warnings and navigator status. */
final class HudExtras {
    private HudExtras() {}

    static void render(GuiGraphicsExtractor g, Minecraft mc, ClickGui gui) {
        var mods = VortexClient.modules;
        int w = g.guiWidth(), h = g.guiHeight();
        Font font = mc.font;

        // ---- navigator status (top center) ----
        if (mods.get(NavSettings.class).hudStatus.on()) {
            String st = Navigator.INSTANCE.hudStatus();
            if (st != null) {
                String text = "▶ " + st;
                int tw = font.width(text), x = (w - tw) / 2, y = 4;
                g.fill(x - 5, y - 2, x + tw + 5, y + 11, 0xB0101014);
                g.fill(x - 5, y + 10, x + tw + 5, y + 11, gui.accent(0));
                g.text(font, text, x, y + 1, 0xFFFFFFFF, true);
            }
        }

        HudElements.render(g, mc, gui);

        // ---- radar ----
        Radar radar = mods.get(Radar.class);
        if (radar.isEnabled()) drawRadar(g, mc, gui, radar, w, h);

        // ---- target HUD ----
        if (mods.get(TargetHud.class).isEnabled()) {
            Entity t = mods.get(KillAura.class).target();
            if (t == null && mc.hitResult instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity) t = ehr.getEntity();
            if (t instanceof LivingEntity le) drawTarget(g, mc, gui, le, w, h);
        }

        // ---- danger alerts ----
        DangerAlert da = mods.get(DangerAlert.class);
        if (da.isEnabled()) {
            List<String> warns = dangers(mc, da);
            int y = h / 2 - 40 - warns.size() * 11;
            boolean flash = System.currentTimeMillis() / 400 % 2 == 0;
            for (String s : warns) {
                int tw = font.width(s);
                g.text(font, s, (w - tw) / 2, y, flash ? 0xFFFF5555 : 0xFFFFAA00, true);
                y += 11;
            }
        }
    }

    private static void drawRadar(GuiGraphicsExtractor g, Minecraft mc, ClickGui gui, Radar r, int w, int h) {
        int size = r.size.i();
        int x0, y0;
        switch (r.position.get()) {
            case "Bottom Right" -> { x0 = w - size - 4; y0 = h - size - 4; }
            case "Bottom Left" -> { x0 = 4; y0 = h - size - 40; }
            default -> { x0 = 4; y0 = 20; }
        }
        int cx = x0 + size / 2, cy = y0 + size / 2;
        g.fill(x0, y0, x0 + size, y0 + size, 0xB0101014);
        int acc = gui.accent(0);
        g.fill(x0, y0, x0 + size, y0 + 1, acc); g.fill(x0, y0 + size - 1, x0 + size, y0 + size, acc);
        g.fill(x0, y0, x0 + 1, y0 + size, acc); g.fill(x0 + size - 1, y0, x0 + size, y0 + size, acc);
        g.fill(cx, y0 + 2, cx + 1, y0 + size - 2, 0x30FFFFFF); g.fill(x0 + 2, cy, x0 + size - 2, cy + 1, 0x30FFFFFF);
        double scale = (size / 2.0 - 3) / r.range.d();
        double yaw = Math.toRadians(mc.player.getYRot());
        double sin = Math.sin(yaw), cos = Math.cos(yaw);
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player) continue;
            int color;
            if (e instanceof Player) { if (!r.players.on()) continue; color = 0xFFFF5555; }
            else if (e instanceof ItemEntity) { if (!r.items.on()) continue; color = 0xFF55FFFF; }
            else if (Targets.isHostile(e)) { if (!r.hostiles.on()) continue; color = 0xFFFFAA00; }
            else if (e instanceof LivingEntity && Targets.isPassive(e)) { if (!r.passives.on()) continue; color = 0xFF55FF55; }
            else continue;
            double dx = e.getX() - mc.player.getX(), dz = e.getZ() - mc.player.getZ();
            if (dx * dx + dz * dz > r.range.d() * r.range.d()) continue;
            double rx = dx, ry = dz;
            if (r.rotate.on()) { rx = dx * cos + dz * sin; ry = -dx * sin + dz * cos; rx = -rx; ry = -ry; }
            int px = cx + (int) Math.round(rx * scale), py = cy + (int) Math.round(ry * scale);
            g.fill(px - 1, py - 1, px + 2, py + 2, color);
            if (e instanceof Player p && r.names.on()) {
                String n = p.getName().getString();
                g.text(mc.font, n, px - mc.font.width(n) / 2, py - 10, 0xFFFFFFFF, true);
            }
        }
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF); // you
    }

    private static void drawTarget(GuiGraphicsExtractor g, Minecraft mc, ClickGui gui, LivingEntity t, int w, int h) {
        Font font = mc.font;
        int bw = 140, bh = 46, x = w / 2 + 12, y = h / 2 + 12;
        g.fill(x, y, x + bw, y + bh, 0xC0101014);
        g.fill(x, y, x + 2, y + bh, gui.accent(0));
        g.text(font, t.getName().getString(), x + 6, y + 4, 0xFFFFFFFF, true);
        String dist = String.format("%.1fm", t.distanceTo(mc.player));
        g.text(font, dist, x + bw - 5 - font.width(dist), y + 4, 0xFFAAAAAA, false);
        float hp = t.getHealth() + t.getAbsorptionAmount(), max = t.getMaxHealth();
        float frac = Math.max(0, Math.min(1, t.getHealth() / max));
        int barW = bw - 12;
        g.fill(x + 6, y + 16, x + 6 + barW, y + 20, 0xFF2A2A33);
        g.fill(x + 6, y + 16, x + 6 + (int) (barW * frac), y + 20, Colors.lerp(0xFFEF4444, 0xFF22C55E, frac));
        String hpText = String.format("%.1f ❤", hp / 2f);
        g.text(font, hpText, x + 6, y + 23, 0xFFFF7777, false);
        boolean winning = mc.player.getHealth() + mc.player.getAbsorptionAmount() >= hp;
        String verdict = winning ? "winning" : "losing";
        g.text(font, verdict, x + bw - 5 - font.width(verdict), y + 23, winning ? 0xFF55FF55 : 0xFFFF5555, false);
        int ix = x + 6;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            ItemStack st = t.getItemBySlot(s);
            if (!st.isEmpty()) g.item(st, ix, y + bh - 17);
            ix += 18;
            if (ix > x + bw - 16) break;
        }
    }

    private static List<String> dangers(Minecraft mc, DangerAlert da) {
        List<String> out = new ArrayList<>();
        double pr = da.playerRange.d();
        for (Player p : mc.level.players()) {
            if (p == mc.player) continue;
            double d = p.distanceTo(mc.player);
            if (d <= pr) out.add("⚠ " + p.getName().getString() + " " + Math.round(d) + "m");
            if (out.size() >= 4) break;
        }
        if (da.creepers.on()) {
            for (Entity e : mc.level.entitiesForRendering()) {
                double d = e.distanceTo(mc.player);
                if (net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().equals("creeper") && d < 7) { out.add("⚠ Creeper " + Math.round(d) + "m"); break; }
                if (e instanceof PrimedTnt tnt && d < 9) { out.add("⚠ TNT " + String.format("%.1fs", tnt.getFuse() / 20f)); break; }
            }
        }
        if (da.armor.on()) {
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                ItemStack st = mc.player.getItemBySlot(s);
                if (st.isDamageableItem() && st.getMaxDamage() - st.getDamageValue() < st.getMaxDamage() * 0.1) {
                    out.add("⚠ " + st.getHoverName().getString() + " almost broken"); break;
                }
            }
        }
        if (da.totems.on() && VortexClient.modules.isActive(com.natan.vortex.module.combat.AutoTotem.class)
            && !mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING) && com.natan.vortex.util.Inv.findAny(s -> s.is(Items.TOTEM_OF_UNDYING)) == -1)
            out.add("⚠ No totems left!");
        if (da.fall.on() && mc.player.fallDistance > 12 && !mc.player.isFallFlying()) out.add("⚠ Falling " + Math.round(mc.player.fallDistance) + " blocks");
        return out;
    }
}
