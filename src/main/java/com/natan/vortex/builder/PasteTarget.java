package com.natan.vortex.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** Pastes the copy buffer at a world anchor (the buffer's min corner lands on the anchor). */
public final class PasteTarget implements BuildTarget {
    private final BlockPos anchor;
    private final List<BlockPos> cells = new ArrayList<>();
    /** Snapshot, so a new .copy during the paste doesn't change what we build. */
    private final java.util.Map<BlockPos, BlockState> buf = new java.util.HashMap<>(CopyBuffer.blocks);

    public PasteTarget(BlockPos anchor) {
        this.anchor = anchor;
        for (BlockPos rel : buf.keySet()) cells.add(anchor.offset(rel));
    }

    @Override public List<BlockPos> cells() { return cells; }
    @Override public BlockState desired(BlockPos w) {
        return buf.get(w.subtract(anchor));
    }
    @Override public String label() { return "paste (" + buf.size() + ")"; }
}
