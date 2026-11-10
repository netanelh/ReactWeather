package com.natan.vortex.hud;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.client.ClickGui;
import com.natan.vortex.module.render.ServerLag;
import com.natan.vortex.module.render.Tracers;
import com.natan.vortex.util.Friends;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

/** Tracer arrows and the server-lag meter. */
final class HudMore {
    private HudMore() {}

    static void render(GuiGraphicsExtractor g, Minecraft mc, ClickGui gui) {
        var m = VortexClient.modules;
        int w = g.guiWidth(), h = g.guiHeight();
        if (m.isActive(ServerLag.class)) {
            double tps = ServerLag.tps();
            String col = tps > 18 ? "§a" : tps > 12 ? "§e" : "§c";
            String s = "TPS " + col + String.format("%.1f", tps);
            g.text(mc.font, s, w - mc.font.width(s) - 4, h - 12, 0xFFFFFFFF, true);
            double frozen = ServerLag.frozenFor();
            if (frozen > 0) {
                String warn = String.format("Server not responding: %.1fs", frozen);
                g.text(mc.font, warn, (w - mc.font.width(warn)) / 2, h / 4, 0xFFFF5555, true);
            }
        }
        Tracers tr = m.get(Tracers.class);
        if (tr.isEnabled() && mc.player != null) {
            int cx = w / 2, cy = h / 2, r = tr.radius.i();
            float yaw = mc.player.getYRot();
            for (Player p : mc.level.players()) {
                if (p == mc.player || p.isSpectator()) continue;
                boolean friend = Friends.is(p.getName().getString());
                if (friend && !tr.friends.on()) continue;
                double dx = p.getX() - mc.player.getX(), dz = p.getZ() - mc.player.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > tr.range.d()) continue;
                double targetYaw = Math.toDegrees(Math.atan2(-dx, dz));
                double rel = Math.toRadians(targetYaw - yaw);
                int x = cx + (int) (Math.sin(rel) * r), y = cy - (int) (Math.cos(rel) * r);
                int color = friend ? 0xFF22C55E : dist < 16 ? 0xFFEF4444 : dist < 48 ? 0xFFF59E0B : 0xFFE5E7EB;
                // little arrow: a 5px diamond
                g.fill(x - 1, y - 3, x + 2, y + 4, color);
                g.fill(x - 3, y - 1, x + 4, y + 2, color);
                if (tr.names.on()) {
                    String label = p.getName().getString() + " " + Math.round(dist) + "m";
                    g.text(mc.font, label, x - mc.font.width(label) / 2, y + 6, color, true);
                }
            }
        }
    }
}
