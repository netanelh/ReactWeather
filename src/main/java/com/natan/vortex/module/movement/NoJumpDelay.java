package com.natan.vortex.module.movement;

import com.natan.vortex.mixin.LivingEntityAccessor;
import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

public class NoJumpDelay extends Module {
    public NoJumpDelay() { super("NoJumpDelay", "Jump again the instant you land", Category.MOVEMENT); }

    @Override public void onTick() { if (inGame()) ((LivingEntityAccessor) mc.player).vortex$setNoJumpDelay(0); }
}
