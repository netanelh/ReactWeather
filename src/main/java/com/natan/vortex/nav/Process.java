package com.natan.vortex.nav;

/** A high-level task (goto, mine, follow...). The navigator asks it for a goal each tick. */
public interface Process {
    String name();

    /** Short live status for the HUD, e.g. "3 diamonds left". */
    default String status() { return ""; }

    /** Called every tick before pathing. */
    default void tick() {}

    /** Where to go right now, or null if the process is acting in place (mining, farming, tunnelling). */
    Goal goal();

    /** Called each tick while standing at the goal. */
    default void atGoal() {}

    boolean isDone();

    default void onStop() {}
}
