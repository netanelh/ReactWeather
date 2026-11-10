package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Move;

public class LongJump extends Module {
    private final NumberSetting boost = add(new NumberSetting("Boost", "Horizontal speed on takeoff (blocks/tick)", 1.0, 0.3, 3, 0.05));
    private final BoolSetting autoDisable = add(new BoolSetting("Auto Off", "Turn off after one jump", false));
    private boolean wasOnGround, jumped;

    public LongJump() { super("LongJump", "Jump much further", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        var p = mc.player;
        if (wasOnGround && !p.onGround() && p.getDeltaMovement().y > 0.3 && Move.isMoving()) {
            Move.setHorizontal(boost.d());
            jumped = true;
        }
        if (jumped && p.onGround()) { jumped = false; if (autoDisable.on()) toggle(); }
        wasOnGround = p.onGround();
    }
}
