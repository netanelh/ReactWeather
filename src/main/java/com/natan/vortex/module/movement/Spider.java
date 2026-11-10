package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;

public class Spider extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", "Climb speed (blocks/tick)", 0.2, 0.05, 1, 0.05));

    public Spider() { super("Spider", "Climb walls like a spider", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame() || !mc.player.horizontalCollision) return;
        var v = mc.player.getDeltaMovement();
        if (v.y < speed.d()) mc.player.setDeltaMovement(v.x, speed.d(), v.z);
    }
}
