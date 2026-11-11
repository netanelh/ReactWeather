package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.*;
import com.natan.vortex.util.Move;

public class AntiVoid extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Bounce = jump back up, Hover = stop falling", "Bounce", "Bounce", "Hover"));

    public AntiVoid() { super("AntiVoid", "Saves you from falling into the void", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        var p = mc.player;
        // void damage starts at minY - 64; let the player fall, catch them right before that
        double trigger = mc.level.getMinY() - 58;
        if (p.getY() > trigger || p.getDeltaMovement().y >= 0) return;
        p.setDeltaMovement(p.getDeltaMovement().x, mode.is("Bounce") ? 1.2 : 0, p.getDeltaMovement().z);
        p.fallDistance = 0;
    }
}
