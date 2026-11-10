package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.*;
import com.natan.vortex.util.Move;

public class AquaSpeed extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", "Swim speed (blocks/tick)", 0.5, 0.1, 2, 0.05));
    private final BoolSetting lava = add(new BoolSetting("Lava", "Also in lava", true));

    public AquaSpeed() { super("AquaSpeed", "Swim fast in water (and lava)", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        var p = mc.player;
        if (!(p.isInWater() || lava.on() && p.isInLava()) || !Move.isMoving()) return;
        double[] h = Move.forSpeed(speed.d());
        double y = mc.options.keyJump.isDown() ? speed.d() * 0.6 : mc.options.keyShift.isDown() ? -speed.d() * 0.6 : p.getDeltaMovement().y;
        p.setDeltaMovement(h[0], y, h[1]);
    }
}
