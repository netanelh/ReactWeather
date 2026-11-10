package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.ModeSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Inv;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Keeps a chosen item in the offhand; falls back to a totem when health is low. */
public class Offhand extends Module {
    private final ModeSetting item = add(new ModeSetting("Item", "What to hold in the offhand", "Golden Apple", "Golden Apple", "Crystal", "Shield", "Totem", "Firework"));
    private final NumberSetting totemHealth = add(new NumberSetting("Totem Below", "Switch to a totem at or below these hearts", 6, 0, 20, 0.5));
    private int timer;

    public Offhand() { super("Offhand", "Auto-fills your offhand (gapple/crystal/shield), totem when low", Category.COMBAT); }

    @Override public String info() { return item.get(); }

    @Override
    public void onTick() {
        if (!inGame() || timer-- > 0) return;
        if (mc.gui.screen() instanceof AbstractContainerScreen<?> && !(mc.gui.screen() instanceof InventoryScreen)) return;
        float hearts = (mc.player.getHealth() + mc.player.getAbsorptionAmount()) / 2f;
        Item want = hearts <= totemHealth.d() ? Items.TOTEM_OF_UNDYING : switch (item.get()) {
            case "Crystal" -> Items.END_CRYSTAL;
            case "Shield" -> Items.SHIELD;
            case "Totem" -> Items.TOTEM_OF_UNDYING;
            case "Firework" -> Items.FIREWORK_ROCKET;
            default -> Items.GOLDEN_APPLE;
        };
        if (mc.player.getOffhandItem().is(want)) return;
        int slot = Inv.findAny(s -> s.is(want));
        if (slot == -1 && want == Items.GOLDEN_APPLE) slot = Inv.findAny(s -> s.is(Items.ENCHANTED_GOLDEN_APPLE));
        if (slot == -1) return;
        Inv.swapToOffhand(slot);
        timer = 3;
    }
}
