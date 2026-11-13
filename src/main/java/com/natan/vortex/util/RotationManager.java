package com.natan.vortex.util;

import com.natan.vortex.VortexClient;
import com.natan.vortex.module.client.RotationSettings;

/**
 * Holds the latest silent rotation request. Requests are made at the end of client tick N and are
 * applied to the movement packet sent at the start of tick N+1 (LocalPlayer.sendPosition), then dropped.
 */
public final class RotationManager {
    private RotationManager() {}

    private static long tick;
    private static long reqTick = Long.MIN_VALUE;
    private static float reqYaw, reqPitch;
    private static float savedYaw, savedPitch;
    private static boolean applied;

    /** Called once per client tick (end of tick). */
    public static void endTick() { tick++; }

    public static boolean active() {
        return VortexClient.modules != null && VortexClient.modules.isActive(RotationSettings.class);
    }

    public static void request(float yaw, float pitch) {
        if (!active()) return;
        reqYaw = yaw; reqPitch = pitch; reqTick = tick;
    }

    /** sendPosition HEAD: swap in the requested look. */
    public static void beforeSend(net.minecraft.client.player.LocalPlayer p) {
        applied = false;
        if (reqTick != tick || !active()) return;
        savedYaw = p.getYRot(); savedPitch = p.getXRot();
        p.setYRot(reqYaw); p.setXRot(reqPitch);
        applied = true;
        reqTick = Long.MIN_VALUE;
    }

    /** sendPosition RETURN: restore the real camera look. */
    public static void afterSend(net.minecraft.client.player.LocalPlayer p) {
        if (!applied) return;
        p.setYRot(savedYaw); p.setXRot(savedPitch);
        applied = false;
    }

    public static void reset() { reqTick = Long.MIN_VALUE; applied = false; }
}
