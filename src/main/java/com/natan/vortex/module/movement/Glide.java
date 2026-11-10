package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;

/** Fall slowly like a feather. */
public class Glide extends Module {
    private final NumberSetting fall = add(new NumberSetting("Fall Speed", "Max fall speed (blocks/tick)", 0.05, 0.0, 0.5, 0.01));

    public Glide() { super("Glide", "Fall slowly", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame() || mc.player.onGround() || mc.player.isFallFlying() || mc.player.isInWater() || mc.player.getAbilities().flying) return;
        var v = mc.player.getDeltaMovement();
        if (v.y < -fall.d() && !mc.options.keyShift.isDown()) mc.player.setDeltaMovement(v.x, -fall.d(), v.z);
        mc.player.resetFallDistance();
    }
}
