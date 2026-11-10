package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

public class AutoWalk extends Module {
    public AutoWalk() { super("AutoWalk", "Holds W for you", Category.MOVEMENT); }

    @Override public void onTick() { if (inGame()) mc.options.keyUp.setDown(true); }

    @Override public void onDisable() { mc.options.keyUp.setDown(false); }
}
