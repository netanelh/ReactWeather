package com.natan.vortex.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public final class Inv {
    public static final int OFFHAND_BUTTON = 40;

    private Inv() {}

    public static int findHotbar(Predicate<ItemStack> p) {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < 9; i++) if (p.test(mc.player.getInventory().getItem(i))) return i;
        return -1;
    }

    /** Searches main inventory + hotbar (0-35). Returns the inventory index. */
    public static int findAny(Predicate<ItemStack> p) {
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < 36; i++) if (p.test(mc.player.getInventory().getItem(i))) return i;
        return -1;
    }

    /** Converts an inventory index (0-35) to a slot id in the player's InventoryMenu. */
    public static int toMenuSlot(int invIndex) { return invIndex < 9 ? 36 + invIndex : invIndex; }

    public static void select(int hotbarSlot) {
        Minecraft mc = Minecraft.getInstance();
        if (hotbarSlot >= 0 && hotbarSlot < 9) mc.player.getInventory().setSelectedSlot(hotbarSlot);
    }

    public static int selected() { return Minecraft.getInstance().player.getInventory().getSelectedSlot(); }

    public static void click(int menuSlot, int button, ContainerInput type) {
        Minecraft mc = Minecraft.getInstance();
        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId, menuSlot, button, type, mc.player);
    }

    public static void swapToOffhand(int invIndex) { click(toMenuSlot(invIndex), OFFHAND_BUTTON, ContainerInput.SWAP); }

    public static void quickMove(int invIndex) { click(toMenuSlot(invIndex), 0, ContainerInput.QUICK_MOVE); }
}
