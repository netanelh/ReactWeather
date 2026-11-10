package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.world.entity.Entity;

public class BoatFly extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", "Horizontal speed", 1.2, 0.2, 5, 0.1));
    private final NumberSetting vSpeed = add(new NumberSetting("Vertical", "Up speed (jump) / down (sprint key)", 0.5, 0.1, 2, 0.1));

    public BoatFly() { super("BoatFly", "Fly with boats, horses and other vehicles", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame() || !mc.player.isPassenger()) return;
        Entity v = mc.player.getVehicle();
        if (v == null) return;
        v.setYRot(mc.player.getYRot());
        double[] h = com.natan.vortex.util.Move.forSpeed(speed.d());
        double y = mc.options.keyJump.isDown() ? vSpeed.d() : mc.options.keySprint.isDown() ? -vSpeed.d() : 0;
        v.setDeltaMovement(h[0], y, h[1]);
    }
}
