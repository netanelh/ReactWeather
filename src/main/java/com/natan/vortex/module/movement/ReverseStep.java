package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.*;
import com.natan.vortex.util.Move;

public class ReverseStep extends Module {
    private final NumberSetting height = add(new NumberSetting("Height", "Max drop to snap down (blocks)", 3, 1, 10, 0.5));
    private final NumberSetting speed = add(new NumberSetting("Speed", "Fall speed (blocks/tick)", 1.5, 0.3, 4, 0.1));
    private boolean wasOnGround;

    public ReverseStep() { super("ReverseStep", "Snap down off edges instantly", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        var p = mc.player;
        if (wasOnGround && !p.onGround() && p.getDeltaMovement().y <= 0 && !p.isInWater() && !mc.options.keyJump.isDown()) {
            var box = p.getBoundingBox().move(0, -height.d(), 0);
            if (!mc.level.noCollision(p, box)) p.setDeltaMovement(p.getDeltaMovement().x, -speed.d(), p.getDeltaMovement().z);
        }
        wasOnGround = p.onGround();
    }
}
