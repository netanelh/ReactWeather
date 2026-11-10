package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.world.item.BowItem;

public class BowSpam extends Module {
    private final NumberSetting charge = add(new NumberSetting("Charge", "Ticks to draw before releasing (3 = spam, 20 = full power)", 3, 3, 20, 1));

    public BowSpam() { super("BowSpam", "Fires arrows as fast as possible while holding right click", Category.COMBAT); }

    @Override
    public void onTick() {
        if (!inGame() || !(mc.player.getMainHandItem().getItem() instanceof BowItem)) return;
        if (mc.player.isUsingItem() && mc.player.getTicksUsingItem() >= charge.i()) {
            mc.gameMode.releaseUsingItem(mc.player);
        }
    }
}
