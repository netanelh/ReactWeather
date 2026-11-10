package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;

/** Generic one-shot process for goals like ".surface" and ".flee". */
public class SimpleGoalProcess implements Process {
    private final String name;
    private final Goal goal;
    private boolean done;

    public SimpleGoalProcess(String name, Goal goal) { this.name = name; this.goal = goal; }

    @Override public String name() { return name; }
    @Override public String status() { return goal.describe(); }
    @Override public Goal goal() { return goal; }
    @Override public void atGoal() { done = true; }
    @Override public boolean isDone() { return done; }
}
