package com.natan.vortex.module.player;

import com.natan.vortex.mixin.MinecraftAccessor;
import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** Removes the delay between right-clicks (eat, shoot, use) so items work at full speed. */
public class FastUse extends Module {
    public FastUse() { super("FastUse", "No cooldown between right-clicks", Category.PLAYER); }

    @Override
    public void onTick() {
        if (inGame()) ((MinecraftAccessor) (Object) mc).vortex$setRightClickDelay(0);
    }
}
