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
import net.minecraft.world.level.block.VineBlock;

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

    /** Hostile-mob positions captured on the main thread for the pathfinder to avoid. */
    private static volatile int[] hostiles = new int[0];
    public static void setHostiles(int[] packed) { hostiles = packed; }
    /** Extra path cost near remembered hostile mobs (0 if none close). */
    public static double hostilePenalty(int x, int y, int z) {
        int[] h = hostiles;
        double worst = 0;
        for (int i = 0; i + 2 < h.length; i += 3) {
            int dx = h[i] - x, dy = h[i + 1] - y, dz = h[i + 2] - z;
            int d2 = dx * dx + dy * dy + dz * dz;
            if (d2 < 64) worst = Math.max(worst, (64 - d2) * 2.0); // within 8 blocks
        }
        return worst;
    }

    /** Steps that failed while walking; the pathfinder makes them more expensive so it tries another way. */
    private static final java.util.concurrent.ConcurrentHashMap<Long, Integer> failed = new java.util.concurrent.ConcurrentHashMap<>();
    public static void markFailed(BlockPos p) { failed.merge(p.asLong(), 40, (a, b) -> Math.min(200, a + b)); }
    public static void clearFailed() { failed.clear(); }
    public static double failPenalty(int x, int y, int z) {
        Integer v = failed.get(BlockPos.asLong(x, y, z));
        return v == null ? 0 : v;
    }

    /** Can we step up onto this cell with a single jump? (no fences/walls/doors, which are taller than a block) */
    public static boolean stepUp(int x, int y, int z) {
        BlockState s = state(x, y, z);
        if (s.isAir() || isWater(s) || isHandDoor(s) || DANGER_FLOOR.contains(s.getBlock())) return false;
        var sh = s.getCollisionShape(mc().level, new BlockPos(x, y, z));
        return !sh.isEmpty() && sh.max(Direction.Axis.Y) <= 1.0;
    }

    /** A door/gate/trapdoor we can open by hand (not iron). */
    public static boolean isHandDoor(BlockState s) {
        Block b = s.getBlock();
        if (b == Blocks.IRON_DOOR || b == Blocks.IRON_TRAPDOOR) return false;
        return b instanceof DoorBlock || b instanceof FenceGateBlock || b instanceof TrapDoorBlock;
    }

    public static boolean isClimbable(int x, int y, int z) {
        Block b = state(x, y, z).getBlock();
        return b instanceof LadderBlock || b instanceof VineBlock || b instanceof ScaffoldingBlock;
    }

    public static boolean isLava(BlockState s) { return s.getFluidState().is(FluidTags.LAVA); }
    public static boolean isWater(BlockState s) { return s.getFluidState().is(FluidTags.WATER); }

    /** Can a player's body occupy this cell? */
    public static boolean passable(int x, int y, int z) {
        BlockState s = state(x, y, z);
        if (s.isAir()) return true;
        if (isLava(s) || DANGER_PASS.contains(s.getBlock())) return false;
        if (isHandDoor(s)) return true; // the executor opens it on the way through
        if (s.getBlock() instanceof LadderBlock) return true; // thin box: we climb inside it
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
