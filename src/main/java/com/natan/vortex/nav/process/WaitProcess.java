package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;

/** Pauses a chain for a number of seconds. */
public class WaitProcess implements Process {
    private int ticks;
    public WaitProcess(double seconds) { this.ticks = (int) (seconds * 20); }
    @Override public String name() { return "Wait"; }
    @Override public String status() { return (ticks / 20) + "s left"; }
    @Override public Goal goal() { return null; }
    @Override public void tick() { if (ticks > 0) ticks--; }
    @Override public boolean isDone() { return ticks <= 0; }
    @Override public void atGoal() {}
}
