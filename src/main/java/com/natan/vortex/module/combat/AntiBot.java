package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

/** Read by Targets.valid: players missing from the tab list are treated as bots/NPCs. */
public class AntiBot extends Module {
    public AntiBot() { super("AntiBot", "Combat modules ignore fake players / NPCs", Category.COMBAT); }
}
