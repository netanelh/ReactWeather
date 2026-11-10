package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.ModeSetting;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class ElytraFly extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Control = fly like creative, Boost = vanilla glide + speed", "Control", "Control", "Boost"));
    private final NumberSetting speed = add(new NumberSetting("Speed", "Horizontal speed (blocks/tick)", 1.8, 0.5, 5, 0.1));
    private final NumberSetting vSpeed = add(new NumberSetting("Vertical", "Up/down speed", 1.0, 0.1, 3, 0.1).visibleWhen(() -> mode.is("Control")));
    private final BoolSetting autoStart = add(new BoolSetting("Auto Start", "Open the elytra automatically when you fall", true));
    private final BoolSetting hover = add(new BoolSetting("Hover", "Hold position when no keys are pressed", true).visibleWhen(() -> mode.is("Control")));

    public ElytraFly() { super("ElytraFly", "Full control of elytra flight, no rockets needed", Category.MOVEMENT); }

    @Override public String info() { return mode.get(); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        var p = mc.player;
        boolean wearing = p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA);
        if (!wearing) return;
        if (!p.isFallFlying()) {
            if (autoStart.on() && !p.onGround() && p.getDeltaMovement().y < -0.1 && !p.isInWater())
                mc.getConnection().send(new ServerboundPlayerCommandPacket(p, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
            return;
        }
        if (mode.is("Boost")) {
            if (mc.options.keyUp.isDown()) {
                Vec3 look = p.getLookAngle();
                Vec3 v = p.getDeltaMovement();
                double cur = v.horizontalDistance();
                if (cur < speed.d()) p.setDeltaMovement(v.add(look.x * 0.08, 0, look.z * 0.08));
            }
            return;
        }
        double y = 0;
        if (mc.options.keyJump.isDown()) y += vSpeed.d();
        if (mc.options.keyShift.isDown()) y -= vSpeed.d();
        double[] h = com.natan.vortex.util.Move.forSpeed(speed.d());
        if (h[0] == 0 && h[1] == 0 && y == 0 && !hover.on()) return;
        p.setDeltaMovement(h[0], y == 0 && hover.on() ? 0 : y, h[1]);
    }
}
