package com.natan.vortex.module.player;

import com.natan.vortex.mixin.MinecraftAccessor;
import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;

public class FastPlace extends Module {
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between right-clicks (vanilla is 4)", 0, 0, 3, 1));

    public FastPlace() { super("FastPlace", "Place blocks and use items faster", Category.PLAYER); }

    @Override
    public void onTick() {
        MinecraftAccessor acc = (MinecraftAccessor) mc;
        if (acc.vortex$getRightClickDelay() > delay.i()) acc.vortex$setRightClickDelay(delay.i());
    }
}
