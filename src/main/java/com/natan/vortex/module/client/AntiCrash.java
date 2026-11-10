package com.natan.vortex.module.client;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;

/** Drops server particle packets that would freeze your game. */
public class AntiCrash extends Module {
    private final NumberSetting maxParticles = add(new NumberSetting("Max Particles", "Ignore particle packets bigger than this", 1000, 50, 10000, 50));
    private int blocked;

    public AntiCrash() { super("AntiCrash", "Blocks lag/crash particle packets", Category.CLIENT); }
    @Override public String info() { return blocked > 0 ? Integer.toString(blocked) : null; }

    public boolean blockParticles(int count) {
        if (!isEnabled() || count <= maxParticles.i()) return false;
        blocked++;
        return true;
    }
}
