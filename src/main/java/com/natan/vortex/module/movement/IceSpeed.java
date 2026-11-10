package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.*;
import com.natan.vortex.util.Move;

public class IceSpeed extends Module {
    private final NumberSetting speed = add(new NumberSetting("Speed", "Speed on ice (blocks/tick)", 0.9, 0.3, 3, 0.05));

    public IceSpeed() { super("IceSpeed", "Go very fast on ice and packed ice", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame() || !mc.player.onGround() || !Move.isMoving()) return;
        String below = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(mc.player.blockPosition().below()).getBlock()).getPath();
        if (below.contains("ice")) Move.setHorizontal(speed.d());
    }
}
