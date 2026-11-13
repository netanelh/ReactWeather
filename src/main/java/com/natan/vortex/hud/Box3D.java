package com.natan.vortex.hud;

import com.natan.vortex.util.Project;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.phys.AABB;

/**
 * Wireframe 3D boxes and lines drawn on the GUI layer: world points are moved to camera space,
 * edges are clipped at the near plane (so boxes stay correct when partly behind you) and projected.
 * Works with Sodium because it never touches the world render pipeline.
 */
public final class Box3D {
    private Box3D() {}

    private static final int[][] EDGES = {
        {0, 1}, {2, 3}, {4, 5}, {6, 7},   // along X
        {0, 2}, {1, 3}, {4, 6}, {5, 7},   // along Y
        {0, 4}, {1, 5}, {2, 6}, {3, 7}};  // along Z

    /** Draws the 12 edges of a world-space box. */
    public static void box(GuiGraphicsExtractor g, AABB bb, int col, int w, int h) {
        double[][] c = new double[8][];
        for (int i = 0; i < 8; i++)
            c[i] = Project.toCamera((i & 1) == 0 ? bb.minX : bb.maxX, (i & 2) == 0 ? bb.minY : bb.maxY, (i & 4) == 0 ? bb.minZ : bb.maxZ);
        for (int[] e : EDGES) camLine(g, c[e[0]], c[e[1]], col, w, h, 1);
    }

    /** World-space line, clipped at the near plane. */
    public static void line(GuiGraphicsExtractor g, double x1, double y1, double z1, double x2, double y2, double z2, int col, int w, int h, int thick) {
        camLine(g, Project.toCamera(x1, y1, z1), Project.toCamera(x2, y2, z2), col, w, h, thick);
    }

    private static void camLine(GuiGraphicsExtractor g, double[] a, double[] b, int col, int w, int h, int thick) {
        double n = Project.NEAR;
        if (a[2] < n && b[2] < n) return;
        if (a[2] < n) a = clip(b, a, n);
        else if (b[2] < n) b = clip(a, b, n);
        screenLine(g, Project.camToScreen(a, w, h), Project.camToScreen(b, w, h), col, thick);
    }

    /** Point on in→out where depth == n. */
    private static double[] clip(double[] in, double[] out, double n) {
        double t = (in[2] - n) / (in[2] - out[2]);
        return new double[]{in[0] + (out[0] - in[0]) * t, in[1] + (out[1] - in[1]) * t, n};
    }

    /** Screen-space line as a rotated thin rectangle (GUI has no line primitive). */
    public static void screenLine(GuiGraphicsExtractor g, float[] a, float[] b, int col, int thick) {
        float dx = b[0] - a[0], dy = b[1] - a[1];
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.5f || len > 8000) return;
        var m = g.pose();
        m.pushMatrix();
        m.translate(a[0], a[1]);
        m.rotate((float) Math.atan2(dy, dx));
        g.fill(0, 0, Math.round(len), Math.max(1, thick), col);
        m.popMatrix();
    }
}
