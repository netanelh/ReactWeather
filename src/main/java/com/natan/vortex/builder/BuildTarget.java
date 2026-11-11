package com.natan.vortex.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Something that can be built: a set of world cells each with a desired block state (null = should be air). */
public interface BuildTarget {
    List<BlockPos> cells();
    BlockState desired(BlockPos world);
    String label();
}
