package com.natan.vortex.module.pathing;

import com.natan.vortex.nav.Process;
import com.natan.vortex.nav.process.MineProcess;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.ModeSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.BlockSets;

public class AutoMine extends ProcessModule {
    private final ModeSetting target = add(new ModeSetting("Target", "What to mine", "Diamonds", BlockSets.MINE_PRESETS));
    private final NumberSetting radius = add(new NumberSetting("Scan Radius", "How far to look for targets (blocks)", 24, 8, 48, 4));
    private final NumberSetting limit = add(new NumberSetting("Stop After", "Stop after mining this many (0 = never)", 0, 0, 256, 1));
    private final BoolSetting deposit = add(new BoolSetting("Unload At Home", "When full, walk to the 'home' waypoint, fill a chest there and come back", true));

    public AutoMine() { super("AutoMine", "Finds, walks to and mines ores on its own"); }

    @Override
    protected Process create() {
        return new MineProcess(BlockSets.preset(target.get()), target.get(), radius.i(), limit.i(), deposit.on());
    }
}
