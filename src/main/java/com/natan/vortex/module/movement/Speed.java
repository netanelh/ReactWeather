package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.*;
import com.natan.vortex.util.Move;

public class Speed extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Strafe = bhop, Ground = no jumping, Vanilla = constant", "Strafe", "Strafe", "Ground", "Vanilla"));
    private final NumberSetting speed = add(new NumberSetting("Speed", "Target speed in blocks/tick (sprint is ~0.28)", 0.45, 0.1, 3, 0.01));
    private final BoolSetting autoJump = add(new BoolSetting("Auto Jump", "Strafe: jump automatically while moving", true).visibleWhen(() -> mode.is("Strafe")));
    private final BoolSetting inLiquid = add(new BoolSetting("In Liquids", "Also apply while swimming", false));

    public Speed() { super("Speed", "Move faster on the ground", Category.MOVEMENT); }

    @Override public String info() { return mode.get(); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        var p = mc.player;
        if (p.getAbilities().flying || p.isFallFlying() || p.isPassenger()) return;
        if (!inLiquid.on() && (p.isInWater() || p.isInLava())) return;
        if (!Move.isMoving()) {
            if (!mode.is("Vanilla")) return;
            p.setDeltaMovement(0, p.getDeltaMovement().y, 0);
            return;
        }
        switch (mode.get()) {
            case "Strafe" -> {
                if (p.onGround() && autoJump.on()) p.jumpFromGround();
                // keep full control in the air: redirect momentum to where we press
                Move.setHorizontal(Math.max(speed.d() * (p.onGround() ? 1 : 0.95), Move.horizontalSpeed()));
            }
            case "Ground" -> { if (p.onGround()) Move.setHorizontal(speed.d()); }
            default -> Move.setHorizontal(speed.d());
        }
    }
}
