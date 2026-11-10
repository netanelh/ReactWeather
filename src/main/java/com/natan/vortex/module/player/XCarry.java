package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** Keeps items in your 2x2 crafting grid as 4 extra slots (the server never hears you closed the inventory). */
public class XCarry extends Module {
    public XCarry() { super("XCarry", "Use the 2x2 crafting grid as extra storage", Category.PLAYER); }

    public boolean blockClose(int containerId) { return isEnabled() && containerId == 0; }
}
