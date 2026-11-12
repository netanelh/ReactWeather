package com.natan.vortex.hud;

import com.natan.vortex.nav.Executor;
import com.natan.vortex.nav.Navigator;
import com.natan.vortex.nav.Step;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;

import java.util.List;

/**
 * Draws the navigator's planned path on screen: the world matrices are captured each frame while
 * the level renders (GameRendererMixin), then every path point is projected to screen space and
 * joined with thin coloured lines on the HUD layer. (No depth test: the line shows through walls.)
 */
public final class PathOverlay {
    private static final Matrix4f PROJ = new Matrix4f(), VIEW = new Matrix4f();
    private static Vec3 cam = Vec3.ZERO;
    private static boolean valid;

    private PathOverlay() {}

    public static void capture(Matrix4f proj, Matrix4fc view) {
        PROJ.set(proj);
        VIEW.set(view);
        cam = Minecraft.getInstance().gameRenderer.mainCamera().position();
        valid = true;
    }

    private static float[] project(double x, double y, double z, int w, int h) {
        Vector4f v = new Vector4f((float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z), 1f);
        v.mul(VIEW);
        v.mul(PROJ);
        if (v.w <= 0.05f) return null; // behind the camera
        return new float[]{(v.x / v.w * 0.5f + 0.5f) * w, (0.5f - v.y / v.w * 0.5f) * h};
    }

    public static void render(GuiGraphicsExtractor g) {
        Executor ex = Navigator.INSTANCE.executor();
        if (!valid || ex == null) return;
        List<Step> st = ex.steps();
        if (st.isEmpty()) return;
        int w = g.guiWidth(), h = g.guiHeight();
        float[] prev = null;
        for (int i = Math.max(0, ex.index() - 1); i < st.size() && i < ex.index() + 64; i++) {
            BlockPos b = st.get(i).pos();
            float[] s = project(b.getX() + 0.5, b.getY() + 0.1, b.getZ() + 0.5, w, h);
            if (s != null && prev != null) line(g, prev, s, color(st.get(i)));
            prev = s;
        }
        // goal marker: small square at the end of the path
        BlockPos end = st.get(st.size() - 1).pos();
        float[] e = project(end.getX() + 0.5, end.getY() + 0.5, end.getZ() + 0.5, w, h);
        if (e != null) g.fill(Math.round(e[0]) - 3, Math.round(e[1]) - 3, Math.round(e[0]) + 3, Math.round(e[1]) + 3, 0xC040A0FF);
    }

    private static int color(Step s) {
        if (!s.toBreak().isEmpty()) return 0xC0FF4040;       // mining
        if (s.toPlace() != null) return 0xC04080FF;          // bridging / pillaring
        return switch (s.move()) {
            case ASCEND, PILLAR -> 0xC0FFD040;
            case DESCEND, DIG_DOWN -> 0xC0FF9030;
            case PARKOUR -> 0xC0FF40FF;
            case CLIMB, SWIM_UP -> 0xC040E0FF;
            default -> 0xC040FF60;
        };
    }

    private static void line(GuiGraphicsExtractor g, float[] a, float[] b, int col) {
        float dx = b[0] - a[0], dy = b[1] - a[1];
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1 || len > 4000) return;
        var m = g.pose();
        m.pushMatrix();
        m.translate(a[0], a[1]);
        m.rotate((float) Math.atan2(dy, dx));
        g.fill(0, -1, Math.round(len), 1, col);
        m.popMatrix();
    }
}
