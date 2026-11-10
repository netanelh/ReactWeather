package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;

public class Parkour extends Module {
    public Parkour() { super("Parkour", "Jumps automatically at the very edge of a block", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        var p = mc.player;
        if (!p.onGround() || p.isShiftKeyDown() || mc.options.keyJump.isDown() || !com.natan.vortex.util.Move.isMoving()) return;
        var box = p.getBoundingBox().move(0, -0.5, 0).inflate(-0.001, 0, -0.001);
        if (mc.level.noCollision(p, box)) p.jumpFromGround();
    }
}
