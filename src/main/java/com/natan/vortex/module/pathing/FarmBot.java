package com.natan.vortex.module.pathing;

import com.natan.vortex.nav.Process;
import com.natan.vortex.nav.process.FarmProcess;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;

public class FarmBot extends ProcessModule {
    private final NumberSetting radius = add(new NumberSetting("Radius", "Farm area around where you start it", 12, 4, 32, 1));
    private final BoolSetting replant = add(new BoolSetting("Replant", "Plant seeds back after harvesting", true));

    public FarmBot() { super("FarmBot", "Walks your farm, harvests ripe crops and replants"); }

    @Override protected Process create() { return new FarmProcess(radius.i(), replant.on()); }
}
