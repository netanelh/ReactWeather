package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;

public class AirJump extends Module {
    private final NumberSetting power = add(new NumberSetting("Power", "Upward velocity per air jump", 0.42, 0.2, 1.5, 0.02));
    private boolean wasDown;

    public AirJump() { super("AirJump", "Jump again while in the air", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        boolean down = mc.options.keyJump.isDown();
        if (down && !wasDown && !mc.player.onGround() && mc.gui.screen() == null) {
            var v = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(v.x, power.d(), v.z);
        }
        wasDown = down;
    }
}
