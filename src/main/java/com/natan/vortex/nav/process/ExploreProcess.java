package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import net.minecraft.client.Minecraft;

/** Walks an expanding square spiral around the start point so new chunks keep loading. */
public class ExploreProcess implements Process {
    private final int originX, originZ, step;
    private int leg = 1, legProgress, dir, x, z, visited;
    private Goal goal;

    public ExploreProcess(int step) {
        var p = Minecraft.getInstance().player;
        originX = p.getBlockX(); originZ = p.getBlockZ();
        this.step = step;
        next();
    }

    private void next() {
        int[][] d = {{1, 0}, {0, 1}, {-1, 0}, {0, -1}};
        x += d[dir][0]; z += d[dir][1];
        if (++legProgress >= leg) { legProgress = 0; dir = (dir + 1) % 4; if (dir % 2 == 0) leg++; }
        goal = new Goal.XZ(originX + x * step, originZ + z * step);
    }

    @Override public String name() { return "Explore"; }
    @Override public String status() { return visited + " points, radius " + leg * step; }
    @Override public Goal goal() { return goal; }
    @Override public void atGoal() { visited++; next(); }
    @Override public boolean isDone() { return false; }
}
