package com.natan.vortex.module.world;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.ModeSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Rotations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Nuker extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "All = everything, Flatten = only at/above your feet, Ores = only ores, Smash = instant-break only", "All", "All", "Flatten", "Ores", "Smash"));
    private final NumberSetting range = add(new NumberSetting("Range", "Radius in blocks", 4, 1, 6, 0.5));
    private final NumberSetting perTick = add(new NumberSetting("Per Tick", "Blocks per tick in creative / for instant-breaks", 6, 1, 40, 1));
    private final BoolSetting rotate = add(new BoolSetting("Rotate", "Look at blocks (server side) while breaking", true));
    private final BoolSetting swing = add(new BoolSetting("Swing", "Swing your arm", true));

    private BlockPos current;

    public Nuker() { super("Nuker", "Breaks blocks around you", Category.WORLD); }

    @Override public String info() { return mode.get(); }

    @Override public void onDisable() { if (inGame() && current != null) mc.gameMode.stopDestroyBlock(); current = null; }

    @Override
    public void onTick() {
        if (!inGame()) return;
        List<BlockPos> targets = collect();
        if (targets.isEmpty()) { current = null; return; }

        boolean creative = mc.player.getAbilities().instabuild;
        if (creative || mode.is("Smash")) {
            int n = 0;
            for (BlockPos pos : targets) {
                if (n++ >= perTick.i()) break;
                breakOnce(pos);
            }
            return;
        }
        // survival: mine one block at a time, keep going on the same one until it's gone
        if (current == null || !targets.contains(current)) current = targets.get(0);
        if (rotate.on()) { float[] a = Rotations.anglesTo(Vec3.atCenterOf(current)); Rotations.sendSilent(a[0], a[1]); }
        if (mc.gameMode.isDestroying()) mc.gameMode.continueDestroyBlock(current, face(current));
        else mc.gameMode.startDestroyBlock(current, face(current));
        if (swing.on()) mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private void breakOnce(BlockPos pos) {
        if (rotate.on()) { float[] a = Rotations.anglesTo(Vec3.atCenterOf(pos)); Rotations.sendSilent(a[0], a[1]); }
        mc.gameMode.startDestroyBlock(pos, face(pos));
        if (swing.on()) mc.player.swing(InteractionHand.MAIN_HAND);
    }

    private Direction face(BlockPos pos) {
        Vec3 eye = mc.player.getEyePosition();
        Vec3 d = eye.subtract(Vec3.atCenterOf(pos));
        double ax = Math.abs(d.x), ay = Math.abs(d.y), az = Math.abs(d.z);
        if (ay >= ax && ay >= az) return d.y > 0 ? Direction.UP : Direction.DOWN;
        if (ax >= az) return d.x > 0 ? Direction.EAST : Direction.WEST;
        return d.z > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private List<BlockPos> collect() {
        List<BlockPos> out = new ArrayList<>();
        Vec3 eye = mc.player.getEyePosition();
        int r = (int) Math.ceil(range.d());
        BlockPos center = mc.player.blockPosition();
        int feetY = center.getY();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (Vec3.atCenterOf(pos).distanceTo(eye) > range.d()) continue;
            BlockState st = mc.level.getBlockState(pos);
            if (st.isAir() || !st.getFluidState().isEmpty() && st.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) continue;
            float hardness = st.getDestroySpeed(mc.level, pos);
            if (hardness < 0) continue; // bedrock, barriers
            switch (mode.get()) {
                case "Flatten" -> { if (pos.getY() < feetY) continue; }
                case "Ores" -> { if (!com.natan.vortex.util.Items2.isOre(st.getBlock())) continue; }
                case "Smash" -> { if (hardness != 0) continue; }
                default -> {}
            }
            out.add(pos.immutable());
        }
        out.sort(Comparator.comparingDouble(p -> Vec3.atCenterOf(p).distanceToSqr(eye)));
        return out;
    }
}
