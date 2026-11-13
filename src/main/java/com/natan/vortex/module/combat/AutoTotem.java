package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.ModeSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Inv;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.item.Items;

public class AutoTotem extends Module {
    private final ModeSetting mode = add(new ModeSetting("Mode", "Always keep a totem, or only when low", "Always", "Always", "Smart"));
    private final NumberSetting health = add(new NumberSetting("Health", "Smart mode: equip at or below this many hearts", 6, 1, 20, 0.5).visibleWhen(() -> mode.is("Smart")));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between swaps", 0, 0, 20, 1));

    private int timer;

    public AutoTotem() { super("AutoTotem", "Keeps a Totem of Undying in your offhand", Category.COMBAT); }

    @Override
    public String info() {
        if (!inGame()) return null;
        int count = 0;
        for (int i = 0; i < 36; i++) if (mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) count += mc.player.getInventory().getItem(i).getCount();
        if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) count++;
        return Integer.toString(count);
    }

    /** True while AutoTotem wants a totem in the offhand (Offhand yields to it, so they don't fight). */
    public boolean wantsTotem() {
        if (!isEnabled() || !inGame()) return false;
        if (!mode.is("Smart")) return true;
        float hearts = (mc.player.getHealth() + mc.player.getAbsorptionAmount()) / 2f;
        return hearts <= health.d() || mc.player.fallDistance > 10 || mc.player.isFallFlying();
    }

    @Override
    public void onTick() {
        if (!inGame()) return;
        if (timer > 0) { timer--; return; }
        // A swap while a chest/furnace is open would click that container instead
        if (mc.gui.screen() instanceof AbstractContainerScreen<?> && !(mc.gui.screen() instanceof InventoryScreen)) return;
        if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;

        if (mode.is("Smart")) {
            float hearts = (mc.player.getHealth() + mc.player.getAbsorptionAmount()) / 2f;
            boolean danger = hearts <= health.d() || mc.player.fallDistance > 10 || mc.player.isFallFlying();
            if (!danger) return;
        }
        int slot = Inv.findAny(s -> s.is(Items.TOTEM_OF_UNDYING));
        if (slot == -1) return;
        Inv.swapToOffhand(slot);
        timer = delay.i();
    }
}
