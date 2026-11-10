package com.natan.vortex.module.world;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import com.natan.vortex.util.Inv;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.EntityHitResult;

import java.util.HashMap;
import java.util.Map;

public class AutoBreed extends Module {
    private final NumberSetting range = add(new NumberSetting("Range", "Feeding range", 4.5, 2, 6, 0.5));
    private final Map<Integer, Integer> fed = new HashMap<>();
    private int tick;

    public AutoBreed() { super("AutoBreed", "Feeds and breeds nearby animals with the right food", Category.WORLD); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        tick++;
        fed.values().removeIf(t -> tick - t > 6000); // breeding cooldown ~5 min
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof Animal a) || a.isBaby() || fed.containsKey(a.getId()) || a.distanceTo(mc.player) > range.d()) continue;
            int slot = Inv.findHotbar(a::isFood);
            if (slot == -1) continue;
            Inv.select(slot);
            mc.gameMode.interact(mc.player, a, new EntityHitResult(a, a.getBoundingBox().getCenter()), InteractionHand.MAIN_HAND);
            mc.player.swing(InteractionHand.MAIN_HAND);
            fed.put(a.getId(), tick);
            return;
        }
    }
}
