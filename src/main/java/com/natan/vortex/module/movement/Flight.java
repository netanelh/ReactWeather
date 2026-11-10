package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.*;
import com.natan.vortex.util.Move;

public class Flight extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Velocity = direct control, Vanilla = creative-style flying", "Velocity", "Velocity", "Vanilla", "Glide"));
    private final NumberSetting speed = add(new NumberSetting("Speed", "Horizontal speed (blocks/tick)", 1.0, 0.1, 5, 0.1));
    private final NumberSetting vSpeed = add(new NumberSetting("Vertical", "Up/down speed (blocks/tick)", 0.6, 0.1, 3, 0.1).visibleWhen(() -> !mode.is("Glide")));
    private final NumberSetting glideFall = add(new NumberSetting("Fall Speed", "Glide: downward speed", 0.03, 0, 0.3, 0.01).visibleWhen(() -> mode.is("Glide")));
    private final BoolSetting antiKick = add(new BoolSetting("Anti Kick", "Dips slightly every 2s so vanilla servers don't kick for flying", true));

    private int ticks;

    public Flight() { super("Flight", "Lets you fly in survival", Category.MOVEMENT); }

    @Override public String info() { return mode.get(); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        var p = mc.player;
        ticks++;
        switch (mode.get()) {
            case "Vanilla" -> {
                p.getAbilities().mayfly = true;
                p.getAbilities().flying = true;
                p.getAbilities().setFlyingSpeed(speed.f() / 10f);
            }
            case "Glide" -> {
                if (!p.onGround() && p.getDeltaMovement().y < 0)
                    p.setDeltaMovement(p.getDeltaMovement().x, -glideFall.d(), p.getDeltaMovement().z);
                return;
            }
            default -> {
                p.getAbilities().flying = false;
                double y = 0;
                if (mc.options.keyJump.isDown()) y += vSpeed.d();
                if (mc.options.keyShift.isDown()) y -= vSpeed.d();
                double[] h = Move.forSpeed(speed.d());
                p.setDeltaMovement(h[0], y, h[1]);
            }
        }
        if (antiKick.on() && ticks % 40 == 0 && !p.onGround()) {
            p.setDeltaMovement(p.getDeltaMovement().x, -0.04, p.getDeltaMovement().z);
        }
    }

    @Override
    public void onDisable() {
        if (!inGame()) return;
        var a = mc.player.getAbilities();
        boolean canFly = mc.player.isCreative() || mc.player.isSpectator();
        a.mayfly = canFly;
        if (!canFly) a.flying = false;
        a.setFlyingSpeed(0.05f);
    }
}
