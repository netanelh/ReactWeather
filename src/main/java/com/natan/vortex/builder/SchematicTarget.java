package com.natan.vortex.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** A loaded schematic anchored at a world origin (its 0,0,0 corner). */
public final class SchematicTarget implements BuildTarget {
    private final Schematic sch;
    private final BlockPos origin;
    private final List<BlockPos> cells = new ArrayList<>();

    public SchematicTarget(Schematic sch, BlockPos origin) {
        this.sch = sch; this.origin = origin;
        for (int y = 0; y < sch.height; y++) for (int z = 0; z < sch.length; z++) for (int x = 0; x < sch.width; x++)
            if (sch.at(x, y, z) != null) cells.add(origin.offset(x, y, z));
    }

    @Override public List<BlockPos> cells() { return cells; }
    @Override public BlockState desired(BlockPos w) {
        return sch.at(w.getX() - origin.getX(), w.getY() - origin.getY(), w.getZ() - origin.getZ());
    }
    @Override public String label() { return sch.name; }
}
