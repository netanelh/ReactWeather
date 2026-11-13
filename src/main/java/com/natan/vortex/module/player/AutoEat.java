package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Items2;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public class AutoEat extends Module {
    private final NumberSetting hunger = add(new NumberSetting("Hunger", "Eat when food is at or below this (20 = full)", 14, 1, 19, 1));
    private final NumberSetting health = add(new NumberSetting("Health", "Also eat when health is at or below this (hearts)", 6, 0, 20, 0.5));
    private final BoolSetting avoidBad = add(new BoolSetting("Avoid Bad Food", "Skip rotten flesh, spider eyes, pufferfish, chorus fruit", true));

    private boolean eating;
    private int prevSlot = -1;

    public AutoEat() { super("AutoEat", "Eats food from your hotbar when hungry", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame() || mc.player.isCreative()) return;
        int food = mc.player.getFoodData().getFoodLevel();
        boolean want = food < 20 && (food <= hunger.i() || mc.player.getHealth() / 2f <= health.d());

        if (eating) {
            if (!want || !mc.player.getMainHandItem().has(DataComponents.FOOD)) { stop(); return; }
            com.natan.vortex.util.Keys.hold(mc.options.keyUse, true);
            return;
        }
        if (!want || mc.gui.screen() != null) return;
        int slot = Inv.findHotbar(this::isGoodFood);
        if (slot == -1) return;
        prevSlot = Inv.selected();
        Inv.select(slot);
        eating = true;
        com.natan.vortex.util.Keys.hold(mc.options.keyUse, true);
    }

    private boolean isGoodFood(ItemStack s) {
        if (!s.has(DataComponents.FOOD)) return false;
        if (!avoidBad.on()) return true;
        String id = Items2.id(s);
        return !(id.equals("rotten_flesh") || id.equals("spider_eye") || id.equals("pufferfish") || id.equals("poisonous_potato") || id.equals("chorus_fruit"));
    }

    private void stop() {
        eating = false;
        com.natan.vortex.util.Keys.hold(mc.options.keyUse, false);
        if (prevSlot != -1) Inv.select(prevSlot);
        prevSlot = -1;
    }

    @Override public void onDisable() { if (eating) stop(); }

    @Override public void onWorldChange() { if (eating && mc.options != null) com.natan.vortex.util.Keys.hold(mc.options.keyUse, false); eating = false; prevSlot = -1; }
}
