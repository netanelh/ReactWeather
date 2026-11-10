package com.natan.vortex.module.pathing;

import com.natan.vortex.nav.Process;
import com.natan.vortex.nav.process.ExploreProcess;
import com.natan.vortex.setting.NumberSetting;

public class Explore extends ProcessModule {
    private final NumberSetting step = add(new NumberSetting("Step", "Distance between spiral points (blocks)", 64, 16, 256, 16));

    public Explore() { super("Explore", "Walks an outward spiral to load new terrain"); }

    @Override protected Process create() { return new ExploreProcess(step.i()); }
}
