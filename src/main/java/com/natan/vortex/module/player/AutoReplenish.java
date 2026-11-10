package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Inv;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.ItemStack;

public class AutoReplenish extends Module {
    private final NumberSetting threshold = add(new NumberSetting("Threshold", "Refill a hotbar stack when it drops to this", 8, 1, 63, 1));
    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between refills", 2, 0, 20, 1));
    private int timer;

    public AutoReplenish() { super("AutoReplenish", "Refills hotbar stacks from your inventory", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame() || timer-- > 0) return;
        if (mc.gui.screen() instanceof AbstractContainerScreen<?> && !(mc.gui.screen() instanceof InventoryScreen)) return;
        if (!mc.player.containerMenu.getCarried().isEmpty()) return;
        var inv = mc.player.getInventory();
        for (int h = 0; h < 9; h++) {
            ItemStack hs = inv.getItem(h);
            if (hs.isEmpty() || !hs.isStackable() || hs.getCount() > threshold.i()) continue;
            for (int i = 9; i < 36; i++) {
                ItemStack st = inv.getItem(i);
                if (st.isEmpty() || !ItemStack.isSameItemSameComponents(st, hs)) continue;
                Inv.click(Inv.toMenuSlot(i), 0, ContainerInput.PICKUP);
                Inv.click(Inv.toMenuSlot(h), 0, ContainerInput.PICKUP);
                if (!mc.player.containerMenu.getCarried().isEmpty()) Inv.click(Inv.toMenuSlot(i), 0, ContainerInput.PICKUP);
                timer = delay.i();
                return;
            }
        }
    }
}
