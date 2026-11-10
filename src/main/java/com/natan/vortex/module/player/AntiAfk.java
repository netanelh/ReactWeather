package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.world.InteractionHand;

import java.util.concurrent.ThreadLocalRandom;

public class AntiAfk extends Module {
    private final NumberSetting interval = add(new NumberSetting("Interval", "Seconds between actions", 15, 3, 120, 1));
    private final BoolSetting jump = add(new BoolSetting("Jump", "Jump", true));
    private final BoolSetting rotate = add(new BoolSetting("Rotate", "Turn a little", true));
    private final BoolSetting swing = add(new BoolSetting("Swing", "Swing your arm", false));

    private int timer;

    public AntiAfk() { super("AntiAFK", "Keeps you from being kicked for being idle", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame() || ++timer < interval.i() * 20) return;
        timer = 0;
        var r = ThreadLocalRandom.current();
        if (jump.on() && mc.player.onGround()) mc.player.jumpFromGround();
        if (rotate.on()) mc.player.setYRot(mc.player.getYRot() + (float) r.nextDouble(-30, 30));
        if (swing.on()) mc.player.swing(InteractionHand.MAIN_HAND);
    }
}
