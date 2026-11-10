package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.*;
import com.natan.vortex.util.Move;

/** Full air control: changes direction mid-air instantly, keeping your speed. */
public class Strafe extends Module {
    private final BoolSetting groundToo = add(new BoolSetting("On Ground", "Also apply on the ground", false));

    public Strafe() { super("Strafe", "Turn instantly in the air without losing speed", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame() || mc.player.getAbilities().flying || mc.player.isInWater()) return;
        if (mc.player.onGround() && !groundToo.on()) return;
        if (!Move.isMoving()) return;
        Move.setHorizontal(Math.max(Move.horizontalSpeed(), 0.2));
    }
}
