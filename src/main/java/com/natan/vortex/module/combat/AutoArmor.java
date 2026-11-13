package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Inv;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoArmor extends Module {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final int[] ARMOR_MENU_SLOT = {5, 6, 7, 8};

    private final NumberSetting delay = add(new NumberSetting("Delay", "Ticks between armor moves", 2, 0, 20, 1));
    private final BoolSetting upgrade = add(new BoolSetting("Upgrade", "Replace worn armor with better pieces", true));
    private final BoolSetting keepElytra = add(new BoolSetting("Keep Elytra", "Never swap an elytra for a chestplate", true));

    private int timer;
    // failure detection: a worn piece that doesn't come off (curse of binding) is skipped until it changes
    private final ItemStack[] lastWorn = new ItemStack[4];
    private final int[] removeTries = new int[4];

    private boolean hasFreeSlot() {
        for (int i = 0; i < 36; i++) if (mc.player.getInventory().getItem(i).isEmpty()) return true;
        return false;
    }

    public AutoArmor() { super("AutoArmor", "Equips the best armor in your inventory", Category.COMBAT); }

    @Override
    public void onTick() {
        if (!inGame() || mc.player.isCreative()) return;
        if (timer > 0) { timer--; return; }
        if (mc.gui.screen() instanceof AbstractContainerScreen<?> && !(mc.gui.screen() instanceof InventoryScreen)) return;
        if (!mc.player.containerMenu.getCarried().isEmpty()) return;

        for (int s = 0; s < SLOTS.length; s++) {
            EquipmentSlot slot = SLOTS[s];
            ItemStack worn = mc.player.getItemBySlot(slot);
            if (keepElytra.on() && worn.is(Items.ELYTRA)) continue;

            int bestIdx = -1;
            double bestScore = worn.isEmpty() ? -1 : score(worn);
            for (int i = 0; i < 36; i++) {
                ItemStack st = mc.player.getInventory().getItem(i);
                var eq = st.get(DataComponents.EQUIPPABLE);
                if (eq == null || eq.slot() != slot || st.is(Items.ELYTRA)) continue;
                String id = com.natan.vortex.util.Items2.id(st); // pumpkins/heads are "equippable" but aren't armor
                if (id.contains("pumpkin") || id.contains("head") || id.contains("skull")) continue;
                double sc = score(st);
                if (sc > bestScore) { bestScore = sc; bestIdx = i; }
            }
            if (bestIdx == -1) continue;
            if (!worn.isEmpty()) {
                if (!upgrade.on() || !hasFreeSlot()) continue;
                if (lastWorn[s] != null && ItemStack.isSameItemSameComponents(lastWorn[s], worn)) {
                    if (removeTries[s] >= 2) continue; // didn't come off twice: stop trying
                } else removeTries[s] = 0;
                lastWorn[s] = worn.copy(); removeTries[s]++;
                // take the old piece off first (shift-click moves it to the inventory)
                Inv.click(ARMOR_MENU_SLOT[s], 0, net.minecraft.world.inventory.ContainerInput.QUICK_MOVE);
            }
            Inv.quickMove(bestIdx);
            timer = delay.i();
            return;
        }
    }

    /** Ranks armor by material tier (from the item id) plus enchantment count and durability left. */
    private static double score(ItemStack s) {
        String id = com.natan.vortex.util.Items2.id(s);
        double tier = id.contains("netherite") ? 6 : id.contains("diamond") ? 5 : id.contains("iron") ? 4
            : id.contains("turtle") ? 3.5 : id.contains("chainmail") ? 3 : id.contains("gold") ? 2 : id.contains("copper") ? 2.5 : 1;
        double ench = s.isEnchanted() ? s.getEnchantments().size() * 0.3 : 0;
        double dura = s.isDamageableItem() ? 0.2 * (1 - (double) s.getDamageValue() / Math.max(1, s.getMaxDamage())) : 0;
        return tier * 10 + ench + dura;
    }
}
