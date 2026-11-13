package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Inv;
import net.minecraft.world.item.Items;

public class AutoGap extends Module {
    private final NumberSetting health = add(new NumberSetting("Health", "Eat at or below this many hearts", 7, 1, 20, 0.5));
    private final BoolSetting preferEnchanted = add(new BoolSetting("Prefer God Apple", "Use enchanted golden apples first", false));

    private boolean eating;
    private int prev = -1;

    public AutoGap() { super("AutoGap", "Eats golden apples when your health is low", Category.COMBAT); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        boolean low = (mc.player.getHealth() + mc.player.getAbsorptionAmount()) / 2f <= health.d();
        if (eating) {
            var held = mc.player.getMainHandItem();
            if (!low || !(held.is(Items.GOLDEN_APPLE) || held.is(Items.ENCHANTED_GOLDEN_APPLE))) { stop(); return; }
            com.natan.vortex.util.Keys.hold(mc.options.keyUse, true);
            return;
        }
        if (!low || mc.gui.screen() != null) return;
        int god = Inv.findHotbar(s -> s.is(Items.ENCHANTED_GOLDEN_APPLE)), gap = Inv.findHotbar(s -> s.is(Items.GOLDEN_APPLE));
        int slot = preferEnchanted.on() ? (god != -1 ? god : gap) : (gap != -1 ? gap : god);
        if (slot == -1) return;
        prev = Inv.selected();
        Inv.select(slot);
        eating = true;
        com.natan.vortex.util.Keys.hold(mc.options.keyUse, true);
    }

    private void stop() {
        eating = false;
        com.natan.vortex.util.Keys.hold(mc.options.keyUse, false);
        if (prev != -1) Inv.select(prev);
        prev = -1;
    }

    @Override public void onDisable() { if (eating) stop(); }

    @Override public void onWorldChange() { if (eating && mc.options != null) com.natan.vortex.util.Keys.hold(mc.options.keyUse, false); eating = false; prev = -1; }
}
