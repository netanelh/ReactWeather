package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import com.natan.vortex.util.Inv;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;

/** Names every unnamed mob around you with the name tags in your hotbar (stops despawning). */
public class AutoNametag extends Module {
    private final BoolSetting hostiles = add(new BoolSetting("Hostiles", "Also name hostile mobs", false));
    private int timer;

    public AutoNametag() { super("AutoNametag", "Uses named name tags on nearby unnamed mobs", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame() || timer-- > 0) return;
        int slot = Inv.findHotbar(s -> s.is(Items.NAME_TAG) && s.has(DataComponents.CUSTOM_NAME));
        if (slot == -1) return;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le) || e instanceof Player || le.hasCustomName() || e.distanceToSqr(mc.player) > 16) continue;
            if (!hostiles.on() && com.natan.vortex.util.Targets.isHostile(e)) continue;
            Inv.select(slot);
            mc.gameMode.interact(mc.player, e, new EntityHitResult(e), InteractionHand.MAIN_HAND);
            timer = 5;
            return;
        }
    }
}
