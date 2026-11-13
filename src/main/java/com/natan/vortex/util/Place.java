package com.natan.vortex.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Set;
import java.util.function.Predicate;

/** Shared block-placing helper used by Scaffold, Surround, AutoTrap and the navigator. */
public final class Place {
    private static final Set<String> NOT_THROWAWAY = Set.of("chest", "trapped_chest", "barrel", "shulker_box", "ender_chest",
        "crafting_table", "furnace", "anvil", "tnt", "sand", "red_sand", "gravel", "scaffolding", "beacon", "spawner");

    private Place() {}

    /** Blocks that react to right-click (would open/toggle instead of getting a block placed on them). */
    public static boolean isInteractable(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level.getBlockEntity(pos) != null) return true; // chests, furnaces, hoppers, signs, beds...
        var b = mc.level.getBlockState(pos).getBlock();
        if (b instanceof net.minecraft.world.level.block.DoorBlock || b instanceof net.minecraft.world.level.block.FenceGateBlock
            || b instanceof net.minecraft.world.level.block.TrapDoorBlock || b instanceof net.minecraft.world.level.block.ButtonBlock
            || b instanceof net.minecraft.world.level.block.LeverBlock || b instanceof net.minecraft.world.level.block.BedBlock) return true;
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath();
        return id.contains("crafting_table") || id.contains("anvil") || id.equals("note_block") || id.equals("bell")
            || id.contains("table") || id.equals("stonecutter") || id.equals("grindstone") || id.equals("loom") || id.equals("repeater") || id.equals("comparator");
    }

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
    public static boolean at(BlockPos pos, int hotbarSlot, boolean rotate) { return at(pos, hotbarSlot, rotate, null); }

    public static boolean at(BlockPos pos, int hotbarSlot, boolean rotate, Direction preferFace) { return at(pos, hotbarSlot, rotate, preferFace, 0); }

    /** @param yOff shifts the click up/down on a side face (+0.25 = top half of slabs/stairs) */
    public static boolean at(BlockPos pos, int hotbarSlot, boolean rotate, Direction preferFace, double yOff) {
        Minecraft mc = Minecraft.getInstance();
        if (hotbarSlot < 0 || !mc.level.getBlockState(pos).canBeReplaced()) return false;
        Vec3 eye = mc.player.getEyePosition();
        Direction bestFace = null; BlockPos bestNeighbor = null; double bestDist = Double.MAX_VALUE;
        for (Direction d : Direction.values()) {
            if (preferFace != null && d != preferFace && d != preferFace.getOpposite()) continue;
            BlockPos neighbor = pos.relative(d);
            if (mc.level.getBlockState(neighbor).canBeReplaced() || isInteractable(neighbor)) continue;
            Direction face = d.getOpposite();
            Vec3 hit = Vec3.atCenterOf(neighbor).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
            double dist = hit.distanceTo(eye);
            if (dist < bestDist) { bestDist = dist; bestFace = face; bestNeighbor = neighbor; }
        }
        if (bestFace == null || bestDist > 5.0) return false;
        Vec3 hit = Vec3.atCenterOf(bestNeighbor).add(bestFace.getStepX() * 0.5, bestFace.getStepY() * 0.5, bestFace.getStepZ() * 0.5);
        if (bestFace.getAxis() != Direction.Axis.Y) hit = hit.add(0, yOff, 0);
        Inv.select(hotbarSlot);
        if (rotate) { float[] a = Rotations.anglesTo(hit); Rotations.sendSilent(a[0], a[1]); }
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, bestFace, bestNeighbor, false));
        mc.player.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    /**
     * Places a block trying to match the desired state's orientation: aims the player's yaw for
     * horizontal facing, pitch for vertical facing / slab half, and prefers the face that sets the
     * log/pillar axis. Best-effort — falls back to a plain place when it can't match.
     */
    public static boolean atOriented(BlockPos pos, int hotbarSlot, BlockState desired, boolean rotate) {
        Minecraft mc = Minecraft.getInstance();
        if (desired == null) return at(pos, hotbarSlot, rotate);
        Float yaw = null, pitch = null;
        Direction preferFace = null;
        double yOff = 0;
        // horizontal facing: stairs/doors/gates/beds face the way the player looks; furnaces, chests etc. face the player
        var blk = desired.getBlock();
        boolean facesPlayerDir = blk instanceof net.minecraft.world.level.block.StairBlock || blk instanceof net.minecraft.world.level.block.DoorBlock
            || blk instanceof net.minecraft.world.level.block.FenceGateBlock || blk instanceof net.minecraft.world.level.block.BedBlock;
        if (desired.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            Direction f = desired.getValue(BlockStateProperties.HORIZONTAL_FACING);
            yaw = facesPlayerDir ? f.toYRot() : f.toYRot() + 180f;
        }
        if (desired.hasProperty(BlockStateProperties.FACING)) {
            Direction f = desired.getValue(BlockStateProperties.FACING);
            if (f.getAxis() == Direction.Axis.Y) pitch = f == Direction.UP ? 80f : -80f;
            else yaw = f.toYRot() + 180f;
        }
        // slab / stairs half is decided by where on the face we click (above or below the middle)
        if (desired.hasProperty(BlockStateProperties.SLAB_TYPE)) {
            String t = desired.getValue(BlockStateProperties.SLAB_TYPE).toString();
            if (t.equals("top")) yOff = 0.25; else if (t.equals("bottom")) yOff = -0.25;
        }
        if (desired.hasProperty(BlockStateProperties.HALF)) {
            String h = desired.getValue(BlockStateProperties.HALF).toString();
            yOff = h.equals("top") ? 0.25 : -0.25;
        }
        // log / pillar axis -> click the matching face
        if (desired.hasProperty(BlockStateProperties.AXIS)) {
            Direction.Axis ax = desired.getValue(BlockStateProperties.AXIS);
            preferFace = switch (ax) { case Y -> Direction.UP; case X -> Direction.EAST; case Z -> Direction.NORTH; };
        }
        if (yaw != null || pitch != null) {
            float ny = yaw != null ? yaw : mc.player.getYRot();
            float np = pitch != null ? pitch : mc.player.getXRot();
            Rotations.sendSilent(ny, np);
        }
        if (at(pos, hotbarSlot, rotate && yaw == null && pitch == null, preferFace, yOff)) return true;
        return preferFace == null ? false : at(pos, hotbarSlot, false, null, yOff); // fall back if the preferred face had no support
    }

    public static int findSlot(Predicate<ItemStack> p) { return Inv.findHotbar(p); }

    /** A slot to seal with: obsidian/crying first; if anyBlock, fall back to any cheap full block. */
    public static int buildBlockSlot(boolean anyBlock) {
        int o = findSlot(s -> s.is(net.minecraft.world.item.Items.OBSIDIAN) || s.is(net.minecraft.world.item.Items.CRYING_OBSIDIAN));
        if (o != -1 || !anyBlock) return o;
        return findSlot(Place::isThrowaway);
    }
}
