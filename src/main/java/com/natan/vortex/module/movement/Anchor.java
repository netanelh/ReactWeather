package com.natan.vortex.module.movement;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Pulls you into the 1x1 safe hole below you (PvP). */
public class Anchor extends Module {
    private final NumberSetting depth = add(new NumberSetting("Depth", "How far down to look for a hole", 4, 1, 8, 1));

    public Anchor() { super("Anchor", "Snaps you into safe holes below you", Category.MOVEMENT); }

    @Override
    public void onTick() {
        if (!inGame() || mc.player.onGround()) return;
        BlockPos base = mc.player.blockPosition();
        for (int y = 1; y <= depth.i(); y++) {
            BlockPos p = base.below(y);
            if (!mc.level.getBlockState(p).isAir()) {
                if (y > 1 && hole(p.above())) snap(p.above());
                return;
            }
            if (hole(p)) { snap(p); return; }
        }
    }

    private boolean hole(BlockPos p) {
        if (!mc.level.getBlockState(p).isAir() || !mc.level.getBlockState(p.above()).isAir() || mc.level.getBlockState(p.below()).isAir()) return false;
        for (Direction d : Direction.Plane.HORIZONTAL) if (mc.level.getBlockState(p.relative(d)).canBeReplaced()) return false;
        return true;
    }

    private void snap(BlockPos p) {
        mc.player.setPos(p.getX() + 0.5, mc.player.getY(), p.getZ() + 0.5);
        mc.player.setDeltaMovement(0, Math.min(mc.player.getDeltaMovement().y, -0.5), 0);
    }
}
