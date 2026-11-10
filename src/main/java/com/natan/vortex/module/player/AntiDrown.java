package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;

public class AntiDrown extends Module {
    private final NumberSetting air = add(new NumberSetting("Air %", "Swim up when air drops below this", 40, 5, 90, 5));

    public AntiDrown() { super("AntiDrown", "Swims you up to the surface before you drown", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame() || !mc.player.isUnderWater()) return;
        if (mc.player.getAirSupply() * 100 / Math.max(1, mc.player.getMaxAirSupply()) < air.d()) {
            var v = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(v.x, Math.max(v.y, 0.3), v.z);
        }
    }
}
