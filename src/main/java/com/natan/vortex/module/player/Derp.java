package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.util.Rotations;

/** Other players see your head spinning around (your camera doesn't move). */
public class Derp extends Module {
    private final BoolSetting spin = add(new BoolSetting("Spin", "Spin smoothly instead of random jerks", true));
    private float yaw;

    public Derp() { super("Derp", "Makes your head spin for everyone else", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame() || mc.options.keyAttack.isDown() || mc.options.keyUse.isDown()) return;
        yaw = spin.on() ? yaw + 35 : (float) (Math.random() * 360);
        Rotations.sendSilent(yaw, spin.on() ? 0 : (float) (Math.random() * 180 - 90));
    }
}
