package com.natan.vortex.module.render;

import com.natan.vortex.VortexClient;
import com.natan.vortex.hud.Box3D;
import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;

/** Leaves a line behind you so you can retrace your way out of caves or the Nether. */
public class Breadcrumbs extends Module {
    private final NumberSetting maxPoints = add(new NumberSetting("Max Points", "How many trail points to keep", 500, 50, 3000, 50));
    private final NumberSetting spacing = add(new NumberSetting("Spacing", "Blocks between trail points", 0.5, 0.2, 3, 0.1));
    private final NumberSetting range = add(new NumberSetting("Render Range", "Only draw trail points this close (blocks)", 128, 16, 512, 16));

    private final ArrayDeque<Vec3> points = new ArrayDeque<>();

    public Breadcrumbs() { super("Breadcrumbs", "Draw a trail of where you walked", Category.RENDER); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        Vec3 p = mc.player.position();
        double sp = spacing.d();
        if (points.isEmpty() || points.peekLast().distanceToSqr(p) > sp * sp) points.addLast(p);
        while (points.size() > maxPoints.i()) points.pollFirst();
    }

    @Override public void onWorldChange() { points.clear(); }
    @Override public void onDisable() { points.clear(); }

    /** Called from HudRenderer (same thread as onTick). */
    public void renderTrail(GuiGraphicsExtractor g) {
        if (!isEnabled() || mc.player == null || points.size() < 2) return;
        int w = g.guiWidth(), h = g.guiHeight();
        int col = VortexClient.modules.clickGui().accent(0);
        double r2 = range.d() * range.d();
        Vec3 me = mc.player.position();
        Vec3 prev = null;
        for (Vec3 cur : points) {
            if (prev != null && prev.distanceToSqr(me) <= r2 && cur.distanceToSqr(me) <= r2
                    && prev.distanceToSqr(cur) <= 256) // don't join points across teleports/respawns
                Box3D.line(g, prev.x, prev.y + 0.1, prev.z, cur.x, cur.y + 0.1, cur.z, col, w, h, 1);
            prev = cur;
        }
    }
}
