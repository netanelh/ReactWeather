package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;

public class GotoProcess implements Process {
    private final Goal goal;
    private final String label;
    private boolean done;

    public GotoProcess(Goal goal, String label) { this.goal = goal; this.label = label; }

    @Override public String name() { return "Goto"; }
    @Override public String status() { return label; }
    @Override public Goal goal() { return goal; }
    @Override public void atGoal() { done = true; }
    @Override public boolean isDone() { return done; }
}
