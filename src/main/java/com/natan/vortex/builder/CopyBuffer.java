package com.natan.vortex.builder;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;

/** A clipboard of block states copied from a selection, stored relative to its min corner. */
public final class CopyBuffer {
    public static final Map<BlockPos, BlockState> blocks = new LinkedHashMap<>();
    public static BlockPos size = BlockPos.ZERO;
    private CopyBuffer() {}

    /** Snapshot every non-air block in the box (relative to the min corner). */
    public static int copy(BlockPos a, BlockPos b) {
        Minecraft mc = Minecraft.getInstance();
        blocks.clear();
        int x1 = Math.min(a.getX(), b.getX()), x2 = Math.max(a.getX(), b.getX());
        int y1 = Math.min(a.getY(), b.getY()), y2 = Math.max(a.getY(), b.getY());
        int z1 = Math.min(a.getZ(), b.getZ()), z2 = Math.max(a.getZ(), b.getZ());
        for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) for (int x = x1; x <= x2; x++) {
            BlockState st = mc.level.getBlockState(new BlockPos(x, y, z));
            if (!st.isAir()) blocks.put(new BlockPos(x - x1, y - y1, z - z1), st);
        }
        size = new BlockPos(x2 - x1, y2 - y1, z2 - z1);
        return blocks.size();
    }
}
