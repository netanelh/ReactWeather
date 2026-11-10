package com.natan.vortex.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.function.Predicate;

/** Shared block-placing helper used by Scaffold, Surround, AutoTrap and the navigator. */
public final class Place {
    private static final Set<String> NOT_THROWAWAY = Set.of("chest", "trapped_chest", "barrel", "shulker_box", "ender_chest",
        "crafting_table", "furnace", "anvil", "tnt", "sand", "red_sand", "gravel", "scaffolding", "beacon", "spawner");

    private Place() {}

    /** Cheap full blocks that are fine to use as building material. */
    public static boolean isThrowaway(ItemStack s) {
        if (!(s.getItem() instanceof BlockItem bi)) return false;
        String id = Items2.id(s);
        if (NOT_THROWAWAY.contains(id) || id.endsWith("shulker_box") || id.endsWith("_ore")) return false;
        return bi.getBlock().defaultBlockState().isSolidRender();
    }

    public static boolean canPlaceAt(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (!mc.level.getBlockState(pos).canBeReplaced()) return false;
        for (Direction d : Direction.values()) if (!mc.level.getBlockState(pos.relative(d)).canBeReplaced()) return true;
        return false;
    }

    /**
     * Places a block from the given hotbar slot at pos, against any solid neighbour.
     * @return true if a placement packet was sent
     */
    public static boolean at(BlockPos pos, int hotbarSlot, boolean rotate) {
        Minecraft mc = Minecraft.getInstance();
        if (hotbarSlot < 0 || !mc.level.getBlockState(pos).canBeReplaced()) return false;
        Vec3 eye = mc.player.getEyePosition();
        Direction bestFace = null; BlockPos bestNeighbor = null; double bestDist = Double.MAX_VALUE;
        for (Direction d : Direction.values()) {
            BlockPos neighbor = pos.relative(d);
            if (mc.level.getBlockState(neighbor).canBeReplaced()) continue;
            Direction face = d.getOpposite();
            Vec3 hit = Vec3.atCenterOf(neighbor).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            double dist = hit.distanceTo(eye);
            if (dist < bestDist) { bestDist = dist; bestFace = face; bestNeighbor = neighbor; }
        }
        if (bestFace == null || bestDist > 5.0) return false;
        Vec3 hit = Vec3.atCenterOf(bestNeighbor).add(bestFace.getStepX() * 0.5, bestFace.getStepY() * 0.5, bestFace.getStepZ() * 0.5);
        Inv.select(hotbarSlot);
        if (rotate) { float[] a = Rotations.anglesTo(hit); Rotations.sendSilent(a[0], a[1]); }
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, bestFace, bestNeighbor, false));
        mc.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    public static int findSlot(Predicate<ItemStack> p) { return Inv.findHotbar(p); }
}
