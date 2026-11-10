package com.natan.vortex.module.combat;

import com.natan.vortex.module.Category;
import com.natan.vortex.module.Module;
import com.natan.vortex.setting.NumberSetting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Sidesteps arrows and tridents that are about to hit you. */
public class ArrowDodge extends Module {
    private final NumberSetting strength = add(new NumberSetting("Strength", "Sidestep speed", 0.6, 0.2, 1.5, 0.1));

    public ArrowDodge() { super("ArrowDodge", "Automatically sidesteps incoming arrows", Category.COMBAT); }

    @Override
    public void onTick() {
        if (!inGame()) return;
        Vec3 me = mc.player.getBoundingBox().getCenter();
        for (Entity e : mc.level.entitiesForRendering()) {
            String id = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath();
            if (!(id.contains("arrow") || id.equals("trident"))) continue;
            Vec3 v = e.getDeltaMovement();
            if (v.lengthSqr() < 0.25 || e.distanceToSqr(mc.player) > 40 * 40) continue;
            Vec3 rel = me.subtract(e.position());
            double t = rel.dot(v) / v.lengthSqr();
            if (t < 0 || t > 25) continue;
            Vec3 closest = e.position().add(v.scale(t));
            if (closest.distanceTo(me) > 1.6) continue;
            Vec3 side = new Vec3(-v.z, 0, v.x).normalize();
            if (side.dot(new Vec3(rel.x, 0, rel.z)) < 0) side = side.scale(-1);
            mc.player.setDeltaMovement(mc.player.getDeltaMovement().add(side.scale(strength.d())));
            return;
        }
    }
}
