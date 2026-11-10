package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import net.minecraft.world.effect.MobEffects;

/** Shulker bullets can't lift you. */
public class AntiLevitation extends Module {
    public AntiLevitation() { super("AntiLevitation", "Ignore the levitation effect", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (inGame() && mc.player.hasEffect(MobEffects.LEVITATION)) mc.player.removeEffect(MobEffects.LEVITATION);
    }
}
