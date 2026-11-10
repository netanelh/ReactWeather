package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** ConnectionMixin drops "start sprinting" packets so the server never charges sprint exhaustion. */
public class AntiHunger extends Module {
    /** Set by SuperKnockback around its own sprint packet so AntiHunger lets it through. */
    public static boolean bypass;

    public AntiHunger() { super("AntiHunger", "Sprint without the server draining your hunger", Category.PLAYER); }
}
