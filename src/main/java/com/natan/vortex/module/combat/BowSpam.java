package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;

public class BowSpam extends Module {
    private final NumberSetting charge = add(new NumberSetting("Charge", "Ticks to draw before releasing (10+ for real damage, 20 = full power)", 14, 4, 20, 1));

    public BowSpam() { super("BowSpam", "Fires arrows as fast as possible while holding right click", Category.COMBAT); }

    @Override
    public void onDisable() { if (inGame()) com.natan.vortex.util.Keys.hold(mc.options.keyUse, false); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        boolean bow = mc.player.getMainHandItem().getItem() instanceof BowItem || mc.player.getMainHandItem().getItem() instanceof CrossbowItem;
        if (!bow) { com.natan.vortex.util.Keys.hold(mc.options.keyUse, false); return; }
        if (mc.player.isUsingItem() && mc.player.getTicksUsingItem() >= charge.i()) {
            mc.gameMode.releaseUsingItem(mc.player);
            com.natan.vortex.util.Keys.hold(mc.options.keyUse, false);   // let go, then re-draw next tick
        } else {
            com.natan.vortex.util.Keys.hold(mc.options.keyUse, true);    // auto-draw: no need to hold right-click
        }
    }
}
