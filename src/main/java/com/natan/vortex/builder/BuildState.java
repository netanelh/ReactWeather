package com.natan.vortex.builder;

import net.minecraft.core.BlockPos;

/** Current schematic and selection corners for the build commands. */
public final class BuildState {
    public static Schematic schematic;
    public static BlockPos sel1, sel2;
    private BuildState() {}
}
