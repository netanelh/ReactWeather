package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.*;
import com.natan.vortex.util.Move;

public class EntitySpeed extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", "Speed of horses, pigs, striders (blocks/tick)", 1.0, 0.2, 4, 0.1));

    public EntitySpeed() { super("EntitySpeed", "Ride horses and other mounts much faster", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame() || !mc.player.isPassenger() || mc.player.getVehicle() == null || !Move.isMoving()) return;
        var v = mc.player.getVehicle();
        double[] h = Move.forSpeed(speed.d());
        v.setDeltaMovement(h[0], v.getDeltaMovement().y, h[1]);
    }
}
