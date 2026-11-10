package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Items2;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ContainerInput;

import java.util.Set;

public class InventoryCleaner extends Module {
    private static final Set<String> JUNK = Set.of("rotten_flesh", "poisonous_potato", "spider_eye", "bone", "string", "gunpowder",
        "wheat_seeds", "beetroot_seeds", "pumpkin_seeds", "melon_seeds", "feather", "egg", "leather", "ink_sac", "kelp", "stick");
    private static final Set<String> STONE = Set.of("cobblestone", "cobbled_deepslate", "diorite", "andesite", "granite", "tuff", "netherrack", "dirt", "gravel", "blackstone", "calcite");

    private final BoolSetting mobDrops = add(new BoolSetting("Mob Drops", "Rotten flesh, bones, string, seeds...", true));
    private final BoolSetting stone = add(new BoolSetting("Stone Types", "Diorite, andesite, granite, tuff, dirt, gravel...", false));
    private final NumberSetting keepStone = add(new NumberSetting("Keep Stacks", "Stone stacks to keep for building", 2, 0, 9, 1).visibleWhen(stone::on));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between throws", 3, 0, 20, 1));
    private int timer;

    public InventoryCleaner() { super("InvCleaner", "Throws away junk items automatically", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame() || timer-- > 0) return;
        if (mc.gui.screen() instanceof AbstractContainerScreen<?> && !(mc.gui.screen() instanceof InventoryScreen)) return;
        var inv = mc.player.getInventory();
        int stoneStacks = 0;
        for (int i = 0; i < 36; i++) if (STONE.contains(Items2.id(inv.getItem(i)))) stoneStacks++;
        for (int i = 9; i < 36; i++) {
            String id = Items2.id(inv.getItem(i));
            if (inv.getItem(i).isEmpty()) continue;
            boolean junk = mobDrops.on() && JUNK.contains(id);
            if (!junk && stone.on() && STONE.contains(id) && stoneStacks > keepStone.i()) { junk = true; stoneStacks--; }
            if (!junk) continue;
            Inv.click(Inv.toMenuSlot(i), 1, ContainerInput.THROW);
            timer = delay.i();
            return;
        }
    }
}
