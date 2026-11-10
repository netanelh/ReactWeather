package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** ConnectionMixin drops "start sprinting" packets so the server never charges sprint exhaustion. */
public class AntiHunger extends Module {
    public AntiHunger() { super("AntiHunger", "Sprint without the server draining your hunger", Category.PLAYER); }
}
