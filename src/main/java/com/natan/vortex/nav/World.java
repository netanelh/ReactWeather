package com.natan.vortex.nav;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.Set;

/**
 * Block queries used by the pathfinder. Safe to call from the pathing thread:
 * it only reads block states from already-loaded client chunks.
 */
public final class World {
    private static final Set<Block> DANGER_FLOOR = Set.of(Blocks.MAGMA_BLOCK, Blocks.CACTUS, Blocks.CAMPFIRE, Blocks.SOUL_CAMPFIRE,
        Blocks.POINTED_DRIPSTONE, Blocks.SWEET_BERRY_BUSH, Blocks.POWDER_SNOW);
    private static final Set<Block> DANGER_PASS = Set.of(Blocks.FIRE, Blocks.SOUL_FIRE, Blocks.SWEET_BERRY_BUSH, Blocks.COBWEB,
        Blocks.POWDER_SNOW, Blocks.CACTUS, Blocks.WITHER_ROSE, Blocks.END_PORTAL, Blocks.NETHER_PORTAL);
    private static final Set<Block> NEVER_BREAK = Set.of(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.BARREL,
        Blocks.SPAWNER, Blocks.TRIAL_SPAWNER, Blocks.VAULT, Blocks.BEACON, Blocks.END_PORTAL_FRAME, Blocks.SHULKER_BOX,
        Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.CRAFTING_TABLE, Blocks.ANVIL);

    private World() {}

    private static Minecraft mc() { return Minecraft.getInstance(); }

    public static BlockState state(int x, int y, int z) {
        return mc().level.getBlockState(new BlockPos(x, y, z));
    }

    public static boolean loaded(int x, int z) {
        return mc().level.getChunkSource().hasChunk(x >> 4, z >> 4);
    }

    public static boolean isLava(BlockState s) { return s.getFluidState().is(FluidTags.LAVA); }
    public static boolean isWater(BlockState s) { return s.getFluidState().is(FluidTags.WATER); }

    /** Can a player's body occupy this cell? */
    public static boolean passable(int x, int y, int z) {
        BlockState s = state(x, y, z);
        if (s.isAir()) return true;
        if (isLava(s) || DANGER_PASS.contains(s.getBlock())) return false;
        BlockPos p = new BlockPos(x, y, z);
        return s.getCollisionShape(mc().level, p).isEmpty();
    }

    /** Can a player stand on top of this cell? */
    public static boolean standable(int x, int y, int z) {
        BlockState s = state(x, y, z);
        if (s.isAir() || DANGER_FLOOR.contains(s.getBlock())) return false;
        if (isWater(s)) return true; // swimming on the surface
        BlockPos p = new BlockPos(x, y, z);
        return !s.getCollisionShape(mc().level, p).isEmpty();
    }

    public static boolean replaceable(int x, int y, int z) {
        BlockState s = state(x, y, z);
        return s.canBeReplaced() && !isLava(s);
    }

    /** Breakable without exposing lava or destroying storage. Returns -1 if not allowed, else a tick-cost estimate. */
    public static double breakCost(int x, int y, int z) {
        BlockState s = state(x, y, z);
        if (s.isAir()) return 0;
        if (!s.getFluidState().isEmpty() || NEVER_BREAK.contains(s.getBlock())) return -1;
        Block b = s.getBlock();
        if (b instanceof DoorBlock || b instanceof TrapDoorBlock || b instanceof FenceGateBlock || b instanceof BedBlock
            || b instanceof LadderBlock || b instanceof ScaffoldingBlock || b instanceof ShulkerBoxBlock
            || b instanceof StainedGlassBlock || b instanceof StainedGlassPaneBlock || b == Blocks.GLASS) return -1; // don't wreck the base
        BlockPos p = new BlockPos(x, y, z);
        float hardness = s.getDestroySpeed(mc().level, p);
        if (hardness < 0) return -1;
        // never open a hole into lava or a water column above
        for (Direction d : Direction.values()) {
            if (d == Direction.DOWN) continue;
            BlockState n = mc().level.getBlockState(p.relative(d));
            if (isLava(n)) return -1;
            if (d == Direction.UP && !n.getFluidState().isEmpty()) return -1;
        }
        // falling blocks above make tunnels collapse onto the player
        BlockState above = mc().level.getBlockState(p.above());
        if (above.getBlock() == Blocks.GRAVEL || above.getBlock() == Blocks.SAND || above.getBlock() == Blocks.RED_SAND) return 10 + hardness * 12 + 25;
        return 10 + hardness * 12;
    }

    public static boolean inWater(int x, int y, int z) { return isWater(state(x, y, z)); }

    public static boolean isLavaAt(int x, int y, int z) { return isLava(state(x, y, z)); }

    public static FluidState fluid(BlockPos p) { return mc().level.getFluidState(p); }

    /** Air-like for the memory scanner: no collision and not a hazard to pass. */
    public static boolean air2(BlockState s) {
        if (s.isAir()) return true;
        if (isLava(s) || DANGER_PASS.contains(s.getBlock())) return false;
        return s.getCollisionShape(mc().level, BlockPos.ZERO).isEmpty();
    }

    /** Standable for the memory scanner (floor the player can stand on). */
    public static boolean standable2(BlockState s, BlockPos p) {
        if (s.isAir() || DANGER_FLOOR.contains(s.getBlock())) return false;
        if (isWater(s)) return true;
        return !s.getCollisionShape(mc().level, p).isEmpty();
    }
}
