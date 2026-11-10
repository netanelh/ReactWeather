package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.*;
import com.natan.vortex.util.Move;

/** Hooks live in LocalPlayerMixin, EntityMixin, WebBlockMixin and SweetBerryBushMixin. */
public class NoSlow extends Module {
    public final BoolSetting items = add(new BoolSetting("Items", "Full speed while eating, drinking, blocking, drawing bows", true));
    public final BoolSetting sneaking = add(new BoolSetting("Sneaking", "Full speed while sneaking", false));
    public final BoolSetting cobweb = add(new BoolSetting("Cobwebs", "Walk through cobwebs normally", true));
    public final BoolSetting berryBush = add(new BoolSetting("Berry Bushes", "No slowdown or damage from berry bushes", true));
    public final BoolSetting soulSand = add(new BoolSetting("Soul Sand", "Normal speed on soul sand", true));
    public final BoolSetting honey = add(new BoolSetting("Honey", "Normal speed on honey blocks", true));

    public NoSlow() { super("NoSlow", "Removes slowdowns from items, webs, soul sand and more", Category.MOVEMENT); }

    public boolean on(BoolSetting s) { return isEnabled() && s.on(); }
}
