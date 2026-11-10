package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.*;
import com.natan.vortex.util.Move;

public class Sprint extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Legit = forward only, Omni = every direction", "Legit", "Legit", "Omni"));

    public Sprint() { super("Sprint", "Sprints automatically", Category.MOVEMENT); }

    @Override public String info() { return mode.get(); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        var p = mc.player;
        boolean moving = mode.is("Omni") ? Move.isMoving() : p.zza > 0;
        boolean canSprint = !p.isShiftKeyDown() && !p.isUsingItem() && (p.getFoodData().getFoodLevel() > 6 || p.getAbilities().mayfly);
        if (moving && canSprint && !p.horizontalCollision) p.setSprinting(true);
    }
}
