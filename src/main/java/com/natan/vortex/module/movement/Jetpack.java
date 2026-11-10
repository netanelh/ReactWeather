package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;

public class Jetpack extends Module {
    private final NumberSetting power = add(new NumberSetting("Power", "Upward speed while holding jump", 0.42, 0.1, 1.5, 0.02));

    public Jetpack() { super("Jetpack", "Hold jump to fly upward like a jetpack", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame() || !mc.options.keyJump.isDown() || mc.gui.screen() != null) return;
        var v = mc.player.getDeltaMovement();
        mc.player.setDeltaMovement(v.x, power.d(), v.z);
        mc.player.fallDistance = 0;
    }
}
