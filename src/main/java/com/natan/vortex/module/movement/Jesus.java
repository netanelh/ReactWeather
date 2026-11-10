package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.core.BlockPos;

public class Jesus extends Module {
    private final BoolSetting lava = add(new BoolSetting("Lava", "Also walk on lava", true));
    private final NumberSetting speed = add(new NumberSetting("Speed", "Horizontal speed while on liquid", 0.3, 0.1, 1, 0.05));

    public Jesus() { super("Jesus", "Walk on water (and lava)", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        var p = mc.player;
        if (p.isShiftKeyDown() || p.getAbilities().flying) return;
        boolean inLiquid = p.isInWater() || (lava.on() && p.isInLava());
        if (inLiquid) {
            // pop up to the surface
            p.setDeltaMovement(p.getDeltaMovement().x, 0.11, p.getDeltaMovement().z);
            return;
        }
        // standing just above liquid: cancel sinking so you "walk" on it
        BlockPos below = BlockPos.containing(p.getX(), p.getY() - 0.05, p.getZ());
        var fluid = mc.level.getFluidState(below);
        boolean liquidBelow = !fluid.isEmpty() && (lava.on() || !fluid.is(net.minecraft.tags.FluidTags.LAVA));
        if (liquidBelow && p.getDeltaMovement().y < 0 && p.getY() - below.getY() < 1.2) {
            p.setDeltaMovement(p.getDeltaMovement().x, 0, p.getDeltaMovement().z);
            p.setOnGround(true);
            if (com.natan.vortex.util.Move.isMoving()) com.natan.vortex.util.Move.setHorizontal(speed.d());
        }
    }
}
