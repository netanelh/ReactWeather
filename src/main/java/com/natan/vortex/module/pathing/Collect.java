package com.natan.vortex.module.pathing;

import com.natan.vortex.nav.Process;
import com.natan.vortex.nav.process.CollectProcess;
import com.natan.vortex.setting.NumberSetting;

public class Collect extends ProcessModule {
    private final NumberSetting radius = add(new NumberSetting("Radius", "How far to look for drops", 24, 6, 64, 2));

    public Collect() { super("Collect", "Walks around picking up every dropped item (.collect <item>)"); }

    @Override protected Process create() { return new CollectProcess(null, radius.i()); }
}
