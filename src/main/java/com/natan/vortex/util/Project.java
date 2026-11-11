package com.natan.vortex.util;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/** Projects a world point onto the (GUI-scaled) screen, matrix-free, for on-screen ESP. */
public final class Project {
    private Project() {}

    /** @return {screenX, screenY} in GUI coords, or null if the point is behind the camera. */
    public static float[] toScreen(Vec3 world, int width, int height) {
        Minecraft mc = Minecraft.getInstance();
        Camera cam = mc.gameRenderer.getMainCamera();
        Vec3 c = cam.position();
        double rx = world.x - c.x, ry = world.y - c.y, rz = world.z - c.z;
        Vector3fc f = cam.forwardVector(), u = cam.upVector(), l = cam.leftVector();
        double cz = rx * f.x() + ry * f.y() + rz * f.z();
        if (cz < 0.05) return null;
        double cx = rx * l.x() + ry * l.y() + rz * l.z();
        double cy = rx * u.x() + ry * u.y() + rz * u.z();
        double fov = Math.toRadians(Math.max(1, cam.getFov()));
        double scale = (height / 2.0) / Math.tan(fov / 2.0);
        return new float[]{(float) (width / 2.0 - (cx / cz) * scale), (float) (height / 2.0 - (cy / cz) * scale)};
    }
}
