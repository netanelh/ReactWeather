package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;

public class HighJump extends Module {
    private final NumberSetting multiplier = add(new NumberSetting("Multiplier", "Jump height multiplier", 2, 1, 6, 0.1));
    private boolean wasOnGround;

    public HighJump() { super("HighJump", "Jump higher", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        var p = mc.player;
        var v = p.getDeltaMovement();
        // first airborne tick after a jump: scale the jump impulse
        if (wasOnGround && !p.onGround() && v.y > 0.3 && mc.options.keyJump.isDown())
            p.setDeltaMovement(v.x, v.y * multiplier.d(), v.z);
        wasOnGround = p.onGround();
    }
}
