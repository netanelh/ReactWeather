package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.*;
import com.natan.vortex.util.Move;

public class TridentBoost extends Module {
    private final NumberSetting multiplier = add(new NumberSetting("Multiplier", "Riptide launch strength", 2, 1, 6, 0.25));
    private boolean was;

    public TridentBoost() { super("TridentBoost", "Riptide launches you much further", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        boolean spin = mc.player.isAutoSpinAttack();
        if (spin && !was) {
            var v = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(v.scale(multiplier.d()));
        }
        was = spin;
    }
}
