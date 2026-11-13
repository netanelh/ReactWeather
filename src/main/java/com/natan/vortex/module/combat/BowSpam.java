package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.world.item.BowItem;

public class BowSpam extends Module {
    private final NumberSetting charge = add(new NumberSetting("Charge", "Ticks to draw before releasing (10+ for real damage, 20 = full power)", 14, 4, 20, 1));

    public BowSpam() { super("BowSpam", "Fires arrows as fast as possible while holding right click", Category.COMBAT); }

    private boolean pressed; // only release the use key if we pressed it (eating/shields keep working)

    private void hold(boolean down) {
        if (down) { com.natan.vortex.util.Keys.hold(mc.options.keyUse, true); pressed = true; }
        else if (pressed) { com.natan.vortex.util.Keys.hold(mc.options.keyUse, false); pressed = false; }
    }

    @Override
    public void onDisable() { if (inGame()) hold(false); pressed = false; }

    @Override
    public void onTick() {
        if (!inGame()) return;
        boolean bow = mc.player.getMainHandItem().getItem() instanceof BowItem; // crossbows need 25 ticks: not spammable
        if (!bow) { hold(false); return; }
        if (mc.player.isUsingItem() && mc.player.getTicksUsingItem() >= charge.i()) {
            mc.gameMode.releaseUsingItem(mc.player);
            hold(false);   // let go, then re-draw next tick
        } else {
            hold(true);    // auto-draw: no need to hold right-click
        }
    }
}
