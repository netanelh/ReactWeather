package com.natan.vortex.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class Chat {
    private Chat() {}

    /** Prints a client-side message (only you see it). */
    public static void info(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.execute(() -> { if (mc.player != null) mc.player.sendSystemMessage(Component.literal("§5[Vortex]§r " + msg)); });
    }

    public static void error(String msg) { info("§c" + msg); }
}
