package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import com.natan.vortex.util.Chat;

import java.util.List;

/** Runs several tasks back-to-back (something Baritone can't do): goto → mine → home, etc. */
public class SequenceProcess implements Process {
    private final List<Process> steps;
    private final String label;
    private int i;

    public SequenceProcess(List<Process> steps, String label) { this.steps = steps; this.label = label; }

    private Process cur() { return i < steps.size() ? steps.get(i) : null; }

    @Override public String name() { return "Chain"; }
    @Override public String status() {
        Process c = cur();
        return "(" + (i + 1) + "/" + steps.size() + ") " + (c == null ? label : c.name() + (c.status().isEmpty() ? "" : " · " + c.status()));
    }
    @Override public boolean isDone() { return i >= steps.size(); }

    @Override
    public void tick() {
        Process c = cur();
        if (c == null) return;
        c.tick();
        if (c.isDone()) {
            Chat.info("Chain step " + (i + 1) + "/" + steps.size() + " done.");
            c.onStop(); // release keys / clean up before the next step
            i++;
        }
    }

    @Override public Goal goal() { Process c = cur(); return c == null ? null : c.goal(); }
    @Override public void atGoal() { Process c = cur(); if (c != null) c.atGoal(); }
    @Override public boolean onPathFailed() { Process c = cur(); return c != null && c.onPathFailed(); }
    @Override public void onStop() { Process c = cur(); if (c != null) c.onStop(); }
}
