package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;

/** The actual scaling happens in ClientPacketListenerMixin. */
public class Velocity extends Module {
    public final NumberSetting horizontal = add(new NumberSetting("Horizontal", "% of horizontal knockback you take", 0, 0, 100, 5));
    public final NumberSetting vertical = add(new NumberSetting("Vertical", "% of vertical knockback you take", 0, 0, 100, 5));

    public Velocity() { super("Velocity", "Reduces knockback from hits and explosions", Category.COMBAT); }

    @Override public String info() { return horizontal.i() + "% " + vertical.i() + "%"; }
}
