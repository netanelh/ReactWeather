package com.natan.vortex.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Pastes the copy buffer at a world anchor (the buffer's min corner lands on the anchor). */
public final class PasteTarget implements BuildTarget {
    private final BlockPos anchor;
    private final List<BlockPos> cells = new ArrayList<>();

    public PasteTarget(BlockPos anchor) {
        this.anchor = anchor;
        for (BlockPos rel : CopyBuffer.blocks.keySet()) cells.add(anchor.offset(rel));
    }

    @Override public List<BlockPos> cells() { return cells; }
    @Override public BlockState desired(BlockPos w) {
        return CopyBuffer.blocks.get(w.subtract(anchor));
    }
    @Override public String label() { return "paste (" + CopyBuffer.blocks.size() + ")"; }
}
