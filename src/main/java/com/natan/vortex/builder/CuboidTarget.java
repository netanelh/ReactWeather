package com.natan.vortex.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Fill (or, with air, clear) a cuboid region with one block state. */
public final class CuboidTarget implements BuildTarget {
    private final BlockState block; // null = clear to air
    private final List<BlockPos> cells = new ArrayList<>();
    private final String label;

    public CuboidTarget(BlockPos a, BlockPos b, BlockState block, String label) {
        this.block = block; this.label = label;
        int x1 = Math.min(a.getX(), b.getX()), x2 = Math.max(a.getX(), b.getX());
        int y1 = Math.min(a.getY(), b.getY()), y2 = Math.max(a.getY(), b.getY());
        int z1 = Math.min(a.getZ(), b.getZ()), z2 = Math.max(a.getZ(), b.getZ());
        for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) for (int x = x1; x <= x2; x++)
            cells.add(new BlockPos(x, y, z));
    }

    @Override public List<BlockPos> cells() { return cells; }
    @Override public BlockState desired(BlockPos w) { return block; }
    @Override public String label() { return label; }
}
