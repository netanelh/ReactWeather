package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Enemies;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Items2;
import com.natan.vortex.util.Place;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Nether/End bed bombing: places a bed by the enemy and right-clicks it to explode. */
public class BedAura extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Enemy range", 6, 3, 8, 0.5));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between steps", 3, 0, 10, 1));
    private int timer;

    public BedAura() { super("BedAura", "Explodes beds on enemies in the Nether / End", Category.COMBAT); }

    @Override
    public void onTick() {
        if (!inGame() || mc.level.dimension().equals(Level.OVERWORLD) || timer-- > 0) return;
        Player t = Enemies.nearest(range.d());
        if (t == null) return;
        BlockPos head = t.blockPosition().above();
        BlockPos[] spots = {head.above(), head.north(), head.south(), head.east(), head.west()};
        for (BlockPos p : spots) {
            if (BuiltInRegistries.BLOCK.getKey(mc.level.getBlockState(p).getBlock()).getPath().endsWith("_bed")) {
                if (mc.player.distanceToSqr(Vec3.atCenterOf(p)) < t.distanceToSqr(Vec3.atCenterOf(p))) return;
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(p), Direction.UP, p, false));
                timer = delay.i();
                return;
            }
        }
        int bed = Inv.findHotbar(s -> Items2.id(s).endsWith("_bed"));
        if (bed == -1) return;
        for (BlockPos p : spots) {
            if (mc.player.distanceToSqr(Vec3.atCenterOf(p)) < t.distanceToSqr(Vec3.atCenterOf(p))) continue;
            if (Place.canPlaceAt(p) && Place.at(p, bed, true)) { timer = delay.i(); return; }
        }
    }
}
