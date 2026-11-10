package com.natan.vortex.module.player;

import com.natan.vortex.mixin.FishingHookAccessor;
import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public class AutoFish extends Module {
    private final NumberSetting recastDelay = add(new NumberSetting("Recast Delay", "Ticks before casting again", 15, 5, 60, 1));

    private int recastIn = -1;

    public AutoFish() { super("AutoFish", "Reels in and recasts your fishing rod", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        InteractionHand hand = mc.player.getMainHandItem().is(Items.FISHING_ROD) ? InteractionHand.MAIN_HAND
            : mc.player.getOffhandItem().is(Items.FISHING_ROD) ? InteractionHand.OFF_HAND : null;
        if (hand == null) return;

        if (recastIn > 0) { recastIn--; return; }
        if (recastIn == 0) {
            recastIn = -1;
            if (mc.player.fishing == null) mc.gameMode.useItem(mc.player, hand);
            return;
        }
        var hook = mc.player.fishing;
        if (hook != null && ((FishingHookAccessor) hook).vortex$isBiting()) {
            mc.gameMode.useItem(mc.player, hand); // reel in
            recastIn = recastDelay.i();
        }
    }
}
