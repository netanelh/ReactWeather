package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;

public class FollowProcess implements Process {
    private final String target; // null = nearest player
    private final double distance;
    private Player player;
    private BlockPos lastPos;
    private Goal goal;

    public FollowProcess(String target, double distance) { this.target = target; this.distance = distance; }

    @Override public String name() { return "Follow"; }
    @Override public String status() { return player == null ? "looking for " + (target == null ? "a player" : target) : player.getName().getString(); }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        player = null;
        double best = Double.MAX_VALUE;
        for (Player p : mc.level.players()) {
            if (p == mc.player) continue;
            if (target != null && !p.getName().getString().equalsIgnoreCase(target)) continue;
            double d = p.distanceToSqr(mc.player);
            if (d < best) { best = d; player = p; }
        }
        if (player == null) { goal = null; return; }
        BlockPos pos = player.blockPosition();
        if (lastPos == null || lastPos.distSqr(pos) > 4 || goal == null) {
            lastPos = pos;
            goal = new Goal.Near(pos, distance);
        }
    }

    @Override public Goal goal() { return goal; }
    @Override public boolean isDone() { return false; }
}
