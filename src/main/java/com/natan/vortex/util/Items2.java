package com.natan.vortex.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class Items2 {
    private Items2() {}

    /** Registry path, e.g. "diamond_sword". */
    public static String id(ItemStack s) { return BuiltInRegistries.ITEM.getKey(s.getItem()).getPath(); }

    public static boolean isOre(net.minecraft.world.level.block.Block b) {
        String id = BuiltInRegistries.BLOCK.getKey(b).getPath();
        return id.endsWith("_ore") || id.equals("ancient_debris");
    }
}
