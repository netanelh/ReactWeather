package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;

/** Mobs, players and water currents can't push you. */
public class NoPush extends Module {
    public final BoolSetting entities = add(new BoolSetting("Entities", "Not pushed by mobs/players", true));
    public final BoolSetting water = add(new BoolSetting("Water", "Not pushed by flowing water", true));

    public NoPush() { super("NoPush", "Entities and water can't push you", Category.MOVEMENT); }
    public boolean on(BoolSetting s) { return isEnabled() && s.on(); }
}
