package com.natan.vortex.module.render;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

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

    private volatile Set<Block> visible = Set.of(); // swapped atomically: read from chunk-building threads
    private int settingsHash;

    public Xray() { super("Xray", "See ores and loot through walls", Category.RENDER); }

    @Override public void onEnable() { rebuild(); reload(); }
    @Override public void onDisable() { reload(); }

    @Override
    public void onTick() {
        int h = java.util.Objects.hash(ores.get(), diamonds.get(), emeralds.get(), debris.get(), nether.get(), storage.get(), spawners.get(), valuables.get());
        if (h != settingsHash) { settingsHash = h; rebuild(); reload(); }
    }

    /** Called from the block renderer mixins (render threads, so keep it cheap). */
    public boolean shows(Block b) { return visible.contains(b); }

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
        if (storage.on()) s.addAll(Set.of(Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.BARREL, Blocks.ENDER_CHEST, Blocks.SHULKER_BOX));
        if (spawners.on()) s.addAll(Set.of(Blocks.SPAWNER, Blocks.TRIAL_SPAWNER, Blocks.VAULT));
        if (valuables.on()) s.addAll(Set.of(Blocks.DIAMOND_BLOCK, Blocks.EMERALD_BLOCK, Blocks.GOLD_BLOCK, Blocks.IRON_BLOCK,
            Blocks.NETHERITE_BLOCK, Blocks.BEACON, Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN));
        visible = s;
    }
}
