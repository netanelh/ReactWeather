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
    public void onDisable() { if (inGame()) mc.options.keyUse.setDown(false); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        boolean bow = mc.player.getMainHandItem().getItem() instanceof BowItem || mc.player.getMainHandItem().getItem() instanceof CrossbowItem;
        if (!bow) { mc.options.keyUse.setDown(false); return; }
        if (mc.player.isUsingItem() && mc.player.getTicksUsingItem() >= charge.i()) {
            mc.gameMode.releaseUsingItem(mc.player);
            mc.options.keyUse.setDown(false);   // let go, then re-draw next tick
        } else {
            mc.options.keyUse.setDown(true);    // auto-draw: no need to hold right-click
        }
    }
}
