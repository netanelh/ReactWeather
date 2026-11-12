package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.*;
import com.natan.vortex.util.Move;

public class Sneak extends Module {
    public Sneak() { super("Sneak", "Always sneak (others see you crouching)", Category.MOVEMENT); }

    @Override public void onTick() { if (inGame() && mc.gui.screen() == null) com.natan.vortex.util.Keys.hold(mc.options.keyShift, true); }
    @Override public void onDisable() { com.natan.vortex.util.Keys.hold(mc.options.keyShift, false); }
}
