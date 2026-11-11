package com.natan.vortex.hud;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Xray;
import com.natan.vortex.util.Project;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

/** Draws projected boxes around Xray ore/loot blocks on the GUI layer (Sodium-safe). */
final class BlockEspRender {
    private BlockEspRender() {}

    static void render(GuiGraphicsExtractor g, Minecraft mc) {
        Xray xray = VortexClient.modules.get(Xray.class);
        if (!xray.isEnabled() || mc.level == null) return;
        var found = xray.found();
        if (found.isEmpty()) return;
        int w = g.guiWidth(), h = g.guiHeight();
        int drawn = 0;
        for (BlockPos p : found) {
            if (drawn >= 300) break;
            int col = xray.colorOf(mc.level.getBlockState(p).getBlock());
            float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
            boolean any = false;
            for (int i = 0; i < 8; i++) {
                Vec3 c = new Vec3(p.getX() + (i & 1), p.getY() + ((i >> 1) & 1), p.getZ() + ((i >> 2) & 1));
                float[] s = Project.toScreen(c, w, h);
                if (s == null) continue;
                any = true;
                minX = Math.min(minX, s[0]); maxX = Math.max(maxX, s[0]);
                minY = Math.min(minY, s[1]); maxY = Math.max(maxY, s[1]);
            }
            if (!any || maxX < 0 || minX > w || maxY < 0 || minY > h) continue;
            int x1 = (int) minX, y1 = (int) minY, x2 = (int) maxX, y2 = (int) maxY;
            g.fill(x1, y1, x2 + 1, y1 + 1, col); g.fill(x1, y2, x2 + 1, y2 + 1, col);
            g.fill(x1, y1, x1 + 1, y2 + 1, col); g.fill(x2, y1, x2 + 1, y2 + 1, col);
            drawn++;
        }
    }
}
