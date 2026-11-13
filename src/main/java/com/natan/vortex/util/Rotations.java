package com.natan.vortex.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Rotations {
    private Rotations() {}

    /** Closest point on the target's hitbox to our eyes — aims where a hit is most likely to register. */
    public static Vec3 aimPoint(Entity target) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 eye = mc.player.getEyePosition();
        AABB box = target.getBoundingBox();
        return new Vec3(
            Mth.clamp(eye.x, box.minX, box.maxX),
            Mth.clamp(eye.y, box.minY + (box.maxY - box.minY) * 0.15, box.maxY - (box.maxY - box.minY) * 0.1),
            Mth.clamp(eye.z, box.minZ, box.maxZ));
    }

    public static float[] anglesTo(Vec3 pos) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 eye = mc.player.getEyePosition();
        double dx = pos.x - eye.x, dy = pos.y - eye.y, dz = pos.z - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, h));
        return new float[]{mc.player.getYRot() + Mth.wrapDegrees(yaw - mc.player.getYRot()), Mth.clamp(pitch, -90f, 90f)};
    }

    /** Angle (degrees) between where we look and the target. */
    public static double angleTo(Entity e) {
        Minecraft mc = Minecraft.getInstance();
        float[] a = anglesTo(e.getBoundingBox().getCenter());
        double dyaw = Mth.wrapDegrees(a[0] - mc.player.getYRot());
        double dpitch = a[1] - mc.player.getXRot();
        return Math.sqrt(dyaw * dyaw + dpitch * dpitch);
    }

    /** Moves the client camera toward the target angles, at most maxStep degrees per tick. */
    public static void stepClient(float[] target, float maxStep) {
        Minecraft mc = Minecraft.getInstance();
        float dyaw = Mth.wrapDegrees(target[0] - mc.player.getYRot());
        float dpitch = target[1] - mc.player.getXRot();
        mc.player.setYRot(mc.player.getYRot() + Mth.clamp(dyaw, -maxStep, maxStep));
        mc.player.setXRot(Mth.clamp(mc.player.getXRot() + Mth.clamp(dpitch, -maxStep, maxStep), -90f, 90f));
    }

    /** Tells the server we look somewhere without moving the camera. */
    public static void sendSilent(float yaw, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
        RotationManager.request(yaw, pitch); // Rotations module on: also hold it for the next movement packet
    }
}
