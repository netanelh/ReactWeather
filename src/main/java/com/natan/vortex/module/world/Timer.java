package com.natan.vortex.module.world;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;

/** Speeds up (or slows down) the client game clock. Hook in DeltaTrackerTimerMixin. */
public class Timer extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", "Game speed multiplier", 1.5, 0.1, 5, 0.05));

    public Timer() { super("Timer", "Speed up the whole game on your side (fast mining/walking)", Category.WORLD); }

    @Override public String info() { return speed.display() + "x"; }

    public float multiplier() { return isEnabled() ? speed.f() : 1f; }
}
