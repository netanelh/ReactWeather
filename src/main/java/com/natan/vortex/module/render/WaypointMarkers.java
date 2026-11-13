package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.nav.Waypoints;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Project;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

/** Shows saved waypoints (.wp, home, death) as name + distance tags on screen. */
public class WaypointMarkers extends Module {
    private final NumberSetting maxDistance = add(new NumberSetting("Max Distance", "Hide waypoints farther than this (blocks)", 1000, 16, 10000, 16));
    private final BoolSetting deathOnly = add(new BoolSetting("Death Only", "Only show the automatic 'death' waypoint", false));

    public WaypointMarkers() { super("Waypoints", "Show saved waypoints on screen", Category.RENDER); }

    /** Called from HudRenderer. */
    public void renderMarkers(GuiGraphicsExtractor g) {
        if (!isEnabled() || mc.player == null || mc.level == null) return;
        String dim = mc.level.dimension().identifier().getPath();
        int w = g.guiWidth(), h = g.guiHeight();
        double max = maxDistance.d();
        for (var e : Waypoints.INSTANCE.here().entrySet()) {
            Waypoints.Point p = e.getValue();
            if (p == null || !dim.equals(p.dim())) continue;
            if (deathOnly.on() && !e.getKey().equals("death")) continue;
            Vec3 at = new Vec3(p.x() + 0.5, p.y() + 1.5, p.z() + 0.5);
            double distSq = mc.player.position().distanceToSqr(at);
            if (distSq > max * max) continue;
            float[] s = Project.toScreen(at, w, h);
            if (s == null || s[0] < 0 || s[0] > w || s[1] < 0 || s[1] > h) continue;
            int x = (int) s[0], y = (int) s[1];
            int color = e.getKey().equals("death") ? 0xFFFF5555 : e.getKey().equals("home") ? 0xFF55FF55 : 0xFF55CCFF;
            String label = e.getKey() + " " + (int) Math.sqrt(distSq) + "m";
            int tw = mc.font.width(label);
            g.fill(x - 2, y - 2, x + 2, y + 2, color);
            g.fill(x - tw / 2 - 2, y - 14, x + tw / 2 + 2, y - 4, 0x90101014);
            g.text(mc.font, label, x - tw / 2, y - 13, color, true);
        }
    }
}
