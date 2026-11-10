package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** Removes the 5-tick pause between breaking blocks (MultiPlayerGameModeMixin). */
public class FastBreak extends Module {
    public FastBreak() { super("FastBreak", "No delay between breaking blocks", Category.PLAYER); }
}
