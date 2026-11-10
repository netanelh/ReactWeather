package com.natan.vortex.module.pathing;

import com.natan.vortex.nav.Process;
import com.natan.vortex.nav.process.HuntProcess;
import com.natan.vortex.setting.ModeSetting;
import com.natan.vortex.setting.NumberSetting;

public class Hunt extends ProcessModule {
    private final ModeSetting target = add(new ModeSetting("Target", "What to hunt", "Hostiles", "Hostiles", "Animals", "zombie", "skeleton", "creeper", "spider", "enderman", "cow", "pig", "sheep", "chicken"));
    private final NumberSetting limit = add(new NumberSetting("Stop After", "Stop after this many kills (0 = never)", 0, 0, 200, 1));

    public Hunt() { super("Hunt", "Hunts down mobs on its own (.hunt <mob>)"); }

    @Override protected Process create() { return new HuntProcess(HuntProcess.byName(target.get()), target.get(), limit.i()); }
}
