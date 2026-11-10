package com.natan.vortex.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Set;

public final class BlockSets {
    public static final String[] MINE_PRESETS = {"Diamonds", "All Ores", "Ancient Debris", "Iron", "Gold", "Emerald", "Coal", "Copper", "Redstone", "Lapis", "Quartz", "Custom"};
    public static Set<Block> custom = new HashSet<>(); // filled by .mine <block...>

    private BlockSets() {}

    public static Set<Block> preset(String name) {
        return switch (name) {
            case "Diamonds" -> Set.of(Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
            case "Ancient Debris" -> Set.of(Blocks.ANCIENT_DEBRIS);
            case "Iron" -> Set.of(Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
            case "Gold" -> Set.of(Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
            case "Emerald" -> Set.of(Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
            case "Coal" -> Set.of(Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
            case "Copper" -> Set.of(Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
            case "Redstone" -> Set.of(Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
            case "Lapis" -> Set.of(Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
            case "Quartz" -> Set.of(Blocks.NETHER_QUARTZ_ORE);
            case "Custom" -> custom.isEmpty() ? Set.of(Blocks.DIAMOND_ORE) : Set.copyOf(custom);
            default -> allOres();
        };
    }

    public static Set<Block> allOres() {
        Set<Block> s = new HashSet<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            String id = BuiltInRegistries.BLOCK.getKey(b).getPath();
            if (id.endsWith("_ore")) s.add(b);
        }
        s.add(Blocks.ANCIENT_DEBRIS);
        return s;
    }

    /** Parses "diamond_ore", "minecraft:iron_ore" or shorthand "diamond" (adds ore + deepslate variant). */
    public static Set<Block> parse(String token) {
        Set<Block> out = new HashSet<>();
        String t = token.toLowerCase();
        if (t.equals("ores")) return allOres();
        for (String cand : new String[]{t, t + "_ore", "deepslate_" + t + "_ore", "deepslate_" + t, "nether_" + t + "_ore"}) {
            Identifier id = cand.contains(":") ? Identifier.tryParse(cand) : Identifier.withDefaultNamespace(cand);
            if (id == null) continue;
            BuiltInRegistries.BLOCK.getOptional(id).ifPresent(b -> { if (b != Blocks.AIR) out.add(b); });
        }
        return out;
    }
}
