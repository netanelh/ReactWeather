package com.natan.vortex.module.world;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Inv;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

public class AutoShear extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Shearing range", 4.5, 2, 6, 0.5));

    public AutoShear() { super("AutoShear", "Shears every woolly sheep in reach", Category.WORLD); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        int slot = Inv.findHotbar(s -> s.is(Items.SHEARS));
        if (slot == -1) return;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof Sheep s) || s.isSheared() || s.isBaby() || s.distanceTo(mc.player) > range.d()) continue;
            Inv.select(slot);
            mc.gameMode.interact(mc.player, s, new EntityHitResult(s, s.getBoundingBox().getCenter()), InteractionHand.MAIN_HAND);
            mc.player.swing(InteractionHand.MAIN_HAND);
            return;
        }
    }
}
