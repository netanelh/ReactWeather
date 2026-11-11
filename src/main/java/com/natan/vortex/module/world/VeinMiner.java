package com.natan.vortex.module.world;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.nav.Actions;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/** Break one ore/log and it keeps mining every connected block of the same type in reach. */
public class VeinMiner extends Module {
    private final NumberSetting max = add(new NumberSetting("Max Blocks", "Largest vein to mine", 64, 4, 256, 4));
    private final Deque<BlockPos> queue = new ArrayDeque<>();
    private Block type;
    private BlockPos watching;

    public VeinMiner() { super("VeinMiner", "Mines the whole ore vein / tree you start breaking", Category.WORLD); }

    @Override public String info() { return queue.isEmpty() ? null : Integer.toString(queue.size()); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        if (!queue.isEmpty()) {
            BlockPos p = queue.peek();
            if (mc.level.getBlockState(p).getBlock() != type || Vec3.atCenterOf(p).distanceTo(mc.player.getEyePosition()) > 5) { queue.poll(); return; }
            if (Actions.mine(p)) queue.poll();
            return;
        }
        // watch the block you're mining; when it breaks, queue its neighbours
        if (mc.options.keyAttack.isDown() && mc.hitResult instanceof BlockHitResult bh) {
            var st = mc.level.getBlockState(bh.getBlockPos());
            String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(st.getBlock()).getPath();
            if (id.endsWith("_ore") || id.endsWith("_log") || id.endsWith("_stem") || id.equals("ancient_debris")) { watching = bh.getBlockPos(); type = st.getBlock(); }
        }
        if (watching != null && mc.level.getBlockState(watching).isAir()) {
            flood(watching);
            watching = null;
        }
    }

    private void flood(BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> todo = new ArrayDeque<>();
        todo.add(start);
        while (!todo.isEmpty() && queue.size() < max.i()) {
            BlockPos p = todo.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos n = p.offset(dx, dy, dz);
                if (!seen.add(n) || mc.level.getBlockState(n).getBlock() != type) continue;
                if (Vec3.atCenterOf(n).distanceTo(mc.player.getEyePosition()) > 5) continue;
                queue.add(n);
                todo.add(n);
            }
        }
    }

    @Override public void onDisable() { queue.clear(); watching = null; Actions.stopMining(); }
}
