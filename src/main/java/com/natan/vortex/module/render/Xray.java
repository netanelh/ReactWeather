package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import com.natan.vortex.setting.NumberSetting;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import java.util.HashSet;
import java.util.Set;

public class Xray extends Module {
    private final BoolSetting ores = add(new BoolSetting("Ores", "Coal, iron, copper, gold, redstone, lapis", true));
    private final BoolSetting diamonds = add(new BoolSetting("Diamonds", "Diamond ore", true));
    private final BoolSetting emeralds = add(new BoolSetting("Emeralds", "Emerald ore", true));
    private final BoolSetting debris = add(new BoolSetting("Ancient Debris", "Netherite source", true));
    private final BoolSetting nether = add(new BoolSetting("Nether Ores", "Nether gold and quartz", true));
    private final BoolSetting storage = add(new BoolSetting("Storage", "Chests, barrels, shulkers, ender chests", true));
    private final BoolSetting spawners = add(new BoolSetting("Spawners", "Mob spawners and trial spawners", true));
    private final BoolSetting valuables = add(new BoolSetting("Valuables", "Mineral blocks, beacons, obsidian", false));

    private final NumberSetting range = add(new NumberSetting("Range", "How far to highlight ores (blocks)", 24, 8, 48, 4));
    private final BoolSetting realXray = add(new BoolSetting("Real Xray", "See-through terrain like Meteor (works with Sodium)", true));
    private final BoolSetting boxes = add(new BoolSetting("Boxes", "Also draw boxes around ores through walls", false));
    private final BoolSetting hideBlocks = add(new BoolSetting("Hide Terrain", "Also make stone invisible (needs a non-Sodium chunk renderer)", false).visibleWhen(() -> !com.natan.vortex.VortexClient.SODIUM));

    private volatile Set<Block> visible = Set.of(); // swapped atomically: read from chunk-building threads
    private volatile List<BlockPos> found = List.of();
    private int settingsHash, scanTimer;

    public Xray() { super("Xray", "See ores and loot through walls", Category.RENDER); }

    @Override public void onEnable() { rebuild(); reload(); }
    @Override public void onDisable() { reload(); }

    @Override
    public void onTick() {
        int h = java.util.Objects.hash(ores.get(), diamonds.get(), emeralds.get(), debris.get(), nether.get(), storage.get(), spawners.get(), valuables.get(), hideBlocks.get(), realXray.get());
        if (h != settingsHash) { settingsHash = h; rebuild(); reload(); }
        if (boxesOn() && scanTimer-- <= 0) { scanTimer = 15; scan(); }
    }

    /** Called from the block renderer mixins (render threads, so keep it cheap). */
    public boolean shows(Block b) { return visible.contains(b); }

    /** Pipeline hiding only applies when Hide Terrain is on (otherwise boxes do the work, Sodium-safe). */
    public boolean hideTerrain() { return (hideBlocks.on() || realXray.on()) && !com.natan.vortex.VortexClient.SODIUM; }

    /** Real see-through Xray (via the Sodium mixins, or the vanilla pipeline without Sodium). */
    public boolean realXray() { return realXray.on(); }

    /** Boxes: on when chosen, or as the fallback under Sodium when real Xray is off. */
    public boolean boxesOn() { return boxes.on() || (com.natan.vortex.VortexClient.SODIUM && !realXray.on()); }

    public List<BlockPos> found() { return found; }

    /** A color per ore type for the boxes. */
    public int colorOf(Block b) {
        String id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(b).getPath();
        if (id.contains("diamond")) return 0xFF22D3EE;
        if (id.contains("emerald")) return 0xFF22C55E;
        if (id.contains("gold") || id.contains("debris")) return 0xFFF59E0B;
        if (id.contains("iron") || id.contains("copper")) return 0xFFFCA5A5;
        if (id.contains("redstone")) return 0xFFEF4444;
        if (id.contains("lapis")) return 0xFF3B82F6;
        if (id.contains("spawner") || id.contains("vault")) return 0xFFA855F7;
        if (id.contains("chest") || id.contains("barrel") || id.contains("shulker")) return 0xFFEAB308;
        return 0xFFFFFFFF;
    }

    private void scan() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || visible.isEmpty()) { found = List.of(); return; }
        int r = range.i();
        BlockPos me = mc.player.blockPosition();
        List<BlockPos> hits = new ArrayList<>();
        var level = mc.level;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int minY = level.getMinY(), maxY = minY + level.getHeight();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
            int wx = me.getX() + dx, wz = me.getZ() + dz;
            if (!level.getChunkSource().hasChunk(wx >> 4, wz >> 4)) continue;
            for (int dy = -r; dy <= r; dy++) {
                int wy = me.getY() + dy;
                if (wy < minY || wy >= maxY) continue;
                m.set(wx, wy, wz);
                if (visible.contains(level.getBlockState(m).getBlock())) { hits.add(m.immutable()); if (hits.size() >= 600) break; }
            }
        }
        hits.sort(Comparator.comparingDouble(p -> p.distSqr(me)));
        found = hits;
    }

    private void reload() { if (mc.levelExtractor != null) mc.levelExtractor.allChanged(); }

    private void rebuild() {
        Set<Block> s = new HashSet<>();
        if (ores.on()) s.addAll(Set.of(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE,
            Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE,
            Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE));
        if (diamonds.on()) s.addAll(Set.of(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE));
        if (emeralds.on()) s.addAll(Set.of(Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE));
        if (debris.on()) s.add(Blocks.ANCIENT_DEBRIS);
        if (nether.on()) s.addAll(Set.of(Blocks.NETHER_GOLD_ORE, Blocks.NETHER_QUARTZ_ORE, Blocks.GILDED_BLACKSTONE));
        if (storage.on()) {
            s.addAll(Set.of(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL, Blocks.ENDER_CHEST, Blocks.SHULKER_BOX));
            // every colour of shulker box, not just the undyed one
            for (net.minecraft.world.level.block.Block b : net.minecraft.core.registries.BuiltInRegistries.BLOCK)
                if (b instanceof net.minecraft.world.level.block.ShulkerBoxBlock) s.add(b);
        }
        if (spawners.on()) s.addAll(Set.of(Blocks.SPAWNER, Blocks.TRIAL_SPAWNER, Blocks.VAULT));
        if (valuables.on()) s.addAll(Set.of(Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.GOLD_BLOCK, Blocks.IRON_BLOCK,
            Blocks.NETHERITE_BLOCK, Blocks.BEACON, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN));
        visible = s;
    }
}
