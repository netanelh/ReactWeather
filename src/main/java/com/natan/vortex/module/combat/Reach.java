package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;

/** Values are added in PlayerMixin to the vanilla interaction ranges. */
public class Reach extends Module {
    public final NumberSetting entity = add(new NumberSetting("Entity", "Extra attack reach (blocks)", 1.0, 0, 3, 0.1));
    public final NumberSetting block = add(new NumberSetting("Block", "Extra block reach (blocks)", 1.0, 0, 3, 0.1));

    public Reach() { super("Reach", "Hit and place from further away", Category.COMBAT); }

    @Override public String info() { return "+" + entity.display(); }
}
