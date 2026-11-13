package com.natan.vortex.hud;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.Xray;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;

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
            Box3D.box(g, new net.minecraft.world.phys.AABB(p), col, w, h);
            drawn++;
        }
    }
}
