package com.natan.vortex.module.world;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Crops;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Rotations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Harvests and replants crops within reach while you stand still (use FarmBot for walking). */
public class AutoFarm extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Reach in blocks", 4.5, 2, 5, 0.5));
    private final NumberSetting perTick = add(new NumberSetting("Per Tick", "Actions per tick", 2, 1, 10, 1));
    private final BoolSetting replant = add(new BoolSetting("Replant", "Plant on empty farmland", true));
    private final BoolSetting boneMeal = add(new BoolSetting("Bone Meal", "Use bone meal on growing crops", false));

    public AutoFarm() { super("AutoFarm", "Harvests and replants crops around you", Category.WORLD); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        int r = (int) Math.ceil(range.d()), actions = 0;
        Vec3 eye = mc.player.getEyePosition();
        BlockPos me = mc.player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-r, -2, -r), me.offset(r, 2, r))) {
            if (actions >= perTick.i()) return;
            if (Vec3.atCenterOf(p).distanceTo(eye) > range.d()) continue;
            var st = mc.level.getBlockState(p);
            if (Crops.isMature(st) && st.getBlock() != Blocks.MELON && st.getBlock() != Blocks.PUMPKIN) {
                mc.gameMode.startDestroyBlock(p.immutable(), Direction.UP);
                actions++;
            } else if (replant.on() && st.isAir() && (mc.level.getBlockState(p.below()).is(Blocks.FARMLAND) || mc.level.getBlockState(p.below()).is(Blocks.SOUL_SAND))) {
                boolean nether = mc.level.getBlockState(p.below()).is(Blocks.SOUL_SAND);
                int slot = Inv.findHotbar(s -> nether ? s.is(net.minecraft.world.item.Items.NETHER_WART)
                    : s.is(net.minecraft.world.item.Items.WHEAT_SEEDS) || s.is(net.minecraft.world.item.Items.CARROT)
                    || s.is(net.minecraft.world.item.Items.POTATO) || s.is(net.minecraft.world.item.Items.BEETROOT_SEEDS));
                if (slot == -1) continue;
                use(p.below().immutable(), slot);
                actions++;
            } else if (boneMeal.on() && Crops.seedFor(st.getBlock()) != null && !Crops.isMature(st)) {
                int slot = Inv.findHotbar(s -> s.is(net.minecraft.world.item.Items.BONE_MEAL));
                if (slot == -1) continue;
                use(p.immutable(), slot);
                actions++;
            }
        }
    }

    private void use(BlockPos target, int slot) {
        Inv.select(slot);
        Vec3 hit = Vec3.atCenterOf(target).add(0, 0.5, 0);
        float[] a = Rotations.anglesTo(hit);
        Rotations.sendSilent(a[0], a[1]);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, target, false));
    }
}
