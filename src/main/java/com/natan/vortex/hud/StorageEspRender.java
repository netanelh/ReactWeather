package com.natan.vortex.hud;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.render.StorageEsp;
import com.natan.vortex.util.Project;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Draws StorageESP boxes/tracers on the GUI layer. */
final class StorageEspRender {
    private StorageEspRender() {}

    static void render(GuiGraphicsExtractor g, Minecraft mc) {
        StorageEsp se = VortexClient.modules.get(StorageEsp.class);
        if (!se.isEnabled() || mc.level == null) return;
        var found = se.found;
        int[] cols = se.colors;
        int n = Math.min(found.size(), cols.length);
        if (n == 0) return;
        int w = g.guiWidth(), h = g.guiHeight();
        for (int i = 0; i < n; i++) {
            BlockPos p = found.get(i);
            int col = cols[i];
            Box3D.box(g, new AABB(p).deflate(0.0625), col, w, h);
            if (se.tracers.on()) {
                float[] s = Project.toScreen(Vec3.atCenterOf(p), w, h);
                if (s != null) Box3D.screenLine(g, new float[]{w / 2f, h}, s, col, 1);
            }
        }
    }
}
