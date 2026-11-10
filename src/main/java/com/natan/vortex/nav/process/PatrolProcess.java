package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import net.minecraft.core.BlockPos;

import java.util.List;

/** Walks between waypoints in a loop forever (guarding a base, farm rounds). ".patrol home farm mine". */
public class PatrolProcess implements Process {
    private final List<BlockPos> points;
    private final List<String> names;
    private int index, laps;

    public PatrolProcess(List<String> names, List<BlockPos> points) { this.names = names; this.points = points; }

    @Override public String name() { return "Patrol"; }
    @Override public String status() { return "→ " + names.get(index) + " · lap " + (laps + 1); }
    @Override public Goal goal() { return new Goal.Near(points.get(index), 1.5); }
    @Override public void atGoal() { index++; if (index >= points.size()) { index = 0; laps++; } }
    @Override public boolean isDone() { return false; }
}
