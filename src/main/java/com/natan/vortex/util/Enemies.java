package com.natan.vortex.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Nearest non-friend player in range. */
public final class Enemies {
    private Enemies() {}
    public static Player nearest(double range) {
        Minecraft mc = Minecraft.getInstance();
        Player best = null; double bd = range * range;
        for (Player p : mc.level.players()) {
            if (p == mc.player || !p.isAlive() || p.isSpectator() || Friends.is(p.getName().getString())) continue;
            double d = p.distanceToSqr(mc.player);
            if (d < bd) { bd = d; best = p; }
        }
        return best;
    }
}
