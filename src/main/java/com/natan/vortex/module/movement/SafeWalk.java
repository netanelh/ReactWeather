package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** Hooked in PlayerMixin: makes the game treat you as sneaking at edges. */
public class SafeWalk extends Module {
    public SafeWalk() { super("SafeWalk", "Stops you from walking off edges", Category.MOVEMENT); }
}
