package com.natan.vortex.module.world;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Inv;
import com.natan.vortex.util.Place;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.LightLayer;

/** Places a torch at your feet when it gets dark enough for mobs to spawn. */
public class AutoTorch extends Module {
    private final NumberSetting light = add(new NumberSetting("Light Level", "Place a torch when block light is at or below this", 1, 0, 7, 1));
    private int cooldown;

    public AutoTorch() { super("AutoTorch", "Lights up dark areas as you walk (caves!)", Category.WORLD); }

    @Override
    public void onTick() {
        if (!inGame() || cooldown-- > 0 || !mc.player.onGround()) return;
        BlockPos feet = mc.player.blockPosition();
        if (mc.level.getBrightness(LightLayer.BLOCK, feet) > light.i()) return;
        if (!mc.level.getBlockState(feet).isAir() || mc.level.getBlockState(feet.below()).canBeReplaced()) return;
        int slot = Inv.findHotbar(s -> s.is(Items.TORCH) || s.is(Items.SOUL_TORCH));
        if (slot == -1) return;
        int prev = Inv.selected();
        if (Place.at(feet, slot, true)) cooldown = 10;
        Inv.select(prev);
    }
}
