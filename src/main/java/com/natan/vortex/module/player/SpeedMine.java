package com.natan.vortex.module.player;

import com.natan.vortex.mixin.MultiPlayerGameModeAccessor;
import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;

public class SpeedMine extends Module {
    private final NumberSetting boost = add(new NumberSetting("Boost", "Extra mining progress per tick (0.1 = ~30% faster)", 0.15, 0.02, 1, 0.01));

    public SpeedMine() { super("SpeedMine", "Mine blocks faster", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame() || !mc.gameMode.isDestroying()) return;
        MultiPlayerGameModeAccessor acc = (MultiPlayerGameModeAccessor) mc.gameMode;
        float p = acc.vortex$getDestroyProgress();
        if (p > 0 && p < 1) acc.vortex$setDestroyProgress(Math.min(1f, p + boost.f()));
    }
}
