package com.natan.vortex.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec2;

public final class Move {
    private Move() {}

    public static boolean isMoving() {
        Vec2 v = Minecraft.getInstance().player.input.getMoveVector();
        return v.x != 0 || v.y != 0;
    }

    /** Horizontal velocity (x, z) for the current WASD input at the given speed (blocks/tick). */
    public static double[] forSpeed(double speed) {
        Minecraft mc = Minecraft.getInstance();
        Vec2 in = mc.player.input.getMoveVector();
        double forward = in.y, strafe = in.x;
        if (forward == 0 && strafe == 0) return new double[]{0, 0};
        double len = Math.sqrt(forward * forward + strafe * strafe);
        forward /= len; strafe /= len;
        double rad = Math.toRadians(mc.player.getYRot());
        double sin = Math.sin(rad), cos = Math.cos(rad);
        return new double[]{(-sin * forward + cos * strafe) * speed, (cos * forward + sin * strafe) * speed};
    }

    public static void setHorizontal(double speed) {
        Minecraft mc = Minecraft.getInstance();
        double[] v = forSpeed(speed);
        mc.player.setDeltaMovement(v[0], mc.player.getDeltaMovement().y, v[1]);
    }

    public static double horizontalSpeed() {
        var d = Minecraft.getInstance().player.getDeltaMovement();
        return Math.sqrt(d.x * d.x + d.z * d.z);
    }
}
