package com.natan.vortex.util;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/** Projects a world point onto the (GUI-scaled) screen, matrix-free, for on-screen ESP. */
public final class Project {
    private Project() {}

    /** Near plane used for clipping (camera-space depth). */
    public static final double NEAR = 0.05;

    /** @return {screenX, screenY} in GUI coords, or null if the point is behind the camera. */
    public static float[] toScreen(Vec3 world, int width, int height) {
        double[] c = toCamera(world.x, world.y, world.z);
        if (c[2] < NEAR) return null;
        return camToScreen(c, width, height);
    }

    /** World point to camera space {left, up, forward}. */
    public static double[] toCamera(double x, double y, double z) {
        Camera cam = Minecraft.getInstance().gameRenderer.mainCamera();
        Vec3 c = cam.position();
        double rx = x - c.x, ry = y - c.y, rz = z - c.z;
        Vector3fc f = cam.forwardVector(), u = cam.upVector(), l = cam.leftVector();
        return new double[]{rx * l.x() + ry * l.y() + rz * l.z(), rx * u.x() + ry * u.y() + rz * u.z(), rx * f.x() + ry * f.y() + rz * f.z()};
    }

    /** Camera-space point (depth must be >= NEAR) to GUI coords. */
    public static float[] camToScreen(double[] c, int width, int height) {
        Camera cam = Minecraft.getInstance().gameRenderer.mainCamera();
        double fov = Math.toRadians(Math.max(1, cam.getFov()));
        double scale = (height / 2.0) / Math.tan(fov / 2.0);
        return new float[]{(float) (width / 2.0 - (c[0] / c[2]) * scale), (float) (height / 2.0 - (c[1] / c[2]) * scale)};
    }
}
