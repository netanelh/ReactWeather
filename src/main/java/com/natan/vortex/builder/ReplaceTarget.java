package com.natan.vortex.builder;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Replace one block type with another inside the selection (leaves everything else alone). */
public final class ReplaceTarget implements BuildTarget {
    private final Block from;
    private final BlockState to; // null = remove matches
    private final List<BlockPos> cells = new ArrayList<>();
    private final String label;

    public ReplaceTarget(BlockPos a, BlockPos b, Block from, BlockState to, String label) {
        this.from = from; this.to = to; this.label = label;
        int x1 = Math.min(a.getX(), b.getX()), x2 = Math.max(a.getX(), b.getX());
        int y1 = Math.min(a.getY(), b.getY()), y2 = Math.max(a.getY(), b.getY());
        int z1 = Math.min(a.getZ(), b.getZ()), z2 = Math.max(a.getZ(), b.getZ());
        for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) for (int x = x1; x <= x2; x++)
            cells.add(new BlockPos(x, y, z));
    }

    @Override public List<BlockPos> cells() { return cells; }
    @Override public BlockState desired(BlockPos w) {
        // only cells that currently hold `from` have a desired state; others return their own block (no-op)
        BlockState have = Minecraft.getInstance().level.getBlockState(w);
        if (have.getBlock() == from) return to;
        return have; // BuildProcess sees have==have -> not wrong -> skipped
    }
    @Override public String label() { return label; }
}
