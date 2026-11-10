package com.natan.vortex.nav.process;

import com.natan.vortex.nav.Goal;
import com.natan.vortex.nav.Process;
import com.natan.vortex.nav.World;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;

import java.util.Set;

/** Goes to the nearest block of a type in loaded chunks (".find crafting_table", ".find nether_portal"). */
public class FindProcess implements Process {
    private final Set<Block> blocks;
    private final String label;
    private BlockPos target;
    private boolean done, searched;

    public FindProcess(Set<Block> blocks, String label) { this.blocks = blocks; this.label = label; }

    @Override public String name() { return "Find"; }
    @Override public String status() { return label + (target != null ? " at " + target.toShortString() : searched ? " (not found)" : ""); }
    @Override public boolean isDone() { return done; }

    @Override
    public void tick() {
        if (searched) return;
        searched = true;
        Minecraft mc = Minecraft.getInstance();
        BlockPos me = mc.player.blockPosition();
        int r = 64, best = Integer.MAX_VALUE;
        int minY = mc.level.getMinY(), maxY = minY + mc.level.getHeight();
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
            if (!World.loaded(me.getX() + x, me.getZ() + z)) continue;
            for (int y = Math.max(minY, me.getY() - 40); y < Math.min(maxY, me.getY() + 40); y++) {
                m.set(me.getX() + x, y, me.getZ() + z);
                if (!blocks.contains(mc.level.getBlockState(m).getBlock())) continue;
                int d = (int) m.distSqr(me);
                if (d < best) { best = d; target = m.immutable(); }
            }
        }
        if (target == null) {
            // not in loaded chunks: ask the world memory (everything seen before, any distance)
            var remembered = com.natan.vortex.memory.WorldMemory.INSTANCE.nearest(blocks, me, 1);
            if (!remembered.isEmpty()) {
                target = remembered.get(0);
                com.natan.vortex.util.Chat.info("Remembered a " + label + " " + Math.round(Math.sqrt(target.distSqr(me))) + " blocks away at " + target.toShortString() + ", heading there.");
            } else {
                for (var b : blocks) com.natan.vortex.memory.WorldMemory.INSTANCE.track(b);
                com.natan.vortex.util.Chat.error("No " + label + " in loaded chunks or memory yet. I'll remember it from now on (try .explore, or .locate for structures).");
                done = true;
            }
        }
    }

    @Override
    public Goal goal() {
        if (target == null) return null;
        // far away: walk toward the column; close: stand next to the block
        var me = Minecraft.getInstance().player.blockPosition();
        if (target.distSqr(me) > 96 * 96) return new Goal.XZ(target.getX(), target.getZ());
        return new Goal.Reach(target);
    }


    @Override public void atGoal() { done = true; }
}
