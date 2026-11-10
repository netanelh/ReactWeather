package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.*;
import com.natan.vortex.util.Move;

public class FastClimb extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", "Climb speed on ladders/vines (blocks/tick)", 0.3, 0.12, 1, 0.02));

    public FastClimb() { super("FastClimb", "Climb ladders and vines much faster", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame() || !mc.player.onClimbable()) return;
        var v = mc.player.getDeltaMovement();
        if (mc.player.horizontalCollision || mc.options.keyJump.isDown()) mc.player.setDeltaMovement(v.x, speed.d(), v.z);
    }
}
