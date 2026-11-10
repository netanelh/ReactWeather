package com.natan.vortex.module.world;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Place;
import com.natan.vortex.util.Targets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/** Fills 1x1 safe holes near enemies so they can't hide from crystals. */
public class HoleFiller extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Placement range", 4.5, 2, 5, 0.5));
    private final NumberSetting enemyRange = add(new NumberSetting("Enemy Range", "Only fill holes this close to an enemy", 4, 1, 8, 0.5));

    public HoleFiller() { super("HoleFiller", "Fills safe holes next to enemies with obsidian", Category.WORLD); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        int slot = Place.findSlot(s -> s.is(Items.OBSIDIAN) || s.is(Items.COBBLESTONE));
        if (slot == -1) return;
        int r = (int) Math.ceil(range.d());
        BlockPos me = mc.player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-r, -2, -r), me.offset(r, 2, r))) {
            if (Vec3.atCenterOf(p).distanceTo(mc.player.getEyePosition()) > range.d() || !isHole(p)) continue;
            boolean nearEnemy = false;
            for (Player e : mc.level.players()) if (Targets.valid(e, true, false, false) && e.position().distanceTo(Vec3.atBottomCenterOf(p)) < enemyRange.d()) nearEnemy = true;
            if (!nearEnemy || p.equals(me)) continue;
            int prev = Inv.selected();
            if (Place.at(p.immutable(), slot, true)) { Inv.select(prev); return; }
        }
    }

    private boolean isHole(BlockPos p) {
        if (!mc.level.getBlockState(p).isAir() || !mc.level.getBlockState(p.above()).isAir() || mc.level.getBlockState(p.below()).canBeReplaced()) return false;
        for (Direction d : Direction.Plane.HORIZONTAL) if (mc.level.getBlockState(p.relative(d)).canBeReplaced()) return false;
        return true;
    }
}
