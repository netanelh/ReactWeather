package com.natan.vortex.nav;

import net.minecraft.core.BlockPos;

import java.util.List;

/** One move of a path: walk/jump/fall/pillar/dig from the previous position to pos. */
public record Step(BlockPos pos, Move move, List<BlockPos> toBreak, BlockPos toPlace) {
    public enum Move { START, WALK, DIAGONAL, ASCEND, DESCEND, PILLAR, DIG_DOWN, SWIM_UP, PARKOUR }
}
