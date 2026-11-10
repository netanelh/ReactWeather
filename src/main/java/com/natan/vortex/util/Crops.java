package com.natan.vortex.util;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public final class Crops {
    private Crops() {}

    public static boolean isMature(BlockState s) {
        Block b = s.getBlock();
        if (b == Blocks.WHEAT || b == Blocks.CARROTS || b == Blocks.POTATOES)
            return s.hasProperty(BlockStateProperties.AGE_7) && s.getValue(BlockStateProperties.AGE_7) >= 7;
        if (b == Blocks.BEETROOTS || b == Blocks.NETHER_WART)
            return s.hasProperty(BlockStateProperties.AGE_3) && s.getValue(BlockStateProperties.AGE_3) >= 3;
        if (b == Blocks.MELON || b == Blocks.PUMPKIN) return true;
        return false;
    }

    public static Item seedFor(Block b) {
        if (b == Blocks.WHEAT) return Items.WHEAT_SEEDS;
        if (b == Blocks.CARROTS) return Items.CARROT;
        if (b == Blocks.POTATOES) return Items.POTATO;
        if (b == Blocks.BEETROOTS) return Items.BEETROOT_SEEDS;
        if (b == Blocks.NETHER_WART) return Items.NETHER_WART;
        return null;
    }
}
