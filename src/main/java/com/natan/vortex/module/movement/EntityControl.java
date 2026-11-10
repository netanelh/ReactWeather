package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.world.entity.Entity;

/** Better control of anything you ride: full-power horse jumps, mount flight, steer with the camera. */
public class EntityControl extends Module {
    public final BoolSetting maxJump = add(new BoolSetting("Max Jump", "Horse jumps are always full power (no charging)", true));
    private final BoolSetting steer = add(new BoolSetting("Camera Steer", "The mount instantly faces where you look", true));
    private final BoolSetting fly = add(new BoolSetting("Fly", "Space = up, Sprint key = down, hovers otherwise", false));
    private final NumberSetting flySpeed = add(new NumberSetting("Fly Speed", "Vertical speed while flying", 0.5, 0.1, 2, 0.1).visibleWhen(fly::on));
    private final BoolSetting noFallMount = add(new BoolSetting("No Mount Fall", "The mount takes no fall damage", true));

    public EntityControl() { super("EntityControl", "Max horse jumps, mount flying, camera steering", Category.MOVEMENT); }

    public boolean maxJump() { return isEnabled() && maxJump.on(); }

    @Override
    public void onTick() {
        if (!inGame() || !mc.player.isPassenger()) return;
        Entity v = mc.player.getVehicle();
        if (v == null) return;
        if (steer.on()) { v.setYRot(mc.player.getYRot()); v.yRotO = v.getYRot(); }
        if (fly.on()) {
            double dy = mc.options.keyJump.isDown() ? flySpeed.d() : mc.options.keySprint.isDown() ? -flySpeed.d() : 0;
            var m = v.getDeltaMovement();
            v.setDeltaMovement(m.x, dy, m.z);
        }
        if (noFallMount.on()) v.resetFallDistance();
    }
}
