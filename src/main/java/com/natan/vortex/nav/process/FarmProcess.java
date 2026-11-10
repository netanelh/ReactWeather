package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Actions;
import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import com.natan.vortex.util.Crops;
import com.natan.vortex.util.Inv;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

/** Harvests mature crops around a fixed center, replants them and walks over the drops. */
public class FarmProcess implements Process {
    private final BlockPos center;
    private final int radius;
    private final boolean replant;
    private BlockPos current, replantAt;
    private net.minecraft.world.level.block.Block replantBlock;
    private int harvested, scanCooldown, pickupTicks;
    private final Set<BlockPos> skip = new HashSet<>();

    public FarmProcess(int radius, boolean replant) {
        this.center = Minecraft.getInstance().player.blockPosition();
        this.radius = radius; this.replant = replant;
    }

    @Override public String name() { return "Farm"; }
    @Override public String status() { return harvested + " harvested"; }
    @Override public boolean isDone() { return false; }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (replantAt != null) {
            if (!mc.level.getBlockState(replantAt).isAir()) { replantAt = null; return; }
            var seed = Crops.seedFor(replantBlock);
            int slot = seed == null ? -1 : Inv.findHotbar(s -> s.is(seed));
            if (slot == -1 || Vec3.atCenterOf(replantAt).distanceTo(mc.player.getEyePosition()) > 4.5) { replantAt = null; return; }
            Inv.select(slot);
            BlockPos soil = replantAt.below();
            Vec3 hit = Vec3.atCenterOf(soil).add(0, 0.5, 0);
            Actions.lookAt(hit, 90);
            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, Direction.UP, soil, false));
            mc.player.swing(InteractionHand.MAIN_HAND);
            replantAt = null;
            return;
        }
        if (pickupTicks > 0) { pickupTicks--; return; }
        if (current != null && !Crops.isMature(mc.level.getBlockState(current))) current = null;
        if (current == null && scanCooldown-- <= 0) {
            scanCooldown = 10;
            BlockPos me = mc.player.blockPosition();
            current = BlockPos.betweenClosedStream(center.offset(-radius, -3, -radius), center.offset(radius, 3, radius))
                .filter(p -> !skip.contains(p) && Crops.isMature(mc.level.getBlockState(p)))
                .map(BlockPos::immutable)
                .min(Comparator.comparingDouble(p -> p.distSqr(me))).orElse(null);
        }
    }

    @Override
    public Goal goal() {
        if (replantAt != null) return null;
        if (current == null) return pickupTicks > 0 ? null : new Goal.Near(center, 2);
        if (pickupTicks > 0) return new Goal.Block(current);
        return new Goal.Near(current, 2.2);
    }

    @Override
    public void atGoal() {
        Minecraft mc = Minecraft.getInstance();
        if (current == null) return;
        var state = mc.level.getBlockState(current);
        replantBlock = state.getBlock();
        if (Actions.mine(current) || mc.level.getBlockState(current).isAir()) {
            harvested++;
            if (replant) replantAt = current;
            pickupTicks = 15;
        }
    }
}
