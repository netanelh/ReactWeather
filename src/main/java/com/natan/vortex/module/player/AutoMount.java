package com.natan.vortex.module.player;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.BoolSetting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.EntityHitResult;

/** Gets back into the nearest boat / horse / minecart automatically. */
public class AutoMount extends Module {
    private final BoolSetting boats = add(new BoolSetting("Boats", "Boats and rafts", true));
    private final BoolSetting animals = add(new BoolSetting("Animals", "Horses, donkeys, camels, striders...", true));
    private final BoolSetting carts = add(new BoolSetting("Minecarts", "Minecarts", false));
    private int timer;
    private boolean wasRiding;
    private int leftCooldown;

    public AutoMount() { super("AutoMount", "Rides the nearest boat, horse or minecart", Category.PLAYER); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        if (mc.player.isPassenger()) { wasRiding = true; return; }
        if (wasRiding) { wasRiding = false; leftCooldown = 100; } // just got off: leave it alone for 5s
        if (leftCooldown > 0) { leftCooldown--; return; }
        if (mc.options.keyShift.isDown() || timer-- > 0) return;
        timer = 10;
        Entity best = null; double bd = 16;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e == mc.player || !e.getPassengers().isEmpty()) continue;
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
            boolean ok = (boats.on() && (id.contains("boat") || id.contains("raft")))
                || (carts.on() && id.equals("minecart"))
                || (animals.on() && (id.equals("horse") || id.equals("donkey") || id.equals("mule") || id.equals("camel") || id.equals("strider") || id.equals("skeleton_horse") || id.equals("zombie_horse") || id.equals("pig")));
            if (!ok) continue;
            double d = e.distanceToSqr(mc.player);
            if (d < bd) { bd = d; best = e; }
        }
        if (best != null) mc.gameMode.interact(mc.player, best, new EntityHitResult(best), InteractionHand.MAIN_HAND);
    }

    @Override
    public void onDisable() { wasRiding = false; leftCooldown = 0; }
}
