package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.*;
import com.natan.vortex.util.Move;

public class AutoJump extends Module {
    private final BoolSetting onlyMoving = add(new BoolSetting("Only Moving", "Jump only while walking", true));

    public AutoJump() { super("AutoJump", "Keeps jumping (bunny hop)", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame() || !mc.player.onGround() || mc.player.isInWater()) return;
        if (onlyMoving.on() && !Move.isMoving()) return;
        mc.player.jumpFromGround();
    }
}
