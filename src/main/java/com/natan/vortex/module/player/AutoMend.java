package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Rotations;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AutoMend extends Module {
    private final NumberSetting threshold = add(new NumberSetting("Below %", "Repair when gear durability is below this", 80, 10, 99, 1));
    private final NumberSetting rate = add(new NumberSetting("Rate", "Ticks between bottles", 2, 1, 10, 1));
    private int timer;

    public AutoMend() { super("AutoMend", "Throws XP bottles at your feet to repair mending gear", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame() || timer-- > 0 || mc.gui.screen() != null) return;
        boolean needs = false;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            ItemStack st = mc.player.getItemBySlot(s);
            if (st.isDamageableItem() && st.isEnchanted() && 100.0 * (st.getMaxDamage() - st.getDamageValue()) / st.getMaxDamage() < threshold.d()) { needs = true; break; }
        }
        if (!needs) return;
        int slot = Inv.findHotbar(s -> s.is(Items.EXPERIENCE_BOTTLE));
        if (slot == -1) return;
        int prev = Inv.selected();
        Inv.select(slot);
        Rotations.sendSilent(mc.player.getYRot(), 90f);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        Inv.select(prev);
        timer = rate.i();
    }
}
