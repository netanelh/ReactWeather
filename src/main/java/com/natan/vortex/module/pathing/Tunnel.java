package com.natan.vortex.module.pathing;

import com.natan.vortex.nav.Process;
import com.natan.vortex.nav.process.TunnelProcess;
import com.natan.vortex.setting.ModeSetting;
import com.natan.vortex.setting.NumberSetting;

public class Tunnel extends ProcessModule {
    private final ModeSetting size = add(new ModeSetting("Size", "Tunnel width x height", "1x2", "1x2", "2x2", "3x3"));
    private final NumberSetting length = add(new NumberSetting("Length", "Blocks to dig", 64, 8, 1024, 8));

    public Tunnel() { super("Tunnel", "Digs a straight tunnel where you face, stops at lava"); }

    @Override
    protected Process create() {
        int w = size.is("1x2") ? 1 : size.is("2x2") ? 2 : 3, h = size.is("3x3") ? 3 : 2;
        return new TunnelProcess(length.i(), w, h);
    }
}
