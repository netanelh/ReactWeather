package com.natan.vortex.util;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.phys.Vec3;

/** Moves you in hops the server accepts (vanilla rejects >10 blocks per movement packet). */
public final class Teleport {
    private Teleport() {}
    public static void to(Vec3 to) {
        Minecraft mc = Minecraft.getInstance();
        Vec3 from = mc.player.position();
        double hopSize = 8.5;
        var ps = com.natan.vortex.VortexClient.modules.get(com.natan.vortex.module.client.PaperSafe.class);
        if (ps.isEnabled()) hopSize = ps.hop();
        int hops = Math.max(1, (int) Math.ceil(from.distanceTo(to) / hopSize));
        for (int i = 1; i <= hops; i++) {
            Vec3 p = from.lerp(to, (double) i / hops);
            mc.getConnection().send(new ServerboundMovePlayerPacket.Pos(p.x, p.y, p.z, false, false));
        }
        mc.player.setPos(to.x, to.y, to.z);
        mc.player.setDeltaMovement(Vec3.ZERO);
    }
}
